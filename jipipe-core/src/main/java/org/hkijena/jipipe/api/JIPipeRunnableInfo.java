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
public class JIPipeRunnableInfo {
    private EventBus eventBus = new EventBus();
    private AtomicBoolean cancelled = new AtomicBoolean();
    private AtomicInteger progress = new AtomicInteger(0);
    private AtomicInteger maxProgress = new AtomicInteger(1);
    private StringBuilder log = new StringBuilder();
    private String logPrepend = "";

    public JIPipeRunnableInfo() {
    }

    public JIPipeRunnableInfo(JIPipeRunnableInfo other) {
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
        log.append("<").append(progress).append("/").append(maxProgress).append("> ").append(logPrepend).append(message).append("\n");
    }

    public synchronized JIPipeRunnableInfo resolve(String logPrepend) {
        JIPipeRunnableInfo result = new JIPipeRunnableInfo(this);
        if(StringUtils.isNullOrEmpty(result.logPrepend))
            result.logPrepend = logPrepend;
        else
            result.logPrepend = result.logPrepend + " | " + logPrepend;
        return result;
    }

    public synchronized JIPipeRunnableInfo resolve(String text, int index, int size) {
        return resolve(text + " " + (index + 1) + "/" + size);
    }

    public synchronized JIPipeRunnableInfo resolveAndLog(String logPrepend) {
        JIPipeRunnableInfo resolve = resolve(logPrepend);
        resolve.log("");
        return resolve;
    }

    public synchronized JIPipeRunnableInfo resolveAndLog(String text, int index, int size) {
        return resolveAndLog(text + " " + (index + 1) + "/" + size);
    }

    public void setProgress(int count, int total) {
        setProgress(count);
        setMaxProgress(total);
    }
}
