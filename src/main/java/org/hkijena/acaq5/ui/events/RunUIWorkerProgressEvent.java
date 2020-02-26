package org.hkijena.acaq5.ui.events;

import org.hkijena.acaq5.api.ACAQRunnable;
import org.hkijena.acaq5.api.ACAQRunnerStatus;
import org.hkijena.acaq5.ui.running.ACAQRunWorker;

public class RunUIWorkerProgressEvent {
    private ACAQRunWorker worker;
    private ACAQRunnerStatus status;

    public RunUIWorkerProgressEvent(ACAQRunWorker worker, ACAQRunnerStatus status) {
        this.worker = worker;
        this.status = status;
    }

    public ACAQRunWorker getWorker() {
        return worker;
    }

    public ACAQRunnable getRun() {
        return worker.getRun();
    }

    public ACAQRunnerStatus getStatus() {
        return status;
    }
}
