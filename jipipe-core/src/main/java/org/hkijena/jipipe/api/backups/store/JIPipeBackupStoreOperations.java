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

import org.hkijena.jipipe.api.JIPipeProgressInfo;
import org.hkijena.jipipe.api.backups.JIPipeProjectBackupSessionInfo;
import org.hkijena.jipipe.api.project.JIPipeProject;
import org.hkijena.jipipe.api.project.jipx.JIPipeProjectWorkspace;
import org.hkijena.jipipe.utils.json.JsonUtils;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.nio.file.attribute.FileTime;
import java.security.DigestInputStream;
import java.security.MessageDigest;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HexFormat;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;
import java.util.stream.Stream;

/**
 * Snapshot creation for both project formats and snapshot persistence
 * (spec §3.6). Pure static operations on the {@link JIPipeBackupObjectStore},
 * so the backup run (Task 5) only wires them up — no window access here.
 * <p>
 * Concurrency: the create and persist operations are
 * <b>caller-holds-lock</b> — the store write lock is the OUTER lock and
 * must be held across the whole create + persist sequence (Task 5's run
 * acquires it once and releases it after {@link #persistSnapshot}). The
 * jipx create additionally takes the workspace monitor
 * ({@code synchronized (workspace)}) INSIDE that — around materialize +
 * hash — so a concurrent save can never mutate a part file mid-hash.
 * Lock nesting is always store → workspace: the JIPX writer path only ever
 * takes the workspace lock (never the store lock), so no code path acquires
 * the pair in the opposite order and deadlock is impossible.
 * <p>
 * The restore and verify operations are <b>lock-FREE</b>: they only read
 * the immutable objects ({@link JIPipeBackupObjectStore#get(String)} is
 * lock-free by design) and write to user-chosen paths OUTSIDE the store,
 * so a backup run holding the write lock can never block a restore or a
 * verification (and vice versa).
 */
public final class JIPipeBackupStoreOperations {

    /** The single entry path of a .jip snapshot (metadata only; the entry
     * count of 1 + the format tag drive restore). */
    public static final String JIP_ENTRY_PATH = "project.jip";

    /** The project format tag of a .jip snapshot. */
    public static final String FORMAT_JIP = "jip";

    /** The project format tag of a .jipx snapshot. */
    public static final String FORMAT_JIPX = "jipx";

    private static final HexFormat HEX = HexFormat.of();

    private JIPipeBackupStoreOperations() {
    }

    /**
     * Creates a snapshot for a {@code .jip} project: serializes the model via
     * the stream writer ({@code project.saveProject(OutputStream)} — the
     * frozen #753 asymmetry: stream writes do NOT merge authors, exactly what
     * a backup wants) and stores it as a single {@value #JIP_ENTRY_PATH} entry.
     * The serialized bytes live in a {@link ByteArrayOutputStream} first —
     * .jip models are MBs, not GBs, so the memory footprint is fine.
     * <p>
     * MUST be called while holding the store write lock.
     *
     * @param store        the object store
     * @param project      the project whose model is backed up
     * @param projectName  the original project file name (e.g. {@code "my-project.jip"})
     * @param progressInfo progress logging / cancellation
     * @return the created snapshot (not yet persisted)
     * @throws IOException if the model cannot be serialized or stored
     */
    public static JIPipeBackupSnapshot createJipSnapshot(
            JIPipeBackupObjectStore store, JIPipeProject project,
            String projectName, JIPipeProgressInfo progressInfo) throws IOException {
        progressInfo.log("Creating .jip backup snapshot of \"" + projectName + "\"");

        ByteArrayOutputStream serialized = new ByteArrayOutputStream();
        project.saveProject(serialized);
        byte[] bytes = serialized.toByteArray();
        String hash = store.put(new ByteArrayInputStream(bytes), bytes.length);

        JIPipeBackupSnapshot snapshot = new JIPipeBackupSnapshot();
        snapshot.setSnapshotId(UUID.randomUUID().toString());
        snapshot.setCreated(LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME));
        snapshot.setProjectFormat(FORMAT_JIP);
        snapshot.setProjectName(projectName);
        JIPipeBackupSnapshot.SnapshotEntry entry = new JIPipeBackupSnapshot.SnapshotEntry();
        entry.setPath(JIP_ENTRY_PATH);
        entry.setSize(bytes.length);
        entry.setHash(hash);
        snapshot.getEntries().add(entry);
        return snapshot;
    }

    /**
     * Creates a snapshot for a {@code .jipx} project: under the workspace
     * monitor, {@code materializeFromModel} refreshes the part files from the
     * live model (captures UNSAVED edits; payload files are never touched),
     * then every workspace file is hashed and stored — the
     * {@code filesCache} is consulted first, so an unchanged file (size +
     * mtime match) reuses its cached hash without being re-read.
     * Snapshot entries list every file, sorted by path.
     * <p>
     * The workspace monitor ({@code synchronized (workspace)}) covers the
     * materialization AND the hash/put loop: a concurrent save must not
     * mutate a part file mid-hash. The monitor is taken INSIDE the
     * caller-held store lock (nesting: store → workspace, see class Javadoc).
     * <p>
     * MUST be called while holding the store write lock.
     *
     * @param store        the object store
     * @param project      the project whose workspace is backed up
     * @param filesCache   the per-session files cache (hits skip the re-read)
     * @param projectName  the original project file name (e.g. {@code "my-project.jipx"})
     * @param progressInfo progress logging / cancellation
     * @return the created snapshot (not yet persisted)
     * @throws IOException if the workspace cannot be materialized or a file cannot be stored
     */
    public static JIPipeBackupSnapshot createJipxSnapshot(
            JIPipeBackupObjectStore store, JIPipeProject project,
            JIPipeBackupFilesCache filesCache, String projectName,
            JIPipeProgressInfo progressInfo) throws IOException {
        JIPipeProjectWorkspace workspace = project.getWorkspace();
        if (workspace == null) {
            throw new IOException("The project is closed — its workspace is unavailable, no snapshot can be created");
        }

        List<JIPipeBackupSnapshot.SnapshotEntry> entries = new ArrayList<>();
        // The whole workspace-touching sequence — materialize + walk +
        // hash/put — under the workspace monitor, exactly like the JIPX
        // writer's save sequence.
        synchronized (workspace) {
            // (a) Refresh the part files from the in-memory model: unsaved
            // edits are captured, payload files survive untouched.
            workspace.materializeFromModel(progressInfo.resolve("Materialize JIPX workspace for backup"));

            // (b) Collect the regular files with their current size/mtime.
            Path baseDirectory = workspace.getBaseDirectory();
            List<Path> files = new ArrayList<>();
            if (Files.isDirectory(baseDirectory)) {
                try (Stream<Path> walk = Files.walk(baseDirectory)) {
                    walk.filter(Files::isRegularFile)
                            .sorted()
                            .forEach(files::add);
                }
            }

            // (c) Hash + store each file (cache consulted first; on a miss,
            // store.put reads the file exactly once — no separate pre-hash
            // pass, the store dedups anyway).
            for (Path file : files) {
                if (progressInfo.isCancelled())
                    throw new IOException("Backup snapshot creation was cancelled");
                long size = Files.size(file);
                FileTime mtime = Files.getLastModifiedTime(file);
                String relativePath = baseDirectory.relativize(file).toString().replace(File.separatorChar, '/');

                String hash = filesCache.hashIfUnchanged(relativePath, size, mtime)
                        .orElseGet(() -> {
                            try {
                                // Cache miss: put reads the file (one read
                                // total) and dedups against existing objects.
                                String stored = store.put(file);
                                filesCache.record(relativePath, size, mtime, stored);
                                return stored;
                            } catch (IOException e) {
                                throw new UncheckedIOException("Could not store backup file " + file, e);
                            }
                        });

                JIPipeBackupSnapshot.SnapshotEntry entry = new JIPipeBackupSnapshot.SnapshotEntry();
                entry.setPath(relativePath);
                entry.setSize(size);
                entry.setHash(hash);
                entries.add(entry);
            }
        }
        // Sorted by path for deterministic output (the walk is sorted on the
        // absolute path; re-sort on the relative path to be exact)
        entries.sort((x, y) -> x.getPath().compareTo(y.getPath()));

        JIPipeBackupSnapshot snapshot = new JIPipeBackupSnapshot();
        snapshot.setSnapshotId(UUID.randomUUID().toString());
        snapshot.setCreated(LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME));
        snapshot.setProjectFormat(FORMAT_JIPX);
        snapshot.setProjectName(projectName);
        snapshot.setEntries(entries);
        progressInfo.log("Created .jipx backup snapshot of \"" + projectName + "\": "
                + entries.size() + " files, " + snapshot.getTotalSize() + " bytes (total)");
        return snapshot;
    }

    /**
     * Persists a created snapshot:
     * <ol>
     *     <li>appends the snapshot id to {@code <sessionDir>/snapshots.json}
     *     (atomic rewrite of the {@code List<String>}),</li>
     *     <li>updates {@code <sessionDir>/backup-info.json}
     *     (schema unchanged — {@code last-backup-date-time} refreshed),</li>
     *     <li>writes {@code snapshots/<id>.json} <b>last</b> (atomic
     *     temp+move, spec §3.1): all referenced objects exist before the
     *     manifest becomes visible — a crash mid-backup leaves orphan
     *     objects that GC sweeps, never a manifest referencing missing
     *     objects.</li>
     * </ol>
     * The files cache is persisted by the caller (it is the run's state).
     * <p>
     * MUST be called while holding the store write lock — together with the
     * create operation, that is the store mutation the lock protects.
     *
     * @param store          the object store (its backup root holds {@code snapshots/})
     * @param snapshot       the snapshot to persist
     * @param sessionDirectory the per-session backup directory (unchanged layout)
     * @param sessionInfo    the session info to (re)write as {@code backup-info.json}
     * @throws IOException if the manifest, index or session info cannot be written
     */
    public static void persistSnapshot(
            JIPipeBackupObjectStore store, JIPipeBackupSnapshot snapshot,
            Path sessionDirectory, JIPipeProjectBackupSessionInfo sessionInfo) throws IOException {
        Path backupRoot = store.getStoreDirectory().getParent();
        Path snapshotsDirectory = backupRoot.resolve("snapshots");

        // 1. Session index (atomic rewrite)
        Files.createDirectories(sessionDirectory);
        Path indexFile = sessionDirectory.resolve("snapshots.json");
        List<String> index = Files.isRegularFile(indexFile)
                ? new ArrayList<>(JsonUtils.readListFromFile(indexFile, String.class))
                : new ArrayList<>();
        if (!index.contains(snapshot.getSnapshotId()))
            index.add(snapshot.getSnapshotId());
        writeJsonAtomically(index, indexFile);

        // 2. Session info (schema unchanged)
        sessionInfo.setLastDateTimeInfo(snapshot.getCreated());
        JsonUtils.saveToFile(sessionInfo, sessionDirectory.resolve("backup-info.json"));

        // 3. The manifest, LAST (atomic temp+move)
        Files.createDirectories(snapshotsDirectory);
        writeJsonAtomically(snapshot, snapshotsDirectory.resolve(snapshot.getSnapshotId() + ".json"));
    }

    /**
     * Deletes a snapshot: removes {@code <backupRoot>/snapshots/<snapshotId>.json}
     * and the id from {@code <sessionDirectory>/snapshots.json} (atomic
     * rewrite). Objects are NOT deleted here — the content-addressed store
     * only releases them via {@link JIPipeBackupObjectStore#garbageCollect(JIPipeProgressInfo)},
     * which the thinning runs trigger (Task 6).
     * <p>
     * MUST be called while holding the store write lock.
     *
     * @param backupRoot       the backup root holding {@code snapshots/}
     * @param snapshotId       the id of the snapshot to delete
     * @param sessionDirectory the per-session backup directory
     * @param progressInfo     progress logging / cancellation
     * @throws IOException if the manifest or the session index cannot be written
     */
    public static void deleteSnapshot(Path backupRoot, String snapshotId, Path sessionDirectory,
                                      JIPipeProgressInfo progressInfo) throws IOException {
        Path manifest = backupRoot.resolve("snapshots").resolve(snapshotId + ".json");
        Files.deleteIfExists(manifest);

        Path indexFile = sessionDirectory.resolve("snapshots.json");
        List<String> index = Files.isRegularFile(indexFile)
                ? new ArrayList<>(JsonUtils.readListFromFile(indexFile, String.class))
                : new ArrayList<>();
        index.remove(snapshotId);
        writeJsonAtomically(index, indexFile);

        progressInfo.log("Deleted backup snapshot " + snapshotId
                + " (objects remain until garbage collection)");
    }

    /**
     * Assembles a snapshot into a project file at the user-chosen path.
     * <ul>
     *     <li>{@code .jip}: the single entry's object is streamed to a temp
     *     file in the target's directory (re-hashed while copying) and
     *     moved into place with an atomic rename.</li>
     *     <li>{@code .jipx}: every entry's object is assembled into a temp
     *     directory (each object re-hashed while copying into
     *     {@code <tmp>/<entry.path>}), then the directory CONTENTS are
     *     zipped FLAT (entries at the ZIP root, sorted by path) into a temp
     *     zip next to the target, which is moved into place atomically.</li>
     * </ul>
     * Every object is verified while being read: a missing object aborts
     * with {@code "Backup object <hash> for <path> is missing — the backup
     * store is damaged"}, a hash mismatch aborts with {@code "Backup object
     * <hash> does not match its checksum — the backup store is damaged"}.
     * All failures abort BEFORE the final move — the target file is never
     * half-written (an existing target survives a failed restore untouched).
     * <p>
     * LOCK-FREE: restores only read the immutable objects
     * ({@link JIPipeBackupObjectStore#get(String)} is lock-free by design)
     * and write OUTSIDE the store, so no store write lock is taken here.
     *
     * @param store        the object store the snapshot's objects live in
     * @param snapshot     the snapshot to restore
     * @param targetFile   the user-chosen target file ({@code .jip}/{@code .jipx};
     *                     parent directories are created as needed)
     * @param progressInfo progress logging / cancellation
     * @throws IOException if an object is missing or corrupted, the container
     *                     cannot be assembled, or the final move fails
     */
    public static void restoreSnapshot(JIPipeBackupObjectStore store, JIPipeBackupSnapshot snapshot,
                                       Path targetFile, JIPipeProgressInfo progressInfo) throws IOException {
        if (store == null || snapshot == null || targetFile == null)
            throw new IOException("restoreSnapshot requires a store, a snapshot and a target file");
        if (targetFile.getParent() == null)
            throw new IOException("The restore target file must have a parent directory: " + targetFile);
        progressInfo.log("Restoring backup snapshot " + snapshot.getSnapshotId() + " to " + targetFile);
        Files.createDirectories(targetFile.getParent());

        switch (snapshot.getProjectFormat()) {
            case FORMAT_JIP:
                restoreJip(store, snapshot, targetFile);
                break;
            case FORMAT_JIPX:
                restoreJipx(store, snapshot, targetFile, progressInfo);
                break;
            default:
                throw new IOException("Unsupported project format in snapshot "
                        + snapshot.getSnapshotId() + ": \"" + snapshot.getProjectFormat() + "\"");
        }
        progressInfo.log("Restored backup snapshot " + snapshot.getSnapshotId() + " to " + targetFile);
    }

    /**
     * Verifies every object of every snapshot without writing anything
     * (dry run): enumerates {@code <backupRoot>/snapshots/*.json}, re-hashes
     * each referenced object fully, and accumulates the problems in the
     * returned report. Objects are deduplicated by hash across snapshots —
     * an object referenced by several snapshots is checked and counted
     * once. Unreadable manifests and missing objects become problems
     * (they are not counted as checked).
     * <p>
     * LOCK-FREE: verification only reads the immutable objects, so no store
     * write lock is taken here (a backup run holding the lock never blocks
     * a verification).
     *
     * @param store        the object store (its backup root holds {@code snapshots/})
     * @param progressInfo progress logging / cancellation
     * @return the verification report
     * @throws IOException if the snapshots directory cannot be read
     */
    public static JIPipeBackupVerificationReport verifyAll(JIPipeBackupObjectStore store,
                                                           JIPipeProgressInfo progressInfo) throws IOException {
        JIPipeBackupVerificationReport report = new JIPipeBackupVerificationReport();
        Path backupRoot = store.getStoreDirectory().getParent();
        Path snapshotsDirectory = backupRoot.resolve("snapshots");
        if (!Files.isDirectory(snapshotsDirectory)) {
            progressInfo.log("Backup verification: no snapshots directory — nothing to check");
            return report;
        }

        // Collect the unique object hashes referenced by ALL manifests
        // (dangling ids in session indices are invisible: only files count)
        Set<String> uniqueHashes = new LinkedHashSet<>();
        try (Stream<Path> manifestStream = Files.list(snapshotsDirectory)) {
            for (Path manifestFile : manifestStream
                    .filter(p -> p.getFileName().toString().endsWith(".json"))
                    .sorted()
                    .toList()) {
                if (progressInfo.isCancelled())
                    throw new IOException("Backup verification was cancelled");
                JIPipeBackupSnapshot snapshot;
                try {
                    snapshot = JsonUtils.readFromFile(manifestFile, JIPipeBackupSnapshot.class);
                } catch (RuntimeException e) {
                    // JsonUtils.readFromFile throws runtime exceptions (Jackson
                    // wraps the IOException); a corrupt manifest is a PROBLEM,
                    // not a hard failure
                    report.problems.add("Could not read snapshot manifest " + manifestFile.getFileName()
                            + ": " + e.getMessage());
                    continue;
                }
                report.snapshotsChecked++;
                for (JIPipeBackupSnapshot.SnapshotEntry entry : snapshot.getEntries())
                    uniqueHashes.add(entry.getHash());
            }
        }

        // Re-hash each unique object once
        for (String hash : uniqueHashes) {
            if (progressInfo.isCancelled())
                throw new IOException("Backup verification was cancelled");
            if (!store.has(hash)) {
                report.problems.add("Backup object " + hash + " is missing — the backup store is damaged");
                continue;
            }
            try (InputStream in = store.get(hash)) {
                byte[] actual = in.readAllBytes();
                report.objectsChecked++;
                report.bytesChecked += actual.length;
                if (!matchesHash(actual, hash))
                    throw new IOException("Backup object " + hash + " does not match its checksum — the backup store is damaged");
            } catch (IOException e) {
                report.problems.add(e.getMessage() != null ? e.getMessage()
                        : "Could not verify backup object " + hash);
            }
        }
        progressInfo.log("Backup verification: " + report.snapshotsChecked + " snapshots, "
                + report.objectsChecked + " objects, " + report.bytesChecked + " bytes checked, "
                + report.problems.size() + " problem(s)");
        return report;
    }

    // ------------------------------------------------------------------
    // Internals
    // ------------------------------------------------------------------

    /**
     * Restores a .jip snapshot: the single entry's object is streamed to a
     * temp file in the target's directory (re-hashed while copying), then
     * moved into place atomically. On a checksum mismatch NOTHING is
     * written to the target — the copy fails before the move and its temp
     * file is deleted; a failed move (rare) is cleaned up here.
     */
    private static void restoreJip(JIPipeBackupObjectStore store, JIPipeBackupSnapshot snapshot,
                                   Path targetFile) throws IOException {
        JIPipeBackupSnapshot.SnapshotEntry entry = snapshot.getEntries().get(0);
        // Temp file in the target's own directory (same file system, required
        // for the atomic move), named <target>.jipipe-zip-<uuid>
        Path tempTarget = targetFile.getParent().resolve(targetFile.getFileName() + ".jipipe-zip-" + UUID.randomUUID());
        try {
            copyObjectToTargetVerified(store, entry, tempTarget);
            atomicMove(tempTarget, targetFile);
        } catch (IOException e) {
            // copyObjectToTargetVerified already deleted its temp on a
            // failed copy; this only covers a failed move (rare)
            deleteTemp(tempTarget);
            throw e;
        }
    }

    /**
     * Restores a .jipx snapshot: every entry's object is assembled into a
     * temp directory (each object re-hashed while copying), then the
     * directory CONTENTS are zipped FLAT (entries at the ZIP root, sorted
     * by path — the same order the snapshot entries use) into a temp zip
     * next to the target, which is moved into place atomically. Any missing
     * or corrupted object aborts BEFORE the final move — the target file is
     * never half-written.
     */
    private static void restoreJipx(JIPipeBackupObjectStore store, JIPipeBackupSnapshot snapshot,
                                    Path targetFile, JIPipeProgressInfo progressInfo) throws IOException {
        // Temp assembly dir + temp zip in the target's own directory (same
        // file system, required for the final atomic move)
        Path tempDir = targetFile.getParent().resolve(".jipipe-restore-" + UUID.randomUUID());
        Path tempZip = targetFile.getParent().resolve(targetFile.getFileName() + ".jipipe-zip-" + UUID.randomUUID());
        Files.createDirectories(tempDir);
        try {
            for (JIPipeBackupSnapshot.SnapshotEntry entry : snapshot.getEntries()) {
                if (progressInfo.isCancelled())
                    throw new IOException("Snapshot restore was cancelled");
                copyObjectToTargetVerified(store, entry, tempDir.resolve(entry.getPath()));
            }
            zipDirectoryContentsFlat(tempDir, tempZip);
            atomicMove(tempZip, targetFile);
        } finally {
            deleteTemp(tempZip);
            deleteTempDir(tempDir);
        }
    }

    /**
     * Streams one snapshot entry's object into the given target file,
     * re-hashing the bytes while copying (verify-on-read):
     * <ul>
     *     <li>missing object → {@code "Backup object <hash> for <path> is
     *     missing — the backup store is damaged"}</li>
     *     <li>hash mismatch → {@code "Backup object <hash> does not match
     *     its checksum — the backup store is damaged"} (a truncated object
     *     fails earlier, in the store's length-checking stream)</li>
     * </ul>
     * Parent directories of the target are created. On any failure the
     * target file is deleted again, so a failure never leaves a partial
     * file behind.
     */
    private static void copyObjectToTargetVerified(JIPipeBackupObjectStore store,
                                                   JIPipeBackupSnapshot.SnapshotEntry entry,
                                                   Path target) throws IOException {
        if (!store.has(entry.getHash()))
            throw new IOException("Backup object " + entry.getHash() + " for " + entry.getPath()
                    + " is missing — the backup store is damaged");
        try {
            Files.createDirectories(target.getParent());
            // Re-hash the object while copying it (verify-on-read)
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            try (InputStream in = store.get(entry.getHash());
                 DigestInputStream digestIn = new DigestInputStream(in, digest)) {
                Files.copy(digestIn, target, StandardCopyOption.REPLACE_EXISTING);
            }
            if (!Arrays.equals(HEX.parseHex(entry.getHash()), digest.digest()))
                throw new IOException("Backup object " + entry.getHash() + " does not match its checksum — the backup store is damaged");
        } catch (java.security.NoSuchAlgorithmException e) {
            throw new IOException("SHA-256 is not available", e);
        } catch (IOException e) {
            // Never leave a partial file behind on a failed copy
            deleteTemp(target);
            throw e;
        }
    }

    /**
     * Zips the CONTENTS of a directory FLAT into the target zip: entries
     * are at the ZIP ROOT (no wrapping folder), named with {@code /}
     * separators and written in ascending relative-path order. NOT
     * {@code ArchiveUtils.compressDirectoryToZip} — that wraps everything
     * in a root folder, which would break the .jipx container layout.
     */
    private static void zipDirectoryContentsFlat(Path dir, Path target) throws IOException {
        List<Path> files = new ArrayList<>();
        if (Files.isDirectory(dir)) {
            try (Stream<Path> walk = Files.walk(dir)) {
                walk.filter(Files::isRegularFile).sorted().forEach(files::add);
            }
        }
        Files.createDirectories(target.getParent());
        try (ZipOutputStream zip = new ZipOutputStream(Files.newOutputStream(target))) {
            for (Path file : files) {
                String relativePath = dir.relativize(file).toString().replace(File.separatorChar, '/');
                zip.putNextEntry(new ZipEntry(relativePath));
                Files.copy(file, zip);
                zip.closeEntry();
            }
        }
    }

    private static boolean matchesHash(byte[] bytes, String hash) {
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256").digest(bytes);
            return Arrays.equals(HEX.parseHex(hash), digest);
        } catch (java.security.NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 is not available", e);
        }
    }

    /**
     * Moves the temp file into the target atomically (a plain rename
     * fallback for platforms without atomic move support).
     */
    private static void atomicMove(Path source, Path target) throws IOException {
        try {
            Files.move(source, target, StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING);
        } catch (AtomicMoveNotSupportedException e) {
            // The temp file is in the target's own directory, so this is
            // only a theoretical fallback
            Files.move(source, target, StandardCopyOption.REPLACE_EXISTING);
        }
    }

    private static void deleteTemp(Path file) {
        if (file == null)
            return;
        try {
            Files.deleteIfExists(file);
        } catch (IOException ignored) {
            // Best effort cleanup
        }
    }

    private static void deleteTempDir(Path dir) {
        if (dir == null || !Files.exists(dir))
            return;
        try (Stream<Path> walk = Files.walk(dir)) {
            for (Path path : walk.sorted(Comparator.reverseOrder()).toList())
                Files.deleteIfExists(path);
        } catch (IOException ignored) {
            // Best effort cleanup
        }
    }

    /**
     * Writes JSON atomically: temp file {@code .tmp-<uuid>} in the SAME
     * directory as the target, then {@code Files.move(ATOMIC_MOVE,
     * REPLACE_EXISTING)}; the temp file is deleted in a finally block so no
     * {@code .tmp-} leftovers survive success or failure.
     */
    private static void writeJsonAtomically(Object value, Path target) throws IOException {
        Path parent = target.getParent() != null ? target.getParent() : Path.of(".");
        Files.createDirectories(parent);
        Path tempFile = parent.resolve(".tmp-" + UUID.randomUUID());
        try {
            JsonUtils.saveToFile(value, tempFile);
            try {
                Files.move(tempFile, target, StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING);
            } catch (AtomicMoveNotSupportedException e) {
                // Same directory, so this is only a theoretical fallback
                Files.move(tempFile, target, StandardCopyOption.REPLACE_EXISTING);
            }
        } finally {
            try {
                Files.deleteIfExists(tempFile);
            } catch (IOException ignored) {
            }
        }
    }
}
