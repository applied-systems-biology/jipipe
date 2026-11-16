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
import org.hkijena.jipipe.api.automation.InstrumentationEventBus;
import org.hkijena.jipipe.api.automation.InstrumentationJobManager;
import org.hkijena.jipipe.api.automation.AutomationOperationRegistry;
import org.hkijena.jipipe.api.nodes.JIPipeGraphNode;
import org.hkijena.jipipe.api.nodes.JIPipeNodeInfo;
import org.hkijena.jipipe.api.project.JIPipeProject;
import org.hkijena.jipipe.plugins.settings.application.JIPipeRuntimeApplicationSettings;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

/**
 * Tests the diagnostics operations: get_errors (project validation + last-run errors) and
 * get_log (progress log of the current run).
 */
public class DiagnosticsOperationsTest {

    private static final ObjectMapper mapper = new ObjectMapper();

    /**
     * Minimal fixture: a project with one compartment and one node with a required input slot,
     * plus the progress handle whose store serves get_log.
     */
    private record Fixture(JIPipeProject project, JIPipeGraphNode node, JIPipeProgressInfo progressInfo) {
    }

    @BeforeAll
    static void ensureJIPipe() {
        JIPipe.ensureInstance();
    }

    private static AutomationContext createContext(Fixture fixture) {
        InstrumentationEventBus eventBus = new InstrumentationEventBus();
        InstrumentationJobManager jobManager = new InstrumentationJobManager(eventBus, "test");
        return new AutomationContext(fixture.project(), "test", fixture.progressInfo(), eventBus, jobManager);
    }

    /**
     * Returns the info of any registered, runnable node type that has a required (non-optional)
     * input slot, or null. Required inputs let the test produce the graph-level
     * "input slot has no incoming data" validation error.
     */
    private static JIPipeNodeInfo findInstantiableNodeInfoWithRequiredInput() {
        for (JIPipeNodeInfo info : JIPipe.getNodes().getRegisteredNodeInfos().values()) {
            try {
                if (!info.isRunnable()) {
                    continue;
                }
                JIPipeGraphNode instance = info.newInstance();
                boolean hasRequiredInput = instance.getInputSlots().stream()
                        .anyMatch(slot -> !slot.getInfo().isOptional());
                if (hasRequiredInput) {
                    return info;
                }
            } catch (Exception ignored) {
                // Uninstantiable node types are skipped
            }
        }
        return null;
    }

    private static Fixture createFixture() {
        JIPipeNodeInfo info = findInstantiableNodeInfoWithRequiredInput();
        assumeTrue(info != null, "no instantiable node type with a required input found in this environment");
        JIPipeProject project = new JIPipeProject();
        var compartment = project.addCompartment("Diagnostics Test Compartment");
        JIPipeGraphNode node = info.newInstance();
        project.getGraph().insertNode(node, compartment.getProjectCompartmentUUID());
        return new Fixture(project, node, new JIPipeProgressInfo());
    }

    private static List<String> messages(JsonNode result) {
        List<String> messages = new ArrayList<>();
        for (JsonNode record : result.get("records")) {
            messages.add(record.get("message").asText());
        }
        return messages;
    }

    @Test
    void getLogWithInfoLevelExcludesDebugRecords() {
        Fixture fixture = createFixture();
        fixture.progressInfo().log("info one");
        fixture.progressInfo().debug("debug one");
        fixture.progressInfo().log("info two");
        fixture.progressInfo().debug("debug two");
        fixture.progressInfo().log("info three");
        ObjectNode params = mapper.createObjectNode();
        params.put("level", "INFO");
        JsonNode result = new DiagnosticsOperations.GetLog().execute(createContext(fixture), params);

        assertFalse(result.has("error"), () -> "get_log must not fail: " + result);
        assertEquals(List.of("info one", "info two", "info three"), messages(result),
                "level=INFO must return only INFO records, newest-last");
        for (JsonNode record : result.get("records")) {
            assertFalse("DEBUG".equals(record.get("level").asText()), "DEBUG records must be excluded");
        }
    }

    @Test
    void getLogWithDebugLevelIncludesDebugAndInfoRecords() {
        Fixture fixture = createFixture();
        fixture.progressInfo().log("info one");
        fixture.progressInfo().debug("debug one");
        fixture.progressInfo().log("info two");
        ObjectNode params = mapper.createObjectNode();
        params.put("level", "DEBUG");
        JsonNode result = new DiagnosticsOperations.GetLog().execute(createContext(fixture), params);

        assertFalse(result.has("error"), () -> "get_log must not fail: " + result);
        assertEquals(List.of("info one", "debug one", "info two"), messages(result),
                "level=DEBUG must return DEBUG and INFO records, newest-last");
    }

    @Test
    void getLogTailReturnsLastTwoMatchingRecords() {
        Fixture fixture = createFixture();
        fixture.progressInfo().log("info one");
        fixture.progressInfo().debug("debug one");
        fixture.progressInfo().log("info two");
        fixture.progressInfo().debug("debug two");
        fixture.progressInfo().log("info three");
        ObjectNode params = mapper.createObjectNode();
        params.put("level", "INFO");
        params.put("tail", 2);
        JsonNode result = new DiagnosticsOperations.GetLog().execute(createContext(fixture), params);

        assertFalse(result.has("error"), () -> "get_log must not fail: " + result);
        assertEquals(List.of("info two", "info three"), messages(result),
                "tail=2 must return exactly the last two matching records, newest-last");
    }

    @Test
    void getLogDefaultsReturnAllInfoRecords() {
        Fixture fixture = createFixture();
        for (int i = 0; i < 5; i++) {
            fixture.progressInfo().log("info " + i);
            fixture.progressInfo().debug("debug " + i);
        }
        JsonNode result = new DiagnosticsOperations.GetLog().execute(createContext(fixture),
                mapper.createObjectNode());

        assertFalse(result.has("error"), () -> "get_log must not fail: " + result);
        assertEquals(5, result.get("records").size(), "the default level INFO must hide the DEBUG records");
        assertEquals(5, result.get("count").asInt());
    }

    @Test
    void getLogFiltersRecordsByNodeReference() {
        Fixture fixture = createFixture();
        JIPipeProgressInfo nodeProgress = fixture.progressInfo().resolve("Fixture node",
                "node://" + fixture.node().getUUIDInParentGraph());
        fixture.progressInfo().log("root message");
        nodeProgress.log("node message");
        ObjectNode params = mapper.createObjectNode();
        params.put("node", fixture.project().getGraph().getAliasIdOf(fixture.node()));
        JsonNode result = new DiagnosticsOperations.GetLog().execute(createContext(fixture), params);

        assertFalse(result.has("error"), () -> "get_log must not fail: " + result);
        assertEquals(List.of("node message"), messages(result),
                "the node filter must keep only records attributed to the node");
        assertEquals(fixture.node().getUUIDInParentGraph().toString(),
                result.get("records").get(0).get("node").asText());
    }

    @Test
    void getLogReturnsErrorForUnknownNode() {
        Fixture fixture = createFixture();
        ObjectNode params = mapper.createObjectNode();
        params.put("node", "no-such-node");
        JsonNode result = new DiagnosticsOperations.GetLog().execute(createContext(fixture), params);

        assertTrue(result.has("error"), "an unknown node must return an error field");
    }

    @Test
    void getLogReturnsEmptyListWithoutRecords() {
        Fixture fixture = createFixture();
        JsonNode result = new DiagnosticsOperations.GetLog().execute(createContext(fixture),
                mapper.createObjectNode());

        assertFalse(result.has("error"), () -> "get_log must not fail: " + result);
        assertEquals(0, result.get("records").size(), "a store without records must return an empty list");
        assertEquals(0, result.get("count").asInt());
    }

    @Test
    void getErrorsReportsInvalidGraphWithNodeReference() {
        Fixture fixture = createFixture();
        boolean old = JIPipeRuntimeApplicationSettings.getInstance().isAllowSkipAlgorithmsWithoutInput();
        JIPipeRuntimeApplicationSettings.getInstance().setAllowSkipAlgorithmsWithoutInput(false);
        try {
            JsonNode result = new DiagnosticsOperations.GetErrors().execute(createContext(fixture),
                    mapper.createObjectNode());

            assertFalse(result.has("error"), () -> "get_errors must not fail: " + result);
            assertTrue(result.get("errors").size() >= 1, "an invalid graph must produce at least one error");
            boolean foundErrorWithNode = false;
            for (JsonNode entry : result.get("errors")) {
                if ("Error".equals(entry.get("level").asText()) && entry.has("node")) {
                    foundErrorWithNode = true;
                }
            }
            assertTrue(foundErrorWithNode, "at least one error entry must reference a node");
        } finally {
            JIPipeRuntimeApplicationSettings.getInstance().setAllowSkipAlgorithmsWithoutInput(old);
        }
    }

    @Test
    void getErrorsFiltersByCompartment() {
        Fixture fixture = createFixture();
        boolean old = JIPipeRuntimeApplicationSettings.getInstance().isAllowSkipAlgorithmsWithoutInput();
        JIPipeRuntimeApplicationSettings.getInstance().setAllowSkipAlgorithmsWithoutInput(false);
        try {
            ObjectNode params = mapper.createObjectNode();
            params.put("compartment", "Diagnostics Test Compartment");
            JsonNode result = new DiagnosticsOperations.GetErrors().execute(createContext(fixture), params);

            assertFalse(result.has("error"), () -> "get_errors must not fail: " + result);
            assertTrue(result.get("errors").size() >= 1, "the compartment filter must keep the node's errors");
            String compartmentUuid = fixture.project().getGraph().getCompartmentUUIDOf(fixture.node()).toString();
            for (JsonNode entry : result.get("errors")) {
                assertEquals(compartmentUuid, entry.get("compartment").asText(),
                        "filtered entries must belong to the requested compartment");
            }
        } finally {
            JIPipeRuntimeApplicationSettings.getInstance().setAllowSkipAlgorithmsWithoutInput(old);
        }
    }

    @Test
    void getErrorsReturnsErrorForUnknownCompartment() {
        Fixture fixture = createFixture();
        ObjectNode params = mapper.createObjectNode();
        params.put("compartment", "No Such Compartment");
        JsonNode result = new DiagnosticsOperations.GetErrors().execute(createContext(fixture), params);

        assertTrue(result.has("error"), "an unknown compartment must return an error field");
    }

    @Test
    void getErrorsReturnsEmptyListForValidProject() {
        Fixture fixture = createFixture();
        fixture.project().getGraph().removeNode(fixture.node(), true);
        JsonNode result = new DiagnosticsOperations.GetErrors().execute(createContext(fixture),
                mapper.createObjectNode());

        assertFalse(result.has("error"), () -> "get_errors must not fail: " + result);
        assertEquals(0, result.get("errors").size(), "a project without nodes must have no errors");
        assertEquals(0, result.get("count").asInt());
    }

    @Test
    void getErrorsIncludesLastErrorRecordsOfTheRun() {
        Fixture fixture = createFixture();
        JIPipeProgressInfo nodeProgress = fixture.progressInfo().resolve("Fixture node",
                "node://" + fixture.node().getUUIDInParentGraph());
        nodeProgress.log(new RuntimeException("boom"));
        JsonNode result = new DiagnosticsOperations.GetErrors().execute(createContext(fixture),
                mapper.createObjectNode());

        assertFalse(result.has("error"), () -> "get_errors must not fail: " + result);
        boolean foundRunError = false;
        for (JsonNode entry : result.get("errors")) {
            if ("run".equals(entry.path("source").asText())
                    && entry.path("message").asText().contains("boom")
                    && fixture.node().getUUIDInParentGraph().toString().equals(entry.path("node").asText())) {
                foundRunError = true;
            }
        }
        assertTrue(foundRunError, "the error record of the run must appear with its node reference");
    }

    @Test
    void diagnosticsOperationsAreRegistered() {
        AutomationOperationRegistry registry = JIPipe.getInstance().getAutomation().getOperationRegistry();
        assertTrue(registry.has("get_errors"), "get_errors must be registered");
        assertTrue(registry.has("get_log"), "get_log must be registered");
    }
}
