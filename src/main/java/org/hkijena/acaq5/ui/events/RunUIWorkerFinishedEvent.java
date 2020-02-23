package org.hkijena.acaq5.ui.events;

import org.hkijena.acaq5.api.ACAQRun;
import org.hkijena.acaq5.api.events.RunFinishedEvent;
import org.hkijena.acaq5.ui.running.ACAQRunWorker;

public class RunUIWorkerFinishedEvent extends RunFinishedEvent {

    private ACAQRunWorker worker;

    public RunUIWorkerFinishedEvent(ACAQRun run, ACAQRunWorker worker) {
        super(run);
        this.worker = worker;
    }

    public ACAQRunWorker getWorker() {
        return worker;
    }
}
