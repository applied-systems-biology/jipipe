package org.hkijena.acaq5.ui.events;

import org.hkijena.acaq5.api.ACAQRun;
import org.hkijena.acaq5.api.events.RunInterruptedEvent;
import org.hkijena.acaq5.ui.running.ACAQRunWorker;

public class RunUIWorkerInterruptedEvent extends RunInterruptedEvent {

    private ACAQRunWorker worker;

    public RunUIWorkerInterruptedEvent(ACAQRun run, Exception exception, ACAQRunWorker worker) {
        super(run, exception);
        this.worker = worker;
    }

    public ACAQRunWorker getWorker() {
        return worker;
    }
}
