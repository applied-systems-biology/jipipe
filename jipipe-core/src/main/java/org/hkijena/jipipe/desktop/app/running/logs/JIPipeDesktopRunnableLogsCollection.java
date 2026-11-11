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

package org.hkijena.jipipe.desktop.app.running.logs;

import org.hkijena.jipipe.api.events.AbstractJIPipeEvent;
import org.hkijena.jipipe.api.events.JIPipeEventEmitter;
import org.hkijena.jipipe.api.project.JIPipeProject;
import org.hkijena.jipipe.api.progress.JIPipeProgressLog;
import org.hkijena.jipipe.api.run.JIPipeGraphRun;
import org.hkijena.jipipe.api.run.JIPipeQueuedRunnableExecutor;
import org.hkijena.jipipe.api.run.JIPipeRunnable;
import org.hkijena.jipipe.api.run.JIPipeRunnableExecutor;
import org.hkijena.jipipe.api.run.JIPipeRunnableLogEntry;
import org.hkijena.jipipe.plugins.settings.application.JIPipeRuntimeApplicationSettings;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;

/**
 * The Tools &gt; Logs collection: the finished runs of this session. Entries hold a reference to
 * the run's NDJSON data file (plus metadata) - not a rendered text copy - and are rendered from
 * that file on demand. The list is bounded by the {@code log-limit} setting: evicting an entry
 * deletes its data file, which is what bounds the disk usage of kept logs. Clearing the
 * collection deletes all data files.
 * <p>
 * Entries are tagged with their owning project (graph runs). Closing a project window drops that
 * project's entries and files; global entries persist for the session (their files live in the
 * global temporary base, which is cleaned up when it gets old).
 */
public class JIPipeDesktopRunnableLogsCollection implements JIPipeRunnable.FinishedEventListener, JIPipeRunnable.InterruptedEventListener {
    private static JIPipeDesktopRunnableLogsCollection INSTANCE;
    private final List<JIPipeRunnableLogEntry> logEntries = new ArrayList<>();
    private final LogEntryAddedEventEmitter logEntryAddedEventEmitter = new LogEntryAddedEventEmitter();
    private final LogClearedEventEmitter logClearedEventEmitter = new LogClearedEventEmitter();
    private final LogUpdatedEventEmitter logUpdatedEventEmitter = new LogUpdatedEventEmitter();
    private final Set<UUID> addedLogs = new HashSet<>();

    public JIPipeDesktopRunnableLogsCollection() {
        registerQueue(JIPipeQueuedRunnableExecutor.getInstance());
    }

    public static JIPipeDesktopRunnableLogsCollection getInstance() {
        if (INSTANCE == null) {
            INSTANCE = new JIPipeDesktopRunnableLogsCollection();
        }
        return INSTANCE;
    }

    public void registerQueue(JIPipeRunnableExecutor queue) {
        queue.getFinishedEventEmitter().subscribe(this);
        queue.getInterruptedEventEmitter().subscribe(this);
    }

    public void unregisterQueue(JIPipeRunnableExecutor queue) {
        queue.getFinishedEventEmitter().unsubscribe(this);
        queue.getInterruptedEventEmitter().unsubscribe(this);
    }

    public LogEntryAddedEventEmitter getLogEntryAddedEventEmitter() {
        return logEntryAddedEventEmitter;
    }

    public LogClearedEventEmitter getLogClearedEventEmitter() {
        return logClearedEventEmitter;
    }

    public List<JIPipeRunnableLogEntry> getLogEntries() {
        return Collections.unmodifiableList(logEntries);
    }

    public void clear() {
        for (JIPipeRunnableLogEntry entry : logEntries) {
            entry.deleteDataFile();
        }
        logEntries.clear();
        logClearedEventEmitter.emit(new LogClearedEvent(this));
        logUpdatedEventEmitter.emit(new LogUpdatedEvent(this));
    }

    /**
     * Removes and deletes the files of all entries owned by the given project. Called when a
     * project window closes (the project's temporary base - which contains the log files of its
     * graph runs - is cleaned up on close).
     *
     * @param project the project
     */
    public void removeProjectEntries(JIPipeProject project) {
        if (project == null) {
            return;
        }
        boolean changed = false;
        Iterator<JIPipeRunnableLogEntry> iterator = logEntries.iterator();
        while (iterator.hasNext()) {
            JIPipeRunnableLogEntry entry = iterator.next();
            if (entry.getProject() == project) {
                entry.deleteDataFile();
                iterator.remove();
                changed = true;
            }
        }
        if (changed) {
            logClearedEventEmitter.emit(new LogClearedEvent(this));
            logUpdatedEventEmitter.emit(new LogUpdatedEvent(this));
        }
    }

    public void pushToLog(JIPipeRunnableLogEntry entry) {
        insertEntry(entry, resolveLogLimit());
    }

    /**
     * Adds an entry to the list, evicting (and deleting the data files of) the oldest entries
     * while the list would exceed the given limit.
     *
     * @param entry the entry to add
     * @param limit the maximum number of kept entries
     */
    void insertEntry(JIPipeRunnableLogEntry entry, int limit) {
        int effectiveLimit = Math.max(1, limit);
        while (logEntries.size() + 1 > effectiveLimit) {
            JIPipeRunnableLogEntry evicted = logEntries.removeFirst();
            evicted.deleteDataFile();
        }
        logEntries.add(entry);
        logEntryAddedEventEmitter.emit(new LogEntryAddedEvent(this, entry));
        logUpdatedEventEmitter.emit(new LogUpdatedEvent(this));
    }

    public void pushToLog(JIPipeRunnable run, boolean success) {
        JIPipeProgressLog store = run.getProgressInfo().getStore();
        // The store file is created lazily on the first flushed record - a run that logged
        // nothing has neither file nor tail content and gets no entry (as before)
        Path file = store != null ? store.getFile() : null;
        boolean hasContent = (file != null && Files.exists(file)) || (store != null && !store.readRecent(1).isEmpty());
        if (!hasContent) {
            return;
        }
        JIPipeRunnableLogEntry entry = new JIPipeRunnableLogEntry(run, success);
        if (run instanceof JIPipeGraphRun) {
            entry.setProject(((JIPipeGraphRun) run).getProject());
        }
        if (run.isLogSilent()) {
            // Auto-mark as read
            entry.setRead(true);
        }
        pushToLog(entry);
    }

    /**
     * Resolves the maximum number of kept log entries from the {@code log-limit} setting.
     *
     * @return the limit (at least 1)
     */
    private static int resolveLogLimit() {
        JIPipeRuntimeApplicationSettings runtimeSettings = JIPipeRuntimeApplicationSettings.getInstance();
        if (runtimeSettings != null) {
            return Math.max(1, runtimeSettings.getLogLimit());
        }
        return Integer.MAX_VALUE;
    }

    public void markAllAsRead() {
        for (JIPipeRunnableLogEntry logEntry : logEntries) {
            logEntry.setRead(true);
        }
        logUpdatedEventEmitter.emit(new LogUpdatedEvent(this));
    }

    public void markAsRead(JIPipeRunnableLogEntry logEntry) {
        logEntry.setRead(true);
        logUpdatedEventEmitter.emit(new LogUpdatedEvent(this));
    }

    @Override
    public void onRunnableFinished(JIPipeRunnable.FinishedEvent event) {
        if (!addedLogs.contains(event.getRun().getRunUUID())) {
            addedLogs.add(event.getRun().getRunUUID());
            pushToLog(event.getRun(), true);
        }
    }

    @Override
    public void onRunnableInterrupted(JIPipeRunnable.InterruptedEvent event) {
        if (!addedLogs.contains(event.getRun().getRunUUID())) {
            addedLogs.add(event.getRun().getRunUUID());
            pushToLog(event.getRun(), false);
        }
    }

    public LogUpdatedEventEmitter getLogUpdatedEventEmitter() {
        return logUpdatedEventEmitter;
    }

    public interface LogClearedEventListener {
        void onLogCleared(LogClearedEvent event);
    }

    public interface LogEntryAddedEventListener {
        void onLogEntryAdded(LogEntryAddedEvent event);
    }

    public interface LogUpdatedEventListener {
        void onLogUpdated(LogUpdatedEvent event);
    }

    public static class LogClearedEvent extends AbstractJIPipeEvent {
        private final JIPipeDesktopRunnableLogsCollection logs;

        public LogClearedEvent(JIPipeDesktopRunnableLogsCollection logs) {
            super(logs);
            this.logs = logs;
        }

        public JIPipeDesktopRunnableLogsCollection getLogs() {
            return logs;
        }
    }

    public static class LogClearedEventEmitter extends JIPipeEventEmitter<LogClearedEvent, LogClearedEventListener> {
        @Override
        protected void call(LogClearedEventListener logClearedEventListener, LogClearedEvent event) {
            logClearedEventListener.onLogCleared(event);
        }
    }

    public static class LogEntryAddedEvent extends AbstractJIPipeEvent {
        private final JIPipeDesktopRunnableLogsCollection logs;
        private final JIPipeRunnableLogEntry entry;

        public LogEntryAddedEvent(JIPipeDesktopRunnableLogsCollection logs, JIPipeRunnableLogEntry entry) {
            super(logs);
            this.logs = logs;
            this.entry = entry;
        }

        public JIPipeDesktopRunnableLogsCollection getLogs() {
            return logs;
        }

        public JIPipeRunnableLogEntry getEntry() {
            return entry;
        }
    }

    public static class LogEntryAddedEventEmitter extends JIPipeEventEmitter<LogEntryAddedEvent, LogEntryAddedEventListener> {

        @Override
        protected void call(LogEntryAddedEventListener logEntryAddedEventListener, LogEntryAddedEvent event) {
            logEntryAddedEventListener.onLogEntryAdded(event);
        }
    }

    public static class LogUpdatedEvent extends AbstractJIPipeEvent {
        private final JIPipeDesktopRunnableLogsCollection logs;

        public LogUpdatedEvent(JIPipeDesktopRunnableLogsCollection logs) {
            super(logs);
            this.logs = logs;
        }

        public JIPipeDesktopRunnableLogsCollection getLogs() {
            return logs;
        }
    }

    public static class LogUpdatedEventEmitter extends JIPipeEventEmitter<LogUpdatedEvent, LogUpdatedEventListener> {

        @Override
        protected void call(LogUpdatedEventListener logUpdatedEventListener, LogUpdatedEvent event) {
            logUpdatedEventListener.onLogUpdated(event);
        }
    }

}
