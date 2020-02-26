package org.hkijena.acaq5.ui.events;

import org.hkijena.acaq5.api.ACAQRunnable;
import org.hkijena.acaq5.ui.running.ACAQRunWorker;

public class RunUIWorkerFinishedEvent {

    private ACAQRunWorker worker;

    public RunUIWorkerFinishedEvent(ACAQRunWorker worker) {
        this.worker = worker;
    }

    public ACAQRunWorker getWorker() {
        return worker;
    }

    public ACAQRunnable getRun() {
        return worker.getRun();
    }
}
