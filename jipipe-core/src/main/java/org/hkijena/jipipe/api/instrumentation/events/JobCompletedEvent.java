package org.hkijena.jipipe.api.instrumentation.events;

import com.fasterxml.jackson.databind.JsonNode;
import org.hkijena.jipipe.api.instrumentation.InstrumentationEvent;
import org.hkijena.jipipe.api.instrumentation.InstrumentationJobStatus;

public class JobCompletedEvent extends InstrumentationEvent {
    private final String jobId;
    private final InstrumentationJobStatus status;
    private final JsonNode result;
    private final String error;

    public JobCompletedEvent(String projectId, String jobId, InstrumentationJobStatus status, JsonNode result, String error) {
        super(projectId);
        this.jobId = jobId;
        this.status = status;
        this.result = result;
        this.error = error;
    }

    public String getJobId() { return jobId; }
    public InstrumentationJobStatus getStatus() { return status; }
    public JsonNode getResult() { return result; }
    public String getError() { return error; }
}
