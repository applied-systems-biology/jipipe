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

import org.hkijena.jipipe.JIPipe;
import org.hkijena.jipipe.api.JIPipeProgressInfo;
import org.hkijena.jipipe.api.backups.CollectBackupsRun;
import org.hkijena.jipipe.api.backups.DeleteBackupsRun;
import org.hkijena.jipipe.api.backups.JIPipeProjectBackupItem;
import org.hkijena.jipipe.api.backups.JIPipeProjectBackupItemCollection;
import org.hkijena.jipipe.api.backups.JIPipeProjectBackupSessionInfo;
import org.hkijena.jipipe.api.backups.PruneBackupsRun;
import org.hkijena.jipipe.api.backups.ThinBackupsRun;
import org.hkijena.jipipe.api.project.JIPipeProject;
import org.hkijena.jipipe.plugins.parameters.library.primitives.optional.OptionalIntegerParameter;
import org.hkijena.jipipe.plugins.parameters.library.primitives.optional.OptionalPathParameter;
import org.hkijena.jipipe.plugins.settings.application.JIPipeBackupApplicationSettings;
import org.hkijena.jipipe.utils.json.JsonUtils;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.FileTime;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Random;
import java.util.Set;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Integration of the content-addressed snapshot store with the backup
 * COLLECTION layer (Task 6): the collector lists snapshot items alongside
 * legacy file items, the thinning run deletes snapshots and triggers
 * garbage collection, the prune run keeps snapshot-only sessions, and the
 * store size limit drops the oldest snapshots.
 * <p>
 * The tests point the backup service component's backup path at a
 * per-test temp directory (via the custom-backup-path setting), so the
 * runs operate on hand-built backup roots. The thinning tests backdate the
 * snapshot {@code created} time, because the cascading rule engine always
 * keeps backups younger than one hour.
 */
class JIPipeBackupIntegrationTest {

    @BeforeAll
    static void ensureInstance() {
        JIPipe.ensureInstance();
    }

    @BeforeEach
    void resetSettings() {
        JIPipeBackupApplicationSettings settings = JIPipeBackupApplicationSettings.getInstance();
        settings.setCustomBackupPath(new OptionalPathParameter());
        JIPipeBackupApplicationSettings.CleanupSettings cleanup = settings.getCleanupSettings();
        cleanup.setMaxAgeDays(new OptionalIntegerParameter(true, 60));
        cleanup.setKeepBackupsPerYear(new OptionalIntegerParameter(true, 12));
        cleanup.setKeepBackupsPerMonth(new OptionalIntegerParameter(true, 4));
        cleanup.setKeepBackupsPerWeek(new OptionalIntegerParameter(true, 4));
        cleanup.setKeepBackupsPerDay(new OptionalIntegerParameter(true, 1));
        cleanup.setKeepBackupsPerHour(new OptionalIntegerParameter(true, 1));
        cleanup.setEnableAutoGc(true);
        cleanup.setStoreSizeLimitMb(new OptionalIntegerParameter(false, 0));
    }

    @AfterEach
    void restoreSettings() {
        JIPipeBackupApplicationSettings.getInstance().setCustomBackupPath(new OptionalPathParameter());
    }

    /**
     * The collector lists snapshot items alongside legacy file items:
     * a session dir with {@code backup-info.json} + {@code snapshots.json}
     * (one persisted snapshot) + one legacy {@code .jip} file, plus a
     * root-level legacy {@code .jip}. The collection must contain the
     * snapshot item (sizes/format set, {@code projectPath} null) and the
     * legacy items must be intact (file paths set, not snapshots).
     */
    @Test
    void collectListsSnapshotAndLegacyItems(@TempDir Path backupRoot) throws Exception {
        JIPipe.getInstance().getProjectBackup().setCustomBackupPath(backupRoot);

        // One real persisted snapshot (jipx, with a payload) in a session dir
        JIPipeProject project = new JIPipeProject();
        JIPipeBackupSnapshot snapshot;
        Path sessionDir = backupRoot.resolve("session-1");
        try {
            project.getMetadata().setName("Integration collect");
            Path payloadFile = project.getWorkspace().getBaseDirectory().resolve("data").resolve("payload.bin");
            Files.createDirectories(payloadFile.getParent());
            byte[] payload = new byte[32 * 1024];
            new Random(1).nextBytes(payload);
            Files.write(payloadFile, payload);

            JIPipeLocalBackupObjectStore store = new JIPipeLocalBackupObjectStore(backupRoot);
            assertTrue(store.acquireWriteLock(5000), "the test must hold the store write lock");
            try {
                snapshot = JIPipeBackupStoreOperations.createJipxSnapshot(
                        store, project, new JIPipeBackupFilesCache(), "collect-me.jipx", JIPipeProgressInfo.SILENT);
                JIPipeBackupStoreOperations.persistSnapshot(store, snapshot, sessionDir,
                        sessionInfo("session-1", "/original/collect-me.jipx"));
            } finally {
                store.releaseWriteLock();
            }
        } finally {
            project.close(JIPipeProgressInfo.SILENT);
        }

        // One legacy .jip inside the same session dir + one root-level legacy .jip
        Path legacyInSession = sessionDir.resolve("2026-01-01T10-00-00.jip");
        Files.writeString(legacyInSession, "legacy-jip-content");
        // The test asserts the legacy file sorts strictly after the snapshot
        // (newest first). The OS would otherwise stamp a just-written file with
        // the filesystem clock, which on some filesystems (the CI runner,
        // virtiofs) lags the wall clock used for snapshot.created by a few ms,
        // inverting the order and flaking the assertion. Stamp the mtime
        // explicitly, a little after the snapshot's created time, so the
        // ordering is deterministic regardless of the filesystem clock.
        Files.setLastModifiedTime(legacyInSession, FileTime.from(
                LocalDateTime.parse(snapshot.getCreated()).plusSeconds(5)
                        .atZone(ZoneId.systemDefault()).toInstant()));
        Path legacyAtRoot = backupRoot.resolve("2026-01-02T10-00-00.jip");
        Files.writeString(legacyAtRoot, "legacy-root-content");

        CollectBackupsRun run = new CollectBackupsRun();
        run.setProgressInfo(JIPipeProgressInfo.SILENT);
        run.run();
        List<JIPipeProjectBackupItemCollection> output = run.getOutput();

        // The session collection holds BOTH the snapshot and the legacy file item
        JIPipeProjectBackupItemCollection sessionCollection = findBySessionId(output, "session-1");
        assertNotNull(sessionCollection, "the session collection must exist: " + render(output));
        assertEquals(2, sessionCollection.getBackupItemList().size(),
                "the session must contain the snapshot item and the legacy file item: "
                        + renderItems(sessionCollection.getBackupItemList()));

        JIPipeProjectBackupItem snapshotItem = sessionCollection.getBackupItemList().stream()
                .filter(JIPipeProjectBackupItem::isSnapshot).findFirst().orElse(null);
        assertNotNull(snapshotItem, "the snapshot item must be listed (isSnapshot)");
        assertEquals(snapshot.getSnapshotId(), snapshotItem.getSnapshotId());
        assertEquals("jipx", snapshotItem.getProjectFormat());
        assertEquals(snapshot.getTotalSize(), snapshotItem.getTotalSizeBytes(),
                "the snapshot item must carry its total size");
        assertEquals(backupRoot.resolve("snapshots").resolve(snapshot.getSnapshotId() + ".json"),
                snapshotItem.getSnapshotManifestPath());
        assertNull(snapshotItem.getProjectPath(), "a snapshot item must not have a project file path");
        assertEquals(LocalDateTime.parse(snapshot.getCreated()), snapshotItem.getBackupTime(),
                "the snapshot item's backup time must be the manifest's created time");
        assertEquals("/original/collect-me.jipx", snapshotItem.getOriginalProjectPath());
        assertEquals("session-1", snapshotItem.getSessionId());

        JIPipeProjectBackupItem legacyItem = sessionCollection.getBackupItemList().stream()
                .filter(i -> !i.isSnapshot()).findFirst().orElse(null);
        assertNotNull(legacyItem, "the legacy file item must be intact");
        assertEquals(legacyInSession, legacyItem.getProjectPath());
        assertNotNull(legacyItem.getBackupTime());
        assertNull(legacyItem.getSnapshotId());

        // Newest first within the collection (the legacy file was written after
        // the snapshot's created time, so it sorts first)
        assertEquals(legacyItem.getBackupTime(),
                sessionCollection.getBackupItemList().getFirst().getBackupTime(),
                "items must be sorted newest first");

        // The root-level legacy .jip becomes its own legacy-* collection, file intact
        JIPipeProjectBackupItemCollection rootLegacy = findBySessionIdPrefix(output, "legacy-");
        assertNotNull(rootLegacy, "the root-level legacy file must form its own legacy collection: " + render(output));
        assertTrue(rootLegacy.isLegacy());
        assertEquals(1, rootLegacy.getBackupItemList().size());
        JIPipeProjectBackupItem rootLegacyItem = rootLegacy.getBackupItemList().getFirst();
        assertEquals(legacyAtRoot, rootLegacyItem.getProjectPath());
        assertFalse(rootLegacyItem.isSnapshot());
    }

    /**
     * Thinning deletes snapshot items and garbage collection frees the
     * objects they referenced: 3 deduplicated snapshots share ONE payload
     * object; after thinning deletes 2 (2 manifests gone, 1 index entry
     * left), the SHARED payload object survives GC (the remaining manifest
     * still references it) while the deleted snapshots' own part objects
     * are freed; after the last snapshot is deleted, GC removes the
     * payload object too.
     */
    @Test
    void thinningDeletesSnapshotsAndGcFreesObjects(@TempDir Path backupRoot) throws Exception {
        JIPipe.getInstance().getProjectBackup().setCustomBackupPath(backupRoot);
        JIPipeBackupApplicationSettings.CleanupSettings cleanup = JIPipeBackupApplicationSettings.getInstance().getCleanupSettings();

        JIPipeLocalBackupObjectStore store = new JIPipeLocalBackupObjectStore(backupRoot);
        Path sharedSession = backupRoot.resolve("sess-shared");
        Path soloSession = backupRoot.resolve("sess-solo");

        // --- Phase 1: three deduplicated snapshots in one session ---
        String keptId;
        String sharedPayloadHash;
        long storeBytesBefore;
        {
            JIPipeProject project = new JIPipeProject();
            try {
                Path payloadFile = project.getWorkspace().getBaseDirectory().resolve("data").resolve("payload.bin");
                Files.createDirectories(payloadFile.getParent());
                byte[] payload = new byte[256 * 1024];
                new Random(2).nextBytes(payload);
                Files.write(payloadFile, payload);

                assertTrue(store.acquireWriteLock(5000), "the test must hold the store write lock");
                JIPipeBackupFilesCache cache = new JIPipeBackupFilesCache();
                JIPipeProjectBackupSessionInfo info = sessionInfo("sess-shared", "/original/shared.jipx");
                // Backdated: the cascading rule engine always keeps backups
                // younger than one hour, so the thinning test needs aged items
                String id1 = persistJipxWithCreated(store, project, cache, sharedSession, info, LocalDateTime.now().minusHours(10));
                String id2 = persistJipxWithCreated(store, project, cache, sharedSession, info, LocalDateTime.now().minusHours(9));
                String id3 = persistJipxWithCreated(store, project, cache, sharedSession, info, LocalDateTime.now().minusHours(8));

                // Dedup precondition: the 256 KB PAYLOAD object is stored once
                // and shared by all three snapshots (the project parts are
                // re-materialized per snapshot and legitimately differ)
                sharedPayloadHash = hashOfEntry(backupRoot, id1, "data/payload.bin");
                assertEquals(sharedPayloadHash, hashOfEntry(backupRoot, id2, "data/payload.bin"),
                        "dedup precondition: the payload object must be shared by snapshots 1 and 2");
                assertEquals(sharedPayloadHash, hashOfEntry(backupRoot, id3, "data/payload.bin"),
                        "dedup precondition: the payload object must be shared by snapshots 1 and 3");
                assertEquals(payload.length, store.sizeOf(sharedPayloadHash));
                keptId = id3;
                store.releaseWriteLock();

                // Settings: nothing matches a keep rule, and the 5-day max age
                // keeps NOTHING (all items are 8-10 h old) — the cascade keeps
                // only the newest of the session and deletes the other 2.
                cleanup.setKeepBackupsPerYear(new OptionalIntegerParameter(false, 0));
                cleanup.setKeepBackupsPerMonth(new OptionalIntegerParameter(false, 0));
                cleanup.setKeepBackupsPerWeek(new OptionalIntegerParameter(false, 0));
                cleanup.setKeepBackupsPerDay(new OptionalIntegerParameter(false, 0));
                cleanup.setKeepBackupsPerHour(new OptionalIntegerParameter(false, 0));
                cleanup.setMaxAgeDays(new OptionalIntegerParameter(true, 5));

                // A second session persisted NOW: its snapshot is young, so the
                // cascade keeps it — and it keeps its own payload object alive
                // for the GC assertions below.
                persistDistinctSession(backupRoot, store, soloSession, "sess-solo", "/original/solo.jipx");

                // Baseline AFTER the solo session (the store only shrinks
                // relative to this point — the cascade + GC never delete
                // the solo session's still-referenced objects)
                storeBytesBefore = store.totalObjectBytes();

                // Run the thinning (collect → cascade → delete → GC → size limit)
                ThinBackupsRun run = new ThinBackupsRun(false);
                run.setProgressInfo(JIPipeProgressInfo.SILENT);
                run.run();
            } finally {
                project.close(JIPipeProgressInfo.SILENT);
            }
        }

        JIPipeLocalBackupObjectStore store2 = new JIPipeLocalBackupObjectStore(backupRoot);

        // 2 manifests gone, the session index holds only the surviving id
        Set<String> remaining = manifestIds(backupRoot);
        assertEquals(Set.of(keptId, latestIdOfSession(backupRoot, soloSession)), remaining,
                "only the newest shared snapshot and the solo snapshot must survive thinning");
        assertEquals(List.of(keptId), readSessionIndex(sharedSession),
                "the deleted snapshots' ids must be removed from the session index");

        // The SHARED payload object survives GC (still referenced by the kept
        // snapshot — the deletion freed nothing that was shared), while the
        // deleted snapshots' own part objects were reclaimed: the store
        // shrank below the post-solo baseline.
        String soloPayloadHash = hashOfEntry(backupRoot, latestIdOfSession(backupRoot, soloSession), "data/payload.bin");
        assertFalse(sharedPayloadHash.equals(soloPayloadHash), "the payload objects must be distinct");
        assertTrue(store2.has(sharedPayloadHash),
                "the shared object must survive thinning (still referenced by the kept snapshot)");
        assertTrue(store2.has(soloPayloadHash), "the solo object must survive");
        assertTrue(store2.totalObjectBytes() < storeBytesBefore,
                "GC must have freed the deleted snapshots' unreferenced part objects");

        // --- Phase 2: delete the last shared snapshot → GC removes the object ---
        JIPipeProjectBackupItem lastItem = new JIPipeProjectBackupItem();
        lastItem.setSnapshotId(keptId);
        lastItem.setSnapshotManifestPath(backupRoot.resolve("snapshots").resolve(keptId + ".json"));
        lastItem.setSessionId("sess-shared");
        lastItem.delete(JIPipeProgressInfo.SILENT);

        assertTrue(store2.acquireWriteLock(5000), "the test must hold the store write lock");
        try {
            store2.garbageCollect(JIPipeProgressInfo.SILENT);
        } finally {
            store2.releaseWriteLock();
        }
        assertFalse(Files.exists(backupRoot.resolve("snapshots").resolve(keptId + ".json")));
        assertEquals(List.of(), readSessionIndex(sharedSession));
        assertFalse(store2.has(sharedPayloadHash),
                "GC must remove the shared object once its last referencing manifest is gone");
        assertTrue(store2.has(soloPayloadHash), "the solo session's object must be untouched");
    }

    /**
     * A snapshot-only session ({@code backup-info.json} +
     * {@code snapshots.json} with one snapshot, NO legacy backup files)
     * survives {@code pruneEmptySessions}; after its snapshot is deleted,
     * the session IS pruned and its manifests are removed with it.
     */
    @Test
    void pruneKeepsSnapshotOnlySessions(@TempDir Path backupRoot) throws Exception {
        JIPipe.getInstance().getProjectBackup().setCustomBackupPath(backupRoot);

        JIPipeLocalBackupObjectStore store = new JIPipeLocalBackupObjectStore(backupRoot);
        Path sessionDir = backupRoot.resolve("snap-only");
        String snapshotId;
        {
            JIPipeProject project = new JIPipeProject();
            try {
                Path payloadFile = project.getWorkspace().getBaseDirectory().resolve("data").resolve("payload.bin");
                Files.createDirectories(payloadFile.getParent());
                byte[] payload = new byte[16 * 1024];
                new Random(3).nextBytes(payload);
                Files.write(payloadFile, payload);

                assertTrue(store.acquireWriteLock(5000), "the test must hold the store write lock");
                JIPipeBackupSnapshot snapshot = JIPipeBackupStoreOperations.createJipxSnapshot(
                        store, project, new JIPipeBackupFilesCache(), "snap-only.jipx", JIPipeProgressInfo.SILENT);
                JIPipeBackupStoreOperations.persistSnapshot(store, snapshot, sessionDir,
                        sessionInfo("snap-only", "/original/snap-only.jipx"));
                snapshotId = snapshot.getSnapshotId();
                store.releaseWriteLock();
            } finally {
                project.close(JIPipeProgressInfo.SILENT);
            }
        }

        // Precondition: the session has a snapshot index + session info but NO backup files
        assertTrue(Files.isRegularFile(sessionDir.resolve("snapshots.json")));
        assertTrue(Files.isRegularFile(sessionDir.resolve("backup-info.json")));

        // The snapshot-only session must SURVIVE pruning
        PruneBackupsRun.pruneEmptySessions(backupRoot, JIPipeProgressInfo.SILENT);
        assertTrue(Files.isDirectory(sessionDir),
                "a session with only snapshot backups must NOT be pruned");
        assertTrue(Files.isRegularFile(backupRoot.resolve("snapshots").resolve(snapshotId + ".json")),
                "the manifest must survive pruning");

        // Delete the snapshot → the session becomes empty and IS pruned.
        // deleteSnapshot rewrites the session index and therefore requires the
        // caller to hold the store write lock (the new contract).
        assertTrue(store.acquireWriteLock(5000), "the test must hold the store write lock");
        try {
            JIPipeBackupStoreOperations.deleteSnapshot(store, snapshotId, sessionDir, JIPipeProgressInfo.SILENT);
        } finally {
            store.releaseWriteLock();
        }
        PruneBackupsRun.pruneEmptySessions(backupRoot, JIPipeProgressInfo.SILENT);
        assertFalse(Files.exists(sessionDir),
                "after the last snapshot is deleted, the empty session must be pruned");
        assertEquals(Set.of(), manifestIds(backupRoot), "no manifests may remain at all");
    }

    /**
     * The store size limit drops the oldest snapshots (across all sessions,
     * by backup time) until the live data is under the limit, then GC frees
     * the objects: 3 deduplicated 1 MB snapshots (~1.25 MB live total with
     * their parts) + a newer 256 KB solo snapshot. A 1 MB limit must drop
     * the 3 shared (older) snapshots and keep the solo one — the cascade
     * keep rules keep everything, so ONLY the size limit may act.
     */
    @Test
    void sizeLimitDropsOldest(@TempDir Path backupRoot) throws Exception {
        JIPipe.getInstance().getProjectBackup().setCustomBackupPath(backupRoot);
        JIPipeBackupApplicationSettings.CleanupSettings cleanup = JIPipeBackupApplicationSettings.getInstance().getCleanupSettings();

        JIPipeLocalBackupObjectStore store = new JIPipeLocalBackupObjectStore(backupRoot);
        Path sharedSession = backupRoot.resolve("sess-lim-shared");
        Path soloSession = backupRoot.resolve("sess-lim-solo");

        // 1 MB payload shared by three snapshots (dedup → one 1 MB object).
        // Backdated a few seconds (still "young" for the cascade, which
        // keeps everything under one hour), but strictly OLDER than the
        // solo snapshot persisted after them.
        List<String> sharedIds = new ArrayList<>();
        String sharedPayloadHash;
        {
            JIPipeProject project = new JIPipeProject();
            try {
                Path payloadFile = project.getWorkspace().getBaseDirectory().resolve("data").resolve("payload.bin");
                Files.createDirectories(payloadFile.getParent());
                byte[] payload = new byte[1024 * 1024];
                new Random(4).nextBytes(payload);
                Files.write(payloadFile, payload);

                assertTrue(store.acquireWriteLock(5000), "the test must hold the store write lock");
                JIPipeBackupFilesCache cache = new JIPipeBackupFilesCache();
                JIPipeProjectBackupSessionInfo info = sessionInfo("sess-lim-shared", "/original/limit-shared.jipx");
                LocalDateTime now = LocalDateTime.now();
                sharedIds.add(persistJipxWithCreated(store, project, cache, sharedSession, info, now.minusSeconds(30)));
                sharedIds.add(persistJipxWithCreated(store, project, cache, sharedSession, info, now.minusSeconds(20)));
                sharedIds.add(persistJipxWithCreated(store, project, cache, sharedSession, info, now.minusSeconds(10)));
                sharedPayloadHash = hashOfEntry(backupRoot, sharedIds.getFirst(), "data/payload.bin");
                store.releaseWriteLock();
            } finally {
                project.close(JIPipeProgressInfo.SILENT);
            }
        }

        // A newer solo session (256 KB, distinct payload) — kept by the cascade
        persistDistinctSession(backupRoot, store, soloSession, "sess-lim-solo", "/original/limit-solo.jipx");

        // Sanity: live data is ~1.25 MB (1 MB shared payload + ~256 KB solo + parts)
        long before = liveBytesOf(backupRoot);
        assertTrue(before > 1200L * 1024 && before < 1600L * 1024,
                "precondition: live data must be ~1.25 MB, got " + before + " bytes");

        // The cascade keeps everything (young items + 365-day max age), so
        // ONLY the size limit may drop snapshots — the oldest ones first.
        cleanup.setMaxAgeDays(new OptionalIntegerParameter(true, 365));
        cleanup.setStoreSizeLimitMb(new OptionalIntegerParameter(true, 1));

        ThinBackupsRun run = new ThinBackupsRun(false);
        run.setProgressInfo(JIPipeProgressInfo.SILENT);
        run.run();

        JIPipeLocalBackupObjectStore afterStore = new JIPipeLocalBackupObjectStore(backupRoot);
        Set<String> remaining = manifestIds(backupRoot);
        String soloId = latestIdOfSession(backupRoot, soloSession);
        assertEquals(Set.of(soloId), remaining,
                "exactly the newest solo snapshot must remain, got: " + remaining);
        for (String id : sharedIds)
            assertFalse(Files.exists(backupRoot.resolve("snapshots").resolve(id + ".json")),
                    "the dropped manifest must be deleted: " + id);
        assertFalse(Files.exists(sharedSession),
                "the emptied shared session must be pruned by the post-processing");

        String soloPayloadHash = hashOfEntry(backupRoot, soloId, "data/payload.bin");
        assertTrue(afterStore.has(soloPayloadHash), "the solo object must survive");
        assertFalse(afterStore.has(sharedPayloadHash),
                "the shared object must be GC'd once its last referencing manifest is gone");
        assertTrue(afterStore.totalObjectBytes() < 1024L * 1024 * 1024,
                "the store must be under the size limit after GC");
        assertTrue(liveBytesOf(backupRoot) < 1024L * 1024 * 1024,
                "the live backup data must be under the size limit");
    }

    /**
     * {@link DeleteBackupsRun} deletes every item it is given — a legacy file
     * (the file goes) and a snapshot (manifest + session-index entry go) — and
     * a single item whose store cannot be locked does NOT abort the run: it is
     * logged, the other items are still deleted, and the run completes without
     * throwing. The "cannot be locked" case is fast by construction: the store
     * root has {@code store} as a regular FILE, so {@code acquireWriteLock}'s
     * {@code Files.createDirectories} throws {@code FileAlreadyExistsException}
     * (caught → {@code false}) instead of retrying to the 60 s bound a real
     * lock holder would cause.
     */
    @Test
    void deleteBackupsRunDeletesItemsAndToleratesFailures(@TempDir Path backupRoot) throws Exception {
        JIPipe.getInstance().getProjectBackup().setCustomBackupPath(backupRoot);
        JIPipeLocalBackupObjectStore store = new JIPipeLocalBackupObjectStore(backupRoot);
        Path sessionDir = backupRoot.resolve("del-session");

        // (a) one persisted .jipx snapshot
        JIPipeProject project = new JIPipeProject();
        String snapshotId;
        try {
            project.getMetadata().setName("Delete run target");
            Path payloadFile = project.getWorkspace().getBaseDirectory().resolve("data").resolve("payload.bin");
            Files.createDirectories(payloadFile.getParent());
            byte[] payload = new byte[16 * 1024];
            new Random(7).nextBytes(payload);
            Files.write(payloadFile, payload);

            assertTrue(store.acquireWriteLock(5000), "the test must hold the store write lock");
            try {
                JIPipeBackupSnapshot snapshot = JIPipeBackupStoreOperations.createJipxSnapshot(
                        store, project, new JIPipeBackupFilesCache(), "del-run-target.jipx", JIPipeProgressInfo.SILENT);
                JIPipeBackupStoreOperations.persistSnapshot(store, snapshot, sessionDir,
                        sessionInfo("del-session", "/original/del-run-target.jipx"));
                snapshotId = snapshot.getSnapshotId();
            } finally {
                store.releaseWriteLock();
            }
        } finally {
            project.close(JIPipeProgressInfo.SILENT);
        }

        // (b) one legacy file item
        Path legacyFile = sessionDir.resolve("2026-01-01T10-00-00.jip");
        Files.writeString(legacyFile, "legacy-delete-me");

        // (c) one item that CANNOT be deleted: a snapshot whose store root has
        //     'store' as a regular file → the write-lock acquisition fails fast
        Path brokenRoot = backupRoot.resolve("del-broken");
        Files.createDirectories(brokenRoot);
        Files.writeString(brokenRoot.resolve("store"), "not a directory");
        JIPipeProjectBackupItem brokenItem = new JIPipeProjectBackupItem();
        brokenItem.setSnapshotId("broken");
        brokenItem.setSnapshotManifestPath(brokenRoot.resolve("snapshots").resolve("broken.json"));
        brokenItem.setSessionId("ghost");

        JIPipeProjectBackupItem snapshotItem = new JIPipeProjectBackupItem();
        snapshotItem.setSnapshotId(snapshotId);
        snapshotItem.setSnapshotManifestPath(backupRoot.resolve("snapshots").resolve(snapshotId + ".json"));
        snapshotItem.setSessionId("del-session");

        JIPipeProjectBackupItem legacyItem = new JIPipeProjectBackupItem();
        legacyItem.setProjectPath(legacyFile);
        legacyItem.setSessionId("del-session");

        // No lock is held here: the broken item's store cannot be locked, so
        // the snapshot deletion (same store) proceeds without contention.
        DeleteBackupsRun run = new DeleteBackupsRun(List.of(legacyItem, snapshotItem, brokenItem));
        run.setProgressInfo(JIPipeProgressInfo.SILENT);
        run.run(); // must complete WITHOUT throwing (the broken item is only logged)

        // Both deletable items are gone, the un-deletable one is untouched
        assertFalse(Files.exists(legacyFile), "the legacy file must be deleted from disk");
        assertFalse(Files.exists(backupRoot.resolve("snapshots").resolve(snapshotId + ".json")),
                "the snapshot manifest must be deleted");
        assertEquals(List.of(), readSessionIndex(sessionDir),
                "the deleted snapshot's id must be removed from the session index");
        assertTrue(Files.isRegularFile(brokenRoot.resolve("store")),
                "the un-deletable item's store must be left in place");
        assertEquals(Set.of(), manifestIds(backupRoot), "no manifest may remain");
    }

    // ------------------------------------------------------------------
    // Helpers
    // ------------------------------------------------------------------

    /** Creates + persists one jipx snapshot with the given (backdated) created time. */
    private static String persistJipxWithCreated(JIPipeBackupObjectStore store, JIPipeProject project,
                                                 JIPipeBackupFilesCache cache, Path sessionDir,
                                                 JIPipeProjectBackupSessionInfo info, LocalDateTime created) throws Exception {
        JIPipeBackupSnapshot snapshot = JIPipeBackupStoreOperations.createJipxSnapshot(
                store, project, cache, info.getProjectStoragePath(), JIPipeProgressInfo.SILENT);
        snapshot.setCreated(created.format(DateTimeFormatter.ISO_LOCAL_DATE_TIME));
        JIPipeBackupStoreOperations.persistSnapshot(store, snapshot, sessionDir, info);
        cache.save(store.getStoreDirectory().getParent().resolve("files-cache")
                .resolve(info.getProjectSessionId() + ".json"));
        return snapshot.getSnapshotId();
    }

    /**
     * A separate session dir + project + distinct 256 KB payload persisted
     * NOW (so it is the newest snapshot overall).
     */
    private static void persistDistinctSession(Path backupRoot, JIPipeLocalBackupObjectStore store,
                                               Path sessionDir, String sessionName, String storagePath) throws Exception {
        JIPipeProject project = new JIPipeProject();
        try {
            project.getMetadata().setName("Distinct session " + sessionName);
            Path payloadFile = project.getWorkspace().getBaseDirectory().resolve("data").resolve("payload.bin");
            Files.createDirectories(payloadFile.getParent());
            byte[] payload = new byte[256 * 1024];
            new Random(99).nextBytes(payload);
            Files.write(payloadFile, payload);

            JIPipeProjectBackupSessionInfo info = sessionInfo(sessionName, storagePath);
            JIPipeBackupSnapshot snapshot = JIPipeBackupStoreOperations.createJipxSnapshot(
                    store, project, new JIPipeBackupFilesCache(), sessionName + ".jipx", JIPipeProgressInfo.SILENT);
            JIPipeBackupStoreOperations.persistSnapshot(store, snapshot, sessionDir, info);
        } finally {
            project.close(JIPipeProgressInfo.SILENT);
        }
    }

    private static JIPipeProjectBackupSessionInfo sessionInfo(String sessionId, String storagePath) {
        JIPipeProjectBackupSessionInfo info = new JIPipeProjectBackupSessionInfo();
        info.setProjectSessionId(sessionId);
        info.setProjectStoragePath(storagePath);
        info.setLastDateTimeInfo(LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME));
        return info;
    }

    private static JIPipeBackupSnapshot readManifest(Path backupRoot, String snapshotId) {
        try {
            return JsonUtils.readFromFile(backupRoot.resolve("snapshots").resolve(snapshotId + ".json"),
                    JIPipeBackupSnapshot.class);
        } catch (Exception e) {
            throw new AssertionError("Could not read manifest " + snapshotId, e);
        }
    }

    private static List<String> readSessionIndex(Path sessionDir) {
        try {
            return JsonUtils.readListFromFile(sessionDir.resolve("snapshots.json"), String.class);
        } catch (Exception e) {
            throw new AssertionError("Could not read session index " + sessionDir, e);
        }
    }

    private static Set<String> manifestIds(Path backupRoot) throws Exception {
        Set<String> ids = new HashSet<>();
        Path snapshotsDir = backupRoot.resolve("snapshots");
        if (Files.isDirectory(snapshotsDir)) {
            try (Stream<Path> s = Files.list(snapshotsDir)) {
                for (Path p : s.filter(f -> f.getFileName().toString().endsWith(".json")).toList()) {
                    String name = p.getFileName().toString();
                    ids.add(readManifest(backupRoot, name.substring(0, name.length() - 5)).getSnapshotId());
                }
            }
        }
        return ids;
    }

    /** The id of the newest snapshot of the given session (the last index entry). */
    private static String latestIdOfSession(Path backupRoot, Path sessionDir) {
        List<String> index = readSessionIndex(sessionDir);
        if (index.isEmpty())
            throw new AssertionError("The session index is empty: " + sessionDir);
        String id = index.getLast();
        assertTrue(Files.isRegularFile(backupRoot.resolve("snapshots").resolve(id + ".json")),
                "the newest index entry must have a manifest: " + id);
        return id;
    }

    private static String hashOfEntry(Path backupRoot, String snapshotId, String path) {
        for (JIPipeBackupSnapshot.SnapshotEntry e : readManifest(backupRoot, snapshotId).getEntries())
            if (e.getPath().equals(path))
                return e.getHash();
        throw new AssertionError("No entry " + path + " in snapshot " + snapshotId);
    }

    private static long sumObjectBytes(JIPipeBackupObjectStore store, Set<String> hashes) {
        long sum = 0;
        for (String h : hashes)
            sum += store.sizeOf(h);
        return sum;
    }

    /**
     * The union of the objects referenced by the REMAINING manifests, summed
     * by their actual object size — the live backup data (the size-limit
     * accounting).
     */
    private static long liveBytesOf(Path backupRoot) throws Exception {
        JIPipeLocalBackupObjectStore store = new JIPipeLocalBackupObjectStore(backupRoot);
        Set<String> hashes = new HashSet<>();
        for (String id : manifestIds(backupRoot)) {
            for (JIPipeBackupSnapshot.SnapshotEntry e : readManifest(backupRoot, id).getEntries())
                hashes.add(e.getHash());
        }
        return sumObjectBytes(store, hashes);
    }

    private static JIPipeProjectBackupItemCollection findBySessionId(List<JIPipeProjectBackupItemCollection> output, String sessionId) {
        return output.stream().filter(c -> c.getSessionId().equals(sessionId)).findFirst().orElse(null);
    }

    private static JIPipeProjectBackupItemCollection findBySessionIdPrefix(List<JIPipeProjectBackupItemCollection> output, String prefix) {
        return output.stream().filter(c -> c.getSessionId().startsWith(prefix)).findFirst().orElse(null);
    }

    private static String render(List<JIPipeProjectBackupItemCollection> output) {
        StringBuilder sb = new StringBuilder();
        for (JIPipeProjectBackupItemCollection c : output)
            sb.append(c.getSessionId()).append("(").append(c.getBackupItemList().size()).append(") ");
        return sb.toString();
    }

    private static String renderItems(List<JIPipeProjectBackupItem> items) {
        StringBuilder sb = new StringBuilder();
        for (JIPipeProjectBackupItem i : items)
            sb.append(i.isSnapshot() ? i.getSnapshotId() : i.getProjectPath()).append(" ");
        return sb.toString();
    }
}
