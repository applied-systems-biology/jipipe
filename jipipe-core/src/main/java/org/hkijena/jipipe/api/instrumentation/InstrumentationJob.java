package org.hkijena.jipipe.api.instrumentation;

import com.fasterxml.jackson.databind.JsonNode;
import org.hkijena.jipipe.api.instrumentation.events.JobCompletedEvent;
import org.hkijena.jipipe.api.instrumentation.events.JobProgressEvent;

import java.time.Instant;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Tracks an async instrumentation job (e.g., a pipeline run).
 * Progress and completion are reported via the {@link InstrumentationEventBus}.
 */
public class InstrumentationJob {
    private final String id;
    private final String operation;
    private final String description;
    private final Instant createdAt;
    private final AtomicReference<InstrumentationJobStatus> status = new AtomicReference<>(InstrumentationJobStatus.PENDING);
    private volatile double progress;
    private volatile String message;
    private volatile JsonNode result;
    private volatile String error;
    private volatile Instant completedAt;
    private final InstrumentationEventBus eventBus;
    private final String projectId;
    private final Thread thread;

    InstrumentationJob(String operation, String description, InstrumentationEventBus eventBus, String projectId) {
        this.id = "job-" + UUID.randomUUID().toString().substring(0, 8);
        this.operation = operation;
        this.description = description;
        this.createdAt = Instant.now();
        this.eventBus = eventBus;
        this.projectId = projectId;
        this.thread = Thread.currentThread();
    }

    public String getId() { return id; }
    public String getOperation() { return operation; }
    public String getDescription() { return description; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getCompletedAt() { return completedAt; }
    public InstrumentationJobStatus getStatus() { return status.get(); }
    public double getProgress() { return progress; }
    public String getMessage() { return message; }
    public JsonNode getResult() { return result; }
    public String getError() { return error; }

    void setStatus(InstrumentationJobStatus newStatus) {
        status.set(newStatus);
    }

    public void updateProgress(double progress, String message) {
        this.progress = progress;
        this.message = message;
        if (status.get() == InstrumentationJobStatus.PENDING) {
            status.set(InstrumentationJobStatus.RUNNING);
        }
        eventBus.publish(new JobProgressEvent(projectId, id, progress, message));
    }

    public void complete(JsonNode result) {
        this.result = result;
        this.completedAt = Instant.now();
        status.set(InstrumentationJobStatus.COMPLETED);
        eventBus.publish(new JobCompletedEvent(projectId, id, InstrumentationJobStatus.COMPLETED, result, null));
    }

    public void fail(String error) {
        this.error = error;
        this.completedAt = Instant.now();
        status.set(InstrumentationJobStatus.FAILED);
        eventBus.publish(new JobCompletedEvent(projectId, id, InstrumentationJobStatus.FAILED, null, error));
    }

    public void cancel() {
        this.completedAt = Instant.now();
        status.set(InstrumentationJobStatus.CANCELLED);
        eventBus.publish(new JobCompletedEvent(projectId, id, InstrumentationJobStatus.CANCELLED, null, "Cancelled by user"));
        thread.interrupt();
    }
}
