package org.hkijena.acaq5.ui.events;

import org.hkijena.acaq5.api.ACAQRunnable;
import org.hkijena.acaq5.ui.running.ACAQRunWorker;

/**
 * Generated when an {@link ACAQRunWorker} finished its work
 */
public class RunUIWorkerFinishedEvent {

    private ACAQRunWorker worker;

    /**
     * @param worker worker that finished
     */
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
