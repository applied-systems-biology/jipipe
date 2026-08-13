package org.hkijena.jipipe.api.instrumentation;

/**
 * Base class for all instrumentation events.
 * Each event carries the project ID it belongs to.
 */
public abstract class InstrumentationEvent {
    private final String projectId;

    protected InstrumentationEvent(String projectId) {
        this.projectId = projectId;
    }

    public String getProjectId() {
        return projectId;
    }
}
