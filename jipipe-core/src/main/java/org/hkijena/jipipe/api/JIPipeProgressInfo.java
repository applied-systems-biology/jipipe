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

package org.hkijena.jipipe.api;

import org.apache.commons.lang3.exception.ExceptionUtils;
import org.hkijena.jipipe.api.events.AbstractJIPipeEvent;
import org.hkijena.jipipe.api.events.JIPipeEventEmitter;
import org.hkijena.jipipe.api.notifications.JIPipeNotificationInbox;
import org.hkijena.jipipe.api.run.JIPipeRunnable;
import org.hkijena.jipipe.utils.StringUtils;
import org.scijava.Cancelable;

import java.util.Collection;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.StampedLock;
import java.util.function.BiConsumer;

/**
 * This object is available inside a {@link JIPipeRunnable} and contains methods to report progress, logs, and request cancellation.
 */
public class JIPipeProgressInfo implements Cancelable {

    public static final JIPipeProgressInfo SILENT = new JIPipeProgressInfo();
    public static final JIPipeProgressInfo STDOUT = new JIPipeProgressInfo();

    private static final String[] SPINNER_1 = new String[]{"⠋", "⠙", "⠹", "⠸", "⠼", "⠴", "⠦", "⠧", "⠇", "⠏"};

    static {
        STDOUT.setLogToStdOut(true);
    }

    private final StampedLock stampedLock;
    private AtomicBoolean cancelled = new AtomicBoolean();
    private AtomicInteger progress = new AtomicInteger(0);
    private AtomicInteger maxProgress = new AtomicInteger(1);
    private AtomicInteger numLines = new AtomicInteger(0);
    private AtomicBoolean withSpinner = new AtomicBoolean(false);
    private StringBuilder log = new StringBuilder();
    private String logPrepend = "";
    private AtomicBoolean logToStdOut = new AtomicBoolean(false);
    private boolean detachedProgress = false;
    private String cancelReason;
    private StatusUpdatedEventEmitter statusUpdatedEventEmitter;
    private JIPipeNotificationInbox notifications = new JIPipeNotificationInbox();

    public JIPipeProgressInfo() {
        this.statusUpdatedEventEmitter = new StatusUpdatedEventEmitter();
        this.stampedLock = new StampedLock();
    }

    public JIPipeProgressInfo(JIPipeProgressInfo other) {
        this.stampedLock = other.stampedLock;
        this.statusUpdatedEventEmitter = other.statusUpdatedEventEmitter;
        this.cancelled = other.cancelled;
        this.progress = other.progress;
        this.maxProgress = other.maxProgress;
        this.log = other.log;
        this.logPrepend = other.logPrepend;
        this.logToStdOut = other.logToStdOut;
        this.detachedProgress = other.detachedProgress;
        this.cancelReason = other.cancelReason;
        this.numLines = other.numLines;
        this.withSpinner = other.withSpinner;
        this.notifications = other.notifications;
    }

    public void clearLog() {
        long stamp = stampedLock.writeLock();
        try {
            log.setLength(0);
        } finally {
            stampedLock.unlock(stamp);
        }
    }

    public void incrementProgress() {
        long stamp = stampedLock.writeLock();
        try {
            progress.getAndIncrement();
        } finally {
            stampedLock.unlock(stamp);
        }
    }

    public String getLogPrepend() {
        return logPrepend;
    }

    public StatusUpdatedEventEmitter getStatusUpdatedEventEmitter() {
        return statusUpdatedEventEmitter;
    }

    public void setStatusUpdatedEventEmitter(StatusUpdatedEventEmitter statusUpdatedEventEmitter) {
        this.statusUpdatedEventEmitter = statusUpdatedEventEmitter;
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

    public StringBuilder getLog() {
        return log;
    }

    public boolean isLogToStdOut() {
        return logToStdOut.get();
    }

    public void setLogToStdOut(boolean value) {
        logToStdOut.set(value);
    }

    public boolean isWithSpinner() {
        return withSpinner.get();
    }

    public void setWithSpinner(boolean b) {
        withSpinner.set(b);
    }

    public JIPipeNotificationInbox getNotifications() {
        return notifications;
    }

    public void setNotifications(JIPipeNotificationInbox notifications) {
        this.notifications = notifications;
    }

    /**
     * Writes a message into the log
     *
     * @param message the message
     */
    public void log(String message) {
        long stamp = stampedLock.writeLock();
        try {
            if (detachedProgress) {
                log.append("SUB ");
            }
            log.append("<").append(progress).append("/").append(maxProgress).append("> ");

            if (withSpinner.get()) {
                log.append(SPINNER_1[numLines.get() % SPINNER_1.length]).append(" ");
            }

            log.append(logPrepend);

            boolean needsSeparator = !StringUtils.isNullOrEmpty(logPrepend) && !StringUtils.isNullOrEmpty(message);
            if (needsSeparator)
                log.append(" | ");
            log.append(" ").append(message);
            log.append("\n");
            StatusUpdatedEvent event = new StatusUpdatedEvent(this, progress.get(), maxProgress.get(), logPrepend + (needsSeparator ? " | " : " ") + message);
            if (logToStdOut.get()) {
                System.out.println(event.render());
            }
            numLines.getAndIncrement();
            statusUpdatedEventEmitter.emit(event);
        } finally {
            stampedLock.unlock(stamp);
        }
    }

    /**
     * Returns true if the progress was detached from the root progress
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
    public JIPipeProgressInfo detachProgress() {
        long stamp = stampedLock.readLock();
        try {
            JIPipeProgressInfo result = new JIPipeProgressInfo(this);
            result.detachedProgress = true;
            result.progress = new AtomicInteger(this.progress.get());
            result.maxProgress = new AtomicInteger(this.maxProgress.get());
            return result;
        } finally {
            stampedLock.unlock(stamp);
        }
    }

    /**
     * Creates a sub-progress that has the same cancellation and progress, but shows the new category in front of sub-messages
     *
     * @param logPrepend the category
     * @return progress info with the same properties as the current one, but with messages prepended
     */
    public JIPipeProgressInfo resolve(String logPrepend) {
        long stamp = stampedLock.readLock();
        try {
            JIPipeProgressInfo result = new JIPipeProgressInfo(this);
            if (StringUtils.isNullOrEmpty(result.logPrepend))
                result.logPrepend = logPrepend;
            else
                result.logPrepend = result.logPrepend + " | " + logPrepend;
            return result;
        } finally {
            stampedLock.unlock(stamp);
        }
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
    public JIPipeProgressInfo resolve(String text, int index, int size) {
        return resolve(text + " " + (index + 1) + "/" + size);
    }

    /**
     * Applies resolve(logPrepend) and then logs an empty message
     *
     * @param logPrepend the category
     * @return progress info with the same properties as the current one, but with messages prepended
     */
    public JIPipeProgressInfo resolveAndLog(String logPrepend) {
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
    public JIPipeProgressInfo resolveAndLog(String text, int index, int size) {
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

    public void addMaxProgress(int maxProgress) {
        this.maxProgress.getAndAccumulate(maxProgress, Integer::sum);
    }

    public void addProgress(int progress) {
        this.progress.getAndAccumulate(progress, Integer::sum);
    }

    public JIPipePercentageProgressInfo percentage(String text) {
        return new JIPipePercentageProgressInfo(resolve(text));
    }

    public void log(Throwable e) {
        if (e == null) {
            return;
        }
        try {
            log(ExceptionUtils.getStackTrace(e));
        } catch (Throwable ex) {
            log(e.getMessage());
        }
    }

    public interface StatusUpdatedEventListener {
        void onProgressStatusUpdated(StatusUpdatedEvent event);
    }

    public static class StatusUpdatedEvent extends AbstractJIPipeEvent {
        private final JIPipeProgressInfo progressInfo;
        private final int progress;
        private final int maxProgress;
        private final String message;

        public StatusUpdatedEvent(JIPipeProgressInfo progressInfo, int progress, int maxProgress, String message) {
            super(progressInfo);
            this.progressInfo = progressInfo;
            this.progress = progress;
            this.maxProgress = maxProgress;
            this.message = message;
        }

        public JIPipeProgressInfo getProgressInfo() {
            return progressInfo;
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
            return (progressInfo.detachedProgress ? "SUB " : "") + "[" + progress + "/" + maxProgress + "] " + message;
        }
    }

    public static class StatusUpdatedEventEmitter extends JIPipeEventEmitter<StatusUpdatedEvent, StatusUpdatedEventListener> {

        @Override
        protected void call(StatusUpdatedEventListener statusUpdatedEventListener, StatusUpdatedEvent event) {
            statusUpdatedEventListener.onProgressStatusUpdated(event);
        }
    }

}
