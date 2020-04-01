package org.hkijena.acaq5.ui.events;

import org.hkijena.acaq5.api.ACAQRunnable;
import org.hkijena.acaq5.ui.running.ACAQRunWorker;

/**
 * Generated when work of an {@link ACAQRunWorker} is interrupted
 */
public class RunUIWorkerInterruptedEvent {

    private Exception exception;
    private ACAQRunWorker worker;

    /**
     * @param worker    the worker
     * @param exception the exception triggered when interrupted
     */
    public RunUIWorkerInterruptedEvent(ACAQRunWorker worker, Exception exception) {
        this.exception = exception;
        this.worker = worker;
    }

    public ACAQRunWorker getWorker() {
        return worker;
    }

    public Exception getException() {
        return exception;
    }

    public ACAQRunnable getRun() {
        return worker.getRun();
    }
}
