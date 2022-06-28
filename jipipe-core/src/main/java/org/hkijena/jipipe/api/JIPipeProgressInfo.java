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
import org.scijava.Cancelable;

import java.util.Collection;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.BiConsumer;

/**
 * This object is available inside a {@link JIPipeRunnable} and contains methods to report progress, logs, and request cancellation.
 */
public class JIPipeProgressInfo implements Cancelable {
    private EventBus eventBus = new EventBus();
    private AtomicBoolean cancelled = new AtomicBoolean();
    private AtomicInteger progress = new AtomicInteger(0);
    private AtomicInteger maxProgress = new AtomicInteger(1);
    private StringBuilder log = new StringBuilder();
    private String logPrepend = "";
    private AtomicBoolean logToStdOut = new AtomicBoolean(false);
    private boolean detachedProgress = false;

    private String cancelReason;

    public JIPipeProgressInfo() {
    }

    public JIPipeProgressInfo(JIPipeProgressInfo other) {
        this.eventBus = other.eventBus;
        this.cancelled = other.cancelled;
        this.progress = other.progress;
        this.maxProgress = other.maxProgress;
        this.log = other.log;
        this.logPrepend = other.logPrepend;
        this.logToStdOut = other.logToStdOut;
        this.detachedProgress = other.detachedProgress;
        this.cancelReason = other.cancelReason;
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

    public boolean isCancelled() {
        return cancelled.get();
    }

    public boolean isCanceled() {
        return cancelled.get();
    }

    @Override
    public void cancel(String reason) {
        cancel();
        cancelReason = reason;
    }

    @Override
    public String getCancelReason() {
        return cancelReason;
    }

    public void cancel() {
        cancelled.set(true);
    }

    public synchronized StringBuilder getLog() {
        return log;
    }

    public boolean isLogToStdOut() {
        return logToStdOut.get();
    }

    public void setLogToStdOut(boolean value) {
        logToStdOut.set(value);
    }

    /**
     * Writes a message into the log
     *
     * @param message the message
     */
    public synchronized void log(String message) {
        if (detachedProgress)
            log.append("SUB ");
        log.append("<").append(progress).append("/").append(maxProgress).append("> ").append(logPrepend);
        boolean needsSeparator = !StringUtils.isNullOrEmpty(logPrepend) && !StringUtils.isNullOrEmpty(message);
        if (needsSeparator)
            log.append(" | ");
        log.append(" ").append(message);
        log.append("\n");
        StatusUpdatedEvent event = new StatusUpdatedEvent(this, progress.get(), maxProgress.get(), logPrepend + (needsSeparator ? " | " : " ") + message);
        if (logToStdOut.get()) {
            System.out.println(event.render());
        }
        eventBus.post(event);
    }

    /**
     * Returns true if the progress was detached from the root {@link JIPipeProgressInfo}
     *
     * @return if the progress was detached
     */
    public boolean isDetachedProgress() {
        return detachedProgress;
    }

    /**
     * Creates a sub-progress that detaches the references to the global process counters.
     *
     * @return progress info with the same properties as the current one, but with a detached progress
     */
    public synchronized JIPipeProgressInfo detachProgress() {
        JIPipeProgressInfo result = new JIPipeProgressInfo(this);
        result.detachedProgress = true;
        result.progress = new AtomicInteger(this.progress.get());
        result.maxProgress = new AtomicInteger(this.maxProgress.get());
        return result;
    }

    /**
     * Creates a sub-progress that has the same cancellation and progress, but shows the new category in front of sub-messages
     *
     * @param logPrepend the category
     * @return progress info with the same properties as the current one, but with messages prepended
     */
    public synchronized JIPipeProgressInfo resolve(String logPrepend) {
        JIPipeProgressInfo result = new JIPipeProgressInfo(this);
        if (StringUtils.isNullOrEmpty(result.logPrepend))
            result.logPrepend = logPrepend;
        else
            result.logPrepend = result.logPrepend + " | " + logPrepend;
        return result;
    }

    /**
     * Creates a sub-progress that has the same cancellation and progress, but shows the new category in front of sub-messages.
     * This is a shortcut for resolve(text + " " + (index + 1) + "/" + size)
     *
     * @param text  the category
     * @param index index of the index operation
     * @param size  size of the index operation
     * @return progress info with the same properties as the current one, but with messages prepended
     */
    public synchronized JIPipeProgressInfo resolve(String text, int index, int size) {
        return resolve(text + " " + (index + 1) + "/" + size);
    }

    /**
     * Applies resolve(logPrepend) and then logs an empty message
     *
     * @param logPrepend the category
     * @return progress info with the same properties as the current one, but with messages prepended
     */
    public synchronized JIPipeProgressInfo resolveAndLog(String logPrepend) {
        JIPipeProgressInfo resolve = resolve(logPrepend);
        resolve.log("");
        return resolve;
    }

    /**
     * Creates a sub-progress that has the same cancellation and progress, but shows the new category in front of sub-messages.
     * Shortcut for resolveAndLog(text + " " + (index + 1) + "/" + size)
     *
     * @param text  the category
     * @param index index of the index operation
     * @param size  size of the index operation
     * @return progress info with the same properties as the current one, but with messages prepended
     */
    public synchronized JIPipeProgressInfo resolveAndLog(String text, int index, int size) {
        return resolveAndLog(text + " " + (index + 1) + "/" + size);
    }

    /**
     * Applies a for-each operation where the progress is logged
     *
     * @param text       the text
     * @param collection the iterated collection
     * @param function   the function executed for each item in the collection
     * @param <T>        collection contents
     */
    public <T> void resolveAndLogForEach(String text, Collection<T> collection, BiConsumer<T, JIPipeProgressInfo> function) {
        int size = collection.size();
        int current = 0;
        for (T item : collection) {
            if (isCancelled())
                return;
            log(text + " " + (current + 1) + "/" + size);
            JIPipeProgressInfo itemProgress = resolveAndLog(text + " " + (current + 1) + "/" + size);
            function.accept(item, itemProgress);
            ++current;
        }
    }

    /**
     * Applies a for-each operation where the progress is logged
     *
     * @param text       the text
     * @param collection the iterated collection
     * @param function   the function executed for each item in the collection
     * @param <T>        collection contents
     */
    public <T> void resolveAndLogForEach(String text, T[] collection, BiConsumer<T, JIPipeProgressInfo> function) {
        int size = collection.length;
        int current = 0;
        for (T item : collection) {
            if (isCancelled())
                return;
            log(text + " " + (current + 1) + "/" + size);
            JIPipeProgressInfo itemProgress = resolveAndLog(text + " " + (current + 1) + "/" + size);
            function.accept(item, itemProgress);
            ++current;
        }
    }

    /**
     * Applies a for-each operation where the progress is logged
     *
     * @param text       the text
     * @param collection the iterated collection
     * @param function   the function executed for each item in the collection
     */
    public void resolveAndLogForEach(String text, int[] collection, BiConsumer<Integer, JIPipeProgressInfo> function) {
        int size = collection.length;
        int current = 0;
        for (int item : collection) {
            if (isCancelled())
                return;
            log(text + " " + (current + 1) + "/" + size);
            JIPipeProgressInfo itemProgress = resolveAndLog(text + " " + (current + 1) + "/" + size);
            function.accept(item, itemProgress);
            ++current;
        }
    }

    /**
     * Applies a for-each operation where the progress is logged
     *
     * @param text       the text
     * @param collection the iterated collection
     * @param function   the function executed for each item in the collection
     */
    public void resolveAndLogForEach(String text, byte[] collection, BiConsumer<Byte, JIPipeProgressInfo> function) {
        int size = collection.length;
        int current = 0;
        for (byte item : collection) {
            if (isCancelled())
                return;
            log(text + " " + (current + 1) + "/" + size);
            JIPipeProgressInfo itemProgress = resolveAndLog(text + " " + (current + 1) + "/" + size);
            function.accept(item, itemProgress);
            ++current;
        }
    }

    /**
     * Applies a for-each operation where the progress is logged
     *
     * @param text       the text
     * @param collection the iterated collection
     * @param function   the function executed for each item in the collection
     */
    public void resolveAndLogForEach(String text, long[] collection, BiConsumer<Long, JIPipeProgressInfo> function) {
        int size = collection.length;
        int current = 0;
        for (long item : collection) {
            if (isCancelled())
                return;
            log(text + " " + (current + 1) + "/" + size);
            JIPipeProgressInfo itemProgress = resolveAndLog(text + " " + (current + 1) + "/" + size);
            function.accept(item, itemProgress);
            ++current;
        }
    }

    /**
     * Applies a for-each operation where the progress is logged
     *
     * @param text       the text
     * @param collection the iterated collection
     * @param function   the function executed for each item in the collection
     */
    public void resolveAndLogForEach(String text, short[] collection, BiConsumer<Short, JIPipeProgressInfo> function) {
        int size = collection.length;
        int current = 0;
        for (short item : collection) {
            if (isCancelled())
                return;
            log(text + " " + (current + 1) + "/" + size);
            JIPipeProgressInfo itemProgress = resolveAndLog(text + " " + (current + 1) + "/" + size);
            function.accept(item, itemProgress);
            ++current;
        }
    }

    /**
     * Applies a for-each operation where the progress is logged
     *
     * @param text       the text
     * @param collection the iterated collection
     * @param function   the function executed for each item in the collection
     */
    public void resolveAndLogForEach(String text, float[] collection, BiConsumer<Float, JIPipeProgressInfo> function) {
        int size = collection.length;
        int current = 0;
        for (float item : collection) {
            if (isCancelled())
                return;
            log(text + " " + (current + 1) + "/" + size);
            JIPipeProgressInfo itemProgress = resolveAndLog(text + " " + (current + 1) + "/" + size);
            function.accept(item, itemProgress);
            ++current;
        }
    }

    /**
     * Applies a for-each operation where the progress is logged
     *
     * @param text       the text
     * @param collection the iterated collection
     * @param function   the function executed for each item in the collection
     */
    public void resolveAndLogForEach(String text, double[] collection, BiConsumer<Double, JIPipeProgressInfo> function) {
        int size = collection.length;
        int current = 0;
        for (double item : collection) {
            if (isCancelled())
                return;
            JIPipeProgressInfo itemProgress = resolveAndLog(text + " " + (current + 1) + "/" + size);
            function.accept(item, itemProgress);
            ++current;
        }
    }

    /**
     * Sets the progress and max progress
     *
     * @param count the progress
     * @param total the max progress
     */
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

    public JIPipePercentageProgressInfo percentage(String text) {
        return new JIPipePercentageProgressInfo(resolve(text));
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

        public String render() {
            return (source.detachedProgress ? "SUB " : "") + "[" + progress + "/" + maxProgress + "] " + message;
        }
    }

}
