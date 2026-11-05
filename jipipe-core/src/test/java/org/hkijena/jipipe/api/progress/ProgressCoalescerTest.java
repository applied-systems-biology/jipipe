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

package org.hkijena.jipipe.api.progress;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Tests for the batch notification coalescer of file-backed progress stores: at most one
 * {@link JIPipeProgressLog.ProgressBatchEvent} per coalesce interval plus a guaranteed final
 * batch, contiguous line coverage (no gaps), and delivery while the run is idle.
 * <p>
 * All emissions happen on the writer thread; {@code finish()}/{@code close()} join that thread,
 * so the collected event lists are complete when these calls return.
 */
class ProgressCoalescerTest {

    @TempDir
    Path tmp;

    private static List<JIPipeProgressLog.ProgressBatchEvent> collect(JIPipeProgressLog store) {
        List<JIPipeProgressLog.ProgressBatchEvent> events = new ArrayList<>();
        store.getBatchEventEmitter().subscribeLambda((emitter, event) -> events.add(event));
        return events;
    }

    /**
     * Asserts that the batches partition the lines 1..lastLine contiguously: they start at line 1,
     * continue where the previous batch ended, and reach the last line; exactly one batch is the
     * final one; the only possibly-empty batch (fromLine &gt; toLine) is the final one.
     */
    private static void assertCoveredWithoutGaps(List<JIPipeProgressLog.ProgressBatchEvent> events, int lastLine) {
        assertFalse(events.isEmpty(), "the final batch must always be emitted");
        long finalCount = events.stream().filter(JIPipeProgressLog.ProgressBatchEvent::finalBatch).count();
        assertEquals(1, finalCount, "exactly one final batch must be emitted");
        int expectedNext = 1;
        for (JIPipeProgressLog.ProgressBatchEvent event : events) {
            if (event.fromLine() > event.toLine()) {
                assertTrue(event.finalBatch(), "only the final batch may carry no records");
                continue;
            }
            assertEquals(expectedNext, event.fromLine(), "batches must continue where the previous ended");
            expectedNext = event.toLine() + 1;
        }
        assertEquals(lastLine + 1, expectedNext, "batches must cover every record with no gaps");
        assertEquals(lastLine, events.get(events.size() - 1).toLine(), "the final batch must reach the last line");
    }

    @Test
    void burstProducesBoundedNotifications() {
        Path file = tmp.resolve("burst.ndjson");
        JIPipeProgressLog store = JIPipeProgressLog.create(file, "burst", "Burst run");
        List<JIPipeProgressLog.ProgressBatchEvent> seen = collect(store);
        try {
            store.setCoalesceIntervalMillis(50);
            int node = store.beginNode(store.getRootNodeId(), "Burst node", JIPipeProgressNodeKind.ITEM);
            // head + n = lines 1-2; the burst covers 3..200002; the s record is line 200003.
            // 200k instead of the brief-sketch's 100k: the burst duration is writer-bound, and only
            // a drain longer than the interval guarantees an intermediate batch (more than one event)
            // R11-corrected window: measured from before the burst loop (the enqueue phase emits
            // batches too — they must be inside the bound) to after finish() returns; finish()
            // joins the writer, so the final batch is emitted before it returns
            long start = System.nanoTime();
            for (int i = 0; i < 200_000; i++) {
                store.log(node, JIPipeLogLevel.DEBUG, "x");
            }
            store.finish(JIPipeProgressNodeState.OK);
            long durationMs = (System.nanoTime() - start) / 1_000_000;
            int expectedMax = 1 + (int) Math.ceil(durationMs / 50.0) + 1;
            assertTrue(seen.size() <= expectedMax,
                    "burst of 200k must not produce 200k events; saw " + seen.size() + " in " + durationMs + " ms (max " + expectedMax + ")");
            assertTrue(seen.size() > 1, "a long-enough burst must produce more than one batch; saw " + seen.size());
            assertCoveredWithoutGaps(seen, 200_003);
        } finally {
            store.close();
        }
    }

    @Test
    void finalBatchAlwaysDelivered() {
        Path file = tmp.resolve("final.ndjson");
        JIPipeProgressLog store = JIPipeProgressLog.create(file, "final", "Final run");
        List<JIPipeProgressLog.ProgressBatchEvent> seen = collect(store);
        try {
            int node = store.beginNode(store.getRootNodeId(), "Node A", JIPipeProgressNodeKind.ITEM);
            store.log(node, JIPipeLogLevel.INFO, "one");
            store.log(node, JIPipeLogLevel.INFO, "two");
            store.log(node, JIPipeLogLevel.INFO, "three");
            // head + n + 3 logs + s = 6 lines; finish() arrives deep inside the interval window
            store.finish(JIPipeProgressNodeState.OK);
            assertCoveredWithoutGaps(seen, 6);
            assertTrue(seen.get(seen.size() - 1).finalBatch(), "the last event is the final batch");
        } finally {
            store.close();
        }
    }

    @Test
    void idleRunStillDeliversBatches() {
        Path file = tmp.resolve("idle.ndjson");
        JIPipeProgressLog store = JIPipeProgressLog.create(file, "idle", "Idle run");
        List<JIPipeProgressLog.ProgressBatchEvent> seen = new ArrayList<>();
        CountDownLatch firstBatch = new CountDownLatch(1);
        store.getBatchEventEmitter().subscribeLambda((emitter, event) -> {
            seen.add(event);
            firstBatch.countDown();
        });
        try {
            store.setCoalesceIntervalMillis(50);
            int node = store.beginNode(store.getRootNodeId(), "Idle node", JIPipeProgressNodeKind.ITEM);
            store.log(node, JIPipeLogLevel.INFO, "single");
            // The writer idles after draining: the record must be emitted while the run is idle,
            // not held until the run finishes. A generous deadline keeps the test robust on a
            // cold JVM (the writer's first JSON serialization may warm up Jackson for ~200 ms).
            assertTrue(firstBatch.await(5, TimeUnit.SECONDS),
                    "the batch must be delivered while the run is idle, before finish()");
            assertEquals(1, seen.size(), "exactly one batch while the run is idle");
            assertFalse(seen.get(0).finalBatch(), "the idle batch is not the final one");
            store.finish(JIPipeProgressNodeState.OK);
            assertCoveredWithoutGaps(seen, 4);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RuntimeException(e);
        } finally {
            store.close();
        }
    }

    @Test
    void finalBatchEmittedOnCloseWithoutFinish() {
        Path file = tmp.resolve("close.ndjson");
        JIPipeProgressLog store = JIPipeProgressLog.create(file, "close", "Close run");
        List<JIPipeProgressLog.ProgressBatchEvent> seen = collect(store);
        try {
            int node = store.beginNode(store.getRootNodeId(), "Node A", JIPipeProgressNodeKind.ITEM);
            store.log(node, JIPipeLogLevel.INFO, "one");
            store.log(node, JIPipeLogLevel.INFO, "two");
            // close() without finish(): no s record; head + n + 2 logs = 4 lines
            store.close();
            assertCoveredWithoutGaps(seen, 4);
            assertTrue(seen.get(seen.size() - 1).finalBatch(), "the close path also delivers the final batch");
            // No further events after close
            int after = seen.size();
            Thread.sleep(100);
            assertEquals(after, seen.size(), "no emissions after close");
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RuntimeException(e);
        }
    }

    @Test
    void noEventsWhenNoRecords() {
        Path file = tmp.resolve("empty.ndjson");
        JIPipeProgressLog store = JIPipeProgressLog.create(file, "empty", "Empty run");
        List<JIPipeProgressLog.ProgressBatchEvent> seen = collect(store);
        store.close();
        assertTrue(seen.isEmpty(), "a store that never recorded anything emits no batches");
        assertFalse(java.nio.file.Files.exists(file), "no file is created");
    }
}
