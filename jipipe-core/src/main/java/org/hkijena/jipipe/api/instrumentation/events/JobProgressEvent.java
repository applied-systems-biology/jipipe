package org.hkijena.jipipe.api.instrumentation.events;

import org.hkijena.jipipe.api.instrumentation.InstrumentationEvent;

public class JobProgressEvent extends InstrumentationEvent {
    private final String jobId;
    private final double progress;
    private final String message;

    public JobProgressEvent(String projectId, String jobId, double progress, String message) {
        super(projectId);
        this.jobId = jobId;
        this.progress = progress;
        this.message = message;
    }

    public String getJobId() { return jobId; }
    public double getProgress() { return progress; }
    public String getMessage() { return message; }
}
