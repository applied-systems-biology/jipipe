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

import java.util.Collection;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

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

    public boolean isCancelled() {
        return cancelled.get();
    }

    public void cancel() {
        cancelled.set(true);
    }

    public synchronized StringBuilder getLog() {
        return log;
    }

    /**
     * Writes a message into the log
     * @param message the message
     */
    public synchronized void log(String message) {
        log.append("<").append(progress).append("/").append(maxProgress).append("> ").append(logPrepend);
        boolean needsSeparator = !StringUtils.isNullOrEmpty(logPrepend) && !StringUtils.isNullOrEmpty(message);
        if (needsSeparator)
            log.append(" | ");
        log.append(" ").append(message);
        log.append("\n");
        eventBus.post(new StatusUpdatedEvent(this, progress.get(), maxProgress.get(), logPrepend + (needsSeparator ? " | " : " ") + message));
    }

    /**
     * Creates a sub-progress that has the same cancellation and progress, but shows the new category in front of sub-messages
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
     * @param text the category
     * @param index index of the index operation
     * @param size size of the index operation
     * @return progress info with the same properties as the current one, but with messages prepended
     */
    public synchronized JIPipeProgressInfo resolve(String text, int index, int size) {
        return resolve(text + " " + (index + 1) + "/" + size);
    }

    /**
     * Applies resolve(logPrepend) and then logs an empty message
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
     * @param text the category
     * @param index index of the index operation
     * @param size size of the index operation
     * @return progress info with the same properties as the current one, but with messages prepended
     */
    public synchronized JIPipeProgressInfo resolveAndLog(String text, int index, int size) {
        return resolveAndLog(text + " " + (index + 1) + "/" + size);
    }

    /**
     * Applies a for-each operation where the progress is logged
     * @param text the text
     * @param collection the iterated collection
     * @param function the function executed for each item in the collection
     * @param <T> collection contents
     */
    public <T> void resolveAndLogForEach(String text, Collection<T> collection, BiConsumer<T, JIPipeProgressInfo> function) {
        int size = collection.size();
        int current = 0;
        for (T item : collection) {
            if(isCancelled())
                return;
            log(text + " " + (current + 1) + "/" + size);
            JIPipeProgressInfo itemProgress = resolveAndLog(text + " " + (current + 1) + "/" + size);
            function.accept(item, itemProgress);
            ++current;
        }
    }

    /**
     * Applies a for-each operation where the progress is logged
     * @param text the text
     * @param collection the iterated collection
     * @param function the function executed for each item in the collection
     * @param <T> collection contents
     */
    public <T> void resolveAndLogForEach(String text, T[] collection, BiConsumer<T, JIPipeProgressInfo> function) {
        int size = collection.length;
        int current = 0;
        for (T item : collection) {
            if(isCancelled())
                return;
            log(text + " " + (current + 1) + "/" + size);
            JIPipeProgressInfo itemProgress = resolveAndLog(text + " " + (current + 1) + "/" + size);
            function.accept(item, itemProgress);
            ++current;
        }
    }

    /**
     * Applies a for-each operation where the progress is logged
     * @param text the text
     * @param collection the iterated collection
     * @param function the function executed for each item in the collection
     */
    public void resolveAndLogForEach(String text, int[] collection, BiConsumer<Integer, JIPipeProgressInfo> function) {
        int size = collection.length;
        int current = 0;
        for (int item : collection) {
            if(isCancelled())
                return;
            log(text + " " + (current + 1) + "/" + size);
            JIPipeProgressInfo itemProgress = resolveAndLog(text + " " + (current + 1) + "/" + size);
            function.accept(item, itemProgress);
            ++current;
        }
    }

    /**
     * Applies a for-each operation where the progress is logged
     * @param text the text
     * @param collection the iterated collection
     * @param function the function executed for each item in the collection
     */
    public void resolveAndLogForEach(String text, byte[] collection, BiConsumer<Byte, JIPipeProgressInfo> function) {
        int size = collection.length;
        int current = 0;
        for (byte item : collection) {
            if(isCancelled())
                return;
            log(text + " " + (current + 1) + "/" + size);
            JIPipeProgressInfo itemProgress = resolveAndLog(text + " " + (current + 1) + "/" + size);
            function.accept(item, itemProgress);
            ++current;
        }
    }

    /**
     * Applies a for-each operation where the progress is logged
     * @param text the text
     * @param collection the iterated collection
     * @param function the function executed for each item in the collection
     */
    public void resolveAndLogForEach(String text, long[] collection, BiConsumer<Long, JIPipeProgressInfo> function) {
        int size = collection.length;
        int current = 0;
        for (long item : collection) {
            if(isCancelled())
                return;
            log(text + " " + (current + 1) + "/" + size);
            JIPipeProgressInfo itemProgress = resolveAndLog(text + " " + (current + 1) + "/" + size);
            function.accept(item, itemProgress);
            ++current;
        }
    }

    /**
     * Applies a for-each operation where the progress is logged
     * @param text the text
     * @param collection the iterated collection
     * @param function the function executed for each item in the collection
     */
    public void resolveAndLogForEach(String text, short[] collection, BiConsumer<Short, JIPipeProgressInfo> function) {
        int size = collection.length;
        int current = 0;
        for (short item : collection) {
            if(isCancelled())
                return;
            log(text + " " + (current + 1) + "/" + size);
            JIPipeProgressInfo itemProgress = resolveAndLog(text + " " + (current + 1) + "/" + size);
            function.accept(item, itemProgress);
            ++current;
        }
    }

    /**
     * Applies a for-each operation where the progress is logged
     * @param text the text
     * @param collection the iterated collection
     * @param function the function executed for each item in the collection
     */
    public void resolveAndLogForEach(String text, float[] collection, BiConsumer<Float, JIPipeProgressInfo> function) {
        int size = collection.length;
        int current = 0;
        for (float item : collection) {
            if(isCancelled())
                return;
            log(text + " " + (current + 1) + "/" + size);
            JIPipeProgressInfo itemProgress = resolveAndLog(text + " " + (current + 1) + "/" + size);
            function.accept(item, itemProgress);
            ++current;
        }
    }

    /**
     * Applies a for-each operation where the progress is logged
     * @param text the text
     * @param collection the iterated collection
     * @param function the function executed for each item in the collection
     */
    public void resolveAndLogForEach(String text, double[] collection, BiConsumer<Double, JIPipeProgressInfo> function) {
        int size = collection.length;
        int current = 0;
        for (double item : collection) {
            if(isCancelled())
                return;
            JIPipeProgressInfo itemProgress = resolveAndLog(text + " " + (current + 1) + "/" + size);
            function.accept(item, itemProgress);
            ++current;
        }
    }

    /**
     * Sets the progress and max progress
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
