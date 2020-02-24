package org.hkijena.acaq5.ui.running;

import com.google.common.eventbus.EventBus;
import org.hkijena.acaq5.api.ACAQRun;
import org.hkijena.acaq5.ui.events.RunUIWorkerFinishedEvent;
import org.hkijena.acaq5.ui.events.RunUIWorkerInterruptedEvent;
import org.hkijena.acaq5.ui.events.RunUIWorkerProgressEvent;

import javax.swing.*;
import java.util.List;
import java.util.concurrent.CancellationException;
import java.util.concurrent.ExecutionException;

public class ACAQRunWorker extends SwingWorker<Exception, Object> {

    private EventBus eventBus = new EventBus();
    private ACAQRun run;
    private int maxProgress;

    public ACAQRunWorker(ACAQRun run) {
        this.run = run;

        maxProgress = run.getGraph().getAlgorithmCount();
    }

    private void onStatus(ACAQRun.Status status) {
        publish(status.getCurrentTask());
        publish(status.getProgress());
    }

    @Override
    protected Exception doInBackground() throws Exception {
        try {
            run.run(this::onStatus, this::isCancelled);
        }
        catch(Exception e) {
            e.printStackTrace();
            return e;
        }

        return null;
    }

    @Override
    protected void process(List<Object> chunks) {
        super.process(chunks);
        for(Object chunk : chunks) {
            if(chunk instanceof Integer) {
                eventBus.post(new RunUIWorkerProgressEvent(this, (Integer) chunk, null));
            }
            else if(chunk instanceof String) {
                eventBus.post(new RunUIWorkerProgressEvent(this, -1, (String) chunk));
            }
        }
    }

    @Override
    protected void done() {
        try {
            if(isCancelled()) {
                postInterruptedEvent(new RuntimeException("Execution was cancelled by user!"));
            }
            else if(get() != null) {
                final Exception e = get();
                postInterruptedEvent(e);
            }
            else {
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

    public EventBus getEventBus() {
        return eventBus;
    }

    public int getMaxProgress() {
        return maxProgress;
    }

    public ACAQRun getRun() {
        return run;
    }
}
