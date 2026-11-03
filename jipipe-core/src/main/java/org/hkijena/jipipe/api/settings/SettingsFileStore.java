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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.time.Instant;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.function.Predicate;

/**
 * File-based storage for a single scoped {@link SettingsDocument}.
 * <p>
 * Writes are atomic: the document is serialized to a temporary file in the
 * target's directory ({@code <file>.tmp.<writerId>}) and moved into place with
 * {@link StandardCopyOption#ATOMIC_MOVE} (falling back to a plain replace
 * move on file systems that do not support atomic moves). A <b>process</b>
 * crash (the JVM dies, the process is killed) therefore never leaves a
 * half-written settings file: readers always see either the old or the new
 * content. A <b>power loss or a kernel/filesystem crash</b> can still lose
 * the rename's metadata or the temp file's data unless the file system
 * journals the data (the file is not fsync-ed), so the guarantee is
 * process-crash safety, not durability against power loss.
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
    private static final Logger logger = LoggerFactory.getLogger(SettingsFileStore.class);
    private static final String TMP_MARKER = ".tmp.";
    private static final String BAD_MARKER = ".bad-";
    /**
     * How many {@code .bad-<timestamp>} backups of one base file name are
     * kept. A profile that repeatedly trips the corruption path would
     * otherwise accumulate them without bound.
     */
    private static final int MAX_BAD_BACKUPS_PER_FILE = 5;
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

    /**
     * Package-visible for the backup-pruning test: 7 backups of one base name
     * leave exactly {@link #MAX_BAD_BACKUPS_PER_FILE} (the newest).
     *
     * @return the number of backups kept per base file name
     */
    static int maxBadBackupsPerFile() {
        return MAX_BAD_BACKUPS_PER_FILE;
    }

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
        } catch (IOException | IllegalArgumentException e) {
            // Exactly the failures a genuinely unreadable/corrupt file
            // produces: an I/O error (which covers Jackson's
            // JsonProcessingException — it extends IOException), or a document
            // that is not a JSON object (fromJson's IllegalArgumentException). Any
            // OTHER RuntimeException is a code bug (an NPE, a bad predicate,
            // a missing mapper module) and must propagate instead of being
            // laundered into a ".bad-" backup.
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
     * <p>
     * <b>Contract: {@code null} means "the file does not exist" and nothing
     * else.</b> The watcher treats {@code null} as absence (it waits for the
     * next successful read), so mapping a permission or file-system error onto
     * {@code null} would silently swallow a foreign change and then report the
     * next readable state as a spurious change. Such an error is therefore
     * logged and rethrown as an {@link UncheckedIOException}.
     *
     * @return the file's size and last-modified time, or {@code null} if the
     * file does not exist (so a first save is detectable as a change)
     * @throws java.io.UncheckedIOException if the file exists but cannot be
     * fingerprint-ed (permissions, I/O error)
     */
    public FileFingerprint fingerprint() {
        if (!Files.isRegularFile(file)) {
            return null; // genuine absence
        }
        try {
            return FileFingerprint.of(file);
        } catch (NoSuchFileException e) {
            return null; // deleted between the check and the read: still absence
        } catch (IOException e) {
            logger.warn("Could not fingerprint settings file {} (it exists, so this is NOT an absence)", file, e);
            throw new UncheckedIOException("Could not fingerprint settings file " + file, e);
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
                    logger.warn("Could not clean up orphan temp file {}", entry, e);
                }
            }
        } catch (IOException e) {
            logger.warn("Could not list orphan temp files in {}", directory, e);
        }
    }

    /**
     * Keeps at most {@link #MAX_BAD_BACKUPS_PER_FILE} {@code .bad-*} backups
     * of this store's base file name, deleting the oldest (by last-modified
     * time, then by name so equal timestamps are deterministic). Only this
     * file's backups are considered: other base names in the same directory
     * (e.g. {@code runtime-settings.json}) are not touched.
     * <p>
     * Best effort: a file that cannot be deleted is logged and skipped.
     */
    void pruneBadBackups() {
        Path dir = file.toAbsolutePath().getParent();
        if (dir == null) {
            return;
        }
        String prefix = file.getFileName().toString() + BAD_MARKER;
        List<Path> backups = new ArrayList<>();
        try (DirectoryStream<Path> stream = Files.newDirectoryStream(dir, prefix + "*")) {
            for (Path entry : stream) {
                if (Files.isRegularFile(entry)) {
                    backups.add(entry);
                }
            }
        } catch (IOException e) {
            logger.warn("Could not list settings backups in {}", dir, e);
            return;
        }
        if (backups.size() <= MAX_BAD_BACKUPS_PER_FILE) {
            return;
        }
        backups.sort(Comparator.comparing((Path p) -> lastModifiedOrEpoch(p))
                .thenComparing(p -> p.getFileName().toString()));
        int excess = backups.size() - MAX_BAD_BACKUPS_PER_FILE;
        for (int i = 0; i < excess; i++) {
            try {
                Files.deleteIfExists(backups.get(i));
            } catch (IOException e) {
                logger.warn("Could not delete old settings backup {}", backups.get(i), e);
            }
        }
    }

    private static long lastModifiedOrEpoch(Path path) {
        try {
            return Files.getLastModifiedTime(path).toMillis();
        } catch (IOException e) {
            return 0L;
        }
    }

    private SettingsDocument emptyDocument() {
        return new SettingsDocument(scope, registeredBlock);
    }

    private SettingsDocument backupAndLog(Throwable cause, String message) {
        Path backup = file.resolveSibling(file.getFileName() + BAD_MARKER + BACKUP_TIMESTAMP.format(Instant.now()));
        try {
            Files.move(file, backup, StandardCopyOption.REPLACE_EXISTING);
            pruneBadBackups();
        } catch (IOException moveError) {
            logger.warn("{} ({}), and the backup move to {} also failed ({}); continuing with an empty document",
                    message, cause.toString(), backup, moveError.toString());
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
    /**
     * Renames this store's file to a {@code .bad-<timestamp>} backup and
     * prunes the old backups, preserving the broken content. This is the
     * backup path for a corrupt file encountered OUTSIDE the store's own
     * {@link #load()} (the foreign-merge recovery in the settings service),
     * so that every {@code .bad-*} creation goes through the same bound as
     * the load path (at most {@link #MAX_BAD_BACKUPS_PER_FILE} per base
     * name).
     * <p>
     * Best effort: a failed rename is logged and the file is left in place
     * (the caller already keeps its local state).
     */
    public void backupCorruptFile() {
        Path backup = file.resolveSibling(file.getFileName() + BAD_MARKER + BACKUP_TIMESTAMP.format(Instant.now()));
        try {
            Files.move(file, backup, StandardCopyOption.REPLACE_EXISTING);
            pruneBadBackups();
        } catch (IOException e) {
            logger.warn("Could not back up the corrupt settings file {} to {}", file, backup, e);
        }
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
