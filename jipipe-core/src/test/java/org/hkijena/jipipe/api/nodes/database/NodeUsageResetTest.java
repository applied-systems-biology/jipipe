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

package org.hkijena.jipipe.api.nodes.database;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Pins {@link NodeUsage#resetUserCounts()}, the settings-page reset target of
 * the "Node search" sheet (#1409, merged-ranking MR D): a reset clears the
 * recorded personal counts (and persists the empty map through the existing
 * persister seam) while the shipped community prior stays active.
 * <p>
 * Both the prior supplier and the persister are injected, so no test touches
 * the shipped resource or the real settings registry.
 */
class NodeUsageResetTest {

    @Test
    void resetClearsRecordedCountsAndKeepsThePrior() {
        List<Map<String, Integer>> snapshots = new ArrayList<>();
        try {
            NodeUsage.setPriorSupplier(() -> Map.of("origin-a", 10, "origin-b", 5));
            NodeUsage.setUserCountsPersister(snapshots::add);

            NodeUsage.record("origin-a");
            NodeUsage.record("origin-a");
            NodeUsage.record("origin-b");
            assertEquals(12, NodeUsage.usage("origin-a"));
            assertEquals(6, NodeUsage.usage("origin-b"));

            int snapshotsBefore = snapshots.size();
            NodeUsage.resetUserCounts();

            // The recorded part is gone, the shipped prior survives.
            assertEquals(10, NodeUsage.usage("origin-a"));
            assertEquals(5, NodeUsage.usage("origin-b"));
            // An id that only had a recorded count is inert after the reset.
            NodeUsage.record("user-only");
            assertEquals(1, NodeUsage.usage("user-only"));

            // The reset persisted the empty map through the injected persister
            // (the injectable seam is honoured, not bypassed).
            assertTrue(snapshots.size() > snapshotsBefore, "the reset must persist through the persister seam");
            Map<String, Integer> atReset = snapshots.get(snapshotsBefore);
            assertTrue(atReset.isEmpty(), "the reset persists an empty counts map");
        } finally {
            NodeUsage.resetForTests();
        }
    }

    @Test
    void resetPersistsEmptyMapWithoutPriorCounts() {
        List<Map<String, Integer>> snapshots = new ArrayList<>();
        try {
            NodeUsage.setPriorSupplier(() -> Map.of("origin-a", 10));
            NodeUsage.setUserCountsPersister(snapshots::add);
            NodeUsage.record("origin-a");

            NodeUsage.resetUserCounts();

            assertEquals(10, NodeUsage.usage("origin-a"), "the prior is not part of the recorded counts");
            assertFalse(snapshots.isEmpty());
            assertEquals(Map.of(), snapshots.get(snapshots.size() - 1));
        } finally {
            NodeUsage.resetForTests();
        }
    }
}
