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

package org.hkijena.jipipe.api.instrumentation;

import org.hkijena.jipipe.api.progress.JIPipeLogLevel;
import org.hkijena.jipipe.api.progress.JIPipeProgressLog;
import org.junit.jupiter.api.Test;

import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

/**
 * Pure tests for the batch → {@code job_progress} message mapping of the instrumentation run engine.
 * The mapping is the contract consumed by the instrumentation API: a batch is forwarded as ONE
 * progress update whose message is the last rendered line of the batch.
 */
public class InstrumentationProgressMappingTest {

    @Test
    void batchWithRenderedLinesMapsToTheLastLine() {
        JIPipeProgressLog store = JIPipeProgressLog.silent();
        // Records on the root node: line 1 is the head record, the log records are lines 2 and 3.
        // A log record renders as "[v/mx] message" (root nodes have no chain).
        store.log(store.getRootNodeId(), JIPipeLogLevel.INFO, "a");
        store.log(store.getRootNodeId(), JIPipeLogLevel.INFO, "b");
        JIPipeProgressLog.ProgressBatchEvent batch = new JIPipeProgressLog.ProgressBatchEvent(
                store, 2, 3, Set.of(store.getRootNodeId()), false);
        assertEquals("[1/1] b", InstrumentationRunEngine.batchProgressMessage(store, batch),
                "the message is the last rendered line of the batch");
    }

    @Test
    void emptyFinalBatchMapsToTheDocumentedFinishedMessage() {
        JIPipeProgressLog store = JIPipeProgressLog.silent();
        // Empty-final-batch convention: fromLine > toLine = run complete, no new records
        JIPipeProgressLog.ProgressBatchEvent batch = new JIPipeProgressLog.ProgressBatchEvent(
                store, 4, 3, Set.of(), true);
        assertEquals("Done", InstrumentationRunEngine.batchProgressMessage(store, batch),
                "the empty final batch carries the documented terminal message");
    }

    @Test
    void nonFinalBatchWithoutRenderableRecordsMapsToNoUpdate() {
        JIPipeProgressLog store = JIPipeProgressLog.silent();
        // A progress record (line 2) renders nothing: no message can be extracted
        store.progress(store.getRootNodeId(), 1, 10);
        JIPipeProgressLog.ProgressBatchEvent batch = new JIPipeProgressLog.ProgressBatchEvent(
                store, 2, 2, Set.of(store.getRootNodeId()), false);
        assertNull(InstrumentationRunEngine.batchProgressMessage(store, batch),
                "a non-final batch without rendered lines produces no progress update");
    }

    @Test
    void finalBatchWithRenderedLinesStillMapsToTheLastLine() {
        JIPipeProgressLog store = JIPipeProgressLog.silent();
        store.log(store.getRootNodeId(), JIPipeLogLevel.INFO, "a");
        store.log(store.getRootNodeId(), JIPipeLogLevel.INFO, "b");
        JIPipeProgressLog.ProgressBatchEvent batch = new JIPipeProgressLog.ProgressBatchEvent(
                store, 2, 3, Set.of(store.getRootNodeId()), true);
        assertEquals("[1/1] b", InstrumentationRunEngine.batchProgressMessage(store, batch),
                "a final batch with records still reports its last rendered line (the terminal 'Done' message is for empty final batches only)");
    }
}
