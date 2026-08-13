package org.hkijena.jipipe.api.instrumentation.events;

import org.hkijena.jipipe.api.instrumentation.InstrumentationEvent;

public class JobStartedEvent extends InstrumentationEvent {
    private final String jobId;
    private final String operation;
    private final String description;

    public JobStartedEvent(String projectId, String jobId, String operation, String description) {
        super(projectId);
        this.jobId = jobId;
        this.operation = operation;
        this.description = description;
    }

    public String getJobId() { return jobId; }
    public String getOperation() { return operation; }
    public String getDescription() { return description; }
}
