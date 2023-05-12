package org.hkijena.jipipe.ui.running;

import com.google.common.eventbus.EventBus;
import com.google.common.eventbus.Subscribe;
import org.hkijena.jipipe.api.JIPipeRunnable;
import org.hkijena.jipipe.api.events.AbstractJIPipeEvent;
import org.hkijena.jipipe.api.events.JIPipeEventEmitter;
import org.hkijena.jipipe.extensions.settings.RuntimeSettings;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class JIPipeLogs implements JIPipeRunnable.FinishedEventListener, JIPipeRunnable.InterruptedEventListener {
    private static JIPipeLogs INSTANCE;
    private final List<LogEntry> logEntries = new ArrayList<>();
    private final LogEntryAddedEventEmitter logEntryAddedEventEmitter = new LogEntryAddedEventEmitter();
    private final LogClearedEventEmitter logClearedEventEmitter = new LogClearedEventEmitter();

    public JIPipeLogs() {
        JIPipeRunnerQueue.getInstance().getFinishedEventEmitter().subscribe(this);
        JIPipeRunnerQueue.getInstance().getInterruptedEventEmitter().subscribe(this);
    }

    public static JIPipeLogs getInstance() {
        if (INSTANCE == null) {
            INSTANCE = new JIPipeLogs();
        }
        return INSTANCE;
    }

    public LogEntryAddedEventEmitter getLogEntryAddedEventEmitter() {
        return logEntryAddedEventEmitter;
    }

    public LogClearedEventEmitter getLogClearedEventEmitter() {
        return logClearedEventEmitter;
    }

    public List<LogEntry> getLogEntries() {
        return Collections.unmodifiableList(logEntries);
    }

    public void clear() {
        logEntries.clear();
        logClearedEventEmitter.emit(new LogClearedEvent(this));
    }

    public void pushToLog(LogEntry entry) {
        logEntries.add(entry);
        logEntryAddedEventEmitter.emit(new LogEntryAddedEvent(this, entry));
    }

    private void pushToLog(JIPipeRunnable run, boolean success) {
        StringBuilder log = run.getProgressInfo().getLog();
        if (log != null && log.length() > 0) {
            final RuntimeSettings runtimeSettings = RuntimeSettings.getInstance();
            if (logEntries.size() + 1 > runtimeSettings.getLogLimit())
                logEntries.remove(0);
            LogEntry entry = new LogEntry(run.getTaskLabel(), LocalDateTime.now(), log.toString(), success);
            logEntries.add(entry);
            logEntryAddedEventEmitter.emit(new LogEntryAddedEvent(this, entry));
        }
    }

    @Override
    public void onRunnableFinished(JIPipeRunnable.FinishedEvent event) {
        pushToLog(event.getRun(), true);
    }

    @Override
    public void onRunnableInterrupted(JIPipeRunnable.InterruptedEvent event) {
        pushToLog(event.getRun(), false);
    }

    public static class LogClearedEvent extends AbstractJIPipeEvent {
        private final JIPipeLogs logs;

        public LogClearedEvent(JIPipeLogs logs) {
            super(logs);
            this.logs = logs;
        }

        public JIPipeLogs getLogs() {
            return logs;
        }
    }

    public interface LogClearedEventListener {
        void onLogCleared(LogClearedEvent event);
    }

    public static class LogClearedEventEmitter extends JIPipeEventEmitter<LogClearedEvent, LogClearedEventListener> {

        @Override
        protected void call(LogClearedEventListener logClearedEventListener, LogClearedEvent event) {
            logClearedEventListener.onLogCleared(event);
        }
    }

    public static class LogEntryAddedEvent extends AbstractJIPipeEvent {
        private final JIPipeLogs logs;
        private final LogEntry entry;

        public LogEntryAddedEvent(JIPipeLogs logs, LogEntry entry) {
            super(logs);
            this.logs = logs;
            this.entry = entry;
        }

        public JIPipeLogs getLogs() {
            return logs;
        }

        public LogEntry getEntry() {
            return entry;
        }
    }

    public interface LogEntryAddedEventListener {
        void onLogEntryAdded(LogEntryAddedEvent event);
    }

    public static class LogEntryAddedEventEmitter extends JIPipeEventEmitter<LogEntryAddedEvent, LogEntryAddedEventListener> {

        @Override
        protected void call(LogEntryAddedEventListener logEntryAddedEventListener, LogEntryAddedEvent event) {
            logEntryAddedEventListener.onLogEntryAdded(event);
        }
    }

    public static class LogEntry {
        private final String name;
        private final LocalDateTime dateTime;
        private final String log;
        private final boolean success;

        public LogEntry(String name, LocalDateTime dateTime, String log, boolean success) {
            this.name = name;
            this.dateTime = dateTime;
            this.log = log;
            this.success = success;
        }

        public LocalDateTime getDateTime() {
            return dateTime;
        }

        public String getLog() {
            return log;
        }

        public boolean isSuccess() {
            return success;
        }

        public String getName() {
            return name;
        }
    }
}
