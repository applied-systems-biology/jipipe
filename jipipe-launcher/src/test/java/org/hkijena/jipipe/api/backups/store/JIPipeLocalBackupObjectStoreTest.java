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
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.util.Arrays;
import java.util.HexFormat;
import java.util.concurrent.TimeUnit;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The content-addressed backup object store: atomic puts, dedup,
 * mark-and-sweep GC, and the bounded cross-process write lock.
 * <p>
 * Pure IO tests — no JIPipe registry / ImageJ involved.
 */
class JIPipeLocalBackupObjectStoreTest {

    private static byte[] contentOf(String s) {
        return s.getBytes(StandardCharsets.UTF_8);
    }

    private static String sha256Hex(byte[] bytes) throws Exception {
        return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(bytes));
    }

    /** Collects all files (recursively) under the given directory. */
    private static long countFiles(Path dir) throws IOException {
        if (!Files.isDirectory(dir))
            return 0;
        try (Stream<Path> s = Files.walk(dir)) {
            return s.filter(Files::isRegularFile).count();
        }
    }

    private static long countTempFiles(Path dir) throws IOException {
        if (!Files.isDirectory(dir))
            return 0;
        try (Stream<Path> s = Files.walk(dir)) {
            return s.filter(Files::isRegularFile)
                    .filter(p -> p.getFileName().toString().startsWith(".tmp-")).count();
        }
    }

    private static void writeSnapshot(Path snapshotsDir, String name, String json) throws IOException {
        Files.createDirectories(snapshotsDir);
        Files.writeString(snapshotsDir.resolve(name), json);
    }

    private static byte[] toBytes(InputStream in) throws IOException {
        try (in) {
            return in.readAllBytes();
        }
    }

    @Test
    void putGetRoundTrip(@TempDir Path tempDir) throws Exception {
        JIPipeLocalBackupObjectStore store = new JIPipeLocalBackupObjectStore(tempDir);
        Path source = tempDir.resolve("input.txt");
        byte[] bytes = contentOf("Hello, backup store!");
        Files.write(source, bytes);

        String hash = store.put(source);
        assertEquals(64, hash.length());
        assertTrue(hash.matches("[0-9a-f]{64}"), "expected lowercase hex, got: " + hash);
        assertEquals(sha256Hex(bytes), hash, "stored hash must be the SHA-256 of the content");

        assertTrue(store.has(hash));
        assertEquals(bytes.length, store.sizeOf(hash));

        assertArrayEquals(bytes, toBytes(store.get(hash)));
    }

    @Test
    void dedupStoresOnce(@TempDir Path tempDir) throws Exception {
        JIPipeLocalBackupObjectStore store = new JIPipeLocalBackupObjectStore(tempDir);
        byte[] bytes = contentOf("deduplicated content");

        Path a = tempDir.resolve("a.bin");
        Path b = tempDir.resolve("b.bin");
        Files.write(a, bytes);
        Files.write(b, bytes);

        String h1 = store.put(a);
        String h2 = store.put(b);
        assertEquals(h1, h2, "same content must produce the same hash");
        assertTrue(store.has(h1));

        Path objects = tempDir.resolve("store").resolve("objects");
        assertEquals(1, countFiles(objects), "exactly one object file for identical content");
    }

    @Test
    void putIsAtomic(@TempDir Path tempDir) throws Exception {
        JIPipeLocalBackupObjectStore store = new JIPipeLocalBackupObjectStore(tempDir);
        for (int i = 0; i < 3; i++) {
            Path f = tempDir.resolve("f" + i + ".bin");
            Files.write(f, contentOf("content-" + i));
            store.put(f);
            byte[] streamed = contentOf("streamed-" + i);
            store.put(new ByteArrayInputStream(streamed), streamed.length);
        }
        Path objects = tempDir.resolve("store").resolve("objects");
        assertEquals(0, countTempFiles(objects), "no temp files may remain after successful puts");
        assertEquals(6, countFiles(objects), "6 distinct objects expected");
    }

    @Test
    void garbageCollectSweepsUnreferencedAndKeepsReferenced(@TempDir Path tempDir) throws Exception {
        JIPipeLocalBackupObjectStore store = new JIPipeLocalBackupObjectStore(tempDir);
        byte[] kept = contentOf("referenced object");
        byte[] swept = contentOf("unreferenced object");
        String keptHash = sha256Hex(kept);
        String sweptHash = sha256Hex(swept);

        Path f1 = tempDir.resolve("1.bin");
        Path f2 = tempDir.resolve("2.bin");
        Files.write(f1, kept);
        Files.write(f2, swept);
        assertEquals(keptHash, store.put(f1));
        assertEquals(sweptHash, store.put(f2));

        // A stray temp file left behind by an interrupted put
        Path objects = tempDir.resolve("store").resolve("objects");
        Path strayDir = objects.resolve(sweptHash.substring(0, 2));
        Files.createDirectories(strayDir);
        Path stray = strayDir.resolve(".tmp-1234");
        Files.writeString(stray, "interrupted");

        // One snapshot manifest referencing only the kept object
        writeSnapshot(tempDir.resolve("snapshots"), "snap-1.json",
                "{\"entries\":[{\"path\":\"a\",\"size\":1,\"hash\":\"" + keptHash + "\"}]}");

        assertTrue(store.acquireWriteLock(5000));
        try {
            store.garbageCollect(JIPipeProgressInfo.SILENT);
        } finally {
            store.releaseWriteLock();
        }

        assertTrue(store.has(keptHash), "referenced object must survive GC");
        assertArrayEquals(kept, toBytes(store.get(keptHash)));
        assertFalse(store.has(sweptHash), "unreferenced object must be swept");
        assertFalse(Files.exists(stray), "stray temp file must be swept");
    }

    @Test
    void garbageCollectIsIdempotent(@TempDir Path tempDir) throws Exception {
        JIPipeLocalBackupObjectStore store = new JIPipeLocalBackupObjectStore(tempDir);
        byte[] bytes = contentOf("idempotent");
        String hash = sha256Hex(bytes);
        Path f = tempDir.resolve("f.bin");
        Files.write(f, bytes);
        store.put(f);
        writeSnapshot(tempDir.resolve("snapshots"), "snap.json",
                "{\"entries\":[{\"path\":\"a\",\"size\":1,\"hash\":\"" + hash + "\"}]}");

        assertTrue(store.acquireWriteLock(5000));
        try {
            store.garbageCollect(JIPipeProgressInfo.SILENT);
            store.garbageCollect(JIPipeProgressInfo.SILENT);
        } finally {
            store.releaseWriteLock();
        }

        assertTrue(store.has(hash), "GC twice must still leave referenced objects intact");
        assertArrayEquals(bytes, toBytes(store.get(hash)));
    }

    @Test
    void truncatedObjectFailsOnRead(@TempDir Path tempDir) throws Exception {
        JIPipeLocalBackupObjectStore store = new JIPipeLocalBackupObjectStore(tempDir);
        byte[] bytes = contentOf("to be truncated");
        String hash = store.put(new ByteArrayInputStream(bytes), bytes.length);
        assertTrue(store.has(hash));

        // Open the stream (the expected length is snapshotted at open time),
        // then corrupt the object on disk by truncating it: reading to the end
        // must fail with an IOException that names the hash.
        InputStream in = store.get(hash);
        Path objectFile = tempDir.resolve("store").resolve("objects")
                .resolve(hash.substring(0, 2)).resolve(hash);
        assertTrue(Files.exists(objectFile));
        Files.write(objectFile, Arrays.copyOf(bytes, bytes.length / 2));

        IOException e = assertThrows(IOException.class, () -> toBytes(in));
        assertTrue(e.getMessage().contains(hash), "error must name the hash, got: " + e.getMessage());
    }

    @Test
    void crossProcessLockIsBounded(@TempDir Path tempDir) throws Exception {
        JIPipeLocalBackupObjectStore first = new JIPipeLocalBackupObjectStore(tempDir);
        JIPipeLocalBackupObjectStore second = new JIPipeLocalBackupObjectStore(tempDir);

        assertTrue(first.acquireWriteLock(5000), "first instance must acquire the lock");
        try {
            long start = System.nanoTime();
            boolean acquired = second.acquireWriteLock(1500);
            long elapsed = TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - start);
            assertFalse(acquired, "second instance must time out while the first holds the lock");
            assertTrue(elapsed >= 1000, "must actually wait ~1.5 s before giving up, took " + elapsed + " ms");
            assertTrue(elapsed < 10_000, "bounded wait must return in sane wall time, took " + elapsed + " ms");

            // Lock-free reads work while another instance holds the write lock
            byte[] bytes = contentOf("readable while locked");
            String hash = first.put(new ByteArrayInputStream(bytes), bytes.length);
            assertTrue(second.has(hash));
            assertEquals(bytes.length, second.sizeOf(hash));
            assertArrayEquals(bytes, toBytes(second.get(hash)));
        } finally {
            first.releaseWriteLock();
        }

        // After release, the second instance acquires
        assertTrue(second.acquireWriteLock(5000), "lock must be acquirable after release");
        second.releaseWriteLock();
    }

    @Test
    void formatVersionGuard(@TempDir Path tempDir) throws Exception {
        Path format = tempDir.resolve("store").resolve("format.json");
        Files.createDirectories(format.getParent());
        Files.writeString(format, "{\"format-version\": 99}");

        JIPipeLocalBackupObjectStore store = new JIPipeLocalBackupObjectStore(tempDir);
        byte[] bytes = contentOf("will not be stored");
        IOException e = assertThrows(IOException.class,
                () -> store.put(new ByteArrayInputStream(bytes), bytes.length));
        assertTrue(e.getMessage().contains("Unsupported store format"), "got: " + e.getMessage());
        assertFalse(store.has(sha256Hex(bytes)), "nothing may be stored against a newer format version");
    }
}
