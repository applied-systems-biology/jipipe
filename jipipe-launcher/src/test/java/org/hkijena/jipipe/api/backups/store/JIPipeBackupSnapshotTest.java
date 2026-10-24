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

package org.hkijena.jipipe.api.backups.store;

import org.hkijena.jipipe.utils.json.JsonUtils;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * The snapshot manifest model: kebab-case JSON round-trip, entry-order
 * preservation, and total-size summation.
 * <p>
 * Pure data tests — no JIPipe registry / ImageJ involved.
 */
class JIPipeBackupSnapshotTest {

    private static JIPipeBackupSnapshot.SnapshotEntry entry(String path, long size, String hash) {
        JIPipeBackupSnapshot.SnapshotEntry e = new JIPipeBackupSnapshot.SnapshotEntry();
        e.setPath(path);
        e.setSize(size);
        e.setHash(hash);
        return e;
    }

    @Test
    void jsonRoundTripPreservesFieldsAndEntryOrder(@TempDir Path tempDir) {
        JIPipeBackupSnapshot snapshot = new JIPipeBackupSnapshot();
        snapshot.setSnapshotId("11111111-2222-3333-4444-555555555555");
        snapshot.setCreated("2026-10-24T12:34:56");
        snapshot.setProjectFormat("jipx");
        snapshot.setProjectName("my-project.jipx");
        // Pre-sorted by path (the creator's job, Task 3); the model must preserve it.
        snapshot.getEntries().add(entry("data/scan001.tiff", 2147483648L, "hash-a"));
        snapshot.getEntries().add(entry("project/graph.json", 12345L, "hash-b"));

        Path file = tempDir.resolve("snapshot.json");
        JsonUtils.saveToFile(snapshot, file);
        JIPipeBackupSnapshot loaded = JsonUtils.readFromFile(file, JIPipeBackupSnapshot.class);

        assertEquals("11111111-2222-3333-4444-555555555555", loaded.getSnapshotId());
        assertEquals("2026-10-24T12:34:56", loaded.getCreated());
        assertEquals("jipx", loaded.getProjectFormat());
        assertEquals("my-project.jipx", loaded.getProjectName());

        List<JIPipeBackupSnapshot.SnapshotEntry> entries = loaded.getEntries();
        assertEquals(2, entries.size(), "entry count must survive the round trip");
        // Order preserved as stored
        assertEquals("data/scan001.tiff", entries.get(0).getPath());
        assertEquals(2147483648L, entries.get(0).getSize());
        assertEquals("hash-a", entries.get(0).getHash());
        assertEquals("project/graph.json", entries.get(1).getPath());
        assertEquals(12345L, entries.get(1).getSize());
        assertEquals("hash-b", entries.get(1).getHash());
    }

    @Test
    void totalSizeSumsEntrySizes() {
        JIPipeBackupSnapshot empty = new JIPipeBackupSnapshot();
        assertEquals(0, empty.getTotalSize(), "no entries -> total size 0");

        JIPipeBackupSnapshot snapshot = new JIPipeBackupSnapshot();
        snapshot.getEntries().add(entry("a", 10L, "h1"));
        snapshot.getEntries().add(entry("b", 20L, "h2"));
        snapshot.getEntries().add(entry("c", 3000000000L, "h3"));
        assertEquals(3000000030L, snapshot.getTotalSize(), "sum of all entry sizes");
    }

    @Test
    void defaultsAreSafe() {
        JIPipeBackupSnapshot snapshot = new JIPipeBackupSnapshot();
        assertEquals(0, snapshot.getEntries().size(), "entries defaults to an empty list, not null");
        assertEquals(0, snapshot.getTotalSize());
    }
}
