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
import org.hkijena.jipipe.api.nodes.JIPipeGraphNode;
import org.hkijena.jipipe.api.compartments.algorithms.JIPipeProjectCompartment;
import org.hkijena.jipipe.api.notifications.JIPipeNotificationInbox;
import org.hkijena.jipipe.api.project.JIPipeProject;
import org.hkijena.jipipe.api.project.jipx.JIPipeXProjectWriter;
import org.hkijena.jipipe.api.validation.JIPipeValidationReport;
import org.hkijena.jipipe.api.validation.contexts.UnspecifiedValidationReportContext;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.stream.Stream;
import java.util.zip.ZipFile;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Snapshot restore ({@code .jip} and {@code .jipx}) with verify-on-read,
 * and whole-store verification (spec §3.7). Restored projects are re-loaded
 * through {@link JIPipeProject#loadProject}, so the JIPipe registry is
 * required.
 * <p>
 * Restore is LOCK-FREE: the tests create + persist under the store write
 * lock, release it, and then restore/verify without re-acquiring.
 */
class JIPipeBackupRestoreTest {

    @BeforeAll
    static void ensureInstance() {
        JIPipe.ensureInstance();
    }

    /**
     * A .jip snapshot (a project with one node) restores to a loadable file
     * whose node count equals the original.
     */
    @Test
    void jipSnapshotRestoresAndLoads(@TempDir Path dir) throws Exception {
        Path backupRoot = dir.resolve("backup");
        JIPipeLocalBackupObjectStore store = new JIPipeLocalBackupObjectStore(backupRoot);
        assertTrue(store.acquireWriteLock(5000), "the test must hold the store write lock");

        JIPipeBackupSnapshot snapshot;
        int originalNodeCount;
        JIPipeProject project = new JIPipeProject();
        try {
            project.getMetadata().setName("JIP restore");
            assertNotNull(JIPipe.getNodes().getInfoById("io-interface"),
                    "the io-interface node must be registered for this test");
            // The node lives in a COMPARTMENT — real projects always place
            // nodes inside a compartment (a bare null-compartment node is a
            // test artifact that the .jip loader drops as a ghost node)
            JIPipeProjectCompartment compartment = project.addCompartment("Main");
            JIPipeGraphNode node = JIPipe.getNodes().getInfoById("io-interface").newInstance();
            project.getGraph().insertNode(node, compartment.getProjectCompartmentUUID());
            originalNodeCount = project.getGraph().getGraphNodes().size();
            assertEquals(1, originalNodeCount, "precondition: the project has one node");

            snapshot = JIPipeBackupStoreOperations.createJipSnapshot(
                    store, project, "restore-me.jip", JIPipeProgressInfo.SILENT);
        } finally {
            project.close(JIPipeProgressInfo.SILENT);
        }
        // Restore is lock-free: the write lock is NOT held for the restore.
        store.releaseWriteLock();

        Path target = dir.resolve("restored.jip");
        JIPipeBackupStoreOperations.restoreSnapshot(store, snapshot, target, JIPipeProgressInfo.SILENT);
        assertTrue(Files.isRegularFile(target), "the restored .jip file must exist");

        JIPipeProject reloaded = JIPipeProject.loadProject(target,
                new UnspecifiedValidationReportContext(), new JIPipeValidationReport(),
                new JIPipeNotificationInbox(), JIPipeProgressInfo.SILENT);
        try {
            assertEquals("JIP restore", reloaded.getMetadata().getName(),
                    "the restored project must deserialize to the same model");
            assertEquals(originalNodeCount, reloaded.getGraph().getGraphNodes().size(),
                    "the restored project must contain the same number of nodes");
        } finally {
            reloaded.close(JIPipeProgressInfo.SILENT);
        }
    }

    /**
     * A .jipx snapshot with a payload file restores to a loadable container
     * whose payload round-trips ONLY through the file: the original project
     * is closed before the restore, so the workspace of the loaded project
     * can only contain the payload if the container carried it. The
     * container entries must be at the ZIP ROOT (no wrapping folder).
     */
    @Test
    void jipxSnapshotWithPayloadRestoresAndLoads(@TempDir Path dir) throws Exception {
        // Build the container + payload + snapshot (the Task-3 idiom)
        byte[] payload = new byte[256 * 1024];
        new Random(7).nextBytes(payload);
        JIPipeBackupSnapshot snapshot = createJipxSnapshotWithPayload(dir, payload);

        // The source project is closed by createJipxSnapshotWithPayload: the
        // restore below can only read from the object store.
        Path target = dir.resolve("restored.jipx");
        JIPipeLocalBackupObjectStore store = new JIPipeLocalBackupObjectStore(dir.resolve("backup"));
        JIPipeBackupStoreOperations.restoreSnapshot(store, snapshot, target, JIPipeProgressInfo.SILENT);
        assertTrue(Files.isRegularFile(target), "the restored .jipx file must exist");

        // The payload must round-trip only through the file
        JIPipeProject reloaded = JIPipeProject.loadProject(target,
                new UnspecifiedValidationReportContext(), new JIPipeValidationReport(),
                new JIPipeNotificationInbox(), JIPipeProgressInfo.SILENT);
        try {
            Path restoredPayload = reloaded.getWorkspace().getBaseDirectory().resolve("data").resolve("payload.bin");
            assertTrue(Files.isRegularFile(restoredPayload),
                    "the payload must be inside the restored container");
            assertArrayEquals(payload, Files.readAllBytes(restoredPayload),
                    "the payload bytes must be identical after the round trip");
        } finally {
            reloaded.close(JIPipeProgressInfo.SILENT);
        }

        // Flat container: entries at the ZIP root, not wrapped in a folder
        assertTrue(zipContains(target, "data/payload.bin"),
                "the payload entry must be at the zip root under data/");
        assertTrue(zipContains(target, "project/metadata.json"),
                "the project parts must be at the zip root");
    }

    /**
     * A corrupted object (bytes flipped, size kept) aborts the restore with
     * a checksum error naming the hash; a deleted object aborts with a
     * missing error. Neither failure may write the target (or leave temp
     * assembly leftovers behind).
     */
    @Test
    void restoreDetectsCorruption(@TempDir Path dir) throws Exception {
        // (a) Corrupted object in a .jipx snapshot
        byte[] payload = new byte[1024];
        new Random(11).nextBytes(payload);
        JIPipeLocalBackupObjectStore store = new JIPipeLocalBackupObjectStore(dir.resolve("backup"));
        JIPipeBackupSnapshot snapshot = createJipxSnapshotWithPayload(dir, payload);
        String payloadHash = entryHashByPath(snapshot, "data/payload.bin");
        corruptObjectFile(dir.resolve("backup"), payloadHash);

        Path target = dir.resolve("corrupt-restore.jipx");
        IOException e = assertThrows(IOException.class,
                () -> JIPipeBackupStoreOperations.restoreSnapshot(store, snapshot, target, JIPipeProgressInfo.SILENT),
                "restoring a corrupted snapshot must fail");
        assertTrue(e.getMessage().contains(payloadHash), "the error must name the hash, got: " + e.getMessage());
        assertTrue(e.getMessage().contains("does not match its checksum"), "got: " + e.getMessage());
        assertFalse(Files.exists(target), "the target must not be written on failure");
        assertNoRestoreLeftovers(dir);

        // (b) Missing object in a .jip snapshot
        Path backupRoot2 = dir.resolve("backup2");
        JIPipeLocalBackupObjectStore store2 = new JIPipeLocalBackupObjectStore(backupRoot2);
        assertTrue(store2.acquireWriteLock(5000));
        JIPipeBackupSnapshot jipSnapshot;
        JIPipeProject project = new JIPipeProject();
        try {
            jipSnapshot = JIPipeBackupStoreOperations.createJipSnapshot(
                    store2, project, "missing.jip", JIPipeProgressInfo.SILENT);
        } finally {
            project.close(JIPipeProgressInfo.SILENT);
            store2.releaseWriteLock();
        }
        String hash = entryHashByPath(jipSnapshot, JIPipeBackupStoreOperations.JIP_ENTRY_PATH);
        deleteObjectFile(backupRoot2, hash);

        Path target2 = dir.resolve("missing-restore.jip");
        IOException e2 = assertThrows(IOException.class,
                () -> JIPipeBackupStoreOperations.restoreSnapshot(store2, jipSnapshot, target2, JIPipeProgressInfo.SILENT),
                "restoring a snapshot with a missing object must fail");
        assertTrue(e2.getMessage().contains(hash), "the error must name the hash, got: " + e2.getMessage());
        assertTrue(e2.getMessage().contains("missing"), "got: " + e2.getMessage());
        assertFalse(Files.exists(target2), "the target must not be written on failure");
    }

    /**
     * A hand-crafted manifest whose entry path escapes the restore assembly
     * directory (e.g. {@code ../evil.bin}) is rejected BEFORE anything is
     * written: the target is never created and no temp leftovers survive.
     */
    @Test
    void restoreRejectsEscapingEntryPath(@TempDir Path dir) throws Exception {
        Path backupRoot = dir.resolve("backup");
        JIPipeLocalBackupObjectStore store = new JIPipeLocalBackupObjectStore(backupRoot);

        // Hand-built snapshot (NOT produced by a creator) with a path that
        // normalizes outside the assembly directory. The object hash need not
        // exist — the guard fires before any object is read.
        JIPipeBackupSnapshot snapshot = new JIPipeBackupSnapshot();
        snapshot.setSnapshotId("escape-test");
        snapshot.setCreated(LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME));
        snapshot.setProjectFormat(JIPipeBackupStoreOperations.FORMAT_JIPX);
        snapshot.setProjectName("evil.jipx");
        JIPipeBackupSnapshot.SnapshotEntry entry = new JIPipeBackupSnapshot.SnapshotEntry();
        entry.setPath("../evil.bin");
        entry.setSize(4);
        entry.setHash("0000000000000000000000000000000000000000000000000000000000000000");
        snapshot.getEntries().add(entry);

        Path target = dir.resolve("evil-restored.jipx");
        IOException e = assertThrows(IOException.class,
                () -> JIPipeBackupStoreOperations.restoreSnapshot(store, snapshot, target, JIPipeProgressInfo.SILENT),
                "an entry path that escapes the restore directory must be rejected");
        assertTrue(e.getMessage().contains("escapes the restore directory"), "got: " + e.getMessage());
        assertTrue(e.getMessage().contains("../evil.bin"), "the error must name the offending path, got: " + e.getMessage());
        assertFalse(Files.exists(target), "the target must not be written");
        assertNoRestoreLeftovers(dir);
    }

    /**
     * verifyAll (lock-free dry run) reports problems and checked counts:
     * healthy store → no problems, exact counts; corrupted object → exactly
     * one checksum problem (the object still counts as checked); deleted
     * object → a missing problem, no longer counted.
     */
    @Test
    void verifyAllReportsProblems(@TempDir Path dir) throws Exception {
        Path backupRoot = dir.resolve("backup");
        JIPipeLocalBackupObjectStore store = new JIPipeLocalBackupObjectStore(backupRoot);
        byte[] payload = new byte[64 * 1024];
        new Random(13).nextBytes(payload);

        // Two snapshots of the same project with an in-memory rename in
        // between: the payload object is shared, one part object is new.
        JIPipeBackupSnapshot snapshotA;
        JIPipeBackupSnapshot snapshotB;
        assertTrue(store.acquireWriteLock(5000));
        JIPipeProject creator = new JIPipeProject();
        Path source = dir.resolve("source.jipx");
        creator.setWorkDirectory(dir);
        try {
            new JIPipeXProjectWriter().write(creator, source, JIPipeProgressInfo.SILENT);
        } finally {
            creator.close(JIPipeProgressInfo.SILENT);
        }
        JIPipeProject project = JIPipeProject.loadProject(source,
                new UnspecifiedValidationReportContext(), new JIPipeValidationReport(),
                new JIPipeNotificationInbox(), JIPipeProgressInfo.SILENT);
        try {
            Path payloadFile = project.getWorkspace().getBaseDirectory().resolve("data").resolve("payload.bin");
            Files.createDirectories(payloadFile.getParent());
            Files.write(payloadFile, payload);
            JIPipeBackupFilesCache cache = new JIPipeBackupFilesCache();
            snapshotA = JIPipeBackupStoreOperations.createJipxSnapshot(
                    store, project, cache, "source.jipx", JIPipeProgressInfo.SILENT);
            project.getMetadata().setName("Second snapshot");
            snapshotB = JIPipeBackupStoreOperations.createJipxSnapshot(
                    store, project, cache, "source.jipx", JIPipeProgressInfo.SILENT);
            Path sessionDir = backupRoot.resolve("session-verify");
            JIPipeProjectBackupSessionInfo info = sessionInfo("sess-verify", source);
            JIPipeBackupStoreOperations.persistSnapshot(store, snapshotA, sessionDir, info);
            JIPipeBackupStoreOperations.persistSnapshot(store, snapshotB, sessionDir, info);
        } finally {
            project.close(JIPipeProgressInfo.SILENT);
            store.releaseWriteLock();
        }
        // Verify is lock-free: the write lock is NOT held for the checks.

        // Expected unique-object counts across both manifests
        Map<String, Long> uniqueObjects = new HashMap<>();
        for (JIPipeBackupSnapshot s : List.of(snapshotA, snapshotB))
            for (JIPipeBackupSnapshot.SnapshotEntry e : s.getEntries())
                uniqueObjects.put(e.getHash(), e.getSize());
        int expectedObjects = uniqueObjects.size();
        long expectedBytes = uniqueObjects.values().stream().mapToLong(Long::longValue).sum();
        String payloadHash = entryHashByPath(snapshotA, "data/payload.bin");
        long payloadSize = payload.length;
        assertEquals(payloadSize, (long) uniqueObjects.get(payloadHash), "precondition: the payload is stored once");

        // (1) Healthy store: exact counts, no problems
        JIPipeBackupVerificationReport healthy = JIPipeBackupStoreOperations.verifyAll(store, JIPipeProgressInfo.SILENT);
        assertEquals(2, healthy.snapshotsChecked, "both persisted manifests must be checked");
        assertEquals(expectedObjects, healthy.objectsChecked, "each unique object must be checked exactly once");
        assertEquals(expectedBytes, healthy.bytesChecked, "the bytes of each unique object must be read exactly once");
        assertTrue(healthy.problems.isEmpty(), "a healthy store must have no problems: " + healthy.problems);

        // (2) Corrupted object (bytes flipped, size kept): exactly one
        // checksum problem naming the hash; the object still counts.
        corruptObjectFile(backupRoot, payloadHash);
        JIPipeBackupVerificationReport corrupt = JIPipeBackupStoreOperations.verifyAll(store, JIPipeProgressInfo.SILENT);
        assertEquals(2, corrupt.snapshotsChecked);
        assertEquals(expectedObjects, corrupt.objectsChecked,
                "a corrupted object was still fully read and re-hashed");
        assertEquals(expectedBytes, corrupt.bytesChecked);
        assertEquals(1, corrupt.problems.size(),
                "exactly one problem expected (dedup across the two snapshots), got: " + corrupt.problems);
        assertTrue(corrupt.problems.get(0).contains(payloadHash), "the problem must name the hash, got: " + corrupt.problems);
        assertTrue(corrupt.problems.get(0).contains("does not match its checksum"), "got: " + corrupt.problems);

        // (3) Deleted object: a missing problem, no longer counted
        deleteObjectFile(backupRoot, payloadHash);
        JIPipeBackupVerificationReport missing = JIPipeBackupStoreOperations.verifyAll(store, JIPipeProgressInfo.SILENT);
        assertEquals(2, missing.snapshotsChecked);
        assertEquals(expectedObjects - 1, missing.objectsChecked, "a missing object is not checked");
        assertEquals(expectedBytes - payloadSize, missing.bytesChecked);
        assertEquals(1, missing.problems.size(), "exactly one problem expected, got: " + missing.problems);
        assertTrue(missing.problems.get(0).contains("missing"), "got: " + missing.problems);
    }

    // ------------------------------------------------------------------
    // Helpers
    // ------------------------------------------------------------------

    /**
     * Builds a .jipx container with the given payload file
     * ({@code data/payload.bin}) in its workspace and snapshots it (the
     * Task-3 idiom). Returns the snapshot; the project is closed when the
     * method returns, so subsequent reads can only come from the store.
     */
    private static JIPipeBackupSnapshot createJipxSnapshotWithPayload(Path dir, byte[] payload) throws Exception {
        JIPipeProject creator = new JIPipeProject();
        Path source = dir.resolve("source.jipx");
        creator.setWorkDirectory(dir);
        new JIPipeXProjectWriter().write(creator, source, JIPipeProgressInfo.SILENT);
        creator.close(JIPipeProgressInfo.SILENT);

        JIPipeProject project = JIPipeProject.loadProject(source,
                new UnspecifiedValidationReportContext(), new JIPipeValidationReport(),
                new JIPipeNotificationInbox(), JIPipeProgressInfo.SILENT);
        try {
            Path payloadFile = project.getWorkspace().getBaseDirectory().resolve("data").resolve("payload.bin");
            Files.createDirectories(payloadFile.getParent());
            Files.write(payloadFile, payload);

            JIPipeLocalBackupObjectStore store = new JIPipeLocalBackupObjectStore(dir.resolve("backup"));
            assertTrue(store.acquireWriteLock(5000), "the test must hold the store write lock");
            try {
                return JIPipeBackupStoreOperations.createJipxSnapshot(
                        store, project, new JIPipeBackupFilesCache(), "source.jipx", JIPipeProgressInfo.SILENT);
            } finally {
                store.releaseWriteLock();
            }
        } finally {
            project.close(JIPipeProgressInfo.SILENT);
        }
    }

    private static String entryHashByPath(JIPipeBackupSnapshot snapshot, String path) {
        for (JIPipeBackupSnapshot.SnapshotEntry e : snapshot.getEntries())
            if (e.getPath().equals(path))
                return e.getHash();
        throw new AssertionError("No entry with path " + path + " in snapshot, got: "
                + snapshot.getEntries().stream().map(JIPipeBackupSnapshot.SnapshotEntry::getPath).toList());
    }

    /** Flips one byte of the object file on disk, keeping its size. */
    private static void corruptObjectFile(Path backupRoot, String hash) throws IOException {
        Path objectFile = backupRoot.resolve("store").resolve("objects").resolve(hash.substring(0, 2)).resolve(hash);
        byte[] bytes = Files.readAllBytes(objectFile);
        bytes[bytes.length / 2] ^= 0x01;
        Files.write(objectFile, bytes);
    }

    private static void deleteObjectFile(Path backupRoot, String hash) throws IOException {
        Files.delete(backupRoot.resolve("store").resolve("objects").resolve(hash.substring(0, 2)).resolve(hash));
    }

    private static boolean zipContains(Path zip, String entryName) throws IOException {
        try (ZipFile zf = new ZipFile(zip.toFile())) {
            return zf.getEntry(entryName) != null;
        }
    }

    private static void assertNoRestoreLeftovers(Path dir) throws IOException {
        try (Stream<Path> listing = Files.list(dir)) {
            List<Path> leftovers = listing
                    .filter(p -> p.getFileName().toString().startsWith(".jipipe-restore-")
                            || p.getFileName().toString().contains(".jipipe-zip-"))
                    .toList();
            assertTrue(leftovers.isEmpty(),
                    "no temp restore leftovers may survive a failed restore: " + leftovers);
        }
    }

    private static JIPipeProjectBackupSessionInfo sessionInfo(String sessionId, Path storagePath) {
        JIPipeProjectBackupSessionInfo info = new JIPipeProjectBackupSessionInfo();
        info.setProjectSessionId(sessionId);
        info.setProjectStoragePath(storagePath.toString());
        info.setLastDateTimeInfo(LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME));
        return info;
    }
}
