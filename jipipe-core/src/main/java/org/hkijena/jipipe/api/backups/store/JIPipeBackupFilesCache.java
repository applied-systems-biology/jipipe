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

import com.fasterxml.jackson.annotation.JsonGetter;
import com.fasterxml.jackson.annotation.JsonSetter;
import org.hkijena.jipipe.utils.json.JsonUtils;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.nio.file.attribute.FileTime;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

/**
 * Per-session speed cache for the content-addressed backup store, serialized
 * as JSON at {@code <backupRoot>/files-cache/<session-id>.json}:
 * <pre>
 * {
 *   "entries": {
 *     "project/graph.json": { "size": 12345, "mtime": 1700000000000, "hash": "&lt;sha256&gt;" },
 *     ...
 *   }
 * }
 * </pre>
 * Maps a workspace-relative path to the file's size, mtime and content hash.
 * At backup time: an entry whose size AND mtime match the file on disk is a
 * hit — the cached hash is reused without reading the file; a mismatch or a
 * missing entry is a miss, the file is hashed and recorded.
 * <p>
 * This is only a speed cache, never a source of truth: a missing or corrupt
 * file degrades to an empty cache, and restore re-verifies every hash against
 * the object store.
 */
public class JIPipeBackupFilesCache {
    private Map<String, CachedInfo> entries = new LinkedHashMap<>();

    /**
     * Loads the cache from the given file.
     *
     * @param file the cache file (may not exist)
     * @return the loaded cache, or an empty cache if the file is missing or unparseable
     */
    public static JIPipeBackupFilesCache load(Path file) {
        if (!Files.isRegularFile(file))
            return new JIPipeBackupFilesCache();
        try {
            JIPipeBackupFilesCache cache = JsonUtils.readFromFile(file, JIPipeBackupFilesCache.class);
            return cache != null ? cache : new JIPipeBackupFilesCache();
        } catch (RuntimeException e) {
            // A corrupt cache is not an error: start fresh (it is only a speed cache)
            return new JIPipeBackupFilesCache();
        }
    }

    /**
     * Saves the cache atomically: writes to a temp file in the same directory,
     * then renames it over the target. No torn file is ever visible.
     *
     * @param file the cache file to write (parent directories are created as needed)
     */
    public void save(Path file) {
        try {
            Path parent = file.getParent() != null ? file.getParent() : Path.of(".");
            Files.createDirectories(parent);
            // Temp file in the SAME directory as the target (same file system,
            // required for the atomic move)
            Path tempFile = parent.resolve(".tmp-" + UUID.randomUUID());
            try {
                JsonUtils.saveToFile(this, tempFile);
                try {
                    Files.move(tempFile, file, StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING);
                } catch (AtomicMoveNotSupportedException e) {
                    // Same directory, so this is only a theoretical fallback
                    Files.move(tempFile, file, StandardCopyOption.REPLACE_EXISTING);
                }
            } finally {
                // No .tmp- leftovers on success or failure
                try {
                    Files.deleteIfExists(tempFile);
                } catch (IOException ignored) {
                }
            }
        } catch (IOException e) {
            throw new UncheckedIOException("Could not save files cache " + file, e);
        }
    }

    /**
     * Looks up the cached hash of a file that may be unchanged.
     *
     * @param relativePath the workspace-relative path (with {@code /} separators)
     * @param size         the current size of the file in bytes
     * @param mtime        the current modification time of the file
     * @return the cached hash if the entry matches both size and mtime (a hit —
     * the file may be reused without re-reading it), or empty if the entry is
     * absent or changed (a miss — the caller must hash the file and record)
     */
    public Optional<String> hashIfUnchanged(String relativePath, long size, FileTime mtime) {
        CachedInfo info = getEntries().get(relativePath);
        if (info == null || info.getMtime() != mtime.toMillis() || info.getSize() != size)
            return Optional.empty();
        return Optional.ofNullable(info.getHash());
    }

    /**
     * Records (or updates) the hash of a file after it has been hashed.
     *
     * @param relativePath the workspace-relative path (with {@code /} separators)
     * @param size         the size of the file in bytes at hashing time
     * @param mtime        the modification time of the file at hashing time
     * @param hash         the SHA-256 hex of the file's exact content
     */
    public void record(String relativePath, long size, FileTime mtime, String hash) {
        CachedInfo info = new CachedInfo();
        info.setSize(size);
        info.setMtime(mtime.toMillis());
        info.setHash(hash);
        getEntries().put(relativePath, info);
    }

    /**
     * @return the path → metadata map (never null)
     */
    @JsonGetter("entries")
    public Map<String, CachedInfo> getEntries() {
        if (entries == null)
            entries = new LinkedHashMap<>();
        return entries;
    }

    @JsonSetter("entries")
    public void setEntries(Map<String, CachedInfo> entries) {
        this.entries = entries != null ? entries : new LinkedHashMap<>();
    }

    /**
     * The cached metadata of one file: size (bytes), mtime (milliseconds since
     * epoch) and the SHA-256 hash of its exact content.
     */
    public static class CachedInfo {
        private long size;
        private long mtime;
        private String hash;

        @JsonGetter("size")
        public long getSize() {
            return size;
        }

        @JsonSetter("size")
        public void setSize(long size) {
            this.size = size;
        }

        @JsonGetter("mtime")
        public long getMtime() {
            return mtime;
        }

        @JsonSetter("mtime")
        public void setMtime(long mtime) {
            this.mtime = mtime;
        }

        @JsonGetter("hash")
        public String getHash() {
            return hash;
        }

        @JsonSetter("hash")
        public void setHash(String hash) {
            this.hash = hash;
        }
    }
}
