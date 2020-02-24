package org.hkijena.acaq5.ui.events;

import org.hkijena.acaq5.api.ACAQRun;
import org.hkijena.acaq5.ui.running.ACAQRunWorker;

public class RunUIWorkerFinishedEvent {

    private ACAQRunWorker worker;

    public RunUIWorkerFinishedEvent(ACAQRunWorker worker) {
        this.worker = worker;
    }

    public ACAQRunWorker getWorker() {
        return worker;
    }

    public ACAQRun getRun() {
        return worker.getRun();
    }
}
