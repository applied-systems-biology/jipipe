package org.hkijena.acaq5.api.events;

import org.hkijena.acaq5.api.ACAQRun;

public class RunFinishedEvent {
    private ACAQRun run;

    public RunFinishedEvent(ACAQRun run) {
        this.run = run;
    }

    public ACAQRun getRun() {
        return run;
    }
}
