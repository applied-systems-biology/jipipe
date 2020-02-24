package org.hkijena.acaq5.ui.events;

import org.hkijena.acaq5.api.ACAQRun;
import org.hkijena.acaq5.ui.running.ACAQRunWorker;

public class RunUIWorkerProgressEvent {
    private ACAQRunWorker worker;
    private int progress;
    private String message;

    public RunUIWorkerProgressEvent(ACAQRunWorker worker, int progress, String message) {
        this.worker = worker;
        this.progress = progress;
        this.message = message;
    }

    public ACAQRunWorker getWorker() {
        return worker;
    }

    public int getProgress() {
        return progress;
    }

    public String getMessage() {
        return message;
    }

    public ACAQRun getRun() {
        return worker.getRun();
    }
}
