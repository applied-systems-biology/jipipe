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

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.BasicFileAttributes;
import java.nio.file.attribute.FileTime;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Tests for the read-only reopen ({@link JIPipeProgressLog#openReadOnly(java.nio.file.Path)}): a
 * reopened store must be indistinguishable from the writer-fed store that wrote the file — same
 * node tree, same record reads (the checkpoint seek path must be byte-exact), same text render —
 * while never mutating the file, never emitting batches, and dropping producer calls (the store
 * is born closed). The scan must also tolerate a truncated final line exactly like a read.
 */
class FileJIPipeProgressLogReadOnlyTest {

    @TempDir
    Path tmp;

    /**
     * Writes a deterministic store with a nested node tree (root → partition → items), log records
     * with multi-byte UTF-8 and JSON-escaped characters, progress and error records, a DEBUG-level
     * node with a location URI, and a run-level notification. The file ends up with more than
     * 20,000 lines, so the checkpoint index covers lines 10,000 and 20,000 and every read past
     * line 10,000 exercises the seek path. The store is finished and closed before it is returned.
     */
    private FileJIPipeProgressLog writeTreeStore(Path file, int loopRecords) {
        FileJIPipeProgressLog store = new FileJIPipeProgressLog(file, "reopen-run", "Reopen run");
        int partition = store.beginNode(store.getRootNodeId(), "Partition", JIPipeProgressNodeKind.PARTITION);
        int[] items = new int[4];
        for (int i = 0; i < items.length; i++) {
            items[i] = store.beginNode(partition, "Item " + (char) ('A' + i), JIPipeProgressNodeKind.ITEM,
                    "node://item-" + i, i == 3 ? JIPipeLogLevel.DEBUG : JIPipeLogLevel.INFO);
        }
        for (int i = 0; i < loopRecords; i++) {
            int node = items[i % items.length];
            switch (i % 16) {
                case 0 -> store.error(node, new IllegalStateException("boom " + i));
                case 1 -> store.progress(node, i % 100, 100);
                default -> {
                    String message = "record " + i + " & \"filter " + (i % 7) + "\"";
                    if (i % 25 == 0) {
                        // Multi-byte UTF-8 content: byte offsets must not be confused with character offsets
                        message = "Ergebnis " + i + " → 結果 😀 " + "x".repeat(i % 200);
                    }
                    store.log(node, i % 5 == 0 ? JIPipeLogLevel.DEBUG : JIPipeLogLevel.INFO, message);
                }
            }
        }
        store.notification("Heading", "Description with\na line break");
        for (int item : items) {
            store.nodeFinished(item, JIPipeProgressNodeState.OK);
        }
        store.nodeFinished(partition, JIPipeProgressNodeState.OK);
        store.finish(JIPipeProgressNodeState.OK);
        store.close();
        return store;
    }

    /**
     * Asserts that the reopened store's node with the given id is a faithful copy of the
     * original's — all metadata the store's node index tracks.
     */
    private static void assertNodeEquals(JIPipeProgressNode expected, JIPipeProgressNode actual, int id) {
        assertNotNull(expected, "the original store must have node " + id);
        assertNotNull(actual, "the reopened store must have node " + id);
        assertEquals(expected.getParentId(), actual.getParentId(), "parentId of node " + id);
        assertEquals(expected.getLabel(), actual.getLabel(), "label of node " + id);
        assertEquals(expected.getKind(), actual.getKind(), "kind of node " + id);
        assertEquals(expected.getLocation(), actual.getLocation(), "location of node " + id);
        assertEquals(expected.getLevel(), actual.getLevel(), "level of node " + id);
        assertEquals(expected.getState(), actual.getState(), "state of node " + id);
        assertEquals(expected.getProgress(), actual.getProgress(), "progress of node " + id);
        assertEquals(expected.getMaxProgress(), actual.getMaxProgress(), "maxProgress of node " + id);
        assertEquals(expected.getDescendantNodeCount(), actual.getDescendantNodeCount(), "descendants of node " + id);
        assertEquals(expected.getRecordCount(), actual.getRecordCount(), "record count of node " + id);
        assertEquals(expected.getFirstLine(), actual.getFirstLine(), "first line of node " + id);
        assertEquals(expected.getLastLine(), actual.getLastLine(), "last line of node " + id);
    }

    private static List<String> renderText(JIPipeProgressLog store) {
        List<String> lines = new ArrayList<>();
        store.renderText(JIPipeLogLevel.DEBUG, lines::add);
        return lines;
    }

    @Test
    void readOnlyReopenMatchesOriginalStore() throws Exception {
        Path file = tmp.resolve("reopen.ndjson");
        // 1 (head) + 5 (n) + 24,980 (loop) + 1 (w) + 5 (N) + 1 (s) = 24,993 lines
        FileJIPipeProgressLog original = writeTreeStore(file, 24_980);

        BasicFileAttributes attributesBefore = Files.readAttributes(file, BasicFileAttributes.class);
        FileTime mtime = attributesBefore.lastModifiedTime();
        long size = attributesBefore.size();

        JIPipeProgressLog reopened = JIPipeProgressLog.openReadOnly(file);

        // The file identity is the same, the store is a distinct view
        assertEquals(file, reopened.getFile());
        assertFalse(reopened == original);

        // The node tree is equal: every id of the original index resolves to an equal node
        assertEquals(original.nextNodeId(), reopened.nextNodeId(), "nextNodeId covers the same ids");
        for (int id = 1; id < original.nextNodeId(); id++) {
            assertNodeEquals(original.getNode(id), reopened.getNode(id), id);
        }
        assertNull(reopened.getNode(original.nextNodeId()), "no node past the allocated ids");

        // Record reads are equal — full range and the checkpoint boundaries (lines 10,000 and
        // 20,000 must be served by the reopened store's own seek index)
        List<JIPipeProgressRecord> reference = original.readRecords(1, 100_000);
        assertTrue(reference.size() > 20_000, "the generated stream must exercise the checkpoint path");
        assertEquals(reference, reopened.readRecords(1, 100_000));
        assertEquals(original.readRecords(9_999, 5), reopened.readRecords(9_999, 5));
        assertEquals(original.readRecords(10_000, 2), reopened.readRecords(10_000, 2));
        assertEquals(original.readRecords(10_001, 2), reopened.readRecords(10_001, 2));
        assertEquals(original.readRecords(20_000, 2), reopened.readRecords(20_000, 2));
        assertEquals(original.readRecords(20_001, 2), reopened.readRecords(20_001, 2));
        assertTrue(reopened.readRecords(reference.size() + 1, 10).isEmpty(), "no records past the end");
        assertEquals(reference.subList(24_000, reference.size()), reopened.readRecords(24_001, 1_000));

        // Level-filtered reads page through the same records
        assertEquals(original.readRecords(5_000, 100, JIPipeLogLevel.INFO), reopened.readRecords(5_000, 100, JIPipeLogLevel.INFO));
        assertEquals(original.readRecords(15_000, 50, JIPipeLogLevel.WARN), reopened.readRecords(15_000, 50, JIPipeLogLevel.WARN));

        // The tail serves the same recent records
        assertEquals(original.readRecent(100), reopened.readRecent(100));

        // The text render is identical
        assertEquals(renderText(original), renderText(reopened));

        // The checkpoint index is rebuilt byte-exact
        FileJIPipeProgressLog.CheckpointIndex originalIndex = original.checkpointIndexForTesting();
        FileJIPipeProgressLog.CheckpointIndex reopenedIndex = ((FileJIPipeProgressLog) reopened).checkpointIndexForTesting();
        assertEquals(2, originalIndex.size(), "checkpoints at lines 10,000 and 20,000");
        assertEquals(originalIndex.size(), reopenedIndex.size());
        for (int i = 0; i < originalIndex.size(); i++) {
            assertEquals(originalIndex.lineAt(i), reopenedIndex.lineAt(i));
            assertEquals(originalIndex.offsetAt(i), reopenedIndex.offsetAt(i));
        }

        // No file mutation
        BasicFileAttributes attributesAfter = Files.readAttributes(file, BasicFileAttributes.class);
        assertEquals(size, attributesAfter.size(), "the reopen must not change the file size");
        assertEquals(mtime, attributesAfter.lastModifiedTime(), "the reopen must not change the file mtime");

        // close() is a no-op — the file stays untouched and the store stays readable
        reopened.close();
        BasicFileAttributes attributesAfterClose = Files.readAttributes(file, BasicFileAttributes.class);
        assertEquals(size, attributesAfterClose.size());
        assertEquals(mtime, attributesAfterClose.lastModifiedTime());
        assertEquals(reference, reopened.readRecords(1, 100_000), "reads still work after close()");

        original.close();
    }

    @Test
    void readOnlyStoreIsInertAndEmitsNothing() throws Exception {
        Path file = tmp.resolve("inert.ndjson");
        FileJIPipeProgressLog original = writeTreeStore(file, 500);
        long size = Files.size(file);
        FileTime mtime = Files.getLastModifiedTime(file);

        JIPipeProgressLog reopened = JIPipeProgressLog.openReadOnly(file);

        AtomicInteger batches = new AtomicInteger();
        reopened.getBatchEventEmitter().subscribe(event -> batches.incrementAndGet());

        // Producer calls are dropped (the store is born closed): no records, no nodes, no batches
        int nodeCount = reopened.nextNodeId();
        reopened.log(reopened.getRootNodeId(), JIPipeLogLevel.INFO, "must be dropped");
        reopened.progress(reopened.getRootNodeId(), 1, 2);
        reopened.error(reopened.getRootNodeId(), new RuntimeException("must be dropped"));
        reopened.notification("heading", "must be dropped");
        reopened.nodeFinished(reopened.getRootNodeId(), JIPipeProgressNodeState.OK);
        int strayNode = reopened.beginNode(reopened.getRootNodeId(), "must be dropped", JIPipeProgressNodeKind.ITEM);
        reopened.finish(JIPipeProgressNodeState.OK);
        // A dropped beginNode returns a distinct, unused id (the closed-store contract) — the id
        // is burned but no node is registered
        assertTrue(strayNode >= nodeCount, "a dropped beginNode returns an unused id");
        assertNull(reopened.getNode(strayNode), "no node was registered");
        assertEquals(nodeCount + 1, reopened.nextNodeId());
        assertEquals(0, batches.get(), "the batch emitter stays idle");

        // No file mutation, and the reads are unaffected
        assertEquals(size, Files.size(file));
        assertEquals(mtime, Files.getLastModifiedTime(file));
        assertFalse(reopened.readRecords(1, 10).isEmpty(), "reads still work");

        // close() is idempotent and does not disable reads
        reopened.close();
        reopened.close();
        assertFalse(reopened.readRecords(1, 10).isEmpty(), "reads still work after close()");

        original.close();
    }

    @Test
    void truncatedFileReopenToleratesTheBrokenTail() throws Exception {
        Path file = tmp.resolve("truncated-reopen.ndjson");
        FileJIPipeProgressLog original = new FileJIPipeProgressLog(file, "trunc-reopen", "Truncation reopen run");
        int node = original.beginNode(original.getRootNodeId(), "Node A", JIPipeProgressNodeKind.ITEM);
        for (int i = 0; i < 12_000; i++) {
            original.log(node, JIPipeLogLevel.INFO, "line " + i);
        }
        original.nodeFinished(node, JIPipeProgressNodeState.OK);
        original.finish(JIPipeProgressNodeState.OK);
        original.close();

        // Cut the final line (the "s" record) in half
        List<String> lines = Files.readAllLines(file, StandardCharsets.UTF_8);
        assertEquals(12_004, lines.size());
        String last = lines.get(lines.size() - 1);
        String truncated = String.join("\n", lines.subList(0, lines.size() - 1)) + "\n" + last.substring(0, last.length() / 2);
        Files.write(file, truncated.getBytes(StandardCharsets.UTF_8));

        long size = Files.size(file);
        FileTime mtime = Files.getLastModifiedTime(file);
        JIPipeProgressLog reopened = JIPipeProgressLog.openReadOnly(file);

        // The original (writer-fed) store reads the file directly and keeps its in-memory tail —
        // the reopened store must match both
        assertEquals(original.readRecords(1, 100_000), reopened.readRecords(1, 100_000),
                "complete records only — the truncated line is dropped");
        assertEquals(original.readRecords(10_500, 2_000), reopened.readRecords(10_500, 2_000),
                "a checkpointed read over a truncated file returns the complete prefix");
        assertEquals(original.readRecent(50), reopened.readRecent(50));
        assertEquals(original.getNode(node).getRecordCount(), reopened.getNode(node).getRecordCount(),
                "the node's record attribution matches the writer-fed read");
        // The truncation cut the run-status record: the reopen must not present the dead run as
        // live (a RUNNING root keeps the progress panel's spinners running forever)
        assertEquals(JIPipeProgressNodeState.CANCELLED, reopened.getNode(reopened.getRootNodeId()).getState());

        // No file mutation
        assertEquals(size, Files.size(file));
        assertEquals(mtime, Files.getLastModifiedTime(file));

        original.close();
        reopened.close();
    }
}
