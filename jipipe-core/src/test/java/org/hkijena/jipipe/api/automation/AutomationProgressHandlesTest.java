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

package org.hkijena.jipipe.api.automation;

import org.hkijena.jipipe.api.JIPipeProgressInfo;
import org.hkijena.jipipe.api.progress.JIPipeProgressLog;
import org.hkijena.jipipe.api.progress.JIPipeProgressNodeState;
import org.hkijena.jipipe.api.progress.JIPipeProgressRecord;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Tests the shared per-project progress chain of the automation layer: two contexts for the
 * same project key share one store, records written through one are readable via the store the
 * other sees, and a store adoption (what a run does to its context handle) moves the chain so
 * later contexts read the adopted store.
 */
public class AutomationProgressHandlesTest {

    private static boolean containsMessage(JIPipeProgressLog store, String message) {
        for (JIPipeProgressRecord record : store.readRecent(100)) {
            if (record instanceof JIPipeProgressRecord.Log log && message.equals(log.m())) {
                return true;
            }
        }
        return false;
    }

    @Test
    void contextsForTheSameProjectKeyShareOneStore() {
        AutomationProgressHandles handles = new AutomationProgressHandles();
        JIPipeProgressInfo first = handles.create("window-1");
        JIPipeProgressInfo second = handles.create("window-1");

        assertNotSame(first, second, "each context gets its own handle");
        assertSame(first.getStore(), second.getStore(), "both contexts share one store");

        first.log("shared record");
        assertTrue(containsMessage(second.getStore(), "shared record"),
                "records written through one context must be readable via the store the other sees");
    }

    @Test
    void adoptedRunStoreIsVisibleToLaterContexts(@TempDir Path tempDir) throws Exception {
        AutomationProgressHandles handles = new AutomationProgressHandles();
        JIPipeProgressInfo first = handles.create("window-1");

        // What a run does to its context handle: adopt a file-backed store
        // (JIPipeGraphRun.run -> setStorageBase) and finish it at the run's end.
        JIPipeProgressLog runStore = JIPipeProgressLog.create(
                tempDir.resolve("run-abc.ndjson"), "run-abc", "Pipeline run");
        first.setStore(runStore);
        first.log("run record");
        runStore.finish(JIPipeProgressNodeState.OK);

        JIPipeProgressInfo second = handles.create("window-1");
        assertSame(runStore, second.getStore(), "the chain must follow the adopted store");
        assertTrue(containsMessage(second.getStore(), "run record"),
                "a later context must read the records of the adopted run store");
    }

    @Test
    void differentProjectKeysGetIndependentChains() {
        AutomationProgressHandles handles = new AutomationProgressHandles();
        JIPipeProgressInfo first = handles.create("window-1");
        JIPipeProgressInfo second = handles.create("window-2");

        assertNotSame(first.getStore(), second.getStore(), "different keys must not share a store");
        first.log("only in one");
        assertFalse(containsMessage(second.getStore(), "only in one"),
                "records must not leak across project keys");
    }

    @Test
    void nullProjectKeyGetsIndependentPrivateHandles() {
        AutomationProgressHandles handles = new AutomationProgressHandles();
        JIPipeProgressInfo first = handles.create(null);
        JIPipeProgressInfo second = handles.create(null);

        assertNotSame(first, second);
        assertNotSame(first.getStore(), second.getStore(), "registry-global operations share nothing");
        assertNotNull(first.getStore());
    }

    @Test
    void cancellationStaysIsolatedPerHandle() {
        // The reason handles are fresh per call: a cancel_job cancels only its own handle, and
        // a cancelled handle must not poison later operations of the same project.
        AutomationProgressHandles handles = new AutomationProgressHandles();
        JIPipeProgressInfo first = handles.create("window-1");
        first.cancel("Cancelled by user");

        JIPipeProgressInfo second = handles.create("window-1");
        assertFalse(second.isCancelled(), "a later context must not inherit the cancellation");
        assertSame(first.getStore(), second.getStore(),
                "the store is still shared (the same chain), only the cancellation is isolated");
    }
}
