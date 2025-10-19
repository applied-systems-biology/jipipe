package org.hkijena.jipipe.api.nodes.iterationstep.solvers;

import org.hkijena.jipipe.api.JIPipeProgressInfo;
import org.hkijena.jipipe.api.annotation.JIPipeTextAnnotation;
import org.hkijena.jipipe.api.data.JIPipeDataSlot;
import org.hkijena.jipipe.api.nodes.iterationstep.IterationStepSolver;
import org.hkijena.jipipe.api.nodes.iterationstep.JIPipeMultiIterationStep;
import org.hkijena.jipipe.api.nodes.iterationstep.JIPipeMultiIterationStepGenerator;

import java.util.*;

/**
 * Composite (N-key) dictionary solver for ExactMatch annotationMatchingMethod.
 * - Supports 0/1/N reference columns with exact matching
 * - Supports "distribution" via all-empty key semantics
 * - Supports forceNAIsAny: empty components fan out to all observed values at that position
 * - Supports applyMerging on/off
 */
public class CompositeDictionarySolver implements IterationStepSolver {

    @Override
    public List<JIPipeMultiIterationStep> solve(JIPipeMultiIterationStepGenerator g, JIPipeProgressInfo progress) {
        final List<JIPipeDataSlot> slots = g.getSlots();
        if (slots.isEmpty()) {
            return new ArrayList<>();
        }

        // Degenerate special cases handled at selection time by the caller (MergeAll/SplitAll)

        final List<String> keyCols = new ArrayList<>(g.getReferenceColumns());
        Collections.sort(keyCols); // deterministic order

        final boolean forceNAIsAny = g.isForceNAIsAny();
        final boolean merging = g.isApplyMerging();

        // For forceNAIsAny expansion: distinct values per key position
        final List<Set<String>> perPosValues = new ArrayList<>();
        for (int i = 0; i < keyCols.size(); i++) {
            perPosValues.add(new HashSet<>());
        }

        // Per-slot Key -> row indices
        final Map<JIPipeDataSlot, Map<Key, List<Integer>>> perSlot = new HashMap<>();
        final Set<Key> globalConcreteKeys = new HashSet<>();
        boolean anyAllEmptyKey = false;

        // Build per-slot maps
        for (JIPipeDataSlot slot : slots) {
            final Map<Key, List<Integer>> map = new HashMap<>();
            perSlot.put(slot, map);

            final int rowCount = slot.getRowCount();
            for (int r = 0; r < rowCount; r++) {
                if ((r & 0x3FFF) == 0 && progress.isCancelled()) {
                    return null;
                }
                final String[] tuple = new String[keyCols.size()];
                boolean allEmpty = true;
                for (int i = 0; i < keyCols.size(); i++) {
                    final String col = keyCols.get(i);
                    final JIPipeTextAnnotation a = slot.getTextAnnotationOr(r, col, null);
                    final String v = a != null ? a.getValue() : "";
                    tuple[i] = v != null ? v : "";
                    if (!tuple[i].isEmpty()) {
                        allEmpty = false;
                        perPosValues.get(i).add(tuple[i]);
                    }
                }
                final Key k = new Key(tuple);
                map.computeIfAbsent(k, _k -> new ArrayList<>()).add(r);
                if (!allEmpty || keyCols.isEmpty()) {
                    globalConcreteKeys.add(k);
                } else {
                    anyAllEmptyKey = true;
                }
            }
        }

        if (keyCols.isEmpty()) {
            // 0-key (degenerate): one global key
            return emitFromKeys(g, progress, perSlot, Collections.singleton(new Key(new String[0])), merging);
        }

        final Set<Key> finalKeys = new HashSet<>(globalConcreteKeys);

        // forceNAIsAny expansion
        if (forceNAIsAny) {
            for (JIPipeDataSlot slot : slots) {
                final Map<Key, List<Integer>> map = perSlot.get(slot);
                if (map == null || map.isEmpty()) {
                    continue;
                }
                final Map<Key, List<Integer>> additions = new HashMap<>();
                for (Map.Entry<Key, List<Integer>> e : map.entrySet()) {
                    final Key k = e.getKey();
                    if (!k.hasEmpty()) {
                        continue;
                    }

                    List<String[]> fills = new ArrayList<>();
                    fills.add(k.values.clone());
                    for (int i = 0; i < k.values.length; i++) {
                        if (k.values[i].isEmpty()) {
                            final List<String[]> next = new ArrayList<>();
                            final Set<String> choices = perPosValues.get(i);
                            if (choices.isEmpty()) {
                                next.addAll(fills);
                            } else {
                                for (String choice : choices) {
                                    for (String[] base : fills) {
                                        final String[] clone = base.clone();
                                        clone[i] = choice;
                                        next.add(clone);
                                    }
                                }
                            }
                            fills = next;
                        }
                    }
                    for (String[] fill : fills) {
                        final Key fk = new Key(fill);
                        additions.computeIfAbsent(fk, _k -> new ArrayList<>()).addAll(e.getValue());
                        finalKeys.add(fk);
                    }
                }
                // merge expansions
                for (Map.Entry<Key, List<Integer>> add : additions.entrySet()) {
                    map.computeIfAbsent(add.getKey(), _k -> new ArrayList<>()).addAll(add.getValue());
                }
            }
        }

        // Distribution (multi-input): replicate all-empty key rows to all concrete keys
        if (slots.size() > 1 && anyAllEmptyKey && !finalKeys.isEmpty()) {
            for (JIPipeDataSlot slot : slots) {
                final Map<Key, List<Integer>> map = perSlot.get(slot);
                if (map == null || map.isEmpty()) {
                    continue;
                }
                final Key allEmpty = Key.allEmpty(keyCols.size());
                final List<Integer> wildcard = map.get(allEmpty);
                if (wildcard == null || wildcard.isEmpty()) {
                    continue;
                }
                for (Key fk : finalKeys) {
                    map.computeIfAbsent(fk, _k -> new ArrayList<>()).addAll(wildcard);
                }
                map.remove(allEmpty);
            }
        }

        return emitFromKeys(g, progress, perSlot, finalKeys, merging);
    }

    /* emission */

    private List<JIPipeMultiIterationStep> emitFromKeys(JIPipeMultiIterationStepGenerator g,
                                                        JIPipeProgressInfo progress,
                                                        Map<JIPipeDataSlot, Map<Key, List<Integer>>> perSlot,
                                                        Set<Key> keys,
                                                        boolean merging) {
        final List<JIPipeDataSlot> slots = g.getSlots();
        final List<JIPipeMultiIterationStep> out = new ArrayList<>();

        if (merging) {
            for (Key key : keys) {
                if (progress.isCancelled()) {
                    return null;
                }
                final JIPipeMultiIterationStep step = new JIPipeMultiIterationStep(g.getNode());
                for (JIPipeDataSlot slot : slots) {
                    final List<Integer> rows = perSlot.getOrDefault(slot, Map.of()).getOrDefault(key, List.of());
                    step.addInputData(slot, new HashSet<>(rows));
                    for (Integer r : rows) {
                        step.addMergedTextAnnotations(slot.getTextAnnotations(r), g.getAnnotationMergeStrategy());
                        step.addMergedDataAnnotations(slot.getDataAnnotations(r), g.getDataAnnotationMergeStrategy());
                    }
                }
                for (JIPipeDataSlot s : slots) {
                    step.addEmptySlot(s);
                }
                out.add(step);
            }
            return out;
        }

        // applyMerging == false -> cartesian products per key
        for (Key key : keys) {
            if (progress.isCancelled()) {
                return null;
            }
            List<JIPipeMultiIterationStep> partial = new ArrayList<>();
            partial.add(new JIPipeMultiIterationStep(g.getNode()));

            for (JIPipeDataSlot slot : slots) {
                final List<Integer> rows = perSlot.getOrDefault(slot, Map.of()).getOrDefault(key, List.of());
                if (!rows.isEmpty()) {
                    final List<JIPipeMultiIterationStep> next = new ArrayList<>(partial.size() * Math.max(1, rows.size()));
                    for (JIPipeMultiIterationStep base : partial) {
                        for (Integer r : rows) {
                            final JIPipeMultiIterationStep copy = new JIPipeMultiIterationStep(base);
                            copy.addInputData(slot, r);
                            copy.addMergedTextAnnotations(slot.getTextAnnotations(r), g.getAnnotationMergeStrategy());
                            copy.addMergedDataAnnotations(slot.getDataAnnotations(r), g.getDataAnnotationMergeStrategy());
                            next.add(copy);
                        }
                    }
                    partial = next;
                } else {
                    for (JIPipeMultiIterationStep base : partial) {
                        base.addEmptySlot(slot);
                    }
                }
            }
            for (JIPipeMultiIterationStep step : partial) {
                for (JIPipeDataSlot s : slots) {
                    step.addEmptySlot(s);
                }
            }
            out.addAll(partial);
        }
        return out;
    }

    /* key */

    private static final class Key {
        final String[] values;

        Key(String[] values) {
            this.values = values;
        }

        static Key allEmpty(int n) {
            String[] v = new String[n];
            Arrays.fill(v, "");
            return new Key(v);
        }

        boolean hasEmpty() {
            for (String v : values) {
                if (v.isEmpty()) {
                    return true;
                }
            }
            return false;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) {
                return true;
            }
            if (!(o instanceof Key)) {
                return false;
            }
            return Arrays.equals(values, ((Key) o).values);
        }

        @Override
        public int hashCode() {
            return Arrays.hashCode(values);
        }

        @Override
        public String toString() {
            return Arrays.toString(values);
        }
    }
}
