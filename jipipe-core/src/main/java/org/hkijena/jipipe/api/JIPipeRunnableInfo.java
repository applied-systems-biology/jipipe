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

/**
 * This object is available inside a {@link JIPipeRunnable} and contains methods to report progress, logs, and request cancellation.
 */
public class JIPipeRunnableInfo {
    private EventBus eventBus = new EventBus();
    private AtomicBoolean cancelled = new AtomicBoolean();
    private int progress = 0;
    private int maxProgress = 1;
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

    public synchronized void incrementProgress() {
        progress += 1;
    }

    public synchronized int getProgress() {
        return progress;
    }

    public synchronized void setProgress(int progress) {
        this.progress = progress;
    }

    public synchronized int getMaxProgress() {
        return maxProgress;
    }

    public synchronized void setMaxProgress(int maxProgress) {
        this.maxProgress = maxProgress;
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
}
