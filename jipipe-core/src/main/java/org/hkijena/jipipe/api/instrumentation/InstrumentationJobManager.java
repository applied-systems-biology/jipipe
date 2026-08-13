package org.hkijena.jipipe.api.instrumentation;

import org.hkijena.jipipe.api.JIPipeProgressInfo;
import org.hkijena.jipipe.api.instrumentation.events.JobStartedEvent;

import java.util.Collection;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Creates and tracks async instrumentation jobs.
 */
public class InstrumentationJobManager {
    private final Map<String, InstrumentationJob> jobs = new ConcurrentHashMap<>();
    private final InstrumentationEventBus eventBus;
    private final String projectId;

    public InstrumentationJobManager(InstrumentationEventBus eventBus, String projectId) {
        this.eventBus = eventBus;
        this.projectId = projectId;
    }

    public InstrumentationJob createJob(String operation, String description) {
        return createJob(operation, description, new JIPipeProgressInfo());
    }

    public InstrumentationJob createJob(String operation, String description, JIPipeProgressInfo progressInfo) {
        InstrumentationJob job = new InstrumentationJob(operation, description, eventBus, projectId, progressInfo);
        jobs.put(job.getId(), job);
        eventBus.publish(new JobStartedEvent(projectId, job.getId(), operation, description));
        return job;
    }

    public InstrumentationJob getJob(String jobId) {
        return jobs.get(jobId);
    }

    public Collection<InstrumentationJob> getAllJobs() {
        return jobs.values();
    }

    public boolean cancelJob(String jobId) {
        InstrumentationJob job = jobs.get(jobId);
        if (job != null && (job.getStatus() == InstrumentationJobStatus.PENDING || job.getStatus() == InstrumentationJobStatus.RUNNING)) {
            job.cancel();
            return true;
        }
        return false;
    }
}
