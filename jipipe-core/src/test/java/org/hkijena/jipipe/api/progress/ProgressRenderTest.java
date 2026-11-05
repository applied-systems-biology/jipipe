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

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Tests for the compatibility text renderer ({@link JIPipeProgressLog#renderText(JIPipeLogLevel,
 * java.util.function.Consumer)}) and the level-filtered read-back: the golden render of a known
 * node tree, the level-switch contract, notification records rendered from a past-run file,
 * page-boundary streaming, and the level-filter overload of {@code readRecords}.
 */
class ProgressRenderTest {

    @TempDir
    Path tmp;

    /**
     * Golden: records on the node path root → "Partition 0" (PARTITION) → "Node A" (ITEM), emitted
     * in the contract's order, render the exact known lines. The prefix of a line is the node's
     * running progress: the percentage compat line is an INFO log record ("50%") that the facade
     * emits before its progress record, so it renders with the progress as of before the report
     * ([5/100]); the error afterwards renders with the updated progress ([50/100]), and the
     * root-run line has no progress record and renders with the initial 1/1. Progress records
     * render nothing — they only update the running progress.
     */
    @Test
    void renderKnownTreeMatchesGolden() {
        Path file = tmp.resolve("golden.ndjson");
        // A fixed stack trace keeps the rendered stack lines deterministic
        RuntimeException boom = new RuntimeException("boom");
        boom.setStackTrace(new StackTraceElement[]{
                new StackTraceElement("org.example.Worker", "run", "Worker.java", 42),
                new StackTraceElement("org.example.Loop", "step", "Loop.java", 7)});
        try (JIPipeProgressLog store = JIPipeProgressLog.create(file, "run-golden", "Pipeline run")) {
            int partition = store.beginNode(store.getRootNodeId(), "Partition 0", JIPipeProgressNodeKind.PARTITION);
            int nodeA = store.beginNode(partition, "Node A", JIPipeProgressNodeKind.ITEM);
            store.progress(nodeA, 5, 100);
            store.log(nodeA, JIPipeLogLevel.INFO, "msg1");
            store.log(nodeA, JIPipeLogLevel.WARN, "[WARNING] clipped");
            store.log(nodeA, JIPipeLogLevel.DEBUG, "debug detail");
            // Percentage report of the facade: the compat line is an INFO log record emitted
            // before the progress record (R15) — the progress record itself renders nothing
            store.log(nodeA, JIPipeLogLevel.INFO, "50%");
            store.progress(nodeA, 50, 100);
            store.error(nodeA, boom);
            store.nodeFinished(nodeA, JIPipeProgressNodeState.OK);
            int preprocessing = store.beginNode(store.getRootNodeId(), "Preprocessing/Tunnels", JIPipeProgressNodeKind.PHASE);
            store.log(preprocessing, JIPipeLogLevel.INFO, "Dissolving tunnels …");
            store.finish(JIPipeProgressNodeState.OK);

            List<String> lines = new ArrayList<>();
            store.renderText(JIPipeLogLevel.INFO, lines::add);
            assertEquals(List.of(
                    "[5/100] Partition 0 | Node A | msg1",
                    "[5/100] Partition 0 | Node A | [WARNING] clipped",
                    "[5/100] Partition 0 | Node A | 50%",
                    "[50/100] Partition 0 | Node A | java.lang.RuntimeException: boom",
                    "  at org.example.Worker.run(Worker.java:42)",
                    "  at org.example.Loop.step(Loop.java:7)",
                    "[1/1] Preprocessing/Tunnels | Dissolving tunnels …"), lines);

            // Level-switch contract: at DEBUG the previously hidden record renders at its position
            List<String> debugLines = new ArrayList<>();
            store.renderText(JIPipeLogLevel.DEBUG, debugLines::add);
            assertEquals(List.of(
                    "[5/100] Partition 0 | Node A | msg1",
                    "[5/100] Partition 0 | Node A | [WARNING] clipped",
                    "[5/100] Partition 0 | Node A | debug detail",
                    "[5/100] Partition 0 | Node A | 50%",
                    "[50/100] Partition 0 | Node A | java.lang.RuntimeException: boom",
                    "  at org.example.Worker.run(Worker.java:42)",
                    "  at org.example.Loop.step(Loop.java:7)",
                    "[1/1] Preprocessing/Tunnels | Dissolving tunnels …"), debugLines);
        }
    }

    /**
     * A notification record renders as today's notification line; head, node, node-finished, and
     * run-status records render nothing. The file here stands in for a past run: the store was
     * never used to write it, so the renderer must build the node paths from the file's own
     * {@code n} records.
     */
    @Test
    void notificationRecordRendersFromPastRunFile() throws Exception {
        Path file = tmp.resolve("past-run.ndjson");
        Files.writeString(file, String.join("\n",
                "{\"t\":\"head\",\"v\":1,\"run\":\"past\",\"label\":\"Pipeline run\",\"app\":\"6.0.0\",\"ts\":1}",
                "{\"t\":\"n\",\"id\":2,\"p\":0,\"l\":\"Partition 0\",\"k\":\"PARTITION\",\"ts\":2}",
                "{\"t\":\"n\",\"id\":3,\"p\":2,\"l\":\"Node A\",\"k\":\"ITEM\",\"ts\":3}",
                "{\"t\":\"p\",\"id\":3,\"v\":5,\"mx\":100,\"ts\":4}",
                "{\"t\":\"l\",\"id\":3,\"lv\":20,\"m\":\"msg1\",\"ts\":5}",
                "{\"t\":\"w\",\"h\":\"Heading\",\"d\":\"Description\",\"ts\":6}",
                "{\"t\":\"N\",\"id\":3,\"s\":\"ok\",\"ts\":7}",
                "{\"t\":\"s\",\"s\":\"finished\",\"ts\":8}",
                "{\"t\":\"l\",\"id\":3,\"lv\":10,\"m\":\"hidden debug\",\"ts\":9}",
                ""), StandardCharsets.UTF_8);
        try (JIPipeProgressLog store = JIPipeProgressLog.create(file, "reader", "Reader")) {
            List<String> lines = new ArrayList<>();
            store.renderText(JIPipeLogLevel.INFO, lines::add);
            assertEquals(List.of(
                    "[5/100] Partition 0 | Node A | msg1",
                    "@ Heading: Description"), lines);
        }
    }

    /**
     * The level-filter overload of {@code readRecords} keeps log records at or above the minimum
     * level and all records without a level, on file-backed and on non-persistent stores.
     */
    @Test
    void levelFilteredReadBackSkipsLowerLevelLogs() {
        Path file = tmp.resolve("filter.ndjson");
        try (JIPipeProgressLog store = JIPipeProgressLog.create(file, "run-filter", "Pipeline run")) {
            int node = store.beginNode(store.getRootNodeId(), "Node A", JIPipeProgressNodeKind.ITEM);
            store.log(node, JIPipeLogLevel.DEBUG, "d1");
            store.log(node, JIPipeLogLevel.INFO, "i1");
            store.progress(node, 1, 2);
            store.log(node, JIPipeLogLevel.DEBUG, "d2");
            store.finish(JIPipeProgressNodeState.OK);
            List<JIPipeProgressRecord> infoRecords = store.readRecords(1, 100, JIPipeLogLevel.INFO);
            // head, n, l(i1), p, s — the DEBUG log records are dropped, all other records pass
            assertEquals(5, infoRecords.size());
            assertTrue(infoRecords.stream().noneMatch(record ->
                    record instanceof JIPipeProgressRecord.Log logRecord
                            && logRecord.lv() < JIPipeLogLevel.INFO.value()));
            assertEquals(7, store.readRecords(1, 100, JIPipeLogLevel.DEBUG).size());
        }
        JIPipeProgressLog silent = JIPipeProgressLog.silent();
        int silentNode = silent.beginNode(silent.getRootNodeId(), "Node A", JIPipeProgressNodeKind.ITEM);
        silent.log(silentNode, JIPipeLogLevel.DEBUG, "d1");
        silent.log(silentNode, JIPipeLogLevel.INFO, "i1");
        List<JIPipeProgressRecord> tailInfo = silent.readRecords(1, 100, JIPipeLogLevel.INFO);
        assertEquals(1, tailInfo.size());
        assertEquals("i1", ((JIPipeProgressRecord.Log) tailInfo.get(0)).m());
    }

    /**
     * The renderer streams the file in pages: node paths and running progress must carry across a
     * page boundary (12,000 records against the 10,000-record page size).
     */
    @Test
    void renderTextStreamsAcrossPageBoundaries() {
        Path file = tmp.resolve("pages.ndjson");
        try (JIPipeProgressLog store = JIPipeProgressLog.create(file, "run-pages", "Pipeline run")) {
            int partition = store.beginNode(store.getRootNodeId(), "Partition 0", JIPipeProgressNodeKind.PARTITION);
            int nodeA = store.beginNode(partition, "Node A", JIPipeProgressNodeKind.ITEM);
            store.progress(nodeA, 5, 100);
            for (int i = 0; i < 12_000; i++) {
                store.log(nodeA, JIPipeLogLevel.INFO, "item " + i);
            }
            store.finish(JIPipeProgressNodeState.OK);
            List<String> lines = new ArrayList<>();
            store.renderText(JIPipeLogLevel.INFO, lines::add);
            assertEquals(12_000, lines.size());
            assertEquals("[5/100] Partition 0 | Node A | item 0", lines.get(0));
            assertEquals("[5/100] Partition 0 | Node A | item 9999", lines.get(9_999));
            assertEquals("[5/100] Partition 0 | Node A | item 10000", lines.get(10_000));
            assertEquals("[5/100] Partition 0 | Node A | item 11999", lines.get(11_999));
        }
    }

    /**
     * Rendering never throws on an empty store, on a store whose file was never written, or after
     * the run finished.
     */
    @Test
    void renderTextNeverThrowsOnEmptyStore() {
        JIPipeProgressLog silent = JIPipeProgressLog.silent();
        List<String> lines = new ArrayList<>();
        silent.renderText(JIPipeLogLevel.INFO, lines::add);
        assertEquals(List.of(), lines);
        silent.finish(JIPipeProgressNodeState.OK);
        silent.renderText(JIPipeLogLevel.INFO, lines::add);
        assertEquals(0, lines.size());

        Path file = tmp.resolve("never-written.ndjson");
        try (JIPipeProgressLog store = JIPipeProgressLog.create(file, "run-empty", "Pipeline run")) {
            List<String> fileLines = new ArrayList<>();
            store.renderText(JIPipeLogLevel.INFO, fileLines::add);
            assertTrue(fileLines.isEmpty());
        }
    }
}
