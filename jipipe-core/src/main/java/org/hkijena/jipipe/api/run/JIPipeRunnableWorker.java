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

package org.hkijena.jipipe.api.run;

import org.apache.commons.lang3.exception.ExceptionUtils;
import org.hkijena.jipipe.JIPipe;
import org.hkijena.jipipe.api.JIPipeProgressInfo;
import org.hkijena.jipipe.api.progress.JIPipeProgressLog;
import org.hkijena.jipipe.api.progress.JIPipeProgressNodeState;

import javax.swing.*;
import java.nio.file.Path;
import java.util.UUID;
import java.util.concurrent.CancellationException;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Wraps a {@link JIPipeRunnable} into a Swing worker for multithreaded runs.
 * <p>
 * Progress reporting: the worker subscribes to the run's progress store's batch emitter and
 * forwards every coalesced {@link JIPipeProgressLog.ProgressBatchEvent} as ONE
 * {@link JIPipeRunnable.ProgressEvent}. No per-record events exist. The events are emitted on the
 * store's writer thread; consumers must hop to the EDT for UI work.
 * <p>
 * Store adoption: a run may adopt its own file-backed store while it executes (e.g. a
 * {@link JIPipeGraphRun} creates its store at the start of {@code run()}); the subscription
 * follows the adoption via {@link JIPipeProgressInfo#subscribeToBatchEvents(JIPipeProgressLog.ProgressBatchEventListener)}.
 * <p>
 * Store attach (non-graph runs): a run submitted through a queue whose progress info still has a
 * silent store receives a file-backed store before execution, so UI consumers are not dark. The
 * attached store is finished and closed by the worker when the run ends (unless the run replaced
 * it - the displaced store never received records and is closed at the adoption point).
 */
public class JIPipeRunnableWorker extends SwingWorker<Throwable, Void> {
    private final JIPipeRunnable run;
    private final AtomicLong startTime = new AtomicLong();
    private final AtomicLong endTime = new AtomicLong();
    private final boolean silent;
    private final JIPipeRunnable.InterruptedEventEmitter interruptedEventEmitter = new JIPipeRunnable.InterruptedEventEmitter();
    private final JIPipeRunnable.FinishedEventEmitter finishedEventEmitter = new JIPipeRunnable.FinishedEventEmitter();
    private final JIPipeRunnable.ProgressEventEmitter progressEventEmitter = new JIPipeRunnable.ProgressEventEmitter();

    /**
     * The file-backed store attached by this worker for a run without one; {@code null} if the run
     * brought its own store. Owned by the worker: finished and closed when the run ends.
     */
    private volatile JIPipeProgressLog attachedLog;
    private JIPipeProgressInfo.BatchEventSubscription batchSubscription;
    private final Runnable storeAdoptedCallback = this::onStoreAdopted;

    /**
     * @param run    The executed run
     * @param silent if no stdout should be printed
     */
    public JIPipeRunnableWorker(JIPipeRunnable run, boolean silent) {
        this.run = run;
        this.silent = silent;
        attachProgressStoreIfSilent();
        subscribeToBatchEvents();
    }

    @Override
    protected Throwable doInBackground() {
        startTime.set(System.currentTimeMillis());
        try {
            run.run();
        } catch (Error | Exception e) {
            if (!silent) {
                run.getProgressInfo().log("An error was encountered");
                run.getProgressInfo().log("------------------------");
                run.getProgressInfo().log(e.toString());
                run.getProgressInfo().log(ExceptionUtils.getStackTrace(e));
                e.printStackTrace();
            }
            return e;
        }
        endTime.set(System.currentTimeMillis());
        return null;
    }

    @Override
    protected void done() {
        endTime.set(System.currentTimeMillis());
        JIPipeProgressNodeState endState = JIPipeProgressNodeState.OK;
        try {
            if (isCancelled()) {
                endState = JIPipeProgressNodeState.CANCELLED;
                postInterruptedEvent(new RuntimeException("Execution was cancelled by user!"));
            } else if (get() != null) {
                endState = JIPipeProgressNodeState.FAILED;
                final Throwable e = get();
                postInterruptedEvent(e);
            } else {
                postFinishedEvent();
            }
        } catch (InterruptedException | ExecutionException e) {
            endState = JIPipeProgressNodeState.FAILED;
            postInterruptedEvent(e);
        } catch (CancellationException e) {
            endState = JIPipeProgressNodeState.CANCELLED;
            postInterruptedEvent(e);
        } finally {
            unsubscribeFromBatchEvents();
            finishAttachedStore(endState);
        }
    }

    public JIPipeRunnable.InterruptedEventEmitter getInterruptedEventEmitter() {
        return interruptedEventEmitter;
    }

    public JIPipeRunnable.ProgressEventEmitter getProgressEventEmitter() {
        return progressEventEmitter;
    }

    public long getRuntimeMillis() {
        return endTime.get() - startTime.get();
    }

    private void postFinishedEvent() {
        finishedEventEmitter.emit(new JIPipeRunnable.FinishedEvent(this));
    }

    public JIPipeRunnable.FinishedEventEmitter getFinishedEventEmitter() {
        return finishedEventEmitter;
    }

    private void postInterruptedEvent(Throwable e) {
        if (!silent) {
            run.getProgressInfo().log("An error was encountered");
            run.getProgressInfo().log("------------------------");
            run.getProgressInfo().log(e.toString());
            run.getProgressInfo().log(ExceptionUtils.getStackTrace(e));
        }

        interruptedEventEmitter.emit(new JIPipeRunnable.InterruptedEvent(this, e));
    }

    /**
     * @return The run
     */
    public JIPipeRunnable getRun() {
        return run;
    }

    /**
     * Attaches a file-backed store to the run's progress info if it still has a silent store.
     * This is the one attach point for non-graph runs submitted through an executor (before
     * execution), so their consumers receive batch events instead of staying dark. Silent queues
     * (no UI) and non-desktop contexts (JIPipe not instantiated) keep their store. Runs that
     * already have a file-backed store are untouched.
     */
    private void attachProgressStoreIfSilent() {
        if (silent || !JIPipe.isInstantiated()) {
            return;
        }
        JIPipeProgressInfo progressInfo = run.getProgressInfo();
        if (progressInfo == null) {
            return;
        }
        JIPipeProgressLog store = progressInfo.getStore();
        if (store == null || store.getFile() != null) {
            return;
        }
        UUID runUuid = run.getRunUUID();
        if (runUuid != null) {
            // The global temporary base is used deliberately: the submission context does not
            // readily know a project, and digging for one here is not worth it.
            progressInfo.setStorageBase((Path) null, runUuid.toString(), run.getTaskLabel());
        } else {
            progressInfo.setStorageBase((Path) null);
        }
        attachedLog = progressInfo.getStore();
    }

    /**
     * Subscribes to the run's progress store's batch emitter (following store adoption) and
     * registers the lifecycle hook that closes the attached store if the run replaces it.
     */
    private void subscribeToBatchEvents() {
        JIPipeProgressInfo progressInfo = run.getProgressInfo();
        if (progressInfo == null) {
            return;
        }
        batchSubscription = progressInfo.subscribeToBatchEvents(this::onProgressBatch);
        progressInfo.addStoreAdoptedCallback(storeAdoptedCallback);
    }

    /**
     * Forwards ONE coalesced batch as ONE progress event. Runs on the store's writer thread.
     */
    private void onProgressBatch(JIPipeProgressLog.ProgressBatchEvent event) {
        progressEventEmitter.emit(new JIPipeRunnable.ProgressEvent(this, event));
    }

    /**
     * Closes the attached store if the run adopted a different store. Adoption happens before the
     * first record of the new store, so the displaced store never received records - closing it
     * (without a finish record) stops its writer thread and releases the log file name.
     */
    private void onStoreAdopted() {
        JIPipeProgressLog attached = attachedLog;
        if (attached != null && run.getProgressInfo().getStore() != attached) {
            attachedLog = null;
            try {
                attached.close();
            } catch (Throwable e) {
                e.printStackTrace();
            }
        }
    }

    private void unsubscribeFromBatchEvents() {
        JIPipeProgressInfo progressInfo = run.getProgressInfo();
        if (progressInfo != null) {
            progressInfo.removeStoreAdoptedCallback(storeAdoptedCallback);
        }
        if (batchSubscription != null) {
            batchSubscription.close();
            batchSubscription = null;
        }
    }

    /**
     * Finishes and closes the attached store: the guaranteed final batch is emitted, the run
     * status record is written, and the writer thread stops.
     *
     * @param endState the final run state
     */
    private void finishAttachedStore(JIPipeProgressNodeState endState) {
        JIPipeProgressLog attached = attachedLog;
        if (attached == null) {
            return;
        }
        attachedLog = null;
        try {
            attached.finish(endState);
        } catch (Throwable e) {
            e.printStackTrace();
        }
    }
}
