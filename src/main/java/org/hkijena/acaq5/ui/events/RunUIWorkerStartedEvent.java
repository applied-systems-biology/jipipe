package org.hkijena.acaq5.ui.events;

import org.hkijena.acaq5.api.ACAQRun;
import org.hkijena.acaq5.ui.running.ACAQRunWorker;

public class RunUIWorkerStartedEvent {

    private ACAQRun run;
    private ACAQRunWorker worker;

    public RunUIWorkerStartedEvent(ACAQRun run, ACAQRunWorker worker) {
        this.run = run;
        this.worker = worker;
    }

    public ACAQRunWorker getWorker() {
        return worker;
    }

    public ACAQRun getRun() {
        return run;
    }
}
