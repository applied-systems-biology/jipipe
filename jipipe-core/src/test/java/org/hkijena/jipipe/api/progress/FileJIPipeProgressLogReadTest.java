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
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Tests for random-range reads ({@code readRecords(fromLine, limit)}) of the file-backed progress
 * store: every range must return exactly the records an independent full scan of the NDJSON file
 * returns — early ranges before the first checkpoint, ranges at checkpoint boundaries, mid-file
 * and near-end ranges, and ranges over a file with a truncated final line. The write side records
 * line→byte-offset checkpoints so reads can seek instead of skip-scanning from the file start
 * (ruling R18); these tests pin that the seek path stays byte-exact.
 */
class FileJIPipeProgressLogReadTest {

    @TempDir
    Path tmp;

    /**
     * Reads and parses all records of an NDJSON file independently of the store's own reader.
     * Only for complete files — a truncated final line makes the raw parse throw.
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

    /**
     * Writes a deterministic mixed record stream: 4 nodes, alternating log/progress records,
     * varied message lengths, multi-byte UTF-8 content, and JSON-escaped characters. The file
     * ends up with {@code loopRecords + 10} lines (head, 4 node records, the loop records,
     * 4 node finishes, the run status). The store is closed before it is returned.
     */
    private FileJIPipeProgressLog writeMixedRecords(Path file, int loopRecords) {
        FileJIPipeProgressLog store = new FileJIPipeProgressLog(file, "seek-run", "Seek run");
        int[] nodes = new int[4];
        for (int i = 0; i < nodes.length; i++) {
            nodes[i] = store.beginNode(store.getRootNodeId(), "Node " + (char) ('A' + i), JIPipeProgressNodeKind.ITEM);
        }
        for (int i = 0; i < loopRecords; i++) {
            int node = nodes[i % nodes.length];
            if (i % 2 == 0) {
                String message = "record " + i + " & \"filter " + (i % 7) + "\"";
                if (i % 25 == 0) {
                    // Multi-byte UTF-8 content: byte offsets must not be confused with character offsets
                    message = "Ergebnis " + i + " → 結果 😀 " + "x".repeat(i % 200);
                }
                store.log(node, i % 3 == 0 ? JIPipeLogLevel.DEBUG : JIPipeLogLevel.INFO, message);
            } else {
                store.progress(node, i % 100, 100);
            }
        }
        for (int node : nodes) {
            store.nodeFinished(node, JIPipeProgressNodeState.OK);
        }
        store.finish(JIPipeProgressNodeState.OK);
        store.close();
        return store;
    }

    /**
     * Asserts that {@code store.readRecords(fromLine, limit)} returns exactly the records the
     * reference full scan holds at these lines.
     */
    private static void assertRangeMatches(FileJIPipeProgressLog store,
                                           List<JIPipeProgressRecord> reference,
                                           int fromLine,
                                           int limit) {
        assertTrue(fromLine - 1 <= reference.size(), "the requested range must start inside the file");
        List<JIPipeProgressRecord> actual = store.readRecords(fromLine, limit);
        int end = Math.min(reference.size(), fromLine - 1 + limit);
        assertEquals(reference.subList(fromLine - 1, end), actual,
                "readRecords(" + fromLine + ", " + limit + ") must match the full scan");
    }

    @Test
    void readRangesMatchFullScanReferenceAtScale() throws Exception {
        Path file = tmp.resolve("seek.ndjson");
        // 1 + 4 + 24,990 + 4 + 1 = 25,000 lines; checkpoints cover lines 10,000 and 20,000
        FileJIPipeProgressLog store = writeMixedRecords(file, 24_990);
        List<JIPipeProgressRecord> reference = readWholeFile(file);
        assertEquals(25_000, reference.size(), "the generated stream must have the expected shape");

        // Early: before the first checkpoint, the read scans from the file start
        assertRangeMatches(store, reference, 1, 50);
        assertRangeMatches(store, reference, 9_999, 3);
        // Checkpoint boundaries: line 10,000 is served by a seek with zero skip,
        // line 10,001 by a seek with a one-line skip
        assertRangeMatches(store, reference, 10_000, 2);
        assertRangeMatches(store, reference, 10_001, 2);
        // Mid-file: seek to the checkpoint at line 10,000, then scan 5,000 lines forward
        assertRangeMatches(store, reference, 15_000, 5_000);
        assertRangeMatches(store, reference, 20_000, 2);
        assertRangeMatches(store, reference, 20_001, 2);
        // Near the end: the range runs into the final records (node finishes, run status)
        assertRangeMatches(store, reference, 24_501, 1_000);
        // Past the end
        assertTrue(store.readRecords(26_000, 10).isEmpty(), "no records past the end of the file");

        // The level-filtered overload pages through readRecords — it must stay consistent with
        // an independent filter over the full-scan reference
        List<JIPipeProgressRecord> expectedInfo = reference.stream()
                .skip(15_000 - 1)
                .filter(r -> AbstractJIPipeProgressLog.passesLevelFilter(r, JIPipeLogLevel.INFO))
                .limit(10)
                .toList();
        assertEquals(expectedInfo, store.readRecords(15_000, 10, JIPipeLogLevel.INFO));
        List<JIPipeProgressRecord> expectedNearEnd = reference.stream()
                .skip(20_000 - 1)
                .filter(r -> AbstractJIPipeProgressLog.passesLevelFilter(r, JIPipeLogLevel.INFO))
                .limit(50)
                .toList();
        assertEquals(expectedNearEnd, store.readRecords(20_000, 50, JIPipeLogLevel.INFO));
        // At the lowest level every record passes, so the overload matches the plain read
        assertEquals(store.readRecords(15_000, 10), store.readRecords(15_000, 10, JIPipeLogLevel.DEBUG),
                "the DEBUG filter passes every record");
    }

    @Test
    void truncatedFinalLineToleratedWithCheckpointSeek() throws Exception {
        Path file = tmp.resolve("truncated-seek.ndjson");
        FileJIPipeProgressLog store = new FileJIPipeProgressLog(file, "trunc-seek", "Truncation seek run");
        int node = store.beginNode(store.getRootNodeId(), "Node A", JIPipeProgressNodeKind.ITEM);
        for (int i = 0; i < 12_000; i++) {
            store.log(node, JIPipeLogLevel.INFO, "line " + i);
        }
        store.nodeFinished(node, JIPipeProgressNodeState.OK);
        store.finish(JIPipeProgressNodeState.OK);
        store.close();

        // Cut the final line (the "s" record) in half — the last checkpoint covers line 10,000,
        // well before the truncation point
        List<String> lines = Files.readAllLines(file, StandardCharsets.UTF_8);
        assertEquals(12_004, lines.size());
        String last = lines.get(lines.size() - 1);
        String truncated = String.join("\n", lines.subList(0, lines.size() - 1)) + "\n" + last.substring(0, last.length() / 2);
        Files.write(file, truncated.getBytes(StandardCharsets.UTF_8));

        // Reference: the store's scan-from-start path (line 1 has no covering checkpoint), which
        // tolerates the truncated final line; the independent parser cannot read this file
        List<JIPipeProgressRecord> reference = store.readRecords(1, 20_000);
        assertEquals(12_003, reference.size(), "complete records only — the truncated line is dropped");
        assertEquals("N", reference.get(reference.size() - 1).type());

        // Ranges that seek to the checkpoint at line 10,000 and read past the truncation point
        assertEquals(reference.subList(10_499, reference.size()), store.readRecords(10_500, 2_000),
                "a checkpointed read over a truncated file returns the complete prefix");
        assertEquals(reference.subList(11_899, reference.size()), store.readRecords(11_900, 500));
    }

    @Test
    void smallFileScansFromStartWithoutCheckpoints() throws Exception {
        Path file = tmp.resolve("small.ndjson");
        // 1 + 4 + 487 + 4 + 1 = 497 lines — fewer lines than the checkpoint interval
        FileJIPipeProgressLog store = writeMixedRecords(file, 487);
        List<JIPipeProgressRecord> reference = readWholeFile(file);
        assertEquals(497, reference.size());

        assertRangeMatches(store, reference, 1, 10);
        assertRangeMatches(store, reference, 250, 100);
        assertRangeMatches(store, reference, 490, 20);
        // The final line and past the end
        assertRangeMatches(store, reference, 497, 100);
        assertTrue(store.readRecords(600, 5).isEmpty(), "no records past the end of the file");
        assertEquals(0, store.checkpointIndexForTesting().size(),
                "a file with fewer lines than the checkpoint interval has no checkpoints");

        // Lines are 1-based; a fromLine below 1 clamps to the first line
        assertEquals(store.readRecords(1, 5), store.readRecords(0, 5));
        assertEquals(store.readRecords(1, 5), store.readRecords(-3, 5));
    }

    @Test
    void checkpointIndexIsByteExact() throws Exception {
        Path file = tmp.resolve("checkpoints.ndjson");
        // 25,000 lines → checkpoints at lines 10,000 and 20,000
        FileJIPipeProgressLog store = writeMixedRecords(file, 24_990);
        FileJIPipeProgressLog.CheckpointIndex index = store.checkpointIndexForTesting();
        assertEquals(2, index.size(), "one checkpoint per 10,000 lines");
        assertEquals(10_000L, index.lineAt(0));
        assertEquals(20_000L, index.lineAt(1));
        // The offsets are the exact byte offsets where the checkpointed lines start
        assertEquals(byteOffsetOfLineStart(file, 10_000), index.offsetAt(0));
        assertEquals(byteOffsetOfLineStart(file, 20_000), index.offsetAt(1));
        // Coverage lookup: the boundary lines 10,000/10,001 and 20,000/20,001 resolve to the
        // right checkpoints; earlier lines have none
        assertEquals(-1, index.floor(9_999));
        assertEquals(0, index.floor(10_000));
        assertEquals(0, index.floor(10_001));
        assertEquals(1, index.floor(20_000));
        assertEquals(1, index.floor(20_001));
        assertEquals(1, index.floor(25_000));
        assertEquals(-1, index.floor(0));
    }

    /**
     * Computes the byte offset where {@code line} (1-based) starts, by counting the UTF-8 bytes
     * of the preceding lines — independently of the store's own bookkeeping.
     */
    private static long byteOffsetOfLineStart(Path file, int line) throws IOException {
        byte[] bytes = Files.readAllBytes(file);
        long offset = 0;
        for (int current = 1; current < line; current++) {
            while (bytes[(int) offset] != '\n') {
                offset++;
            }
            offset++;
        }
        return offset;
    }
}
