package org.hkijena.acaq5.api;

import com.google.common.eventbus.EventBus;

/**
 * An ACAQ5 project.
 * It contains all information to setup and run an analysis
 */
public class ACAQProject {
    private EventBus eventBus = new EventBus();
    private ACAQIO preprocessingOutput = new ACAQIO();
    private ACAQIO analysisOutput = new ACAQIO();

    public ACAQProject() {
    }

    public EventBus getEventBus() {
        return eventBus;
    }

    public ACAQIO getPreprocessingOutput() {
        return preprocessingOutput;
    }

    public ACAQIO getAnalysisOutput() {
        return analysisOutput;
    }
}
