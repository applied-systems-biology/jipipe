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

package org.hkijena.jipipe.api.settings;

import com.fasterxml.jackson.databind.JsonNode;
import org.hkijena.jipipe.utils.json.JsonUtils;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.time.Instant;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.Objects;
import java.util.function.Predicate;

/**
 * File-based storage for a single scoped {@link SettingsDocument}.
 * <p>
 * Writes are atomic: the document is serialized to a temporary file in the
 * target's directory ({@code <file>.tmp.<writerId>}) and moved into place with
 * {@link StandardCopyOption#ATOMIC_MOVE} (falling back to a plain replace
 * move on file systems that do not support atomic moves). A crash can
 * therefore never leave a half-written settings file: readers always see
 * either the old or the new content.
 * <p>
 * Recovery: a settings file that cannot be read — truncated, half-written,
 * or simply not valid JSON — is preserved as {@code <name>.bad-<timestamp>}
 * (timestamp in a filename-safe ISO basic format) and the store then returns
 * an empty document. {@link #load()} never throws: even if the backup move
 * itself fails (e.g. locked file), the failure is logged and an empty
 * document is returned.
 * <p>
 * Temp files that a crashed writer left behind are removed by
 * {@link #cleanUpOrphanTempFiles(Path)}, which is meant to be called at
 * startup: it deletes {@code *.tmp.*} files in a directory that have not been
 * modified within the last 24 hours, and leaves fresher ones alone.
 */
public final class SettingsFileStore {
    private static final String TMP_MARKER = ".tmp.";
    private static final String BAD_MARKER = ".bad-";
    /**
     * Temp files not modified within this window are considered orphans of a
     * crashed writer and may be deleted.
     */
    private static final long ORPHAN_TEMP_FILE_AGE_MILLIS = 24L * 60 * 60 * 1000;
    /**
     * Filename-safe ISO instant for backup names, e.g.
     * {@code 2026-10-27T22-30-00Z} (colons are illegal in Windows file names).
     */
    private static final DateTimeFormatter BACKUP_TIMESTAMP =
            DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH-mm-ss'Z'").withZone(ZoneOffset.UTC);

    private final Path file;
    private final SettingsScope scope;
    private final Predicate<String> registeredBlock;

    /**
     * Creates a store for the given settings file.
     *
     * @param file the settings file to load from and save to
     * @param scope the scope the document at {@code file} belongs to
     * @param registeredBlock a predicate that receives a canonical block path
     *                        and returns {@code true} when the block is
     *                        registered in this version
     */
    public SettingsFileStore(Path file, SettingsScope scope, Predicate<String> registeredBlock) {
        this.file = Objects.requireNonNull(file, "file");
        this.scope = Objects.requireNonNull(scope, "scope");
        this.registeredBlock = Objects.requireNonNull(registeredBlock, "registeredBlock");
    }

    /**
     * @return the settings file this store reads from and writes to
     */
    public Path file() {
        return file;
    }

    /**
     * Loads the settings document from disk.
     * <p>
     * A missing file yields a fresh empty document. An unreadable or corrupt
     * file is preserved as {@code <name>.bad-<timestamp>} and a fresh empty
     * document is returned. This method never throws: if the backup move
     * itself fails, the problem is logged and an empty document is returned
     * as well.
     *
     * @return the loaded document, or a fresh empty document
     */
    public SettingsDocument load() {
        if (!Files.isRegularFile(file)) {
            return emptyDocument();
        }
        try {
            String content = Files.readString(file, StandardCharsets.UTF_8);
            JsonNode node = JsonUtils.getObjectMapper().readTree(content);
            return SettingsDocumentIO.fromJson(scope, node, registeredBlock);
        } catch (IOException e) {
            return backupAndLog(e, "could not read settings file " + file);
        } catch (RuntimeException e) {
            // Jackson parse errors and non-object documents surface as RuntimeExceptions
            return backupAndLog(e, "settings file " + file + " is not valid settings JSON");
        }
    }

    /**
     * Atomically saves the document to disk: the pretty-printed JSON is
     * written to {@code <file>.tmp.<writerId>} in the same directory and then
     * moved over the target with an atomic rename. The target's parent
     * directories are created when missing. On any failure the temporary
     * file is deleted and the target file is left untouched.
     *
     * @param doc the document to save
     */
    public void save(SettingsDocument doc) {
        Objects.requireNonNull(doc, "doc");
        Path directory = file.toAbsolutePath().getParent();
        if (directory != null) {
            try {
                Files.createDirectories(directory);
            } catch (IOException e) {
                throw new UncheckedIOException("Could not create settings directory " + directory, e);
            }
        }
        Path tmp = file.resolveSibling(file.getFileName() + TMP_MARKER + doc.writerId());
        try {
            Files.writeString(tmp, SettingsDocumentIO.toPrettyString(doc), StandardCharsets.UTF_8);
            try {
                Files.move(tmp, file, StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING);
            } catch (AtomicMoveNotSupportedException e) {
                Files.move(tmp, file, StandardCopyOption.REPLACE_EXISTING);
            }
        } catch (IOException e) {
            try {
                Files.deleteIfExists(tmp);
            } catch (IOException ignored) {
                // the orphan cleanup at the next startup will take care of it
            }
            throw new UncheckedIOException("Could not atomically save settings file " + file, e);
        }
    }

    /**
     * Computes a fingerprint of the settings file, so a caller can detect
     * whether the file changed on disk since a previous fingerprint.
     *
     * @return the file's size and last-modified time, or {@code null} if the
     * file does not exist (so a first save is detectable as a change)
     */
    public FileFingerprint fingerprint() {
        try {
            return FileFingerprint.of(file);
        } catch (IOException e) {
            return null;
        }
    }

    /**
     * Deletes orphaned temp files in the given directory: files whose name
     * contains {@code .tmp.} and that have not been modified within the last
     * 24 hours. The name of the settings file itself plays no role, so a
     * stale temp file of <em>any</em> settings file in the directory is
     * covered. Temp files that were touched more recently are left alone, as
     * they may still belong to a live writer. Files that fail to delete are
     * skipped.
     *
     * @param directory the directory to clean up, usually the settings directory
     */
    public static void cleanUpOrphanTempFiles(Path directory) {
        Objects.requireNonNull(directory, "directory");
        if (!Files.isDirectory(directory)) {
            return;
        }
        Instant cutoff = Instant.now().minusMillis(ORPHAN_TEMP_FILE_AGE_MILLIS);
        try (DirectoryStream<Path> stream = Files.newDirectoryStream(directory)) {
            for (Path entry : stream) {
                String name = entry.getFileName().toString();
                if (!name.contains(TMP_MARKER) || !Files.isRegularFile(entry)) {
                    continue;
                }
                try {
                    if (Files.getLastModifiedTime(entry).toInstant().isBefore(cutoff)) {
                        Files.deleteIfExists(entry);
                    }
                } catch (IOException e) {
                    System.err.println("SettingsFileStore: could not clean up orphan temp file " + entry + ": " + e);
                }
            }
        } catch (IOException e) {
            System.err.println("SettingsFileStore: could not list orphan temp files in " + directory + ": " + e);
        }
    }

    private SettingsDocument emptyDocument() {
        return new SettingsDocument(scope, registeredBlock);
    }

    private SettingsDocument backupAndLog(Throwable cause, String message) {
        Path backup = file.resolveSibling(file.getFileName() + BAD_MARKER + BACKUP_TIMESTAMP.format(Instant.now()));
        try {
            Files.move(file, backup, StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException moveError) {
            System.err.println("SettingsFileStore: " + message + " (" + cause + "), "
                    + "and the backup move to " + backup + " also failed (" + moveError + "); "
                    + "continuing with an empty document");
        }
        return emptyDocument();
    }

    /**
     * A lightweight snapshot of a settings file's identity on disk, used to
     * detect changes since a previous snapshot.
     *
     * @param size the file size in bytes
     * @param lastModifiedMillis the last-modified time in epoch milliseconds
     */
    public record FileFingerprint(long size, long lastModifiedMillis) {
        /**
         * Computes the fingerprint of the given file.
         *
         * @param file the file to fingerprint
         * @return the file's size and last-modified time
         * @throws IOException if the file does not exist or an I/O error occurs
         */
        public static FileFingerprint of(Path file) throws IOException {
            return new FileFingerprint(Files.size(file), Files.getLastModifiedTime(file).toMillis());
        }
    }
}
