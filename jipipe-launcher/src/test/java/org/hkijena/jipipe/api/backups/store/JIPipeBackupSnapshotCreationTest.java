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
import org.hkijena.jipipe.api.backups.JIPipeProjectBackupSessionInfo;
import org.hkijena.jipipe.api.notifications.JIPipeNotificationInbox;
import org.hkijena.jipipe.api.project.JIPipeProject;
import org.hkijena.jipipe.api.project.jipx.JIPipeXProjectWriter;
import org.hkijena.jipipe.api.validation.JIPipeValidationReport;
import org.hkijena.jipipe.api.validation.contexts.UnspecifiedValidationReportContext;
import org.hkijena.jipipe.utils.json.JsonUtils;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Snapshot creation for both project formats (spec §3.6) and manifest
 * persistence/deletion. Uses real {@link JIPipeProject} instances, so the
 * JIPipe registry is required.
 * <p>
 * Test 2 is the core dedup proof (spec §5.3): a multi-MB payload stored once
 * by the FIRST snapshot must NOT grow the store again when a second snapshot
 * captures only part-level changes.
 */
class JIPipeBackupSnapshotCreationTest {

    @BeforeAll
    static void ensureInstance() {
        JIPipe.ensureInstance();
    }

    /**
     * A .jip snapshot: exactly one {@code project.jip} entry whose bytes are
     * stored in the object store and are a loadable project model;
     * {@code persistSnapshot} writes the manifest and appends the id to the
     * session index.
     */
    @Test
    void jipSnapshotStoresSingleEntry(@TempDir Path backupRoot, @TempDir Path workDir) throws Exception {
        JIPipeLocalBackupObjectStore store = new JIPipeLocalBackupObjectStore(backupRoot);
        assertTrue(store.acquireWriteLock(5000), "the test must hold the store write lock");
        try {
            JIPipeProject project = new JIPipeProject();
            project.getMetadata().setName("Jip backup round trip");
            try {
                JIPipeBackupSnapshot snapshot = JIPipeBackupStoreOperations.createJipSnapshot(
                        store, project, "my-project.jip", JIPipeProgressInfo.SILENT);

                assertEquals("jip", snapshot.getProjectFormat());
                assertEquals("my-project.jip", snapshot.getProjectName());
                assertNotNull(snapshot.getSnapshotId());
                assertFalse(snapshot.getSnapshotId().isBlank());
                assertNotNull(snapshot.getCreated());
                // created must be an ISO_LOCAL_DATE_TIME string
                LocalDateTime.parse(snapshot.getCreated(), DateTimeFormatter.ISO_LOCAL_DATE_TIME);

                assertEquals(1, snapshot.getEntries().size(), "a .jip snapshot has exactly one entry");
                JIPipeBackupSnapshot.SnapshotEntry entry = snapshot.getEntries().get(0);
                assertEquals("project.jip", entry.getPath());
                assertTrue(entry.getSize() > 0, "the serialized model must be non-empty");
                assertTrue(store.has(entry.getHash()), "the serialized model must be in the store");
                assertEquals(entry.getSize(), store.sizeOf(entry.getHash()));

                // The stored bytes must be a complete, loadable project model
                Path roundTripFile = workDir.resolve("roundtrip.jip");
                try (var in = store.get(entry.getHash())) {
                    Files.copy(in, roundTripFile, StandardCopyOption.REPLACE_EXISTING);
                }
                JIPipeProject reloaded = JIPipeProject.loadProject(roundTripFile,
                        new UnspecifiedValidationReportContext(), new JIPipeValidationReport(),
                        new JIPipeNotificationInbox(), JIPipeProgressInfo.SILENT);
                try {
                    assertEquals("Jip backup round trip", reloaded.getMetadata().getName(),
                            "the stored object must deserialize to the same project model");
                } finally {
                    reloaded.close(JIPipeProgressInfo.SILENT);
                }

                // Persistence
                Path sessionDir = backupRoot.resolve("session-jip");
                JIPipeBackupStoreOperations.persistSnapshot(store, snapshot, sessionDir,
                        sessionInfo("sess-jip", workDir.resolve("my-project.jip")));

                Path manifest = backupRoot.resolve("snapshots").resolve(snapshot.getSnapshotId() + ".json");
                assertTrue(Files.isRegularFile(manifest), "the snapshot manifest must be persisted");
                JIPipeBackupSnapshot persisted = JsonUtils.readFromFile(manifest, JIPipeBackupSnapshot.class);
                assertEquals(snapshot.getSnapshotId(), persisted.getSnapshotId());
                assertEquals("jip", persisted.getProjectFormat());
                assertEquals(1, persisted.getEntries().size());
                assertEquals(entry.getHash(), persisted.getEntries().get(0).getHash());

                List<String> index = JsonUtils.readListFromFile(sessionDir.resolve("snapshots.json"), String.class);
                assertEquals(List.of(snapshot.getSnapshotId()), index, "the session index must contain the new id");
                assertTrue(Files.isRegularFile(sessionDir.resolve("backup-info.json")),
                        "backup-info.json must be written (schema unchanged)");
            } finally {
                project.close(JIPipeProgressInfo.SILENT);
            }
        } finally {
            store.releaseWriteLock();
        }
    }

    /**
     * THE core dedup test (spec §5.3): the multi-MB payload is stored once by
     * the first snapshot; the second snapshot captures an in-memory rename
     * (the metadata part changes) and must grow the store by only the bytes
     * of the changed files — the payload entry hash is identical in both
     * snapshots and the files cache file exists.
     */
    @Test
    void jipxSnapshotDedupsPayloadAcrossSaves(@TempDir Path dir) throws Exception {
        // 1. Create a project and write it as .jipx (the workspace binds),
        // then close the creator (temp-base hygiene, #753 audit lesson).
        JIPipeProject creator = new JIPipeProject();
        Path target = dir.resolve("proj.jipx");
        creator.setWorkDirectory(dir);
        new JIPipeXProjectWriter().write(creator, target, JIPipeProgressInfo.SILENT);
        assertTrue(Files.isRegularFile(target));
        creator.close(JIPipeProgressInfo.SILENT);

        // 2. Open it (the JIPX reader extracts the workspace + parts).
        JIPipeProject project = JIPipeProject.loadProject(target,
                new UnspecifiedValidationReportContext(), new JIPipeValidationReport(),
                new JIPipeNotificationInbox(), JIPipeProgressInfo.SILENT);

        Path backupRoot = dir.resolve("backup");
        JIPipeLocalBackupObjectStore store = new JIPipeLocalBackupObjectStore(backupRoot);
        assertTrue(store.acquireWriteLock(5000), "the test must hold the store write lock");
        JIPipeBackupFilesCache cache = new JIPipeBackupFilesCache();
        byte[] payload = new byte[4 * 1024 * 1024];
        new java.util.Random(42).nextBytes(payload);

        try {
            // 3. Add a multi-MB payload file to the workspace.
            Path payloadFile = project.getWorkspace().getBaseDirectory().resolve("data").resolve("payload.tiff");
            Files.createDirectories(payloadFile.getParent());
            Files.write(payloadFile, payload);

            // 4. Snapshot A (the payload is stored ONCE).
            JIPipeBackupSnapshot snapshotA = JIPipeBackupStoreOperations.createJipxSnapshot(
                    store, project, cache, "proj.jipx", JIPipeProgressInfo.SILENT);
            assertEquals("jipx", snapshotA.getProjectFormat());
            long bytesAfterA = store.totalObjectBytes();

            // 5. Mutate the in-memory project — NO save.
            project.getMetadata().setName("Renamed project for dedup");

            // 6. Snapshot B (same workspace, only part-level changes).
            JIPipeBackupSnapshot snapshotB = JIPipeBackupStoreOperations.createJipxSnapshot(
                    store, project, cache, "proj.jipx", JIPipeProgressInfo.SILENT);
            long bytesAfterB = store.totalObjectBytes();

            Map<String, JIPipeBackupSnapshot.SnapshotEntry> a = entriesByPath(snapshotA);
            Map<String, JIPipeBackupSnapshot.SnapshotEntry> b = entriesByPath(snapshotB);
            assertEquals(a.keySet(), b.keySet(), "both snapshots must list the same workspace files");

            // The mutation must be PROVEN: the metadata part hash changed
            // (the project name is serialized into project/metadata.json).
            assertTrue(a.containsKey("project/metadata.json"),
                    "the metadata part must be snapshotted, got: " + a.keySet());
            assertNotEquals(a.get("project/metadata.json").getHash(),
                    b.get("project/metadata.json").getHash(),
                    "the in-memory rename must change the metadata part hash");

            // The payload entry hash is identical in both snapshots (dedup)
            JIPipeBackupSnapshot.SnapshotEntry payloadA = a.get("data/payload.tiff");
            JIPipeBackupSnapshot.SnapshotEntry payloadB = b.get("data/payload.tiff");
            assertNotNull(payloadA, "the payload must be in snapshot A, got: " + a.keySet());
            assertNotNull(payloadB, "the payload must be in snapshot B, got: " + b.keySet());
            assertEquals(payloadA.getHash(), payloadB.getHash(),
                    "the payload entry hash must be identical in both snapshots");
            assertEquals(payload.length, payloadB.getSize());
            assertTrue(store.has(payloadB.getHash()), "the payload object must exist in the store");

            // Store growth = exactly the bytes of NEW objects (changed files);
            // far below the payload size.
            Set<String> hashesA = new HashSet<>();
            for (JIPipeBackupSnapshot.SnapshotEntry e : a.values())
                hashesA.add(e.getHash());
            long expectedGrowth = 0;
            Set<String> counted = new HashSet<>();
            for (JIPipeBackupSnapshot.SnapshotEntry e : b.values()) {
                if (!hashesA.contains(e.getHash()) && counted.add(e.getHash()))
                    expectedGrowth += e.getSize();
            }
            assertTrue(expectedGrowth > 0, "at least one file must have changed between the snapshots");
            assertEquals(expectedGrowth, bytesAfterB - bytesAfterA,
                    "the store must grow by exactly the bytes of the changed files (dedup)");
            assertTrue(bytesAfterB - bytesAfterA < payload.length / 10,
                    "store growth (" + (bytesAfterB - bytesAfterA)
                            + " bytes) must be far below the payload size (" + payload.length + " bytes)");

            // The files cache file for the session exists (saved by the run,
            // here by the test) and hits on the unchanged payload after reload.
            Path cacheFile = backupRoot.resolve("files-cache").resolve("sess-dedup.json");
            cache.save(cacheFile);
            assertTrue(Files.isRegularFile(cacheFile), "the files cache file for the session must exist");
            JIPipeBackupFilesCache reloaded = JIPipeBackupFilesCache.load(cacheFile);
            assertTrue(reloaded.hashIfUnchanged("data/payload.tiff",
                    Files.size(payloadFile), Files.getLastModifiedTime(payloadFile)).isPresent(),
                    "the unchanged payload must be a cache hit after reload (no re-read next backup)");
        } finally {
            project.close(JIPipeProgressInfo.SILENT);
            store.releaseWriteLock();
        }
    }

    /**
     * Unsaved edits are captured: opening a .jipx and renaming the project in
     * memory (NO save) — the snapshot's metadata part hash differs from the
     * pre-edit snapshot's (materializeFromModel refreshes the parts from the
     * live model).
     */
    @Test
    void jipxSnapshotCapturesUnsavedEdits(@TempDir Path dir) throws Exception {
        JIPipeProject creator = new JIPipeProject();
        Path target = dir.resolve("proj.jipx");
        creator.setWorkDirectory(dir);
        new JIPipeXProjectWriter().write(creator, target, JIPipeProgressInfo.SILENT);
        creator.close(JIPipeProgressInfo.SILENT);

        JIPipeProject project = JIPipeProject.loadProject(target,
                new UnspecifiedValidationReportContext(), new JIPipeValidationReport(),
                new JIPipeNotificationInbox(), JIPipeProgressInfo.SILENT);

        Path backupRoot = dir.resolve("backup");
        JIPipeLocalBackupObjectStore store = new JIPipeLocalBackupObjectStore(backupRoot);
        assertTrue(store.acquireWriteLock(5000), "the test must hold the store write lock");
        JIPipeBackupFilesCache cache = new JIPipeBackupFilesCache();
        try {
            JIPipeBackupSnapshot before = JIPipeBackupStoreOperations.createJipxSnapshot(
                    store, project, cache, "proj.jipx", JIPipeProgressInfo.SILENT);

            // In-memory edit, NOT saved
            project.getMetadata().setName("Edited but unsaved");

            JIPipeBackupSnapshot after = JIPipeBackupStoreOperations.createJipxSnapshot(
                    store, project, cache, "proj.jipx", JIPipeProgressInfo.SILENT);

            Map<String, JIPipeBackupSnapshot.SnapshotEntry> a = entriesByPath(before);
            Map<String, JIPipeBackupSnapshot.SnapshotEntry> b = entriesByPath(after);
            assertNotEquals(a.get("project/metadata.json").getHash(),
                    b.get("project/metadata.json").getHash(),
                    "the unsaved rename must be captured by the snapshot (materialize from the live model)");
        } finally {
            project.close(JIPipeProgressInfo.SILENT);
            store.releaseWriteLock();
        }
    }

    /**
     * Deletion removes the manifest and the session-index entry; the objects
     * themselves SURVIVE (garbage collection is a separate concern, Task 6).
     */
    @Test
    void deleteSnapshotRemovesManifestAndIndexEntry(@TempDir Path backupRoot) throws Exception {
        JIPipeLocalBackupObjectStore store = new JIPipeLocalBackupObjectStore(backupRoot);
        assertTrue(store.acquireWriteLock(5000), "the test must hold the store write lock");
        try {
            JIPipeProject project = new JIPipeProject();
            JIPipeBackupSnapshot snapshot;
            try {
                snapshot = JIPipeBackupStoreOperations.createJipSnapshot(
                        store, project, "to-delete.jip", JIPipeProgressInfo.SILENT);
            } finally {
                project.close(JIPipeProgressInfo.SILENT);
            }

            Path sessionDir = backupRoot.resolve("session-del");
            JIPipeBackupStoreOperations.persistSnapshot(store, snapshot, sessionDir,
                    sessionInfo("sess-del", Path.of("/tmp/to-delete.jip")));
            String hash = snapshot.getEntries().get(0).getHash();
            Path manifest = backupRoot.resolve("snapshots").resolve(snapshot.getSnapshotId() + ".json");
            assertTrue(store.has(hash), "precondition: the object is stored");
            assertTrue(Files.isRegularFile(manifest), "precondition: the manifest is persisted");

            JIPipeBackupStoreOperations.deleteSnapshot(store, snapshot.getSnapshotId(), sessionDir,
                    JIPipeProgressInfo.SILENT);

            assertFalse(Files.exists(manifest), "the manifest must be deleted");
            List<String> index = JsonUtils.readListFromFile(sessionDir.resolve("snapshots.json"), String.class);
            assertEquals(List.of(), index, "the session index must be empty after the last snapshot is deleted");
            assertTrue(store.has(hash), "the object must survive deletion (GC is a separate concern)");
        } finally {
            store.releaseWriteLock();
        }
    }

    // ------------------------------------------------------------------
    // Helpers
    // ------------------------------------------------------------------

    private static JIPipeProjectBackupSessionInfo sessionInfo(String sessionId, Path storagePath) {
        JIPipeProjectBackupSessionInfo info = new JIPipeProjectBackupSessionInfo();
        info.setProjectSessionId(sessionId);
        info.setProjectStoragePath(storagePath.toString());
        info.setLastDateTimeInfo(LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME));
        return info;
    }

    /** path → entry, insertion order (snapshots are stored sorted by path). */
    private static Map<String, JIPipeBackupSnapshot.SnapshotEntry> entriesByPath(JIPipeBackupSnapshot snapshot) {
        Map<String, JIPipeBackupSnapshot.SnapshotEntry> map = new LinkedHashMap<>();
        for (JIPipeBackupSnapshot.SnapshotEntry e : snapshot.getEntries())
            map.put(e.getPath(), e);
        return map;
    }
}
