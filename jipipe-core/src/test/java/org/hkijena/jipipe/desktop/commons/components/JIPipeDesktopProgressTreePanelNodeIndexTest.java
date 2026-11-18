package org.hkijena.jipipe.desktop.commons.components;

import org.junit.jupiter.api.Test;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Tests the incremental node index fold of {@link JIPipeDesktopProgressTreePanel} (scaling
 * limit 1): the rebuild's child map, RUNNING set, parent map and dirty-parent set are folded
 * from record deltas - the contracts here are the ordering/generation rules that keep the fold
 * equivalent to the former full store walk. Pure data structures, no Swing, no store.
 */
class JIPipeDesktopProgressTreePanelNodeIndexTest {

    private static final int GEN = 7;

    private record Folded(Map<Integer, List<Integer>> childMap,
                          Set<Integer> running,
                          Map<Integer, Integer> parentOf,
                          Set<Integer> dirty) {
    }

    private static Folded fold(int generation, int[]... deltas) {
        Queue<int[]> queue = new ArrayDeque<>();
        for (int[] delta : deltas) {
            queue.add(delta);
        }
        Map<Integer, List<Integer>> childMap = new HashMap<>();
        Set<Integer> running = new HashSet<>();
        Map<Integer, Integer> parentOf = new HashMap<>();
        Set<Integer> dirty = new HashSet<>();
        JIPipeDesktopProgressTreePanel.foldNodeIndexDeltas(queue, generation, childMap, running, parentOf, dirty);
        return new Folded(childMap, running, parentOf, dirty);
    }

    /**
     * A creation delta adds the node to the parent's child list and the RUNNING set; a terminal
     * delta (-1) removes it from the RUNNING set without touching the child map (the row still
     * renders). Sort-key marking: the creation marks only the direct parent.
     */
    @Test
    void creationAndTerminalLifecycle() {
        Folded folded = fold(GEN,
                new int[]{GEN, 5, 1},
                new int[]{GEN, 6, 5},
                new int[]{GEN, 5, -1});
        assertEquals(List.of(5), folded.childMap().get(1));
        assertEquals(List.of(6), folded.childMap().get(5));
        assertEquals(Set.of(6), folded.running());
        assertEquals(1, folded.parentOf().get(5));
        assertEquals(5, folded.parentOf().get(6));
        // 5's creation marked parent 1, 6's creation marked parent 5; the terminal marks nothing
        assertEquals(Set.of(1, 5), folded.dirty());
    }

    /**
     * A mid-run attach accumulates the first batch (which can hold terminal records) BEFORE the
     * catch-up scan delivers the earlier creations: the terminal delta for a not-yet-created node
     * removes nothing, and the later creation/terminal pair converges to the same state as the
     * in-order fold.
     */
    @Test
    void outOfOrderAttachConverges() {
        Folded outOfOrder = fold(GEN,
                new int[]{GEN, 5, -1},
                new int[]{GEN, 5, 1},
                new int[]{GEN, 5, -1});
        Folded inOrder = fold(GEN,
                new int[]{GEN, 5, 1},
                new int[]{GEN, 5, -1});
        assertEquals(inOrder.childMap(), outOfOrder.childMap());
        assertEquals(inOrder.running(), outOfOrder.running());
        assertEquals(inOrder.parentOf(), outOfOrder.parentOf());
        assertTrue(outOfOrder.running().isEmpty());
    }

    /**
     * Deltas of a store that was switched away mid catch-up (stale generation) are dropped
     * whole: no child list, RUNNING entry, parent entry or dirty mark may leak into the new
     * store's index.
     */
    @Test
    void staleGenerationDeltasAreDropped() {
        Folded folded = fold(GEN + 1,
                new int[]{GEN, 5, 1},
                new int[]{GEN, 5, -2},
                new int[]{GEN + 1, 6, 1});
        assertTrue(folded.childMap().containsKey(1));
        assertEquals(List.of(6), folded.childMap().get(1));
        assertEquals(Set.of(6), folded.running());
        assertEquals(Set.of(1), folded.dirty());
    }

    /**
     * A work-start delta (-2) marks the whole parent chain of the started node: a descendant
     * starting work can change the subtree-activity start of every ancestor whose own start is
     * still unset, and each of those ancestors sits in its parent's child list.
     */
    @Test
    void workStartMarksAncestorChain() {
        Folded folded = fold(GEN,
                new int[]{GEN, 5, 1},
                new int[]{GEN, 7, 5},
                new int[]{GEN, 9, 7},
                new int[]{GEN, 9, -2});
        assertEquals(Set.of(7, 5, 1), folded.dirty());
    }

    /**
     * Repeated folds over the same structures (successive ticks) accumulate: a second batch
     * appends to the existing lists and adds its dirty marks on top; an empty delta queue is a
     * no-op.
     */
    @Test
    void successiveFoldsAccumulate() {
        Map<Integer, List<Integer>> childMap = new HashMap<>();
        Set<Integer> running = new HashSet<>();
        Map<Integer, Integer> parentOf = new HashMap<>();
        Set<Integer> dirty = new HashSet<>();
        Queue<int[]> queue = new ArrayDeque<>(List.of(new int[]{GEN, 5, 1}));
        JIPipeDesktopProgressTreePanel.foldNodeIndexDeltas(queue, GEN, childMap, running, parentOf, dirty);
        queue = new ArrayDeque<>(List.of(new int[]{GEN, 6, 1}, new int[]{GEN, 6, -1}));
        JIPipeDesktopProgressTreePanel.foldNodeIndexDeltas(queue, GEN, childMap, running, parentOf, dirty);
        assertEquals(new ArrayList<>(List.of(5, 6)), childMap.get(1));
        // node 6 was created and terminated across the two folds, node 5 still runs
        assertEquals(Set.of(5), running);
        assertEquals(Set.of(1), dirty);
    }
}
