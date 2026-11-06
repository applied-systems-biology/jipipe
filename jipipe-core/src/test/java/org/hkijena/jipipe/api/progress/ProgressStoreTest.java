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

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.hkijena.jipipe.utils.json.JsonUtils;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Tests for the NDJSON progress store ({@link JIPipeProgressLog} and its implementations):
 * file round-trip, lazy file creation, bounded tail on non-persistent stores, truncation
 * tolerance, node index maintenance, and write throughput.
 */
class ProgressStoreTest {

    @TempDir
    Path tmp;

    /**
     * Reads and parses all records of an NDJSON file independently of the store's own reader.
     */
    private static List<JIPipeProgressRecord> readWholeFile(Path file) throws IOException {
        ObjectMapper mapper = JsonUtils.getObjectMapper();
        List<JIPipeProgressRecord> result = new ArrayList<>();
        for (String line : Files.readAllLines(file, StandardCharsets.UTF_8)) {
            if (line.isBlank()) {
                continue;
            }
            JsonNode node = mapper.readTree(line);
            String type = node.path("t").asText(null);
            assertNotNull(type, "record line must carry the discriminator t: " + line);
            // Parse with Jackson, never by string splitting (the t field serializes last in the line)
            switch (type) {
                case "head" -> result.add(mapper.treeToValue(node, JIPipeProgressRecord.Head.class));
                case "n" -> result.add(mapper.treeToValue(node, JIPipeProgressRecord.Node.class));
                case "N" -> result.add(mapper.treeToValue(node, JIPipeProgressRecord.NodeFinished.class));
                case "l" -> result.add(mapper.treeToValue(node, JIPipeProgressRecord.Log.class));
                case "p" -> result.add(mapper.treeToValue(node, JIPipeProgressRecord.Progress.class));
                case "e" -> result.add(mapper.treeToValue(node, JIPipeProgressRecord.Error.class));
                case "s" -> result.add(mapper.treeToValue(node, JIPipeProgressRecord.Status.class));
                case "w" -> result.add(mapper.treeToValue(node, JIPipeProgressRecord.Notification.class));
                default -> throw new AssertionError("Unknown record type: " + type);
            }
        }
        return result;
    }

    @Test
    void recordFileRoundTrip() throws Exception {
        Path file = tmp.resolve("run-1.ndjson");
        JIPipeProgressLog store = JIPipeProgressLog.create(file, "run-1", "Pipeline run");
        int n = store.beginNode(store.getRootNodeId(), "Node A", JIPipeProgressNodeKind.ITEM);
        store.log(n, JIPipeLogLevel.INFO, "hello");
        store.progress(n, 5, 100);
        store.nodeFinished(n, JIPipeProgressNodeState.OK);
        store.finish(JIPipeProgressNodeState.OK);
        List<JIPipeProgressRecord> all = readWholeFile(file);
        // The brief's sketch lists head, n, l, p, N; finish() additionally emits the run-status
        // record ("s") per its interface contract and the design spec ("final flush at run end
        // before the s:finished record"). The root node itself is implied by the head record
        // (no "n" record; children of the root carry p=0).
        assertEquals(6, all.size());
        assertEquals("head", all.get(0).type());
        assertEquals("n", all.get(1).type());
        assertEquals("l", all.get(2).type());
        assertEquals("p", all.get(3).type());
        assertEquals("N", all.get(4).type());
        assertEquals("s", all.get(5).type());

        JIPipeProgressRecord.Head head = (JIPipeProgressRecord.Head) all.get(0);
        assertEquals(1, head.v());
        assertEquals("run-1", head.run());
        assertEquals("Pipeline run", head.label());

        JIPipeProgressRecord.Node node = (JIPipeProgressRecord.Node) all.get(1);
        assertEquals("Node A", node.l());
        assertEquals(0, node.p(), "children of the root node reference it via p=0");
        assertEquals(JIPipeProgressNodeKind.ITEM.name(), node.k());

        assertEquals("hello", ((JIPipeProgressRecord.Log) all.get(2)).m());
        assertEquals(JIPipeLogLevel.INFO.value(), ((JIPipeProgressRecord.Log) all.get(2)).lv());
        assertEquals(5, ((JIPipeProgressRecord.Progress) all.get(3)).v());
        assertEquals(100, ((JIPipeProgressRecord.Progress) all.get(3)).mx());

        // Wire state literals are explicit mappings, not enum names
        assertEquals("ok", ((JIPipeProgressRecord.NodeFinished) all.get(4)).s());
        assertEquals(n, ((JIPipeProgressRecord.NodeFinished) all.get(4)).id());
        assertEquals("finished", ((JIPipeProgressRecord.Status) all.get(5)).s());

        // The index reflects the finished run
        assertEquals(JIPipeProgressNodeState.OK, store.getNode(n).getState());
        assertEquals(5, store.getNode(n).getProgress());
        assertEquals(100, store.getNode(n).getMaxProgress());
        assertEquals(JIPipeProgressNodeState.OK, store.getNode(store.getRootNodeId()).getState());
    }

    @Test
    void nodeLocationRoundTripsThroughFileAndIndex() throws Exception {
        Path file = tmp.resolve("loc.ndjson");
        JIPipeProgressLog store = JIPipeProgressLog.create(file, "loc", "Locations");
        int tagged = store.beginNode(store.getRootNodeId(), "Tagged node", JIPipeProgressNodeKind.ITEM, "node://abc");
        int untagged = store.beginNode(store.getRootNodeId(), "Untagged node", JIPipeProgressNodeKind.ITEM);
        store.finish(JIPipeProgressNodeState.OK);

        // The index carries the location (and null for untagged nodes and the root)
        assertEquals("node://abc", store.getNode(tagged).getLocation());
        assertNull(store.getNode(untagged).getLocation());
        assertNull(store.getNode(store.getRootNodeId()).getLocation(), "the root has no location");

        // The wire record carries the optional loc field
        List<JIPipeProgressRecord> all = readWholeFile(file);
        JIPipeProgressRecord.Node taggedRecord = (JIPipeProgressRecord.Node) all.get(1);
        assertEquals("node://abc", taggedRecord.loc());
        assertNull(((JIPipeProgressRecord.Node) all.get(2)).loc(), "absent location: no loc value");
        String taggedLine = Files.readAllLines(file, StandardCharsets.UTF_8).get(1);
        assertTrue(taggedLine.contains("\"loc\":\"node://abc\""), taggedLine);
        String untaggedLine = Files.readAllLines(file, StandardCharsets.UTF_8).get(2);
        assertFalse(untaggedLine.contains("\"loc\""), untaggedLine);

        // Read-back through the store's own reader (an old reader sees no loc; a new one sees the URI)
        JIPipeProgressLog reader = JIPipeProgressLog.create(file, "reader", "Reader");
        JIPipeProgressRecord.Node readBack = (JIPipeProgressRecord.Node) reader.readRecords(2, 1).get(0);
        assertEquals("node://abc", readBack.loc());
        reader.close();
    }

    @Test
    void stateWireLiteralsAreMappedExplicitly() throws Exception {
        Path file = tmp.resolve("states.ndjson");
        try (JIPipeProgressLog store = JIPipeProgressLog.create(file, "states", "States")) {
            int failed = store.beginNode(store.getRootNodeId(), "Failed node", JIPipeProgressNodeKind.ITEM);
            store.nodeFinished(failed, JIPipeProgressNodeState.FAILED);
            int cancelled = store.beginNode(store.getRootNodeId(), "Cancelled node", JIPipeProgressNodeKind.ITEM);
            store.nodeFinished(cancelled, JIPipeProgressNodeState.CANCELLED);
            store.finish(JIPipeProgressNodeState.CANCELLED);
        }
        List<JIPipeProgressRecord> all = readWholeFile(file);
        List<JIPipeProgressRecord.NodeFinished> finishes = all.stream()
                .filter(r -> r instanceof JIPipeProgressRecord.NodeFinished)
                .map(r -> (JIPipeProgressRecord.NodeFinished) r)
                .toList();
        assertEquals(2, finishes.size());
        assertEquals("fail", finishes.get(0).s());
        assertEquals("cancel", finishes.get(1).s());
        JIPipeProgressRecord.Status status = (JIPipeProgressRecord.Status) all.get(all.size() - 1);
        assertEquals("cancelled", status.s());
    }

    @Test
    void fileCreatedLazilyOnlyWhenRecordsExist() {
        JIPipeProgressLog silent = JIPipeProgressLog.silent();
        assertNull(silent.getFile());
        silent.finish(JIPipeProgressNodeState.OK);
        assertNull(silent.getFile());
    }

    @Test
    void fileStoreCreatesFileOnlyWhenRecordsExist() {
        Path file = tmp.resolve("lazy.ndjson");
        JIPipeProgressLog store = JIPipeProgressLog.create(file, "lazy", "Lazy run");
        assertNotNull(store.getFile(), "the file path is available before the file exists");
        store.close();
        assertFalse(Files.exists(file), "a store that never records anything creates no file");
    }

    @Test
    void nonPersistentStoresKeepTailAndIndexButNoReadBack() {
        JIPipeProgressLog stdout = JIPipeProgressLog.stdout();
        int n = stdout.beginNode(stdout.getRootNodeId(), "Node A", JIPipeProgressNodeKind.ITEM);
        stdout.log(n, JIPipeLogLevel.INFO, "hello");
        assertEquals(1, stdout.readRecent(10).size(), "tail maintained on non-persistent stores");
        assertNotNull(stdout.getNode(n), "index maintained on non-persistent stores");
        assertTrue(stdout.readRecords(1, 100).size() <= stdout.readRecent(Integer.MAX_VALUE).size(),
                "readRecords on non-persistent stores never exceeds what the tail holds");
        stdout.close();
    }

    @Test
    void truncatedFinalLineTolerated() throws Exception {
        Path file = tmp.resolve("truncated.ndjson");
        JIPipeProgressLog store = JIPipeProgressLog.create(file, "trunc", "Truncated run");
        int n = store.beginNode(store.getRootNodeId(), "Node A", JIPipeProgressNodeKind.ITEM);
        for (int i = 0; i < 9; i++) {
            store.log(n, JIPipeLogLevel.INFO, "line " + i);
        }
        store.nodeFinished(n, JIPipeProgressNodeState.OK);
        // 12 records so far (head, n, 9 logs, N); close() without finish() adds no status record
        store.close();

        // Cut the last line in half
        List<String> lines = Files.readAllLines(file, StandardCharsets.UTF_8);
        assertEquals(12, lines.size());
        String last = lines.get(lines.size() - 1);
        String truncated = String.join("\n", lines.subList(0, lines.size() - 1)) + "\n" + last.substring(0, last.length() / 2);
        Files.write(file, truncated.getBytes(StandardCharsets.UTF_8));

        JIPipeProgressLog reader = JIPipeProgressLog.create(file, "reader", "Reader");
        List<JIPipeProgressRecord> records = reader.readRecords(1, 100);
        assertEquals(11, records.size(), "the truncated final line is dropped, complete records are returned");
        // Lines 1-11 are head, n, and the 9 log records; line 12 (the N record) is the truncated one
        assertEquals("head", records.get(0).type());
        assertEquals("l", records.get(records.size() - 1).type());
        reader.close();
    }

    @Test
    void errorRecordRoundTripsThroughFile() throws Exception {
        Path file = tmp.resolve("error.ndjson");
        JIPipeProgressLog store = JIPipeProgressLog.create(file, "err", "Errors");
        int n = store.beginNode(store.getRootNodeId(), "Node", JIPipeProgressNodeKind.ITEM);
        store.error(n, new RuntimeException("boom"));
        store.close();

        List<JIPipeProgressRecord> all = readWholeFile(file);
        assertEquals(3, all.size());
        JIPipeProgressRecord.Error error = (JIPipeProgressRecord.Error) all.get(2);
        assertEquals("e", error.type());
        assertEquals(n, error.id());
        assertEquals("java.lang.RuntimeException", error.c());
        assertEquals("boom", error.m());
        assertNotNull(error.st(), "the stack trace is recorded");

        // The store's own reader parses the record back
        JIPipeProgressLog reader = JIPipeProgressLog.create(file, "reader", "Reader");
        JIPipeProgressRecord.Error readBack = (JIPipeProgressRecord.Error) reader.readRecords(3, 1).get(0);
        assertEquals("java.lang.RuntimeException", readBack.c());
        assertEquals("boom", readBack.m());
        reader.close();
    }

    @Test
    void tailEvictsByMessageTextWhenRecordsAreHuge() {
        JIPipeProgressLog store = JIPipeProgressLog.silent();
        store.setTailLimit(100);
        // Four 3 MB messages: 12 MB total, over the ~8 MB text cap, but under the count limit (100)
        for (int i = 1; i <= 4; i++) {
            store.log(store.getRootNodeId(), JIPipeLogLevel.INFO, "huge-" + i + "-" + "a".repeat(3_000_000));
        }
        store.log(store.getRootNodeId(), JIPipeLogLevel.INFO, "small-1");
        store.log(store.getRootNodeId(), JIPipeLogLevel.INFO, "small-2");

        List<JIPipeProgressRecord> recent = store.readRecent(Integer.MAX_VALUE);
        // Eviction happened by message text, not count: the two oldest huge records are gone,
        // and the recent small records survive
        assertEquals(4, recent.size(), "the tail keeps the most recent records within the text cap");
        assertTrue(((JIPipeProgressRecord.Log) recent.get(0)).m().startsWith("huge-3-"));
        assertTrue(((JIPipeProgressRecord.Log) recent.get(1)).m().startsWith("huge-4-"));
        assertEquals("small-1", ((JIPipeProgressRecord.Log) recent.get(2)).m());
        assertEquals("small-2", ((JIPipeProgressRecord.Log) recent.get(3)).m());
        long textChars = recent.stream().mapToLong(r -> ((JIPipeProgressRecord.Log) r).m().length()).sum();
        assertTrue(textChars <= 8_388_608L, "tail text must stay under the ~8 MB cap, was " + textChars + " chars");
        assertTrue(textChars >= 6_000_000L, "the retained huge records are still available, was " + textChars + " chars");

        // Shrinking the count limit also shrinks the tail from the head
        store.setTailLimit(2);
        recent = store.readRecent(Integer.MAX_VALUE);
        assertEquals(2, recent.size());
        assertEquals(List.of("small-1", "small-2"),
                recent.stream().map(r -> ((JIPipeProgressRecord.Log) r).m()).toList());
        store.close();
    }

    @Test
    void resolveStorageBaseIsPure() {
        Path projectBase = tmp.resolve("proj");
        assertEquals(projectBase.resolve("logs"), JIPipeProgressLog.resolveStorageBase(projectBase));
        Path global = JIPipeProgressLog.resolveStorageBase(null);
        assertTrue(global.endsWith("logs"), "global base appends logs, was " + global);
        assertTrue(global.isAbsolute(), "global base is absolute, was " + global);
    }

    @Test
    void nodeIndexTracksTreeAndRecordRanges() {
        JIPipeProgressLog store = JIPipeProgressLog.silent();
        assertEquals(1, store.getRootNodeId());
        assertEquals(2, store.nextNodeId(), "next node id is peeked without creating");

        int partition = store.beginNode(store.getRootNodeId(), "Partition 0", JIPipeProgressNodeKind.PARTITION);
        int a = store.beginNode(partition, "Node A", JIPipeProgressNodeKind.ITEM);
        assertEquals(4, store.nextNodeId());
        store.log(a, JIPipeLogLevel.INFO, "m1");
        int b = store.beginNode(partition, "Node B", JIPipeProgressNodeKind.ITEM);
        store.log(b, JIPipeLogLevel.INFO, "m2");
        store.progress(a, 3, 10);
        store.error(a, new RuntimeException("boom"));

        assertEquals(2, store.getNode(partition).getDescendantNodeCount());
        assertEquals(0, store.getNode(a).getDescendantNodeCount());
        assertEquals(3, store.getNode(a).getProgress());
        assertEquals(10, store.getNode(a).getMaxProgress());

        // Records attributed to node a: its n record, the log, the progress, and the error
        assertEquals(4, store.getNode(a).getRecordCount());
        assertTrue(store.getNode(a).getFirstLine() > 0);
        assertTrue(store.getNode(a).getLastLine() >= store.getNode(a).getFirstLine());
        assertEquals(2, store.getNode(b).getRecordCount(), "Node B: its n record and its log");
        assertEquals(1, store.getNode(partition).getRecordCount(), "the partition only owns its own creation record");
        assertEquals(0, store.getNode(store.getRootNodeId()).getRecordCount(), "no records were attached to the root");

        store.nodeFinished(a, JIPipeProgressNodeState.OK);
        store.nodeFinished(b, JIPipeProgressNodeState.FAILED);
        assertEquals(JIPipeProgressNodeState.OK, store.getNode(a).getState());
        assertEquals(JIPipeProgressNodeState.FAILED, store.getNode(b).getState());

        // The tail holds the renderable records (l, p, e) in order; structural records (head, n, N) are excluded
        List<JIPipeProgressRecord> recent = store.readRecent(100);
        assertEquals(List.of("l", "l", "p", "e"), recent.stream().map(JIPipeProgressRecord::type).toList());

        // readRecords on a non-persistent store serves from the tail, filtered by sequence number
        // (seq 3 is Node A's n record; the renderable tail starts at seq 4)
        List<JIPipeProgressRecord> slice = store.readRecords(7, 10);
        assertEquals(2, slice.size());
        assertEquals("p", slice.get(0).type());
        assertEquals("e", slice.get(1).type());
        assertEquals(1, store.readRecords(4, 1).size(), "the limit is honored");
        assertTrue(store.readRecords(1, 100).size() <= 4);

        store.finish(JIPipeProgressNodeState.CANCELLED);
        assertEquals(JIPipeProgressNodeState.CANCELLED, store.getNode(store.getRootNodeId()).getState());
    }

    @Test
    void batchEmitterPresentButIdle() {
        JIPipeProgressLog store = JIPipeProgressLog.silent();
        assertNotNull(store.getBatchEventEmitter());
        List<JIPipeProgressLog.ProgressBatchEvent> events = new ArrayList<>();
        store.getBatchEventEmitter().subscribeLambda((emitter, event) -> events.add(event));
        int n = store.beginNode(store.getRootNodeId(), "Node A", JIPipeProgressNodeKind.ITEM);
        store.log(n, JIPipeLogLevel.INFO, "hello");
        store.progress(n, 1, 2);
        store.nodeFinished(n, JIPipeProgressNodeState.OK);
        store.finish(JIPipeProgressNodeState.OK);
        assertTrue(events.isEmpty(), "the batch emitter stays idle; coalescing is wired by a later task");
        store.setCoalesceIntervalMillis(500);
        assertTrue(events.isEmpty(), "setting the coalesce interval does not emit events");
    }

    @Test
    void scaleSixMillionRecordsThroughputAndBoundedTail() {
        Path file = tmp.resolve("scale.ndjson");
        JIPipeProgressLog store = JIPipeProgressLog.create(file, "scale-run", "Scale run");
        try {
            store.setTailLimit(8192);
            int node = store.beginNode(store.getRootNodeId(), "Scale node", JIPipeProgressNodeKind.ITEM);
            long start = System.nanoTime();
            for (int i = 0; i < 6_000_000; i++) {
                store.log(node, JIPipeLogLevel.DEBUG, "cand " + i + " & filter " + (i % 7) + " --> overlap");
            }
            store.nodeFinished(node, JIPipeProgressNodeState.OK);
            store.finish(JIPipeProgressNodeState.OK);
            long ms = (System.nanoTime() - start) / 1_000_000;
            double perSecond = 6_000_000.0 / (ms / 1000.0);
            System.out.println("ProgressStoreTest scale: 6,000,000 records in " + ms + " ms (" + (long) perSecond + " records/s)");
            assertTrue(perSecond >= 25_000, "throughput floor 25k/s, was " + ms + "ms (" + (long) perSecond + " records/s)");

            assertEquals(8192, store.readRecent(8192).size(), "tail must be bounded");

            // head=1, n=2 → the first log record is line 3; order preserved
            List<JIPipeProgressRecord> first10 = store.readRecords(3, 10);
            assertEquals(10, first10.size());
            for (int i = 0; i < 10; i++) {
                JIPipeProgressRecord.Log log = (JIPipeProgressRecord.Log) first10.get(i);
                assertEquals("cand " + i + " & filter " + (i % 7) + " --> overlap", log.m());
                assertEquals(JIPipeLogLevel.DEBUG.value(), log.lv());
            }

            // The file ends with the node-finished and run-status records (6,000,004 lines total)
            List<JIPipeProgressRecord> lastLines = store.readRecords(6_000_003, 10);
            assertEquals(2, lastLines.size());
            assertEquals("N", lastLines.get(0).type());
            assertEquals("s", lastLines.get(1).type());
        } finally {
            store.close();
        }
    }
}
