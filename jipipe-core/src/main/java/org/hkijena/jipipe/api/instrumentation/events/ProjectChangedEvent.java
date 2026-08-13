package org.hkijena.jipipe.api.instrumentation.events;

import org.hkijena.jipipe.api.instrumentation.InstrumentationEvent;

public class ProjectChangedEvent extends InstrumentationEvent {
    private final String projectName;

    public ProjectChangedEvent(String projectId, String projectName) {
        super(projectId);
        this.projectName = projectName;
    }

    public String getProjectName() {
        return projectName;
    }
}
