package org.hkijena.acaq5.ui.events;

import org.hkijena.acaq5.api.ACAQRunnable;
import org.hkijena.acaq5.api.ACAQRunnerStatus;
import org.hkijena.acaq5.ui.running.ACAQRunWorker;

/**
 * Generated when an {@link ACAQRunWorker} reports progress
 */
public class RunUIWorkerProgressEvent {
    private ACAQRunWorker worker;
    private ACAQRunnerStatus status;

    /**
     * @param worker the worker
     * @param status the progress status
     */
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
