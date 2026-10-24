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
import java.io.UncheckedIOException;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.nio.file.attribute.FileTime;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
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
 */
public final class JIPipeBackupStoreOperations {

    /** The single entry path of a .jip snapshot (metadata only; the entry
     * count of 1 + the format tag drive restore). */
    public static final String JIP_ENTRY_PATH = "project.jip";

    /** The project format tag of a .jip snapshot. */
    public static final String FORMAT_JIP = "jip";

    /** The project format tag of a .jipx snapshot. */
    public static final String FORMAT_JIPX = "jipx";

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

    // ------------------------------------------------------------------
    // Internals
    // ------------------------------------------------------------------

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
