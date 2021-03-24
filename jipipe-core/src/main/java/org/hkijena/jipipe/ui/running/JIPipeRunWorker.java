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

/**
 * A worker
 */
public class JIPipeRunWorker extends SwingWorker<Exception, Object> {

    private final EventBus eventBus = new EventBus();
    private final JIPipeRunnable run;

    /**
     * @param run The executed run
     */
    public JIPipeRunWorker(JIPipeRunnable run) {
        this.run = run;
        this.run.getProgressInfo().getEventBus().register(this);
    }

    @Subscribe
    public void onStatus(JIPipeProgressInfo.StatusUpdatedEvent status) {
        publish(status);
    }

    @Override
    protected Exception doInBackground() throws Exception {
        try {
            run.run();
        } catch (Exception e) {
            run.getProgressInfo().log("An error was encountered");
            run.getProgressInfo().log("------------------------");
            run.getProgressInfo().log(e.toString());
            run.getProgressInfo().log(ExceptionUtils.getStackTrace(e));
            e.printStackTrace();
            return e;
        }

        return null;
    }

    @Override
    protected void process(List<Object> chunks) {
        super.process(chunks);
        for (Object chunk : chunks) {
            if (chunk instanceof JIPipeProgressInfo.StatusUpdatedEvent) {
                eventBus.post(new RunUIWorkerProgressEvent(this, (JIPipeProgressInfo.StatusUpdatedEvent) chunk));
            }
        }
    }

    @Override
    protected void done() {
        try {
            if (isCancelled()) {
                postInterruptedEvent(new RuntimeException("Execution was cancelled by user!"));
            } else if (get() != null) {
                final Exception e = get();
                postInterruptedEvent(e);
            } else {
                postFinishedEvent();
            }
        } catch (InterruptedException | ExecutionException | CancellationException e) {
            postInterruptedEvent(e);
        }
    }

    private void postFinishedEvent() {
        eventBus.post(new RunUIWorkerFinishedEvent(this));
    }

    private void postInterruptedEvent(Exception e) {
        run.getProgressInfo().log("An error was encountered");
        run.getProgressInfo().log("------------------------");
        run.getProgressInfo().log(e.toString());
        run.getProgressInfo().log(ExceptionUtils.getStackTrace(e));

        eventBus.post(new RunUIWorkerInterruptedEvent(this, e));
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
