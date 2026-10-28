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

import java.awt.GraphicsEnvironment;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.FileSystems;
import java.nio.file.Path;
import java.nio.file.StandardWatchEventKinds;
import java.nio.file.WatchEvent;
import java.nio.file.WatchKey;
import java.nio.file.WatchService;
import java.util.List;
import java.util.Objects;
import java.util.function.Consumer;

/**
 * Watches a settings file for changes made by <em>other</em> processes.
 * <p>
 * Detection runs on two complementary mechanisms:
 * <ul>
 * <li>a {@link WatchService} on the file's parent directory (filtered to the
 * file's own name) provides low latency;
 * <li>a poll thread that re-reads the file's
 * {@link FileFingerprint} every {@code pollIntervalMillis} (default
 * {@link #POLL_INTERVAL_MILLIS}) is the <b>correctness floor</b>: a
 * {@code WatchService} alone is unreliable on some platforms (Windows,
 * network drives, CI file systems), so the poll guarantees that no change
 * is ever missed, only possibly reported later.
 * </ul>
 * <p>
 * <b>Self-write suppression:</b> a process that saves its own settings file
 * must not treat its own write as a foreign change. The store owner calls
 * {@link #setOwnFingerprint(FileFingerprint)} after every save; an observed
 * fingerprint equal to the own fingerprint fires nothing.
 * <p>
 * <b>Debounce:</b> one physical write can surface as several watch events
 * ({@code ENTRY_CREATE} + {@code ENTRY_MODIFY}) or as a delete+create pair
 * (atomic replace), and the poll and the watch thread may both observe the
 * same change. The last <em>delivered</em> fingerprint is remembered; a
 * fingerprint equal to the own fingerprint or to the last delivered one
 * fires at most once. On {@link #start()} the watcher takes a baseline of
 * the file's current fingerprint so that the pre-existing state is not
 * reported as a change.
 * <p>
 * A non-atomic single write can additionally surface as a <em>sequence</em>
 * of intermediate file states (truncated size, then final content) within a
 * few milliseconds. To deliver one callback per physical write, a detected
 * change is settled: the watcher briefly waits, re-reads the fingerprint,
 * and delivers the settled state.
 * <p>
 * <b>Headless:</b> in a headless JVM {@link #start()} is a no-op, so CLI
 * runs never spawn watcher threads. Query the current state with
 * {@link #isHeadlessSafe()}.
 * <p>
 * Both worker threads are daemon threads, and every callback invocation is
 * guarded: a throwing consumer can never kill the polling. {@link #close()}
 * is idempotent and safe to call before {@link #start()}.
 * <p>
 * Known residual race: if another process writes the file between this
 * process performing a save and recording
 * {@link #setOwnFingerprint(FileFingerprint)}, that one write may be
 * reported once as foreign before the own fingerprint is in place. The
 * window is a few instructions wide; a missed foreign change would be far
 * worse, so the design prefers the (harmless) extra report over dropping a
 * real change.
 */
public final class SettingsFileWatcher implements AutoCloseable {
    /**
     * The default poll interval. The poll thread re-checks the file's
     * fingerprint at this interval. This poll is the correctness floor of
     * the watcher, not merely an optimization: {@code WatchService}
     * delivery is best-effort on several platforms.
     */
    public static final long POLL_INTERVAL_MILLIS = 2000;
    /**
     * When a change is detected, the watcher waits up to this long for the
     * write to settle (non-atomic writes may surface as a short sequence of
     * intermediate states, e.g. truncated size) before delivering it, so
     * that one physical write yields exactly one callback.
     */
    private static final long SETTLE_WAIT_MILLIS = 50;
    /**
     * The step between fingerprint re-reads inside {@link #settle}.
     */
    private static final long SETTLE_POLL_STEP_MILLIS = 10;

    private final Path file;
    private final Path parent;
    private final String fileName;
    private final Consumer<FileFingerprint> onForeignChange;
    private final long pollIntervalMillis;
    private final boolean useWatchService;

    /**
     * The last fingerprint delivered to the consumer. Null when nothing has
     * been delivered yet. Only accessed under the monitor of this instance
     * (and from {@link #start()}, which is synchronized), so no atomic
     * wrapper is needed.
     */
    private FileFingerprint lastDelivered;

    private volatile FileFingerprint ownFingerprint;
    private volatile boolean started;
    private volatile boolean closed;
    private volatile WatchService watchService;
    private volatile Thread pollThread;
    private volatile Thread watchThread;

    /**
     * Creates a watcher for the given settings file, using the default poll
     * interval and a {@code WatchService}.
     *
     * @param file the settings file to watch
     * @param onForeignChange callback invoked with the new fingerprint when
     *                        the file changes on disk due to a write this
     *                        process did not perform and did not record via
     *                        {@link #setOwnFingerprint(FileFingerprint)}.
     *                        It is invoked on the poll or watch thread; it
     *                        must not block for long.
     */
    public SettingsFileWatcher(Path file, Consumer<FileFingerprint> onForeignChange) {
        this(file, onForeignChange, POLL_INTERVAL_MILLIS, true);
    }

    /**
     * Test seam: constructor with an explicit poll interval and the option
     * to run without a {@code WatchService} (poll only). Not part of the
     * public API.
     */
    SettingsFileWatcher(Path file, Consumer<FileFingerprint> onForeignChange,
                        long pollIntervalMillis, boolean useWatchService) {
        this.file = Objects.requireNonNull(file, "file");
        this.parent = Objects.requireNonNull(file.toAbsolutePath().getParent(), "parent of " + file);
        this.fileName = file.getFileName().toString();
        this.onForeignChange = Objects.requireNonNull(onForeignChange, "onForeignChange");
        this.pollIntervalMillis = Math.max(1L, pollIntervalMillis);
        this.useWatchService = useWatchService;
    }

    /**
     * Starts watching.
     * <p>
     * In a headless JVM this is a no-op returning immediately: CLI runs do
     * not spawn watcher threads. Calling it when the watcher is already
     * running is a safe no-op. A watcher is single-use: once
     * {@link #close()} has been called, further {@code start()} calls log a
     * message and do nothing. Before any worker thread is started, the
     * current file fingerprint is taken as a baseline so that the
     * pre-existing state is not reported.
     */
    public synchronized void start() {
        if (closed) {
            System.err.println("SettingsFileWatcher: start() after close() is ignored for " + file);
            return;
        }
        if (started) {
            return;
        }
        if (GraphicsEnvironment.isHeadless()) {
            return;
        }
        baseline();
        started = true;
        WatchService service = null;
        try {
            if (useWatchService) {
                service = FileSystems.getDefault().newWatchService();
                parent.register(service,
                        StandardWatchEventKinds.ENTRY_CREATE,
                        StandardWatchEventKinds.ENTRY_MODIFY,
                        StandardWatchEventKinds.ENTRY_DELETE);
                watchService = service;
                watchThread = spawnDaemon("SettingsFileWatcher-watch-" + fileName, this::watchLoop);
            }
            pollThread = spawnDaemon("SettingsFileWatcher-poll-" + fileName, this::pollLoop);
        } catch (IOException e) {
            started = false;
            if (watchService != null) {
                closeQuietly(watchService);
                watchService = null;
            }
            closeQuietly(service);   // a service that failed to register
            if (watchThread != null) {
                watchThread.interrupt();
                watchThread = null;
            }
            throw new UncheckedIOException("Could not start file watcher for settings file " + file, e);
        }
    }

    /**
     * Records the fingerprint this process just wrote to the file. An
     * observed change whose fingerprint equals this one is a self-write and
     * fires no callback.
     *
     * @param ownFingerprint the fingerprint of the file after this process's
     *                       last save, or {@code null} to clear the record
     */
    public void setOwnFingerprint(FileFingerprint ownFingerprint) {
        this.ownFingerprint = ownFingerprint;
    }

    /**
     * @return {@code true} when the JVM is headless, in which case
     * {@link #start()} is a no-op
     */
    public boolean isHeadlessSafe() {
        return GraphicsEnvironment.isHeadless();
    }

    /**
     * Stops both worker threads and closes the watch service. Idempotent;
     * safe to call before {@link #start()}. The worker threads are daemon
     * threads, so a forgotten close cannot keep the JVM alive.
     */
    @Override
    public synchronized void close() {
        if (!closed) {
            closed = true;
        }
        if (watchThread != null) {
            watchThread.interrupt();
            watchThread = null;
        }
        if (pollThread != null) {
            pollThread.interrupt();
            pollThread = null;
        }
        closeQuietly(watchService);
        watchService = null;
        started = false;
    }

    /**
     * Takes the file's current fingerprint as the baseline of
     * {@link #lastDelivered} so that the pre-existing state is not reported
     * as a change. A missing or unreadable file leaves the baseline null,
     * so the first successfully observed fingerprint is reported.
     */
    private void baseline() {
        lastDelivered = readFingerprint();
    }

    private static Thread spawnDaemon(String name, Runnable task) {
        Thread thread = new Thread(task, name);
        thread.setDaemon(true);
        thread.start();
        return thread;
    }

    private static void closeQuietly(WatchService service) {
        if (service != null) {
            try {
                service.close();
            } catch (IOException ignored) {
                // closing a watcher must not fail close()
            }
        }
    }

    /**
     * Blocks on the watch service, filtering events to this watcher's file
     * name. {@link StandardWatchEventKinds#OVERFLOW} is treated as a
     * possible change. Any relevant event merely triggers a fingerprint
     * re-check; the actual change decision (and deduplication) is made in
     * {@link #checkNow()} so that the poll and the watch thread agree.
     */
    private void watchLoop() {
        WatchService service = watchService;
        if (service == null) {
            return;
        }
        while (!closed) {
            WatchKey key;
            try {
                key = service.take();
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                return;
            }
            List<WatchEvent<?>> events = key.pollEvents();
            if (events == null) {
                return;   // the watch service was closed while we were in take()
            }
            boolean relevant = false;
            for (WatchEvent<?> event : events) {
                if (event.kind() == StandardWatchEventKinds.OVERFLOW) {
                    relevant = true;
                    continue;
                }
                Object context = event.context();
                if (context instanceof Path changed && changed.toString().equals(fileName)) {
                    relevant = true;
                }
            }
            if (!key.reset()) {
                return;   // the parent directory is gone
            }
            if (relevant) {
                checkNow();
            }
        }
    }

    /**
     * Re-checks the file's fingerprint at the fixed interval. This loop is
     * the correctness floor of the watcher: it runs even if the watch
     * service never delivers.
     */
    private void pollLoop() {
        while (!closed) {
            checkNow();
            try {
                Thread.sleep(pollIntervalMillis);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                return;
            }
        }
    }

    /**
     * Re-checks the file's fingerprint and, if it changed, settles the
     * change and delivers it. Settling and delivering are done under the
     * monitor so that two concurrent checkers of the same change deliver it
     * exactly once.
     */
    private void checkNow() {
        FileFingerprint current = readFingerprint();
        if (current == null) {
            return;   // the file is absent; the next successful read will fire
        }
        synchronized (this) {
            if (closed) {
                return;
            }
            FileFingerprint own = ownFingerprint;
            if (own != null && own.equals(current)) {
                return;   // this process wrote it
            }
            if (lastDelivered != null && lastDelivered.equals(current)) {
                return;   // already delivered (by either thread)
            }
            lastDelivered = settle(current);
            // The owner may have recorded the fingerprint of this very write
            // while we were settling it (a save whose write we observed):
            // re-check under the same lock, so a self-write is suppressed
            // even if setOwnFingerprint lands inside the settle window.
            FileFingerprint ownAfterSettle = ownFingerprint;
            if (ownAfterSettle != null && ownAfterSettle.equals(lastDelivered)) {
                return;   // this process wrote it
            }
            deliver(lastDelivered);
        }
    }

    /**
     * Waits for the file to stop changing, so that a non-atomic write,
     * which can surface as a short sequence of intermediate states
     * (e.g. truncated size), settles into its final state.
     *
     * @param firstState the state that triggered the settlement
     * @return the settled state
     */
    private FileFingerprint settle(FileFingerprint firstState) {
        FileFingerprint current = firstState;
        long deadline = System.currentTimeMillis() + SETTLE_WAIT_MILLIS;
        while (System.currentTimeMillis() < deadline && !closed) {
            FileFingerprint next = readFingerprint();
            if (next != null && !next.equals(current)) {
                current = next;
            }
            try {
                Thread.sleep(SETTLE_POLL_STEP_MILLIS);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                return current;
            }
        }
        return current;
    }

    /**
     * Invokes the consumer; a throwing consumer must never kill the polling
     * thread.
     */
    private void deliver(FileFingerprint fingerprint) {
        try {
            onForeignChange.accept(fingerprint);
        } catch (Throwable t) {
            System.err.println("SettingsFileWatcher: foreign-change callback for " + file
                    + " threw; the watcher keeps running: " + t);
        }
    }

    /**
     * @return the file's current fingerprint, or {@code null} when the file
     * does not exist or cannot be read
     */
    private FileFingerprint readFingerprint() {
        try {
            return FileFingerprint.of(file);
        } catch (IOException e) {
            return null;
        }
    }
}
