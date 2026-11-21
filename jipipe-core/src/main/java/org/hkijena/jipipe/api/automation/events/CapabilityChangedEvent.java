package org.hkijena.jipipe.api.automation.events;

import org.hkijena.jipipe.api.automation.InstrumentationEvent;

/**
 * Published on the automation event bus when a capability's install finished.
 *
 * @param projectId    the project id of the caller (null for instance-global calls)
 * @param capabilityId the capability id whose install job finished
 * @param jobId        the id of the finished install job
 * @param success      true when the install completed, false when it failed
 * @param error        the failure reason, or null when {@code success} is true
 */
public class CapabilityChangedEvent extends InstrumentationEvent {
    private final String capabilityId;
    private final String jobId;
    private final boolean success;
    private final String error;

    public CapabilityChangedEvent(String projectId, String capabilityId, String jobId, boolean success, String error) {
        super(projectId);
        this.capabilityId = capabilityId;
        this.jobId = jobId;
        this.success = success;
        this.error = error;
    }

    public String getCapabilityId() {
        return capabilityId;
    }

    public String getJobId() {
        return jobId;
    }

    public boolean isSuccess() {
        return success;
    }

    public String getError() {
        return error;
    }
}
