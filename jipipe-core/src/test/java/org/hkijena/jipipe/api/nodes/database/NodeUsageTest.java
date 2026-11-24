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

import org.hkijena.jipipe.utils.json.JsonUtils;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Verifies the {@link NodeUsage} service with INJECTED prior and
 * persister - no test touches the real settings registry (the
 * {@code NodeEntryKind} persistence-helper precedent): the production
 * persister writes the user's settings, which tests must not do.
 */
class NodeUsageTest {

    @AfterEach
    void reset() {
        NodeUsage.resetForTests();
    }

    @Test
    void usageSumsPriorAndUserCounts() {
        NodeUsage.setPriorSupplier(() -> Map.of("a", 10));
        NodeUsage.setUserCountsPersister(m -> {
        });
        NodeUsage.record("a");
        NodeUsage.record("a");
        NodeUsage.record("unknown-id");
        assertEquals(12, NodeUsage.usage("a"));
        assertEquals(1, NodeUsage.usage("unknown-id"));
        assertEquals(0, NodeUsage.usage(null));
        assertEquals(0, NodeUsage.usage("never-seen"));
    }

    @Test
    void concurrentRecordsDoNotLoseUpdates() throws Exception {
        NodeUsage.setUserCountsPersister(m -> {
        });
        Thread[] threads = new Thread[8];
        for (int t = 0; t < 8; t++) {
            threads[t] = new Thread(() -> {
                for (int i = 0; i < 100; i++) {
                    NodeUsage.record("x");
                }
            });
            threads[t].start();
        }
        for (Thread thread : threads) {
            thread.join();
        }
        assertEquals(800, NodeUsage.usage("x"));
    }

    @Test
    void persisterSeesUpdatedCounts() {
        List<Map<String, Integer>> snapshots = new ArrayList<>();
        NodeUsage.setUserCountsPersister(snapshots::add);
        NodeUsage.record("a");
        NodeUsage.record("b");
        assertFalse(snapshots.isEmpty());
        Map<String, Integer> last = snapshots.get(snapshots.size() - 1);
        assertEquals(1, last.get("a"));
        assertEquals(1, last.get("b"));
    }

    @Test
    void resetForTestsClearsEverything() {
        NodeUsage.setPriorSupplier(() -> Map.of("a", 10));
        List<Map<String, Integer>> snapshots = new ArrayList<>();
        NodeUsage.setUserCountsPersister(snapshots::add);
        NodeUsage.record("a");
        NodeUsage.resetForTests();
        // After the reset the injected prior/persister are gone; a usage
        // read must not throw and must not carry the recorded count.
        // (The production defaults re-arm lazily.)
        assertEquals(0, NodeUsage.usage("a"));
        int snapshotsBefore = snapshots.size();
        NodeUsage.record("a");
        assertEquals(snapshotsBefore, snapshots.size(), "the injected persister must be detached by reset");
    }

    @Test
    void corruptPriorJsonYieldsEmptyMap() {
        var garbage = JsonUtils.getObjectMapper().createObjectNode().put("counts", "not-an-object");
        assertTrue(NodeUsage.parsePriorJson(garbage).isEmpty());
    }

    @Test
    void parsePriorJsonReadsInstances() {
        var document = JsonUtils.getObjectMapper().createObjectNode();
        var counts = document.putObject("counts");
        counts.putObject("some-node").put("instances", 42).put("pipelines", 7);
        assertEquals(42, NodeUsage.parsePriorJson(document).get("some-node"));
    }

    @Test
    void corruptRegistryCountsAreDropped() {
        Map<String, Object> raw = Map.of("a", 5, "b", "not-a-number", "c", 2.0);
        Map<String, Integer> coerced = NodeUsage.coerceCounts(raw);
        assertEquals(5, coerced.get("a"));
        assertEquals(2, coerced.get("c"));
        assertFalse(coerced.containsKey("b"));
    }
}
