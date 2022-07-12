package org.hkijena.jipipe.ui.running;

import com.google.common.eventbus.EventBus;
import com.google.common.eventbus.Subscribe;
import org.hkijena.jipipe.api.JIPipeRunnable;
import org.hkijena.jipipe.extensions.settings.RuntimeSettings;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class JIPipeLogs {
    private static JIPipeLogs INSTANCE;

    private final EventBus eventBus = new EventBus();
    private final List<LogEntry> logEntries = new ArrayList<>();

    public JIPipeLogs() {
        JIPipeRunnerQueue.getInstance().getEventBus().register(this);
    }

    public static JIPipeLogs getInstance() {
        if (INSTANCE == null) {
            INSTANCE = new JIPipeLogs();
        }
        return INSTANCE;
    }

    public List<LogEntry> getLogEntries() {
        return Collections.unmodifiableList(logEntries);
    }

    public void clear() {
        logEntries.clear();
        eventBus.post(new LogClearedEvent(this));
    }

    public void pushToLog(LogEntry entry) {
        logEntries.add(entry);
        eventBus.post(new LogEntryAddedEvent(this, entry));
    }

    private void pushToLog(JIPipeRunnable run, boolean success) {
        StringBuilder log = run.getProgressInfo().getLog();
        if (log != null && log.length() > 0) {
            final RuntimeSettings runtimeSettings = RuntimeSettings.getInstance();
            if (logEntries.size() + 1 > runtimeSettings.getLogLimit())
                logEntries.remove(0);
            LogEntry entry = new LogEntry(run.getTaskLabel(), LocalDateTime.now(), log.toString(), success);
            logEntries.add(entry);
            eventBus.post(new LogEntryAddedEvent(this, entry));
        }
    }

    public EventBus getEventBus() {
        return eventBus;
    }

    @Subscribe
    public void onRunFinished(RunWorkerFinishedEvent event) {
        pushToLog(event.getRun(), true);
    }

    @Subscribe
    public void onRunCancelled(RunWorkerInterruptedEvent event) {
        pushToLog(event.getRun(), false);
    }

    public static class LogClearedEvent {
        private final JIPipeLogs logs;

        public LogClearedEvent(JIPipeLogs logs) {
            this.logs = logs;
        }

        public JIPipeLogs getLogs() {
            return logs;
        }
    }

    public static class LogEntryAddedEvent {
        private final JIPipeLogs logs;
        private final LogEntry entry;

        public LogEntryAddedEvent(JIPipeLogs logs, LogEntry entry) {
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
