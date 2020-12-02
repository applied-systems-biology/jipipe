/*
 * Copyright by Zoltán Cseresnyés, Ruman Gerst
 *
 * Research Group Applied Systems Biology - Head: Prof. Dr. Marc Thilo Figge
 * https://www.leibniz-hki.de/en/applied-systems-biology.html
 * HKI-Center for Systems Biology of Infection
 * Leibniz Institute for Natural Product Research and Infection Biology - Hans Knöll Institute (HKI)
 * Adolf-Reichwein-Straße 23, 07745 Jena, Germany
 *
 * The project code is licensed under BSD 2-Clause.
 * See the LICENSE file provided with the code for the full license.
 */

package org.hkijena.jipipe.api;

import com.google.common.eventbus.EventBus;
import org.hkijena.jipipe.utils.StringUtils;

import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * This object is available inside a {@link JIPipeRunnable} and contains methods to report progress, logs, and request cancellation.
 */
public class JIPipeProgressInfo {
    private EventBus eventBus = new EventBus();
    private AtomicBoolean cancelled = new AtomicBoolean();
    private AtomicInteger progress = new AtomicInteger(0);
    private AtomicInteger maxProgress = new AtomicInteger(1);
    private StringBuilder log = new StringBuilder();
    private String logPrepend = "";

    public JIPipeProgressInfo() {
    }

    public JIPipeProgressInfo(JIPipeProgressInfo other) {
        this.eventBus = other.eventBus;
        this.cancelled = other.cancelled;
        this.progress = other.progress;
        this.maxProgress = other.maxProgress;
        this.log = other.log;
        this.logPrepend = other.logPrepend;
    }

    public synchronized void clearLog() {
        log.setLength(0);
    }

    public synchronized void incrementProgress() {
        progress.getAndIncrement();
    }

    public int getProgress() {
        return progress.get();
    }

    public void setProgress(int progress) {
        this.progress.set(progress);
    }

    public int getMaxProgress() {
        return maxProgress.get();
    }

    public void setMaxProgress(int maxProgress) {
        this.maxProgress.set(maxProgress);
    }

    public AtomicBoolean isCancelled() {
        return cancelled;
    }

    public synchronized StringBuilder getLog() {
        return log;
    }

    public synchronized void log(String message) {
        log.append("<").append(progress).append("/").append(maxProgress).append("> ").append(logPrepend);
        if (!StringUtils.isNullOrEmpty(logPrepend) && !StringUtils.isNullOrEmpty(message))
            log.append(" | ");
        log.append(" ").append(message);
        log.append("\n");
        eventBus.post(new StatusUpdatedEvent(this, progress.get(), maxProgress.get(), logPrepend + " " + message));
    }

    public synchronized JIPipeProgressInfo resolve(String logPrepend) {
        JIPipeProgressInfo result = new JIPipeProgressInfo(this);
        if (StringUtils.isNullOrEmpty(result.logPrepend))
            result.logPrepend = logPrepend;
        else
            result.logPrepend = result.logPrepend + " | " + logPrepend;
        return result;
    }

    public synchronized JIPipeProgressInfo resolve(String text, int index, int size) {
        return resolve(text + " " + (index + 1) + "/" + size);
    }

    public synchronized JIPipeProgressInfo resolveAndLog(String logPrepend) {
        JIPipeProgressInfo resolve = resolve(logPrepend);
        resolve.log("");
        return resolve;
    }

    public synchronized JIPipeProgressInfo resolveAndLog(String text, int index, int size) {
        return resolveAndLog(text + " " + (index + 1) + "/" + size);
    }

    public void setProgress(int count, int total) {
        setProgress(count);
        setMaxProgress(total);
    }

    public EventBus getEventBus() {
        return eventBus;
    }

    public void addMaxProgress(int maxProgress) {
        this.maxProgress.getAndAccumulate(maxProgress, Integer::sum);
    }

    public void addProgress(int progress) {
        this.progress.getAndAccumulate(progress, Integer::sum);
    }

    public static class StatusUpdatedEvent {
        private final JIPipeProgressInfo source;
        private final int progress;
        private final int maxProgress;
        private final String message;

        public StatusUpdatedEvent(JIPipeProgressInfo source, int progress, int maxProgress, String message) {
            this.source = source;
            this.progress = progress;
            this.maxProgress = maxProgress;
            this.message = message;
        }

        public JIPipeProgressInfo getSource() {
            return source;
        }

        public int getProgress() {
            return progress;
        }

        public int getMaxProgress() {
            return maxProgress;
        }

        public String getMessage() {
            return message;
        }
    }
}
