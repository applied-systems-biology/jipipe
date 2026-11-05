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

import org.hkijena.jipipe.api.events.AbstractJIPipeEvent;
import org.hkijena.jipipe.api.events.JIPipeEventEmitter;
import org.hkijena.jipipe.api.notifications.JIPipeNotification;
import org.hkijena.jipipe.api.notifications.JIPipeNotificationInbox;
import org.hkijena.jipipe.api.progress.JIPipeLogLevel;
import org.hkijena.jipipe.api.progress.JIPipeProgressLog;
import org.hkijena.jipipe.api.progress.JIPipeProgressNode;
import org.hkijena.jipipe.api.progress.JIPipeProgressNodeKind;
import org.hkijena.jipipe.api.progress.JIPipeProgressRecord;
import org.hkijena.jipipe.api.run.JIPipeRunnable;
import org.scijava.Cancelable;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.nio.file.Path;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Deque;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.BiConsumer;

/**
 * This object is available inside a {@link JIPipeRunnable} and contains methods to report progress, logs, and request cancellation.
 * <p>
 * Since 6.0.0, this class is a thin handle {@code (store, nodeId)} over a {@link JIPipeProgressLog} (the progress store):
 * every message, progress update, and error is recorded in the store as an NDJSON record attached to the handle's node,
 * and {@link #resolve(String)} creates child nodes, forming the progress tree of the run.
 * {@link #SILENT} is a handle on a store that discards records, {@link #STDOUT} is a handle on a store that renders
 * records to stdout, and {@link #JIPipeProgressInfo()} creates a handle on a private silent store (no file).
 */
public class JIPipeProgressInfo implements Cancelable {

    /**
     * A progress info that discards all records (store {@link JIPipeProgressLog#silent()}).
     */
    public static final JIPipeProgressInfo SILENT = new JIPipeProgressInfo(JIPipeProgressLog.silent());

    /**
     * A progress info that renders each record as a compatibility line to stdout
     * (store {@link JIPipeProgressLog#stdout()}).
     */
    public static final JIPipeProgressInfo STDOUT = new JIPipeProgressInfo(JIPipeProgressLog.stdout());

    private static final String[] SPINNER_1 = new String[]{"⠋", "⠙", "⠹", "⠸", "⠼", "⠴", "⠦", "⠧", "⠇", "⠏"};

    /**
     * The progress store. Volatile: {@link #setStorageBase(Path)} may replace a fresh root's silent
     * store with a file-backed store (root-run creation point, before the first record).
     */
    private volatile JIPipeProgressLog store;
    private volatile int nodeId;
    private final AtomicBoolean cancelled;
    private final AtomicInteger progress;
    private final AtomicInteger maxProgress;
    private final AtomicInteger numLines;
    private final AtomicBoolean withSpinner;
    private final AtomicBoolean logToStdOut;
    private volatile String cancelReason;
    private JIPipeNotificationInbox notifications;
    private final JIPipeNotificationInbox.PushedEventListener notificationListener;
    private boolean detachedProgress;
    private CharSequence logView;

    /**
     * Creates a progress info on a private silent store (no file; the record tail is maintained so
     * {@link #getLog()} works).
     */
    public JIPipeProgressInfo() {
        this(JIPipeProgressLog.silent());
    }

    /**
     * Creates a root progress info handle on an existing store (the handle's node is the store's root node).
     * Used at run-creation points that own their store.
     *
     * @param store the progress store
     */
    public JIPipeProgressInfo(JIPipeProgressLog store) {
        this.store = store;
        this.nodeId = store.getRootNodeId();
        this.cancelled = new AtomicBoolean();
        this.progress = new AtomicInteger(0);
        this.maxProgress = new AtomicInteger(1);
        this.numLines = new AtomicInteger(0);
        this.withSpinner = new AtomicBoolean(false);
        this.logToStdOut = new AtomicBoolean(false);
        this.cancelReason = null;
        this.notificationListener = this::onNotificationPushed;
        this.notifications = new JIPipeNotificationInbox();
        this.notifications.getPushedEventEmitter().subscribeWeak(this.notificationListener);
        this.detachedProgress = false;
    }

    /**
     * Creates a handle that shares the state of another handle (same store and node, same counters,
     * cancellation, notifications, and settings).
     *
     * @param other the other progress info
     */
    public JIPipeProgressInfo(JIPipeProgressInfo other) {
        this.store = other.store;
        this.nodeId = other.nodeId;
        this.cancelled = other.cancelled;
        this.progress = other.progress;
        this.maxProgress = other.maxProgress;
        this.numLines = other.numLines;
        this.withSpinner = other.withSpinner;
        this.logToStdOut = other.logToStdOut;
        this.cancelReason = other.cancelReason;
        this.notifications = other.notifications;
        this.notificationListener = other.notificationListener;
        this.detachedProgress = other.detachedProgress;
    }

    private JIPipeProgressInfo(JIPipeProgressLog store, int nodeId, JIPipeProgressInfo parent,
                               AtomicInteger progress, AtomicInteger maxProgress, boolean detachedProgress) {
        this.store = store;
        this.nodeId = nodeId;
        this.cancelled = parent.cancelled;
        this.progress = progress;
        this.maxProgress = maxProgress;
        this.numLines = parent.numLines;
        this.withSpinner = parent.withSpinner;
        this.logToStdOut = parent.logToStdOut;
        this.cancelReason = parent.cancelReason;
        this.notifications = parent.notifications;
        this.notificationListener = parent.notificationListener;
        this.detachedProgress = detachedProgress;
    }

    /**
     * Adopts the given store as this handle's store and re-roots the handle to the store's root
     * node. All state shared with other handles (cancellation, notifications, progress counters,
     * output flags) is kept: handles previously derived from this handle remain connected to the
     * previous store, while records submitted through this handle afterwards go to the new store.
     * Root-run creation point only: call this before the first record is submitted through this
     * handle for the new run; records submitted beforehand would remain in the previous store.
     * <p>
     * The displaced store is <b>not</b> closed: it may still be referenced by other handles
     * (handles derived via {@link #resolve(String)} keep their store by design, and a handle
     * injected into a run carries a store owned by the caller context that created it). Closing it
     * would silently drop the records of those handles. Its lifecycle — including
     * {@link JIPipeProgressLog#finish(JIPipeProgressNodeState)} and
     * {@link JIPipeProgressLog#close()} — remains with its owner. Adopting the store that is
     * already set is a no-op. The one-run-two-files case is prevented by
     * {@link #setStorageBase(Path, String, String)}, which reuses a store that already writes the
     * run's file.
     *
     * @param store the new progress store
     */
    public void setStore(JIPipeProgressLog store) {
        if (store == null)
            throw new IllegalArgumentException("Store must not be null");
        if (this.store == store) {
            return;
        }
        this.store = store;
        this.nodeId = store.getRootNodeId();
        logView = null;
    }

    /**
     * Replaces the handle's store with a file-backed store that writes into the logs base resolved
     * from the given base ({@link JIPipeProgressLog#resolveStorageBase(Path)}), using a fresh
     * random run UUID and no run label. See {@link #setStore(JIPipeProgressLog)} for the
     * root-run creation point semantics.
     *
     * @param base the storage base (the {@code logs} directory is resolved from it)
     */
    public void setStorageBase(Path base) {
        setStorageBase(base, UUID.randomUUID().toString(), null);
    }

    /**
     * Replaces the handle's store with a file-backed store that writes into the logs base resolved
     * from the given base ({@link JIPipeProgressLog#resolveStorageBase(Path)}). The store file is
     * named {@code <runUuid>.ndjson} and the head record carries the run UUID and label.
     * See {@link #setStore(JIPipeProgressLog)} for the root-run creation point semantics.
     * <p>
     * If the handle's current store already is the file-backed store for this run (same
     * {@code <runUuid>.ndjson} file — e.g. created by the queue before the run started), it is
     * reused unchanged and the label is ignored (the head record was written at store creation):
     * one run never gets a second file (ruling R17). Otherwise the previous store is displaced
     * but <b>not</b> closed — see {@link #setStore(JIPipeProgressLog)} for why.
     *
     * @param base     the storage base (the {@code logs} directory is resolved from it)
     * @param runUuid  the run UUID (file name and head record)
     * @param runLabel the run label (head record)
     */
    public void setStorageBase(Path base, String runUuid, String runLabel) {
        Path file = JIPipeProgressLog.resolveStorageBase(base).resolve(runUuid + ".ndjson");
        JIPipeProgressLog current = store;
        if (file.equals(current.getFile())) {
            return;
        }
        setStore(JIPipeProgressLog.create(file, runUuid, runLabel));
    }

    /**
     * Returns the progress store this handle writes into.
     *
     * @return the store
     */
    public JIPipeProgressLog getStore() {
        return store;
    }

    /**
     * Returns the id of this handle's node inside the store ({@link JIPipeProgressLog#getRootNodeId()} for root handles).
     *
     * @return the node id
     */
    public int getNodeId() {
        return nodeId;
    }

    public void clearLog() {
        logView = null;
    }

    public void incrementProgress() {
        progress.incrementAndGet();
        recordProgress();
    }

    /**
     * Returns the label chain of this handle's node (the labels of all ancestors up to the root,
     * joined with {@code |}; empty for root handles). Supersedes the former {@code logPrepend} field.
     *
     * @return the label chain
     */
    public String getLogPrepend() {
        return getLabelChain();
    }

    public int getProgress() {
        return progress.get();
    }

    public void setProgress(int progress) {
        this.progress.set(progress);
        recordProgress();
    }

    public int getMaxProgress() {
        return maxProgress.get();
    }

    public void setMaxProgress(int maxProgress) {
        this.maxProgress.set(maxProgress);
        recordProgress();
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

    /**
     * Returns the log as a rendered, tail-bounded view of the store's records.
     * The view is lazily rendered on first access and cached until {@link #clearLog()}.
     *
     * @return the log view
     */
    public CharSequence getLog() {
        if (logView == null) {
            logView = new LazyLogView();
        }
        return logView;
    }

    public boolean isLogToStdOut() {
        return logToStdOut.get();
    }

    /**
     * If enabled, each log record is additionally printed as a rendered line to stdout on the
     * calling thread. Handles on a {@link JIPipeProgressLog#stdout()} store do not need this flag
     * (the store renders records itself).
     *
     * @param value whether logs should be printed to stdout
     */
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
        if (this.notifications == notifications) {
            return;
        }
        if (this.notifications != null && notificationListener != null) {
            this.notifications.getPushedEventEmitter().unsubscribe(notificationListener);
        }
        this.notifications = notifications;
        if (notifications != null && notificationListener != null) {
            notifications.getPushedEventEmitter().subscribeWeak(notificationListener);
        }
    }

    /**
     * Writes a message into the log (INFO level)
     *
     * @param message the message
     */
    public void log(String message) {
        log(JIPipeLogLevel.INFO, message);
    }

    /**
     * Writes a message into the log at the given level
     *
     * @param level   the log level
     * @param message the message
     */
    public void log(JIPipeLogLevel level, String message) {
        String msg = message;
        if (withSpinner.get()) {
            msg = SPINNER_1[Math.floorMod(numLines.get(), SPINNER_1.length)] + " " + msg;
        }
        numLines.incrementAndGet();
        JIPipeProgressLog currentStore = store;
        currentStore.log(nodeId, level, msg);
        if (logToStdOut.get()) {
            System.out.println(renderStdoutLine(msg));
        }
    }

    /**
     * Writes a message into the log at DEBUG level
     *
     * @param message the message
     */
    public void debug(String message) {
        log(JIPipeLogLevel.DEBUG, message);
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
     * The sub-progress is a child node with its own progress counters (initialized from the
     * current counters); cancellation and notifications remain shared.
     *
     * @return progress info with the same properties as the current one, but with a detached progress
     */
    public JIPipeProgressInfo detachProgress() {
        JIPipeProgressLog currentStore = store;
        int childNodeId = currentStore.beginNode(nodeId, "", JIPipeProgressNodeKind.ITEM);
        JIPipeProgressInfo result = new JIPipeProgressInfo(currentStore, childNodeId, this,
                new AtomicInteger(progress.get()), new AtomicInteger(maxProgress.get()), true);
        currentStore.progress(childNodeId, progress.get(), maxProgress.get());
        return result;
    }

    /**
     * Creates a sub-progress that has the same cancellation, but shows the new category in front of sub-messages.
     * The sub-progress is a child node of the current node with its own progress counters.
     *
     * @param logPrepend the category
     * @return progress info with the same properties as the current one, but with messages prepended
     */
    public JIPipeProgressInfo resolve(String logPrepend) {
        JIPipeProgressLog currentStore = store;
        int childNodeId = currentStore.beginNode(nodeId, logPrepend, JIPipeProgressNodeKind.ITEM);
        return new JIPipeProgressInfo(currentStore, childNodeId, this,
                new AtomicInteger(0), new AtomicInteger(1), false);
    }

    /**
     * Creates a sub-progress that has the same cancellation, but shows the new category in front of sub-messages.
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
     * Applies resolve(logPrepend). The node row is the rendered line, so no additional record is written.
     *
     * @param logPrepend the category
     * @return progress info with the same properties as the current one, but with messages prepended
     */
    public JIPipeProgressInfo resolveAndLog(String logPrepend) {
        return resolve(logPrepend);
    }

    /**
     * Creates a sub-progress that has the same cancellation, but shows the new category in front of sub-messages.
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
     * Applies a for-each operation where each item is processed with its own child progress
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
            JIPipeProgressInfo itemProgress = resolveAndLog(text + " " + (current + 1) + "/" + size);
            function.accept(item, itemProgress);
            ++current;
        }
    }

    /**
     * Applies a for-each operation where each item is processed with its own child progress
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
            JIPipeProgressInfo itemProgress = resolveAndLog(text + " " + (current + 1) + "/" + size);
            function.accept(item, itemProgress);
            ++current;
        }
    }

    /**
     * Applies a for-each operation where each item is processed with its own child progress
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
            JIPipeProgressInfo itemProgress = resolveAndLog(text + " " + (current + 1) + "/" + size);
            function.accept(item, itemProgress);
            ++current;
        }
    }

    /**
     * Applies a for-each operation where each item is processed with its own child progress
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
            JIPipeProgressInfo itemProgress = resolveAndLog(text + " " + (current + 1) + "/" + size);
            function.accept(item, itemProgress);
            ++current;
        }
    }

    /**
     * Applies a for-each operation where each item is processed with its own child progress
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
            JIPipeProgressInfo itemProgress = resolveAndLog(text + " " + (current + 1) + "/" + size);
            function.accept(item, itemProgress);
            ++current;
        }
    }

    /**
     * Applies a for-each operation where each item is processed with its own child progress
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
            JIPipeProgressInfo itemProgress = resolveAndLog(text + " " + (current + 1) + "/" + size);
            function.accept(item, itemProgress);
            ++current;
        }
    }

    /**
     * Applies a for-each operation where each item is processed with its own child progress
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
            JIPipeProgressInfo itemProgress = resolveAndLog(text + " " + (current + 1) + "/" + size);
            function.accept(item, itemProgress);
            ++current;
        }
    }

    /**
     * Applies a for-each operation where each item is processed with its own child progress
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
        progress.set(count);
        maxProgress.set(total);
        recordProgress();
    }

    public void addMaxProgress(int maxProgress) {
        this.maxProgress.addAndGet(maxProgress);
        recordProgress();
    }

    public void addProgress(int progress) {
        this.progress.addAndGet(progress);
        recordProgress();
    }

    public JIPipePercentageProgressInfo percentage(String text) {
        return new JIPipePercentageProgressInfo(resolve(text));
    }

    public void log(Throwable e) {
        if (e == null) {
            return;
        }
        JIPipeProgressLog currentStore = store;
        currentStore.error(nodeId, e);
        if (logToStdOut.get()) {
            for (String line : renderErrorLines(e.getClass().getName(),
                    e.getMessage() == null ? "" : e.getMessage(),
                    renderStackTrace(e))) {
                System.out.println(line);
            }
        }
    }

    public void error(String message) {
        log(JIPipeLogLevel.ERROR, "[ERROR] " + message);
    }

    public void warn(String message) {
        log(JIPipeLogLevel.WARN, "[WARNING] " + message);
    }

    public void aggressiveError(String... text) {
        if (text.length == 0) {
            return;
        }
        int length = Arrays.stream(text).map(String::length).reduce(0, Integer::max);

        error("  " + "v".repeat(length));
        for (String s : text) {
            error("| " + s);
        }
        error("  " + "^".repeat(length));
    }

    public void aggressiveWarn(String... text) {
        if (text.length == 0) {
            return;
        }
        int length = Arrays.stream(text).map(String::length).reduce(0, Integer::max);

        warn("  " + "v".repeat(length));
        for (String s : text) {
            warn("| " + s);
        }
        warn("  " + "^".repeat(length));
    }

    /**
     * Throws an exception if the process is cancelled
     */
    public void cancellationCheck() {
        if (isCancelled()) {
            throw new RuntimeException(new InterruptedException("Cancellation was requested"));
        }
    }

    // ---------------------------------------------------------------------
    // Internals
    // ---------------------------------------------------------------------

    /**
     * Writes the current counters into the node's progress record.
     */
    private void recordProgress() {
        JIPipeProgressLog currentStore = store;
        currentStore.progress(nodeId, progress.get(), maxProgress.get());
    }

    /**
     * Writes the given counters into the node's progress record and the handle's fast-path counters.
     * Used by subclasses in the same package (for example the percentage progress).
     *
     * @param value the progress value
     * @param max   the progress maximum
     */
    void recordProgress(int value, int max) {
        progress.set(value);
        maxProgress.set(max);
        JIPipeProgressLog currentStore = store;
        currentStore.progress(nodeId, value, max);
    }

    /**
     * Records a run-level notification into the store as a {@code w} record. Subscribed weakly to
     * the notification inbox of the root handle.
     *
     * @param event the push event
     */
    private void onNotificationPushed(JIPipeNotificationInbox.PushedEvent event) {
        JIPipeNotification notification = event.getNotification();
        JIPipeProgressLog currentStore = store;
        currentStore.notification(notification.getHeading(), notification.getDescription());
    }

    /**
     * Renders a log record as a compatibility line for stdout printing
     * ({@code [v/mx] <chain> | message}).
     *
     * @param message the record message
     * @return the line
     */
    private String renderStdoutLine(String message) {
        StringBuilder builder = new StringBuilder();
        builder.append('[').append(progress.get()).append('/').append(maxProgress.get()).append(']').append(' ');
        String chain = getLabelChain();
        if (!chain.isEmpty()) {
            builder.append(chain).append(" | ");
        }
        builder.append(message);
        return builder.toString();
    }

    /**
     * Joins the labels of all ancestors of this handle's node up to the root with {@code |}.
     * The root's label is not part of the chain.
     *
     * @return the label chain (empty for root handles)
     */
    private String getLabelChain() {
        JIPipeProgressLog currentStore = store;
        Deque<String> labels = new ArrayDeque<>();
        JIPipeProgressNode node = currentStore.getNode(nodeId);
        while (node != null && node.getId() != currentStore.getRootNodeId()) {
            labels.addFirst(node.getLabel() == null ? "" : node.getLabel());
            int parentId = node.getParentId();
            if (parentId <= 0 || parentId == currentStore.getRootNodeId()) {
                break;
            }
            node = currentStore.getNode(parentId);
        }
        return String.join(" | ", labels);
    }

    /**
     * Renders an error into compatibility lines: the summary line and the indented stack lines.
     *
     * @param className the exception class name
     * @param message   the exception message
     * @param stack     the stack trace
     * @return the lines
     */
    private static List<String> renderErrorLines(String className, String message, String stack) {
        List<String> lines = new ArrayList<>();
        lines.add(className + (message == null || message.isEmpty() ? "" : ": " + message));
        if (stack != null) {
            String[] stackLines = stack.split("\n");
            for (int i = 1; i < stackLines.length; i++) {
                lines.add("  " + stackLines[i].stripLeading());
            }
        }
        return lines;
    }

    private static String renderStackTrace(Throwable throwable) {
        StringWriter stringWriter = new StringWriter();
        throwable.printStackTrace(new PrintWriter(stringWriter));
        return stringWriter.toString();
    }

    /**
     * Lazily rendered, cached compatibility view of the store's recent records. Non-persistent
     * stores render via {@link JIPipeProgressLog#renderText} (serves the in-memory tail); file-backed
     * stores render from the bounded tail via {@link JIPipeProgressLog#readRecent(int)} — never the
     * whole file.
     */
    private final class LazyLogView implements CharSequence {

        private volatile String rendered;

        private synchronized String text() {
            if (rendered == null) {
                StringBuilder builder = new StringBuilder();
                JIPipeProgressLog currentStore = store;
                if (currentStore.getFile() == null) {
                    // Non-persistent store: renderText walks the in-memory tail (bounded)
                    currentStore.renderText(JIPipeLogLevel.INFO, line -> builder.append(line).append('\n'));
                } else {
                    renderRecentInto(builder);
                }
                rendered = builder.toString();
            }
            return rendered;
        }

        @Override
        public int length() {
            return text().length();
        }

        @Override
        public char charAt(int index) {
            return text().charAt(index);
        }

        @Override
        public CharSequence subSequence(int start, int end) {
            return text().subSequence(start, end);
        }

        @Override
        public String toString() {
            return text();
        }
    }

    /**
     * Renders the recent records of a file-backed store into compatibility lines (bounded by the
     * store's tail; mirrors the shapes of the store's text render).
     *
     * @param builder the target
     */
    private void renderRecentInto(StringBuilder builder) {
        JIPipeProgressLog currentStore = store;
        Map<Integer, int[]> nodeProgress = new HashMap<>();
        for (JIPipeProgressRecord record : currentStore.readRecent(Integer.MAX_VALUE)) {
            if (record instanceof JIPipeProgressRecord.Log logRecord) {
                if (logRecord.lv() >= JIPipeLogLevel.INFO.value()) {
                    String prefix = renderViewPrefix(logRecord.id(), nodeProgress);
                    String[] fragments = (logRecord.m() == null ? "" : logRecord.m()).split("\n", -1);
                    for (int i = 0; i < fragments.length; i++) {
                        String fragment = fragments[i];
                        if (fragment.endsWith("\r")) {
                            fragment = fragment.substring(0, fragment.length() - 1);
                        }
                        builder.append(i == 0 ? prefix + fragment : fragment).append('\n');
                    }
                }
            } else if (record instanceof JIPipeProgressRecord.Progress progressRecord) {
                int percentage = progressRecord.mx() <= 0 ? -1 : (int) (progressRecord.v() * 100L / progressRecord.mx());
                if (percentage == 50 || percentage == 100) {
                    builder.append(renderViewPrefix(progressRecord.id(), nodeProgress))
                            .append(percentage).append('%').append('\n');
                }
                if (progressRecord.mx() > 0) {
                    nodeProgress.put(progressRecord.id(), new int[]{progressRecord.v(), progressRecord.mx()});
                }
            } else if (record instanceof JIPipeProgressRecord.Error errorRecord) {
                List<String> lines = renderErrorLines(errorRecord.c(), errorRecord.m(), errorRecord.st());
                for (int i = 0; i < lines.size(); i++) {
                    if (i == 0) {
                        // The summary line carries the prefix
                        builder.append(renderViewPrefix(errorRecord.id(), nodeProgress)).append(lines.get(0)).append('\n');
                    } else {
                        builder.append(lines.get(i)).append('\n');
                    }
                }
            } else if (record instanceof JIPipeProgressRecord.Notification notificationRecord) {
                builder.append("@ ").append(notificationRecord.h()).append(": ").append(notificationRecord.d()).append('\n');
            }
        }
    }

    /**
     * Builds the {@code [v/mx] <chain> | } prefix of a view line: the node's last known progress
     * (initially 1/1) and its label chain from the node index.
     *
     * @param nodeId       the node id
     * @param nodeProgress the running progress states collected during the render walk
     * @return the prefix
     */
    private String renderViewPrefix(int nodeId, Map<Integer, int[]> nodeProgress) {
        int[] state = nodeProgress.get(nodeId);
        if (state == null) {
            state = new int[]{1, 1};
        }
        StringBuilder builder = new StringBuilder();
        builder.append('[').append(state[0]).append('/').append(state[1]).append(']').append(' ');
        String chain = getLabelChain(nodeId);
        if (!chain.isEmpty()) {
            builder.append(chain).append(" | ");
        }
        return builder.toString();
    }

    /**
     * Joins the labels of all ancestors of the given node up to the root with {@code |}.
     *
     * @param id the node id
     * @return the label chain (empty for the root)
     */
    private String getLabelChain(int id) {
        JIPipeProgressLog currentStore = store;
        Deque<String> labels = new ArrayDeque<>();
        JIPipeProgressNode node = currentStore.getNode(id);
        while (node != null && node.getId() != currentStore.getRootNodeId()) {
            labels.addFirst(node.getLabel() == null ? "" : node.getLabel());
            int parentId = node.getParentId();
            if (parentId <= 0 || parentId == currentStore.getRootNodeId()) {
                break;
            }
            node = currentStore.getNode(parentId);
        }
        return String.join(" | ", labels);
    }

    /**
     * @deprecated superseded by the progress store's batch emitter; removed in the consumer rewrite
     */
    @Deprecated
    public interface StatusUpdatedEventListener {
        void onProgressStatusUpdated(StatusUpdatedEvent event);
    }

    /**
     * @deprecated superseded by the progress store's batch emitter; removed in the consumer rewrite
     */
    @Deprecated
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

    /**
     * @deprecated superseded by the progress store's batch emitter; removed in the consumer rewrite
     */
    @Deprecated
    public static class StatusUpdatedEventEmitter extends JIPipeEventEmitter<StatusUpdatedEvent, StatusUpdatedEventListener> {

        @Override
        protected void call(StatusUpdatedEventListener statusUpdatedEventListener, StatusUpdatedEvent event) {
            statusUpdatedEventListener.onProgressStatusUpdated(event);
        }
    }

}
