package org.hkijena.acaq5.api.events;

import org.hkijena.acaq5.api.ACAQRun;

public class RunInterruptedEvent {
    private ACAQRun run;
    private Exception exception;

    public RunInterruptedEvent(ACAQRun run, Exception exception) {
        this.run = run;
        this.exception = exception;
    }

    public ACAQRun getRun() {
        return run;
    }

    public Exception getException() {
        return exception;
    }
}
