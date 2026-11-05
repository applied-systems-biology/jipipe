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

package org.hkijena.jipipe.desktop.app.running.logs;

import org.hkijena.jipipe.api.DefaultJIPipeRunnable;
import org.hkijena.jipipe.api.JIPipeProgressInfo;
import org.hkijena.jipipe.api.progress.JIPipeLogLevel;
import org.hkijena.jipipe.api.progress.JIPipeProgressLog;
import org.hkijena.jipipe.api.progress.JIPipeProgressNodeState;
import org.hkijena.jipipe.api.run.JIPipeRunnableLogEntry;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Pure-JVM tests for the Tools &gt; Logs collection file lifecycle: eviction over the log limit
 * deletes the evicted entry's NDJSON data file (the disk bound), clearing the collection deletes
 * all data files, and rendering an entry never throws — a missing file renders the placeholder
 * line. No Swing component is instantiated.
 * <p>
 * Eviction is driven through the package-private {@code insertEntry(entry, limit)} helper:
 * the public push paths read the application-settings singleton, which requires JIPipe
 * initialization (see task ruling).
 */
class RunnableLogsCollectionTest {

    @TempDir
    Path tmp;

    /**
     * Creates an entry whose data file is a real, finished NDJSON store file — mirroring the
     * worker, which finishes and closes the store before the log entry is created.
     */
    private JIPipeRunnableLogEntry createEntryWithData(String label) {
        Path file = tmp.resolve(UUID.randomUUID() + ".ndjson");
        JIPipeProgressLog store = JIPipeProgressLog.create(file, UUID.randomUUID().toString(), label);
        store.log(store.getRootNodeId(), JIPipeLogLevel.INFO, "log line of " + label);
        store.finish(JIPipeProgressNodeState.OK);
        DefaultJIPipeRunnable run = new DefaultJIPipeRunnable() {
            @Override
            public String getTaskLabel() {
                return label;
            }

            @Override
            public void run() {
                // Never executed — the store is finished before the entry is created
            }
        };
        run.getProgressInfo().setStore(store);
        return new JIPipeRunnableLogEntry(run, true);
    }

    @Test
    void entryReferencesDataFile() {
        JIPipeRunnableLogEntry entry = createEntryWithData("referencing");
        assertNotNull(entry.getPath(), "file-backed runs must provide their NDJSON data file");
        assertTrue(entry.getPath().toString().endsWith(".ndjson"));
        assertTrue(Files.exists(entry.getPath()));
        assertEquals("referencing", entry.getName());
    }

    @Test
    void renderReadsFromFile() {
        JIPipeRunnableLogEntry entry = createEntryWithData("rendered");
        List<String> lines = new ArrayList<>();
        entry.renderText(lines::add);
        assertTrue(lines.stream().anyMatch(line -> line.contains("log line of rendered")),
                "the entry must render its log from the NDJSON data file");
    }

    @Test
    void evictDeletesFile() {
        JIPipeDesktopRunnableLogsCollection collection = new JIPipeDesktopRunnableLogsCollection();
        JIPipeRunnableLogEntry first = createEntryWithData("first");
        JIPipeRunnableLogEntry second = createEntryWithData("second");
        collection.insertEntry(first, 1);
        collection.insertEntry(second, 1);
        assertEquals(1, collection.getLogEntries().size());
        assertSame(second, collection.getLogEntries().get(0));
        assertFalse(Files.exists(first.getPath()), "the evicted entry's data file must be deleted (disk bound)");
        assertTrue(Files.exists(second.getPath()), "the survivor's data file must be kept");
    }

    @Test
    void clearDeletesFiles() {
        JIPipeDesktopRunnableLogsCollection collection = new JIPipeDesktopRunnableLogsCollection();
        JIPipeRunnableLogEntry first = createEntryWithData("first");
        JIPipeRunnableLogEntry second = createEntryWithData("second");
        collection.insertEntry(first, Integer.MAX_VALUE);
        collection.insertEntry(second, Integer.MAX_VALUE);
        collection.clear();
        assertTrue(collection.getLogEntries().isEmpty());
        assertFalse(Files.exists(first.getPath()), "clear must delete all entry data files");
        assertFalse(Files.exists(second.getPath()));
    }

    @Test
    void renderSurvivesMissingFile() throws IOException {
        JIPipeRunnableLogEntry entry = createEntryWithData("vanishing");
        Path file = entry.getPath();
        assertNotNull(file);
        Files.delete(file);
        List<String> lines = new ArrayList<>();
        entry.renderText(lines::add);
        assertEquals(1, lines.size(), "a missing file must render the placeholder line only");
        assertTrue(lines.get(0).contains("unavailable"), "unexpected placeholder line: " + lines.get(0));
    }

    @Test
    void silentStoreEntryRendersFromStoreTail() {
        // Non-persistent stores (silent/stdout) have no file — entries render from the store's
        // bounded in-memory tail instead
        JIPipeProgressLog silent = JIPipeProgressLog.silent();
        silent.log(silent.getRootNodeId(), JIPipeLogLevel.INFO, "tail content");
        JIPipeRunnableLogEntry entry = new JIPipeRunnableLogEntry(new JIPipeProgressInfo(silent), "silent", true);
        assertNull(entry.getPath());
        List<String> lines = new ArrayList<>();
        entry.renderText(lines::add);
        assertTrue(lines.stream().anyMatch(line -> line.contains("tail content")));
    }

    @Test
    void entryWithoutDataRendersPlaceholder() {
        // A silent store without records renders nothing — the entry-level render must still
        // produce the placeholder line (never an empty view, never an exception)
        JIPipeRunnableLogEntry entry = new JIPipeRunnableLogEntry(new JIPipeProgressInfo(JIPipeProgressLog.silent()), "empty", true);
        assertNull(entry.getPath());
        List<String> lines = new ArrayList<>();
        entry.renderText(lines::add);
        assertEquals(1, lines.size());
        assertTrue(lines.get(0).contains("unavailable"));
    }

    @Test
    void deleteDataFileIsSafeWithoutFile() {
        JIPipeRunnableLogEntry entry = new JIPipeRunnableLogEntry(new JIPipeProgressInfo(JIPipeProgressLog.silent()), "silent", true);
        assertNull(entry.getPath());
        entry.deleteDataFile();
        List<String> lines = new ArrayList<>();
        entry.renderText(lines::add);
        assertEquals(1, lines.size());
        assertTrue(lines.get(0).contains("unavailable"));
    }

    @Test
    void renderFileToleratesTruncatedLastLine() throws IOException {
        // The reader tolerates a truncated final line (writer killed mid-flush)
        Path file = tmp.resolve("truncated.ndjson");
        JIPipeProgressLog store = JIPipeProgressLog.create(file, UUID.randomUUID().toString(), "truncated");
        store.log(store.getRootNodeId(), JIPipeLogLevel.INFO, "complete line");
        store.finish(JIPipeProgressNodeState.OK);
        Files.write(file, (Files.readString(file) + "{\"t\":\"l\"").getBytes(StandardCharsets.UTF_8));
        List<String> lines = new ArrayList<>();
        JIPipeProgressLog.renderFile(file, JIPipeLogLevel.INFO, lines::add);
        assertTrue(lines.stream().anyMatch(line -> line.contains("complete line")));
    }
}
