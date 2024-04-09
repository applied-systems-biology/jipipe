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
import org.hkijena.jipipe.api.JIPipeProgressInfo;

import javax.swing.*;
import java.util.List;
import java.util.concurrent.CancellationException;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Wraps a {@link JIPipeRunnable} into a Swing worker for multithreaded runs
 */
public class JIPipeRunnableWorker extends SwingWorker<Throwable, Object> implements JIPipeProgressInfo.StatusUpdatedEventListener {
    private final JIPipeRunnable run;
    private final AtomicLong startTime = new AtomicLong();
    private final AtomicLong endTime = new AtomicLong();
    private final boolean silent;
    private final JIPipeRunnable.InterruptedEventEmitter interruptedEventEmitter = new JIPipeRunnable.InterruptedEventEmitter();
    private final JIPipeRunnable.FinishedEventEmitter finishedEventEmitter = new JIPipeRunnable.FinishedEventEmitter();
    private final JIPipeRunnable.ProgressEventEmitter progressEventEmitter = new JIPipeRunnable.ProgressEventEmitter();

    /**
     * @param run    The executed run
     * @param silent if no stdout should be printed
     */
    public JIPipeRunnableWorker(JIPipeRunnable run, boolean silent) {
        this.run = run;
        this.silent = silent;
        this.run.getProgressInfo().getStatusUpdatedEventEmitter().subscribe(this);
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
    protected void process(List<Object> chunks) {
        super.process(chunks);
        for (Object chunk : chunks) {
            if (chunk instanceof JIPipeProgressInfo.StatusUpdatedEvent) {
                progressEventEmitter.emit(new JIPipeRunnable.ProgressEvent(this, (JIPipeProgressInfo.StatusUpdatedEvent) chunk));
            }
        }
    }

    @Override
    protected void done() {
        endTime.set(System.currentTimeMillis());
        try {
            if (isCancelled()) {
                postInterruptedEvent(new RuntimeException("Execution was cancelled by user!"));
            } else if (get() != null) {
                final Throwable e = get();
                postInterruptedEvent(e);
            } else {
                postFinishedEvent();
            }
        } catch (InterruptedException | ExecutionException | CancellationException e) {
            postInterruptedEvent(e);
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

    @Override
    public void onProgressStatusUpdated(JIPipeProgressInfo.StatusUpdatedEvent event) {
        publish(event);
    }
}
