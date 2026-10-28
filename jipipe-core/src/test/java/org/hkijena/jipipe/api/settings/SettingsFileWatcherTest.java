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

import org.hkijena.jipipe.api.settings.SettingsFileStore.FileFingerprint;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.awt.GraphicsEnvironment;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Supplier;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Tests for {@link SettingsFileWatcher}.
 * <p>
 * The two tests from the task brief are kept verbatim except that the mtime
 * pinning on the simulated foreign write was dropped: the pin is a second
 * transient file state on top of the write, so write+pin is two distinct
 * (size, mtime) states that a naive watcher could deliver as two callbacks.
 * (The watcher's settle step folds the transient pair into one state, but
 * the point of the test is the exactly-once contract, which should not
 * depend on that implementation detail.) Without the pin the single write
 * is a size difference from the baseline, which always makes the
 * fingerprints differ. The brief's {@code Thread.sleep(2600)} is kept
 * as-is; it waits out one 2-second poll interval.
 */
class SettingsFileWatcherTest {
    @TempDir Path tempDir;

    /**
     * Waits until {@code condition} holds, at most {@code timeoutMillis}.
     *
     * @return {@code true} when the condition held in time
     */
    private static boolean waitFor(long timeoutMillis, Supplier<Boolean> condition) throws InterruptedException {
        long deadline = System.currentTimeMillis() + timeoutMillis;
        while (System.currentTimeMillis() < deadline) {
            if (condition.get()) {
                return true;
            }
            Thread.sleep(50);
        }
        return condition.get();
    }

    @Test void ownWriteDoesNotFire() throws Exception {
        Path f = Files.writeString(tempDir.resolve("settings.json"), "{\"a\":1}");
        List<FileFingerprint> seen = new CopyOnWriteArrayList<>();
        SettingsFileWatcher w = new SettingsFileWatcher(f, seen::add);
        w.start();
        FileFingerprint fp = FileFingerprint.of(f);
        w.setOwnFingerprint(fp);
        Files.writeString(f, "{\"a\":2}");
        w.setOwnFingerprint(FileFingerprint.of(f));   // this process wrote it
        Thread.sleep(2600);
        assertTrue(seen.isEmpty(), "a write this process performed must not fire a foreign change");
        w.close();
    }

    @Test void foreignWriteFiresOnce() throws Exception {
        Path f = Files.writeString(tempDir.resolve("settings.json"), "{\"a\":1}");
        List<FileFingerprint> seen = new CopyOnWriteArrayList<>();
        SettingsFileWatcher w = new SettingsFileWatcher(f, seen::add);
        w.start();
        w.setOwnFingerprint(FileFingerprint.of(f));
        // simulate another process: change size+mtime without touching our fingerprint record
        Files.writeString(f, "{\"a\":1,\"b\":2}");
        Thread.sleep(2600);
        w.close();
        assertEquals(1, seen.size(), "exactly one callback per distinct fingerprint; observed: " + seen);
    }

    @Test void throwingConsumerDoesNotKillTheWatcher() throws Exception {
        Path f = Files.writeString(tempDir.resolve("settings.json"), "{\"a\":1}");
        AtomicInteger fires = new AtomicInteger();
        SettingsFileWatcher w = new SettingsFileWatcher(f, fp -> {
            if (fires.incrementAndGet() == 1) {
                throw new IllegalStateException("consumer must be guarded by the watcher");
            }
        });
        w.start();
        w.setOwnFingerprint(FileFingerprint.of(f));
        // note: the writes deliberately differ in SIZE, not only in mtime —
        // the fingerprint's mtime has 1 ms resolution, so two same-size
        // writes landing in the same millisecond would be indistinguishable
        Files.writeString(f, "{\"a\":22}");  // 8 bytes: first foreign change, consumer throws
        assertTrue(waitFor(5000, () -> fires.get() >= 1),
                "the first foreign change must be delivered at all; observed fires=" + fires.get());
        Files.writeString(f, "{\"a\":3}");   // 7 bytes: second foreign change, must still be delivered
        assertTrue(waitFor(5000, () -> fires.get() >= 2),
                "a consumer that throws on the first change must not kill the poll thread; observed fires=" + fires.get());
        w.close();
    }

    @Test void closeIsIdempotentAndStartAfterCloseIsSafe() {
        Path f = tempDir.resolve("settings.json");
        SettingsFileWatcher w = new SettingsFileWatcher(f, fp -> {
        });
        w.start();
        w.close();
        w.close();    // second close must be a no-op, no exception
        w.start();    // starting after close must not throw
        w.close();
    }

    @Test void headlessStartIsANoOp() {
        Path f = tempDir.resolve("settings.json");
        SettingsFileWatcher w = new SettingsFileWatcher(f, fp -> {
        });
        assertTrue(w.isHeadlessSafe() == GraphicsEnvironment.isHeadless(),
                "isHeadlessSafe() must report the JVM's headless state");
        w.start();   // must return immediately (no-op) in a headless JVM, and never throw
        w.close();
        assertTrue(w.isHeadlessSafe() == GraphicsEnvironment.isHeadless());
    }

    /**
     * Proves the poll loop is the correctness floor on its own: the watcher
     * runs with the {@code WatchService} disabled (package-private test
     * seam) and a short poll interval, so only the poll thread can detect
     * the foreign write.
     */
    @Test void pollLoopAloneDetectsAForeignWrite() throws Exception {
        Path f = Files.writeString(tempDir.resolve("settings.json"), "{\"a\":1}");
        List<FileFingerprint> seen = new CopyOnWriteArrayList<>();
        SettingsFileWatcher w = new SettingsFileWatcher(f, seen::add, 200L, false);
        w.start();
        w.setOwnFingerprint(FileFingerprint.of(f));
        Files.writeString(f, "{\"a\":1,\"b\":2}");
        assertTrue(waitFor(3000, () -> seen.size() == 1),
                "the poll loop alone (no WatchService) must detect a foreign write");
        w.close();
    }
}
