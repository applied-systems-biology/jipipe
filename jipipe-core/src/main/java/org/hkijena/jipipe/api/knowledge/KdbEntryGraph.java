/*
 * Copyright by Zoltán Cseresnyés, Ruman Gerst
 *
 * Research Group Applied Systems Biology - Head: Prof. Dr. Marc Thilo Figge
 * https://www.leibniz-hki.de/en/applied-systems-biology.html
 * HKI-Center for Systems Biology of Infection
 * Leibniz Institute for Natural Product Research and Infection Biology - Hans Knöll Institute (HKI)
 * Adolf-Reichwein-Straße 23, 07745 Jena, Germany
 *
 * The project code is licensed under MIT.
 * See the LICENSE file provided with the code for the full license.
 */

package org.hkijena.jipipe.api.knowledge;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

/**
 * The relation lane: the one-hop ENTRY_REF graph over the loaded corpus
 * (design spec 5.4).
 * <p>
 * Edges are derived from the ENTRY_REF slot values of the entries. The graph
 * is undirected in traversal but directed in evidence: {@link
 * RelatedEntry.Direction#OUTGOING} marks a relation the queried entry itself
 * declares, {@link RelatedEntry.Direction#INCOMING} marks a relation a
 * neighbor declares toward it, labeled with the inverse relation. Where both
 * directions exist between the same pair of entries, the entry's own
 * declaration wins and the neighbor appears once.
 * <p>
 * Traversal is one hop maximum and cycle-guarded by a visited set seeded
 * with the queried entry key. Self references are absorbed by that set;
 * ENTRY_REF values that resolve to no corpus entry are skipped (the
 * integrity gate reports them as findings, the graph only refuses to
 * traverse them). The graph is immutable once built.
 */
public final class KdbEntryGraph {

    /**
     * The inverse relation labels, keyed by the declared ENTRY_REF slot
     * name. Both the slot names and the inverse labels are stable string
     * literals: they surface in rendered entries and in MCP results, so they
     * must never be derived from code. The closed kind set declares every
     * slot listed here; the fallback covers future slots.
     */
    private static final Map<String, String> INVERSE_RELATIONS = Map.of(
            "elaborates", "elaborated-by",
            "prerequisites", "prerequisite-for",
            "options", "option-of",
            "relatedConcepts", "relatedConcepts",
            "crossRefs", "crossRefs");

    /**
     * The inverse label for a relation without an entry in the map: the
     * neighbor does reference the queried entry, just through a slot the map
     * does not know.
     */
    private static final String INVERSE_RELATION_FALLBACK = "referenced-by";

    private final Map<String, List<RelatedEntry>> neighborsByKey;

    /**
     * Builds the graph over the given entries. The entries are not modified
     * and not retained: only the derived neighbor lists are kept.
     *
     * @param entries the loaded corpus entries
     */
    public KdbEntryGraph(List<KdbEntry> entries) {
        Map<String, KdbEntry> entriesByKey = new LinkedHashMap<>();
        for (KdbEntry entry : entries) {
            entriesByKey.put(entry.getEntryKey(), entry);
        }
        Map<String, Map<String, String>> outgoing = new LinkedHashMap<>();
        Map<String, Map<String, String>> incoming = new LinkedHashMap<>();
        for (KdbEntry entry : entries) {
            for (Map.Entry<String, List<Object>> slot : entry.getSlotValues().entrySet()) {
                Optional<KdbSlotDef> slotDef = entry.getKind().getSlot(slot.getKey());
                if (!slotDef.isPresent() || slotDef.get().type() != KdbValueType.ENTRY_REF) {
                    continue;
                }
                for (Object value : slot.getValue()) {
                    if (!(value instanceof String target)) {
                        continue;
                    }
                    if (!entriesByKey.containsKey(target)) {
                        // Dangling reference: the integrity gate reports it,
                        // the graph cannot traverse it.
                        continue;
                    }
                    outgoing.computeIfAbsent(entry.getEntryKey(), key -> new LinkedHashMap<>())
                            .putIfAbsent(target, slot.getKey());
                    incoming.computeIfAbsent(target, key -> new LinkedHashMap<>())
                            .putIfAbsent(entry.getEntryKey(), slot.getKey());
                }
            }
        }
        Map<String, List<RelatedEntry>> neighborsByKey = new HashMap<>();
        for (String entryKey : entriesByKey.keySet()) {
            neighborsByKey.put(entryKey, neighbors(entryKey, outgoing, incoming));
        }
        this.neighborsByKey = neighborsByKey;
    }

    /**
     * Collects the one-hop neighbors of one entry, cycle-guarded by a
     * visited set seeded with the entry itself: outgoing edges first (the
     * entry's own declarations), then incoming edges (inverse labels), with
     * the visited set collapsing duplicates so every neighbor appears once.
     */
    private static List<RelatedEntry> neighbors(String entryKey,
                                                Map<String, Map<String, String>> outgoing,
                                                Map<String, Map<String, String>> incoming) {
        Set<String> visited = new HashSet<>();
        visited.add(entryKey);
        List<RelatedEntry> neighbors = new ArrayList<>();
        for (Map.Entry<String, String> edge : outgoing.getOrDefault(entryKey, Map.of()).entrySet()) {
            if (visited.add(edge.getKey())) {
                neighbors.add(new RelatedEntry(edge.getKey(), edge.getValue(), RelatedEntry.Direction.OUTGOING));
            }
        }
        for (Map.Entry<String, String> edge : incoming.getOrDefault(entryKey, Map.of()).entrySet()) {
            if (visited.add(edge.getKey())) {
                neighbors.add(new RelatedEntry(edge.getKey(), inverseRelation(edge.getValue()),
                        RelatedEntry.Direction.INCOMING));
            }
        }
        neighbors.sort(Comparator.comparing(RelatedEntry::entryKey));
        return List.copyOf(neighbors);
    }

    private static String inverseRelation(String relation) {
        return INVERSE_RELATIONS.getOrDefault(relation, INVERSE_RELATION_FALLBACK);
    }

    /**
     * The one-hop, labeled neighbors of an entry, ordered by entry key.
     *
     * @param entryKey the entry key of the queried entry
     * @return the neighbors; empty for an unknown entry key and for an entry
     * without ENTRY_REF edges
     */
    public List<RelatedEntry> neighbors(String entryKey) {
        return neighborsByKey.getOrDefault(entryKey, List.of());
    }
}
