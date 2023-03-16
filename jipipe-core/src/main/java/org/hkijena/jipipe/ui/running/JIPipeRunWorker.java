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

package org.hkijena.jipipe.ui.running;

import com.google.common.eventbus.EventBus;
import com.google.common.eventbus.Subscribe;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.hkijena.jipipe.api.JIPipeProgressInfo;
import org.hkijena.jipipe.api.JIPipeRunnable;

import javax.swing.*;
import java.util.List;
import java.util.concurrent.CancellationException;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.atomic.AtomicLong;

/**
 * A worker
 */
public class JIPipeRunWorker extends SwingWorker<Throwable, Object> {

    private final EventBus eventBus = new EventBus();
    private final JIPipeRunnable run;
    private final AtomicLong startTime = new AtomicLong();
    private final AtomicLong endTime = new AtomicLong();

    private final boolean silent;

    /**
     * @param run    The executed run
     * @param silent if no stdout should be printed
     */
    public JIPipeRunWorker(JIPipeRunnable run, boolean silent) {
        this.run = run;
        this.silent = silent;
        this.run.getProgressInfo().getEventBus().register(this);
    }

    @Subscribe
    public void onStatus(JIPipeProgressInfo.StatusUpdatedEvent status) {
        publish(status);
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
                eventBus.post(new JIPipeRunnable.ProgressEvent(this, (JIPipeProgressInfo.StatusUpdatedEvent) chunk));
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

    public long getRuntimeMillis() {
        return endTime.get() - startTime.get();
    }

    private void postFinishedEvent() {
        eventBus.post(new JIPipeRunnable.FinishedEvent(this));
    }

    private void postInterruptedEvent(Throwable e) {
        if (!silent) {
            run.getProgressInfo().log("An error was encountered");
            run.getProgressInfo().log("------------------------");
            run.getProgressInfo().log(e.toString());
            run.getProgressInfo().log(ExceptionUtils.getStackTrace(e));
        }

        eventBus.post(new JIPipeRunnable.InterruptedEvent(this, e));
    }

    /**
     * @return The event bus
     */
    public EventBus getEventBus() {
        return eventBus;
    }

    /**
     * @return The run
     */
    public JIPipeRunnable getRun() {
        return run;
    }


}
