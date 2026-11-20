/*
 * Copyright by Zoltán Cseresnyés, Ruman Gerst
 *
 * Research Group Applied Systems Biology - Head: Prof. Dr. Marc Thilo Figge
 * https://www.leibniz-hki.de/en/applied-systems-biology.html
 * HKI-Center for Systems Biology of Infection
 * Leibniz Institute for Natural Product Research and Infection Biology - Hans Knöll Institute (HKI)
 * Adolf-Reichwein-Straße 23, 07745 Jena, Germany
 *
 * The project code is licensed under MIT.
 * See the LICENSE file provided with the code for the full license.
 */

package org.hkijena.jipipe.api.automation.operations;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.hkijena.jipipe.JIPipe;
import org.hkijena.jipipe.api.DefaultJIPipeRunnable;
import org.hkijena.jipipe.api.JIPipeProgressInfo;
import org.hkijena.jipipe.api.automation.AutomationContext;
import org.hkijena.jipipe.api.automation.AutomationOperationRegistry;
import org.hkijena.jipipe.api.automation.InstrumentationEventBus;
import org.hkijena.jipipe.api.automation.InstrumentationJob;
import org.hkijena.jipipe.api.automation.InstrumentationJobManager;
import org.hkijena.jipipe.api.automation.InstrumentationJobStatus;
import org.hkijena.jipipe.api.nodes.JIPipeGraphNode;
import org.hkijena.jipipe.api.nodes.JIPipeNodeInfo;
import org.hkijena.jipipe.api.project.JIPipeProject;
import org.hkijena.jipipe.api.run.JIPipeQueuedRunnableExecutor;
import org.hkijena.jipipe.api.run.JIPipeRunnable;
import org.hkijena.jipipe.api.run.JIPipeRunnableWorker;
import org.hkijena.jipipe.desktop.app.quickrun.JIPipeDesktopQuickRunSettings;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

/**
 * Tests the cache, job, and arrange operations: update_cache schema and conditional
 * requirements, the injectable cache trigger seam and its job lifecycle wiring to the
 * QuickRun on the shared run queue, get_job_status and cancel_job against the real job
 * manager, and the layered layout of arrange_nodes.
 */
public class CacheOperationsTest {

    private static final ObjectMapper mapper = new ObjectMapper();

    private record Fixture(JIPipeProject project, JIPipeGraphNode node) {
    }

    /**
     * Records the delegated cache triggers so tests can assert the operation resolves
     * targets and modes without invoking the real cache services. The stub stays silent
     * about the job: it emulates a queued QuickRun whose wiring has not fired yet.
     */
    private static class RecordingTrigger implements CacheOperations.CacheTrigger {
        final List<JIPipeProject> projects = new ArrayList<>();
        final List<List<JIPipeGraphNode>> targets = new ArrayList<>();
        final List<String> modes = new ArrayList<>();
        final List<InstrumentationJob> jobs = new ArrayList<>();
        final CountDownLatch invoked = new CountDownLatch(1);

        @Override
        public void run(JIPipeProject project, List<JIPipeGraphNode> targetNodes, String mode, InstrumentationJob job) {
            projects.add(project);
            targets.add(targetNodes);
            modes.add(mode);
            jobs.add(job);
            invoked.countDown();
        }
    }

    @BeforeAll
    static void ensureJIPipe() {
        JIPipe.ensureInstance();
    }

    private static AutomationContext createContext(JIPipeProject project) {
        InstrumentationEventBus eventBus = new InstrumentationEventBus();
        InstrumentationJobManager jobManager = new InstrumentationJobManager(eventBus, "test");
        return new AutomationContext(project, "test", new JIPipeProgressInfo(), eventBus, jobManager);
    }

    /**
     * Returns the info of any registered node type that can be instantiated and has at
     * least one input and one output slot (so a chain can be connected), or null.
     */
    private static JIPipeNodeInfo findChainableNodeInfo() {
        for (JIPipeNodeInfo info : JIPipe.getNodes().getRegisteredNodeInfos().values()) {
            try {
                JIPipeGraphNode instance = info.newInstance();
                if (!instance.getInputSlots().isEmpty() && !instance.getOutputSlots().isEmpty()) {
                    return info;
                }
            } catch (Exception ignored) {
                // Uninstantiable node types are skipped
            }
        }
        return null;
    }

    private static Fixture createFixture() {
        JIPipeNodeInfo info = findChainableNodeInfo();
        assumeTrue(info != null, "no chainable node type found in this environment");
        JIPipeProject project = new JIPipeProject();
        var compartment = project.addCompartment("Cache Test Compartment");
        JIPipeGraphNode node = info.newInstance();
        project.getGraph().insertNode(node, compartment.getProjectCompartmentUUID());
        return new Fixture(project, node);
    }

    /**
     * Creates a project with one compartment containing a chain first -> second -> third.
     */
    private record ChainFixture(JIPipeProject project, JIPipeGraphNode first,
                                JIPipeGraphNode second, JIPipeGraphNode third) {
    }

    private static ChainFixture createChainFixture() {
        JIPipeNodeInfo info = findChainableNodeInfo();
        assumeTrue(info != null, "no chainable node type found in this environment");
        JIPipeProject project = new JIPipeProject();
        var compartment = project.addCompartment("Arrange Test Compartment");
        JIPipeGraphNode first = info.newInstance();
        JIPipeGraphNode second = info.newInstance();
        JIPipeGraphNode third = info.newInstance();
        project.getGraph().insertNode(first, compartment.getProjectCompartmentUUID());
        project.getGraph().insertNode(second, compartment.getProjectCompartmentUUID());
        project.getGraph().insertNode(third, compartment.getProjectCompartmentUUID());
        project.getGraph().connect(first.getOutputSlots().get(0), second.getInputSlots().get(0));
        project.getGraph().connect(second.getOutputSlots().get(0), third.getInputSlots().get(0));
        return new ChainFixture(project, first, second, third);
    }

    private static ObjectNode updateCacheParams(String scope, String mode, String compartment, String node) {
        ObjectNode params = mapper.createObjectNode();
        params.put("scope", scope);
        params.put("mode", mode);
        if (compartment != null) {
            params.put("compartment", compartment);
        }
        if (node != null) {
            params.put("node", node);
        }
        return params;
    }

    @Test
    void invalidScopeIsRejectedBySchemaValidation() {
        Fixture fixture = createFixture();
        AutomationOperationRegistry registry = new AutomationOperationRegistry();
        registry.register("update_cache", new CacheOperations.UpdateCache(new RecordingTrigger()));
        ObjectNode params = updateCacheParams("bogus", "full", "Cache Test Compartment",
                fixture.project().getGraph().getAliasIdOf(fixture.node()));

        assertThrows(IllegalArgumentException.class,
                () -> registry.execute("update_cache", createContext(fixture.project()), params));
    }

    @Test
    void invalidModeIsRejectedBySchemaValidation() {
        Fixture fixture = createFixture();
        AutomationOperationRegistry registry = new AutomationOperationRegistry();
        registry.register("update_cache", new CacheOperations.UpdateCache(new RecordingTrigger()));
        ObjectNode params = updateCacheParams("node", "bogus", "Cache Test Compartment",
                fixture.project().getGraph().getAliasIdOf(fixture.node()));

        assertThrows(IllegalArgumentException.class,
                () -> registry.execute("update_cache", createContext(fixture.project()), params));
    }

    @Test
    void updateCacheWithNodeScopeDelegatesToTriggerAndReturnsJobId() throws InterruptedException {
        Fixture fixture = createFixture();
        RecordingTrigger trigger = new RecordingTrigger();
        ObjectNode params = updateCacheParams("node", "full", "Cache Test Compartment",
                fixture.project().getGraph().getAliasIdOf(fixture.node()));
        JsonNode result = new CacheOperations.UpdateCache(trigger).execute(createContext(fixture.project()), params);

        assertTrue(trigger.invoked.await(10, TimeUnit.SECONDS), "the trigger must be invoked");
        assertTrue(result.has("job-id") && !result.get("job-id").asText().isBlank(),
                "update_cache must return a job id: " + result);
        assertEquals(fixture.project(), trigger.projects.get(0));
        assertEquals("full", trigger.modes.get(0));
        assertEquals(List.of(fixture.node()), trigger.targets.get(0), "the node scope must target exactly the addressed node");
    }

    @Test
    void updateCacheWithCompartmentScopeDelegatesRunnableNodes() throws InterruptedException {
        Fixture fixture = createFixture();
        RecordingTrigger trigger = new RecordingTrigger();
        ObjectNode params = updateCacheParams("compartment", "intermediate", "Cache Test Compartment", null);
        JsonNode result = new CacheOperations.UpdateCache(trigger).execute(createContext(fixture.project()), params);

        assertTrue(trigger.invoked.await(10, TimeUnit.SECONDS), "the trigger must be invoked");
        assertTrue(!result.has("error"), () -> "valid args must not produce an error: " + result);
        assertEquals("intermediate", trigger.modes.get(0));
        assertEquals(List.of(fixture.node()), trigger.targets.get(0), "the compartment scope must target its nodes");
    }

    @Test
    void updateCacheWithPipelineScopeDelegatesAllNodes() throws InterruptedException {
        Fixture fixture = createFixture();
        RecordingTrigger trigger = new RecordingTrigger();
        ObjectNode params = updateCacheParams("pipeline", "predecessors", null, null);
        JsonNode result = new CacheOperations.UpdateCache(trigger).execute(createContext(fixture.project()), params);

        assertTrue(trigger.invoked.await(10, TimeUnit.SECONDS), "the trigger must be invoked");
        assertTrue(!result.has("error"), () -> "valid args must not produce an error: " + result);
        assertEquals("predecessors", trigger.modes.get(0));
        assertTrue(trigger.targets.get(0).contains(fixture.node()), "the pipeline scope must target the nodes of the project");
    }

    @Test
    void updateCachePassesJobIntoTheCacheRunWiring() throws InterruptedException {
        Fixture fixture = createFixture();
        RecordingTrigger trigger = new RecordingTrigger();
        ObjectNode params = updateCacheParams("node", "full", "Cache Test Compartment",
                fixture.project().getGraph().getAliasIdOf(fixture.node()));
        JsonNode result = new CacheOperations.UpdateCache(trigger).execute(createContext(fixture.project()), params);

        assertTrue(trigger.invoked.await(10, TimeUnit.SECONDS), "the trigger must be invoked");
        InstrumentationJob job = trigger.jobs.get(0);
        assertNotNull(job, "the wiring contract requires the job to be passed into the trigger");
        assertEquals(job.getId(), result.get("job-id").asText(),
                "the returned job id must be the job handed to the trigger");
        assertEquals(InstrumentationJobStatus.PENDING, job.getStatus(),
                "the operation must not complete the job when the trigger returns: the job completes when the cache run finishes");
    }

    /**
     * Emulates the production wiring: the QuickRun is queued when the trigger returns,
     * and the lifecycle transitions fire later from the run.
     */
    private static class QueuedRunTrigger implements CacheOperations.CacheTrigger {
        InstrumentationJob job;
        final CountDownLatch invoked = new CountDownLatch(1);

        @Override
        public void run(JIPipeProject project, List<JIPipeGraphNode> targetNodes, String mode, InstrumentationJob job) {
            this.job = job;
            invoked.countDown();
        }
    }

    @Test
    void updateCacheJobFollowsTheCacheRunLifecycle() throws InterruptedException {
        Fixture fixture = createFixture();
        AutomationContext ctx = createContext(fixture.project());
        QueuedRunTrigger trigger = new QueuedRunTrigger();
        ObjectNode params = updateCacheParams("pipeline", "full", null, null);
        JsonNode result = new CacheOperations.UpdateCache(trigger).execute(ctx, params);

        assertTrue(trigger.invoked.await(10, TimeUnit.SECONDS), "the trigger must be invoked");
        InstrumentationJob job = ctx.getJobManager().getJob(result.get("job-id").asText());
        assertNotNull(job, "the job handed to the trigger must be tracked by the job manager");
        assertEquals(InstrumentationJobStatus.PENDING, job.getStatus(), "a queued cache run must report PENDING");

        // The executor starts the QuickRun, then the run finishes: the wiring transitions
        // the job exactly like the production wiring reacts to the executor events
        job.setStatus(InstrumentationJobStatus.RUNNING);
        job.complete(null);

        ObjectNode statusParams = mapper.createObjectNode();
        statusParams.put("job-id", job.getId());
        JsonNode status = new ExecutionOperations.GetJobStatus().execute(ctx, statusParams);
        assertEquals(InstrumentationJobStatus.COMPLETED.name(), status.get("state").asText(),
                "get_job_status must report the final state of the cache run");
    }

    @Test
    void updateCacheJobFailsWithTheCacheRunError() throws InterruptedException {
        Fixture fixture = createFixture();
        AutomationContext ctx = createContext(fixture.project());
        QueuedRunTrigger trigger = new QueuedRunTrigger();
        ObjectNode params = updateCacheParams("pipeline", "full", null, null);
        JsonNode result = new CacheOperations.UpdateCache(trigger).execute(ctx, params);

        assertTrue(trigger.invoked.await(10, TimeUnit.SECONDS), "the trigger must be invoked");
        InstrumentationJob job = ctx.getJobManager().getJob(result.get("job-id").asText());
        assertNotNull(job);
        job.setStatus(InstrumentationJobStatus.RUNNING);
        job.fail("cache run failed");

        ObjectNode statusParams = mapper.createObjectNode();
        statusParams.put("job-id", job.getId());
        JsonNode status = new ExecutionOperations.GetJobStatus().execute(ctx, statusParams);
        assertEquals(InstrumentationJobStatus.FAILED.name(), status.get("state").asText());
        assertEquals("cache run failed", status.get("error").asText(), "the status must carry the cache run error");
    }

    /**
     * Emulates the production wiring's cancellation handler: the handler cancels the
     * underlying QuickRun (here: records the invocation).
     */
    private static class CancellableRunTrigger implements CacheOperations.CacheTrigger {
        InstrumentationJob job;
        final CountDownLatch invoked = new CountDownLatch(1);
        final CountDownLatch cancelled = new CountDownLatch(1);

        @Override
        public void run(JIPipeProject project, List<JIPipeGraphNode> targetNodes, String mode, InstrumentationJob job) {
            this.job = job;
            job.setCancellationHandler(cancelled::countDown);
            invoked.countDown();
        }
    }

    @Test
    void cancelJobCancelsTheUnderlyingCacheRun() throws InterruptedException {
        Fixture fixture = createFixture();
        AutomationContext ctx = createContext(fixture.project());
        CancellableRunTrigger trigger = new CancellableRunTrigger();
        ObjectNode params = updateCacheParams("pipeline", "full", null, null);
        JsonNode result = new CacheOperations.UpdateCache(trigger).execute(ctx, params);

        assertTrue(trigger.invoked.await(10, TimeUnit.SECONDS), "the trigger must be invoked");
        ObjectNode cancelParams = mapper.createObjectNode();
        // The run envelope and the cancel schema share the plan-pinned "job-id" key, so the
        // value chains directly from the update_cache result into the cancel_job parameters.
        cancelParams.put("job-id", result.get("job-id").asText());
        JsonNode cancelled = new ExecutionOperations.CancelJob().execute(ctx, cancelParams);

        assertTrue(cancelled.get("cancelled").asBoolean(), "a queued cache run must be cancellable");
        assertTrue(trigger.cancelled.await(10, TimeUnit.SECONDS),
                "cancel_job must run the cancellation handler that cancels the QuickRun");
        assertEquals(InstrumentationJobStatus.CANCELLED, trigger.job.getStatus());
    }

    /**
     * A silent no-op run for the lifecycle wiring tests (never enqueued: the wiring
     * under test subscribes to executor-level events, and the events are emitted here
     * directly, without executing a pipeline).
     */
    private static JIPipeRunnable testCacheRun() {
        return new DefaultJIPipeRunnable() {
            @Override
            public void run() {
            }

            @Override
            public String getTaskLabel() {
                return "Test cache run";
            }
        };
    }

    @Test
    void cacheJobWiringFollowsTheExecutorLifecycleEvents() {
        // A private executor instance: the wiring must not react to events of other runs
        JIPipeQueuedRunnableExecutor executor = new JIPipeQueuedRunnableExecutor("cache-operations-test");
        JIPipeRunnable run = testCacheRun();
        InstrumentationJob job = new InstrumentationJobManager(new InstrumentationEventBus(), "test")
                .createJob("update_cache", "Update cache (full)");
        CacheOperations.wireJobToExecutorLifecycle(job, run, executor);

        JIPipeRunnableWorker worker = new JIPipeRunnableWorker(run, true);
        JIPipeRunnableWorker otherWorker = new JIPipeRunnableWorker(testCacheRun(), true);

        assertEquals(InstrumentationJobStatus.PENDING, job.getStatus());
        executor.getFinishedEventEmitter().emit(new JIPipeRunnable.FinishedEvent(otherWorker));
        assertEquals(InstrumentationJobStatus.PENDING, job.getStatus(), "events of unrelated runs must be ignored");

        // The executor dequeues the run: the wiring turns the job RUNNING
        executor.getStartedEventEmitter().emit(new JIPipeRunnable.StartedEvent(run, worker));
        assertEquals(InstrumentationJobStatus.RUNNING, job.getStatus());

        // The run finishes: the wiring completes the job and unsubscribes the listeners
        executor.getFinishedEventEmitter().emit(new JIPipeRunnable.FinishedEvent(worker));
        assertEquals(InstrumentationJobStatus.COMPLETED, job.getStatus());
        executor.getFinishedEventEmitter().emit(new JIPipeRunnable.FinishedEvent(worker));
        assertEquals(InstrumentationJobStatus.COMPLETED, job.getStatus(), "a late duplicate event must not flip the terminal state");
    }

    @Test
    void cacheJobWiringFailsTheJobWithTheRunError() {
        JIPipeQueuedRunnableExecutor executor = new JIPipeQueuedRunnableExecutor("cache-operations-test");
        JIPipeRunnable run = testCacheRun();
        InstrumentationJob job = new InstrumentationJobManager(new InstrumentationEventBus(), "test")
                .createJob("update_cache", "Update cache (full)");
        CacheOperations.wireJobToExecutorLifecycle(job, run, executor);

        JIPipeRunnableWorker worker = new JIPipeRunnableWorker(run, true);
        executor.getStartedEventEmitter().emit(new JIPipeRunnable.StartedEvent(run, worker));
        executor.getInterruptedEventEmitter().emit(
                new JIPipeRunnable.InterruptedEvent(worker, new RuntimeException("the cache run failed")));

        assertEquals(InstrumentationJobStatus.FAILED, job.getStatus());
        assertEquals("the cache run failed", job.getError(), "the job error must carry the run error message");
    }

    @Test
    void cacheJobWiringKeepsACancelledJobCancelledWhenTheRunIsInterrupted() {
        JIPipeQueuedRunnableExecutor executor = new JIPipeQueuedRunnableExecutor("cache-operations-test");
        JIPipeRunnable run = testCacheRun();
        InstrumentationJob job = new InstrumentationJobManager(new InstrumentationEventBus(), "test")
                .createJob("update_cache", "Update cache (full)");
        CacheOperations.wireJobToExecutorLifecycle(job, run, executor);

        // cancel_job marks the job CANCELLED and runs the handler (executor.cancel),
        // which interrupts the run: the late interruption must not overwrite the state
        job.cancel();
        JIPipeRunnableWorker worker = new JIPipeRunnableWorker(run, true);
        executor.getInterruptedEventEmitter().emit(
                new JIPipeRunnable.InterruptedEvent(worker, new RuntimeException("Operation was cancelled.")));

        assertEquals(InstrumentationJobStatus.CANCELLED, job.getStatus(),
                "the interruption of a cancelled cache run must not overwrite the job state");
        assertNull(job.getError());
    }

    @Test
    void missingCompartmentForCompartmentScopeReturnsError() {
        Fixture fixture = createFixture();
        ObjectNode params = updateCacheParams("compartment", "full", null, null);
        JsonNode result = new CacheOperations.UpdateCache(new RecordingTrigger()).execute(createContext(fixture.project()), params);

        assertTrue(result.has("error"), "a missing compartment for scope=compartment must return an error field");
        assertTrue(result.get("error").asText().contains("compartment"), "the error must name the missing parameter");
    }

    @Test
    void missingNodeForNodeScopeReturnsError() {
        Fixture fixture = createFixture();
        ObjectNode params = updateCacheParams("node", "full", "Cache Test Compartment", null);
        JsonNode result = new CacheOperations.UpdateCache(new RecordingTrigger()).execute(createContext(fixture.project()), params);

        assertTrue(result.has("error"), "a missing node for scope=node must return an error field");
        assertTrue(result.get("error").asText().contains("node"), "the error must name the missing parameter");
    }

    @Test
    void missingCompartmentForNodeScopeReturnsError() {
        Fixture fixture = createFixture();
        ObjectNode params = updateCacheParams("node", "full", null,
                fixture.project().getGraph().getAliasIdOf(fixture.node()));
        JsonNode result = new CacheOperations.UpdateCache(new RecordingTrigger()).execute(createContext(fixture.project()), params);

        assertTrue(result.has("error"), "a missing compartment for scope=node must return an error field");
        assertTrue(result.get("error").asText().contains("compartment"), "the error must name the missing parameter");
    }

    @Test
    void unknownNodeForNodeScopeReturnsError() {
        Fixture fixture = createFixture();
        ObjectNode params = updateCacheParams("node", "full", "Cache Test Compartment", "no-such-node-alias");
        JsonNode result = new CacheOperations.UpdateCache(new RecordingTrigger()).execute(createContext(fixture.project()), params);

        assertTrue(result.has("error"), "an unknown node must return an error field");
    }

    @Test
    void nodeOutsideTheAddressedCompartmentReturnsError() {
        JIPipeNodeInfo info = findChainableNodeInfo();
        assumeTrue(info != null, "no chainable node type found in this environment");
        JIPipeProject project = new JIPipeProject();
        var compartment = project.addCompartment("Compartment With The Node");
        var otherCompartment = project.addCompartment("Other Compartment");
        JIPipeGraphNode node = info.newInstance();
        project.getGraph().insertNode(node, compartment.getProjectCompartmentUUID());
        ObjectNode params = updateCacheParams("node", "full", otherCompartment.getName(),
                project.getGraph().getAliasIdOf(node));
        JsonNode result = new CacheOperations.UpdateCache(new RecordingTrigger()).execute(createContext(project), params);

        assertTrue(result.has("error"), "a node outside the addressed compartment must return an error field");
        assertTrue(result.get("error").asText().contains("not in compartment"),
                "the error must name the membership conflict: " + result);
    }

    @Test
    void unknownCompartmentForCompartmentScopeReturnsError() {
        Fixture fixture = createFixture();
        ObjectNode params = updateCacheParams("compartment", "full", "No Such Compartment", null);
        JsonNode result = new CacheOperations.UpdateCache(new RecordingTrigger()).execute(createContext(fixture.project()), params);

        assertTrue(result.has("error"), "an unknown compartment must return an error field");
    }

    @Test
    void getJobStatusReturnsStateOfKnownJob() {
        Fixture fixture = createFixture();
        AutomationContext ctx = createContext(fixture.project());
        InstrumentationJob job = ctx.getJobManager().createJob("run_pipeline", "Run pipeline");
        ObjectNode params = mapper.createObjectNode();
        params.put("job-id", job.getId());
        JsonNode result = new ExecutionOperations.GetJobStatus().execute(ctx, params);

        assertTrue(!result.has("error"), () -> "a known job must not produce an error: " + result);
        assertEquals(job.getId(), result.get("id").asText());
        assertEquals("run_pipeline", result.get("operation").asText());
        assertEquals(InstrumentationJobStatus.PENDING.name(), result.get("state").asText());
        assertTrue(result.has("progress"), "the status must include the progress");
    }

    @Test
    void getJobStatusIncludesErrorSummaryOfFailedJob() {
        Fixture fixture = createFixture();
        AutomationContext ctx = createContext(fixture.project());
        InstrumentationJob job = ctx.getJobManager().createJob("run_pipeline", "Run pipeline");
        job.fail("boom");
        ObjectNode params = mapper.createObjectNode();
        params.put("job-id", job.getId());
        JsonNode result = new ExecutionOperations.GetJobStatus().execute(ctx, params);

        assertEquals(InstrumentationJobStatus.FAILED.name(), result.get("state").asText());
        assertEquals("boom", result.get("error").asText(), "the status must include the error summary");
    }

    @Test
    void getJobStatusReturnsErrorForUnknownJobId() {
        Fixture fixture = createFixture();
        ObjectNode params = mapper.createObjectNode();
        params.put("job-id", "job-nosuch");
        JsonNode result = new ExecutionOperations.GetJobStatus().execute(createContext(fixture.project()), params);

        assertTrue(result.has("error"), "an unknown job id must return an error field");
    }

    @Test
    void cancelJobCancelsPendingJob() {
        Fixture fixture = createFixture();
        AutomationContext ctx = createContext(fixture.project());
        InstrumentationJob job = ctx.getJobManager().createJob("run_pipeline", "Run pipeline");
        ObjectNode params = mapper.createObjectNode();
        params.put("job-id", job.getId());
        JsonNode result = new ExecutionOperations.CancelJob().execute(ctx, params);

        assertTrue(result.get("cancelled").asBoolean(), "a pending job must be cancellable");
        assertEquals(InstrumentationJobStatus.CANCELLED, job.getStatus());
    }

    @Test
    void cancelJobOnCompletedJobReportsCancelledFalse() {
        Fixture fixture = createFixture();
        AutomationContext ctx = createContext(fixture.project());
        InstrumentationJob job = ctx.getJobManager().createJob("run_pipeline", "Run pipeline");
        job.complete(null);
        ObjectNode params = mapper.createObjectNode();
        params.put("job-id", job.getId());
        JsonNode result = new ExecutionOperations.CancelJob().execute(ctx, params);

        assertTrue(!result.get("cancelled").asBoolean(), "a completed job cannot be cancelled");
        assertEquals(InstrumentationJobStatus.COMPLETED, job.getStatus());
    }

    @Test
    void cancelJobReturnsErrorForUnknownJobId() {
        Fixture fixture = createFixture();
        ObjectNode params = mapper.createObjectNode();
        params.put("job-id", "job-nosuch");
        JsonNode result = new ExecutionOperations.CancelJob().execute(createContext(fixture.project()), params);

        assertTrue(result.has("error"), "an unknown job id must return an error field");
    }

    @Test
    void quickRunSettingsMirrorTheDesktopCacheContextActions() {
        Fixture fixture = createFixture();

        // full: the settings block of UpdateCacheNodeUIContextAction.enqueue
        JIPipeDesktopQuickRunSettings full = CacheOperations.quickRunSettings(fixture.project(), "full");
        assertFalse(full.isSaveToDisk(), "the desktop actions never write to disk");
        assertTrue(full.isStoreToCache(), "the desktop actions always store to the cache");
        assertFalse(full.isStoreIntermediateResults(), "full must not store intermediate results");
        assertFalse(full.isExcludeSelected(), "full must run the selected targets");

        // intermediate: the settings block of UpdateCacheShowIntermediateNodeUIContextAction.enqueue
        JIPipeDesktopQuickRunSettings intermediate = CacheOperations.quickRunSettings(fixture.project(), "intermediate");
        assertFalse(intermediate.isSaveToDisk());
        assertTrue(intermediate.isStoreToCache());
        assertTrue(intermediate.isStoreIntermediateResults(), "intermediate must store intermediate results");
        assertFalse(intermediate.isExcludeSelected(), "intermediate must run the selected targets");

        // predecessors: the settings block of UpdateCacheOnlyPredecessorsNodeUIContextAction.enqueue
        JIPipeDesktopQuickRunSettings predecessors = CacheOperations.quickRunSettings(fixture.project(), "predecessors");
        assertFalse(predecessors.isSaveToDisk());
        assertTrue(predecessors.isStoreToCache());
        assertFalse(predecessors.isStoreIntermediateResults(), "predecessors must not store intermediate results");
        assertTrue(predecessors.isExcludeSelected(), "predecessors must exclude the selected targets");
    }

    @Test
    void arrangeNodesOrdersChainByTopologicalDepth() {
        ChainFixture fixture = createChainFixture();
        JIPipeProject project = fixture.project();
        String compartment = project.getCompartments().values().iterator().next().getName();
        ObjectNode params = mapper.createObjectNode();
        params.put("compartment", compartment);
        // The assertion below pins the apply path (the model carries the new
        // locations); arrange_nodes is preview-by-default since the 1383-G guards
        params.put("apply", true);
        JsonNode result = new ArrangeOperations.ArrangeNodes().execute(createContext(project), params);

        assertTrue(!result.has("error"), () -> "a valid compartment must not produce an error: " + result);
        JsonNode positions = result.get("positions");
        assertNotNull(positions, "arrange_nodes must return the new positions");
        assertEquals(3, positions.size(), "every node of the compartment must appear in the positions");
        int xFirst = positionX(positions, project.getGraph().getAliasIdOf(fixture.first()));
        int xSecond = positionX(positions, project.getGraph().getAliasIdOf(fixture.second()));
        int xThird = positionX(positions, project.getGraph().getAliasIdOf(fixture.third()));
        assertTrue(xFirst < xSecond, "the chain source must be left of its successor");
        assertTrue(xSecond < xThird, "the chain middle must be left of its target");
        assertNotEquals(xFirst, xSecond);
        // The graph model must carry the new locations
        assertEquals(xFirst, fixture.first().getNodeUILocationWithin(
                project.getCompartments().keySet().iterator().next().toString()).x);
    }

    private static int positionX(JsonNode positions, String alias) {
        for (JsonNode position : positions) {
            if (alias.equals(position.get("node").asText())) {
                return position.get("x").asInt();
            }
        }
        throw new AssertionError("no position returned for node " + alias);
    }

    @Test
    void arrangeNodesReturnsErrorForUnknownCompartment() {
        Fixture fixture = createFixture();
        ObjectNode params = mapper.createObjectNode();
        params.put("compartment", "No Such Compartment");
        JsonNode result = new ArrangeOperations.ArrangeNodes().execute(createContext(fixture.project()), params);

        assertTrue(result.has("error"), "an unknown compartment must return an error field");
    }

    @Test
    void operationsAreRegistered() {
        AutomationOperationRegistry registry = JIPipe.getInstance().getAutomation().getOperationRegistry();
        assertTrue(registry.has("update_cache"), "update_cache must be registered");
        assertTrue(registry.has("get_job_status"), "get_job_status must be registered");
        assertTrue(registry.has("cancel_job"), "cancel_job must be registered");
        assertTrue(registry.has("arrange_nodes"), "arrange_nodes must be registered");
    }
}
