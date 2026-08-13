package org.hkijena.jipipe.api.instrumentation;

import org.hkijena.jipipe.api.instrumentation.events.JobCompletedEvent;
import org.hkijena.jipipe.api.instrumentation.events.JobStartedEvent;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class InstrumentationJobManagerTest {

    @Test
    void createJob_returnsJobWithPendingStatus() {
        InstrumentationEventBus eventBus = new InstrumentationEventBus();
        InstrumentationJobManager manager = new InstrumentationJobManager(eventBus, "project-1");

        InstrumentationJob job = manager.createJob("run_node", "Update cache: Node A");

        assertEquals("run_node", job.getOperation());
        assertEquals("Update cache: Node A", job.getDescription());
        assertEquals(InstrumentationJobStatus.PENDING, job.getStatus());
        assertNotNull(job.getId());
    }

    @Test
    void createJob_publishesJobStartedEvent() {
        InstrumentationEventBus eventBus = new InstrumentationEventBus();
        List<JobStartedEvent> events = new ArrayList<>();
        eventBus.subscribe(JobStartedEvent.class, events::add);
        InstrumentationJobManager manager = new InstrumentationJobManager(eventBus, "project-1");

        InstrumentationJob job = manager.createJob("run_node", "Test");

        assertEquals(1, events.size());
        assertEquals(job.getId(), events.get(0).getJobId());
    }

    @Test
    void completeJob_setsStatusAndPublishesEvent() {
        InstrumentationEventBus eventBus = new InstrumentationEventBus();
        List<JobCompletedEvent> events = new ArrayList<>();
        eventBus.subscribe(JobCompletedEvent.class, events::add);
        InstrumentationJobManager manager = new InstrumentationJobManager(eventBus, "project-1");

        InstrumentationJob job = manager.createJob("run_node", "Test");
        job.complete(null);

        assertEquals(InstrumentationJobStatus.COMPLETED, job.getStatus());
        assertEquals(1, events.size());
        assertEquals(job.getId(), events.get(0).getJobId());
        assertEquals(InstrumentationJobStatus.COMPLETED, events.get(0).getStatus());
    }

    @Test
    void failJob_setsFailedStatus() {
        InstrumentationEventBus eventBus = new InstrumentationEventBus();
        InstrumentationJobManager manager = new InstrumentationJobManager(eventBus, "project-1");

        InstrumentationJob job = manager.createJob("run_node", "Test");
        job.fail("Something went wrong");

        assertEquals(InstrumentationJobStatus.FAILED, job.getStatus());
        assertEquals("Something went wrong", job.getError());
    }

    @Test
    void getJob_returnsCreatedJob() {
        InstrumentationEventBus eventBus = new InstrumentationEventBus();
        InstrumentationJobManager manager = new InstrumentationJobManager(eventBus, "project-1");

        InstrumentationJob job = manager.createJob("run_node", "Test");

        assertSame(job, manager.getJob(job.getId()));
    }
}
