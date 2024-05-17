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

package org.hkijena.jipipe.desktop.app.running;

import org.hkijena.jipipe.api.events.AbstractJIPipeEvent;
import org.hkijena.jipipe.api.events.JIPipeEventEmitter;
import org.hkijena.jipipe.api.notifications.JIPipeNotificationInbox;
import org.hkijena.jipipe.api.run.JIPipeRunnable;
import org.hkijena.jipipe.api.run.JIPipeRunnableLogEntry;
import org.hkijena.jipipe.api.run.JIPipeRunnableQueue;
import org.hkijena.jipipe.plugins.settings.JIPipeRuntimeApplicationSettings;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class JIPipeDesktopRunnableLogsCollection implements JIPipeRunnable.FinishedEventListener, JIPipeRunnable.InterruptedEventListener {
    private static JIPipeDesktopRunnableLogsCollection INSTANCE;
    private final List<JIPipeRunnableLogEntry> logEntries = new ArrayList<>();
    private final LogEntryAddedEventEmitter logEntryAddedEventEmitter = new LogEntryAddedEventEmitter();
    private final LogClearedEventEmitter logClearedEventEmitter = new LogClearedEventEmitter();
    private final LogUpdatedEventEmitter logUpdatedEventEmitter = new LogUpdatedEventEmitter();

    public JIPipeDesktopRunnableLogsCollection() {
        // Listen for default queue
        JIPipeRunnableQueue.getInstance().getFinishedEventEmitter().subscribe(this);
        JIPipeRunnableQueue.getInstance().getInterruptedEventEmitter().subscribe(this);

        // Listen for artifacts queue
        JIPipeRunnableQueue.getInstance().getFinishedEventEmitter().subscribe(this);
        JIPipeRunnableQueue.getInstance().getInterruptedEventEmitter().subscribe(this);
    }

    public static JIPipeDesktopRunnableLogsCollection getInstance() {
        if (INSTANCE == null) {
            INSTANCE = new JIPipeDesktopRunnableLogsCollection();
        }
        return INSTANCE;
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
        logEntries.clear();
        logClearedEventEmitter.emit(new LogClearedEvent(this));
        logUpdatedEventEmitter.emit(new LogUpdatedEvent(this));
    }

    public void pushToLog(JIPipeRunnableLogEntry entry) {
        logEntries.add(entry);
        logEntryAddedEventEmitter.emit(new LogEntryAddedEvent(this, entry));
        logUpdatedEventEmitter.emit(new LogUpdatedEvent(this));
    }

    private void pushToLog(JIPipeRunnable run, boolean success) {
        StringBuilder log = run.getProgressInfo().getLog();
        if (log != null && log.length() > 0) {
            final JIPipeRuntimeApplicationSettings runtimeSettings = JIPipeRuntimeApplicationSettings.getInstance();
            if (runtimeSettings != null && logEntries.size() + 1 > runtimeSettings.getLogLimit())
                logEntries.remove(0);
            JIPipeRunnableLogEntry entry = new JIPipeRunnableLogEntry(run.getTaskLabel(), LocalDateTime.now(), log.toString(), new JIPipeNotificationInbox(run.getProgressInfo().getNotifications()), success);
            logEntries.add(entry);
            if (run.isLogSilent()) {
                // Auto-mark as read
                entry.setRead(true);
            }
            logEntryAddedEventEmitter.emit(new LogEntryAddedEvent(this, entry));
            logUpdatedEventEmitter.emit(new LogUpdatedEvent(this));
        }
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
        pushToLog(event.getRun(), true);
    }

    @Override
    public void onRunnableInterrupted(JIPipeRunnable.InterruptedEvent event) {
        pushToLog(event.getRun(), false);
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
