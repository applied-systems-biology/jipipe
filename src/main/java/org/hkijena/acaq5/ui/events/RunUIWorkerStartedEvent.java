package org.hkijena.acaq5.ui.events;

import org.hkijena.acaq5.api.ACAQRun;
import org.hkijena.acaq5.api.ACAQRunnable;
import org.hkijena.acaq5.ui.running.ACAQRunWorker;

public class RunUIWorkerStartedEvent {

    private ACAQRunnable run;
    private ACAQRunWorker worker;

    public RunUIWorkerStartedEvent(ACAQRunnable run, ACAQRunWorker worker) {
        this.run = run;
        this.worker = worker;
    }

    public ACAQRunWorker getWorker() {
        return worker;
    }

    public ACAQRunnable getRun() {
        return run;
    }
}
