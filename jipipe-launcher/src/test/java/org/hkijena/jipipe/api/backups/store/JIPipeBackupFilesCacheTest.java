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

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.FileTime;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The per-session files cache: hit/miss semantics on size+mtime, and the
 * JSON round-trip. A corrupt cache file degrades to an empty cache — it is a
 * speed cache, never a source of truth.
 * <p>
 * Pure data/IO tests — no JIPipe registry / ImageJ involved.
 */
class JIPipeBackupFilesCacheTest {

    private static final long SIZE = 12345L;
    private static final FileTime MTIME = FileTime.fromMillis(1_700_000_000_000L);
    private static final String HASH = "a".repeat(64);

    @Test
    void missWhenNotRecorded() {
        JIPipeBackupFilesCache cache = new JIPipeBackupFilesCache();
        assertTrue(cache.hashIfUnchanged("project.jip", SIZE, MTIME).isEmpty(),
                "an empty cache must be a miss (the caller has to hash the file)");
    }

    @Test
    void hitWhenSizeAndMtimeMatch() {
        JIPipeBackupFilesCache cache = new JIPipeBackupFilesCache();
        cache.record("project.jip", SIZE, MTIME, HASH);

        Optional<String> hit = cache.hashIfUnchanged("project.jip", SIZE, MTIME);
        assertTrue(hit.isPresent(), "size + mtime match must be a hit (no re-read of the file)");
        assertEquals(HASH, hit.get());
    }

    @Test
    void missWhenSizeChanged() {
        JIPipeBackupFilesCache cache = new JIPipeBackupFilesCache();
        cache.record("project.jip", SIZE, MTIME, HASH);

        assertTrue(cache.hashIfUnchanged("project.jip", SIZE + 1, MTIME).isEmpty(),
                "a size change must be a miss");
    }

    @Test
    void missWhenMtimeChanged() {
        JIPipeBackupFilesCache cache = new JIPipeBackupFilesCache();
        cache.record("project.jip", SIZE, MTIME, HASH);

        assertTrue(cache.hashIfUnchanged("project.jip", SIZE, FileTime.fromMillis(MTIME.toMillis() + 1)).isEmpty(),
                "an mtime change must be a miss");
    }

    @Test
    void loadNonexistentFileReturnsEmptyCache(@TempDir Path tempDir) {
        JIPipeBackupFilesCache cache = JIPipeBackupFilesCache.load(tempDir.resolve("no-such-file.json"));
        assertTrue(cache.hashIfUnchanged("project.jip", SIZE, MTIME).isEmpty(),
                "loading a missing cache file must yield an empty cache, not null or an exception");
    }

    @Test
    void loadCorruptFileReturnsEmptyCache(@TempDir Path tempDir) throws Exception {
        Path file = tempDir.resolve("corrupt.json");
        Files.writeString(file, "{this is not valid json");
        JIPipeBackupFilesCache cache = JIPipeBackupFilesCache.load(file);
        assertTrue(cache.hashIfUnchanged("project.jip", SIZE, MTIME).isEmpty(),
                "a corrupt cache file must degrade to an empty cache (it is only a speed cache)");
    }

    @Test
    void saveLoadRoundTrip(@TempDir Path tempDir) throws Exception {
        JIPipeBackupFilesCache cache = new JIPipeBackupFilesCache();
        cache.record("project.jip", SIZE, MTIME, HASH);
        cache.record("data/scan001.tiff", 999L, FileTime.fromMillis(42L), "b".repeat(64));

        Path file = tempDir.resolve("files-cache").resolve("session-1.json");
        cache.save(file);

        JIPipeBackupFilesCache loaded = JIPipeBackupFilesCache.load(file);
        Optional<String> hit1 = loaded.hashIfUnchanged("project.jip", SIZE, MTIME);
        assertTrue(hit1.isPresent(), "recorded entry must survive save + load");
        assertEquals(HASH, hit1.get());

        Optional<String> hit2 = loaded.hashIfUnchanged("data/scan001.tiff", 999L, FileTime.fromMillis(42L));
        assertTrue(hit2.isPresent(), "second recorded entry must survive save + load");
        assertEquals("b".repeat(64), hit2.get());

        // An entry whose metadata no longer matches is still a miss after reload
        assertTrue(loaded.hashIfUnchanged("project.jip", SIZE + 1, MTIME).isEmpty());
    }

    @Test
    void jsonShapeHasTopLevelEntriesMap(@TempDir Path tempDir) throws Exception {
        JIPipeBackupFilesCache cache = new JIPipeBackupFilesCache();
        cache.record("project.jip", SIZE, MTIME, HASH);

        Path file = tempDir.resolve("shape.json");
        cache.save(file);
        String json = Files.readString(file);

        assertTrue(json.contains("\"entries\""), "top-level object must use the 'entries' member, got: " + json);
        assertTrue(json.contains("\"path-a\"") || json.contains("\"project.jip\""),
                "the map must be keyed by the relative path, got: " + json);
        assertTrue(json.contains("\"size\"") && json.contains("\"mtime\"") && json.contains("\"hash\""),
                "each entry must contain size, mtime and hash, got: " + json);
        assertFalse(json.contains("\"entries\":["), "entries must be a map, not a list, got: " + json);
    }
}
