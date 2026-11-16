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
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

/**
 * Tests the cache, job, and arrange operations: update_cache schema and conditional
 * requirements, the injectable cache trigger seam, get_job_status and cancel_job against
 * the real job manager, and the layered layout of arrange_nodes.
 */
public class CacheOperationsTest {

    private static final ObjectMapper mapper = new ObjectMapper();

    private record Fixture(JIPipeProject project, JIPipeGraphNode node) {
    }

    /**
     * Records the delegated cache triggers so tests can assert the operation resolves
     * targets and modes without invoking the real cache services.
     */
    private static class RecordingTrigger implements CacheOperations.CacheTrigger {
        final List<JIPipeProject> projects = new ArrayList<>();
        final List<List<JIPipeGraphNode>> targets = new ArrayList<>();
        final List<String> modes = new ArrayList<>();
        final CountDownLatch invoked = new CountDownLatch(1);

        @Override
        public void run(JIPipeProject project, List<JIPipeGraphNode> targetNodes, String mode) {
            projects.add(project);
            targets.add(targetNodes);
            modes.add(mode);
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
        assertTrue(result.has("jobId") && !result.get("jobId").asText().isBlank(),
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
    void arrangeNodesOrdersChainByTopologicalDepth() {
        ChainFixture fixture = createChainFixture();
        JIPipeProject project = fixture.project();
        String compartment = project.getCompartments().values().iterator().next().getName();
        ObjectNode params = mapper.createObjectNode();
        params.put("compartment", compartment);
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
