package org.hkijena.acaq5.ui.running;

import com.google.common.eventbus.EventBus;
import org.hkijena.acaq5.api.ACAQRunnable;
import org.hkijena.acaq5.api.ACAQRunnerStatus;
import org.hkijena.acaq5.ui.events.RunUIWorkerFinishedEvent;
import org.hkijena.acaq5.ui.events.RunUIWorkerInterruptedEvent;
import org.hkijena.acaq5.ui.events.RunUIWorkerProgressEvent;

import javax.swing.*;
import java.util.List;
import java.util.concurrent.CancellationException;
import java.util.concurrent.ExecutionException;

/**
 * A worker
 */
public class ACAQRunWorker extends SwingWorker<Exception, Object> {

    private EventBus eventBus = new EventBus();
    private ACAQRunnable run;

    /**
     * @param run The executed run
     */
    public ACAQRunWorker(ACAQRunnable run) {
        this.run = run;
    }

    private void onStatus(ACAQRunnerStatus status) {
        publish(status);
    }

    @Override
    protected Exception doInBackground() throws Exception {
        try {
            run.run(this::onStatus, this::isCancelled);
        } catch (Exception e) {
            e.printStackTrace();
            return e;
        }

        return null;
    }

    @Override
    protected void process(List<Object> chunks) {
        super.process(chunks);
        for (Object chunk : chunks) {
            if (chunk instanceof ACAQRunnerStatus) {
                eventBus.post(new RunUIWorkerProgressEvent(this, (ACAQRunnerStatus) chunk));
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
    public ACAQRunnable getRun() {
        return run;
    }
}
