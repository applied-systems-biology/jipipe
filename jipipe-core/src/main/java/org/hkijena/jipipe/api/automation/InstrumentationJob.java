package org.hkijena.jipipe.api.automation;

import com.fasterxml.jackson.databind.JsonNode;
import org.hkijena.jipipe.api.JIPipeProgressInfo;
import org.hkijena.jipipe.api.automation.events.JobCompletedEvent;
import org.hkijena.jipipe.api.automation.events.JobProgressEvent;

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
    private final InstrumentationEventBus eventBus;
    private final String projectId;
    private final JIPipeProgressInfo progressInfo;
    private volatile double progress;
    private volatile String message;
    private volatile JsonNode result;
    private volatile String error;
    private volatile Instant completedAt;
    private volatile Runnable cancellationHandler;

    InstrumentationJob(String operation, String description, InstrumentationEventBus eventBus, String projectId, JIPipeProgressInfo progressInfo) {
        this.id = "job-" + UUID.randomUUID().toString().substring(0, 8);
        this.operation = operation;
        this.description = description;
        this.createdAt = Instant.now();
        this.eventBus = eventBus;
        this.projectId = projectId;
        this.progressInfo = progressInfo != null ? progressInfo : new JIPipeProgressInfo();
    }

    public String getId() {
        return id;
    }

    public String getOperation() {
        return operation;
    }

    public String getDescription() {
        return description;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getCompletedAt() {
        return completedAt;
    }

    public InstrumentationJobStatus getStatus() {
        return status.get();
    }

    /**
     * Sets the job status. Used by delegation seams whose job lifecycle is driven by an
     * underlying run (e.g., the cache operations wire the status to the run queue that
     * executes the cache update).
     *
     * @param newStatus the new status
     */
    public void setStatus(InstrumentationJobStatus newStatus) {
        status.set(newStatus);
    }

    public double getProgress() {
        return progress;
    }

    public String getMessage() {
        return message;
    }

    public JsonNode getResult() {
        return result;
    }

    public String getError() {
        return error;
    }

    /**
     * The progress handle this job was created with (the creating context's handle: the
     * automation layer's shared per-project chain). Used by delegation seams to surface
     * failures of the delegated work in the progress store, where get_errors reads them.
     */
    public JIPipeProgressInfo getProgressInfo() {
        return progressInfo;
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

    /**
     * Registers a handler that cancels the underlying work of this job (e.g., the
     * {@link org.hkijena.jipipe.api.run.JIPipeRunnable} of a cache update). Invoked by
     * {@link #cancel()} after the job state was switched to
     * {@link InstrumentationJobStatus#CANCELLED}. The handler must be safe to call from
     * any thread and must not block for long.
     *
     * @param cancellationHandler the handler, or null to remove it
     */
    public void setCancellationHandler(Runnable cancellationHandler) {
        this.cancellationHandler = cancellationHandler;
    }

    public void cancel() {
        this.completedAt = Instant.now();
        status.set(InstrumentationJobStatus.CANCELLED);
        Runnable handler = cancellationHandler;
        if (handler != null) {
            handler.run();
        }
        eventBus.publish(new JobCompletedEvent(projectId, id, InstrumentationJobStatus.CANCELLED, null, "Cancelled by user"));
        progressInfo.cancel("Cancelled by user");
    }
}
