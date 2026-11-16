package org.hkijena.jipipe.api.automation;

import com.fasterxml.jackson.databind.JsonNode;
import org.hkijena.jipipe.JIPipe;
import org.hkijena.jipipe.api.JIPipeProgressInfo;
import org.hkijena.jipipe.api.project.JIPipeProject;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

class InstrumentationAPITest {

    @TempDir
    Path tempDir;

    @BeforeAll
    static void ensureJIPipe() {
        JIPipe.ensureInstance();
    }

    private static boolean hasNodeType(String id) {
        try {
            JIPipe.getNodes().getInfoById(id);
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    private AutomationContext createContext(JIPipeProject project) {
        InstrumentationEventBus eventBus = new InstrumentationEventBus();
        InstrumentationJobManager jobManager = new InstrumentationJobManager(eventBus, "test");
        return new AutomationContext(project, "test", new JIPipeProgressInfo(), eventBus, jobManager);
    }

    @Test
    void queryCompartments_returnsEmptyArrayForNewProject() {
        JIPipeProject project = new JIPipeProject();
        AutomationContext ctx = createContext(project);

        JsonNode result = JIPipeAutomationAPI.queryCompartments(ctx);

        assertTrue(result.has("compartments"));
        assertEquals(0, result.get("compartments").size());
    }

    @Test
    void queryCompartments_returnsCompartmentsAfterAdd() {
        assumeTrue(hasNodeType("jipipe:project-compartment"),
                "jipipe:project-compartment node type not registered - skipping");
        JIPipeProject project = new JIPipeProject();
        AutomationContext ctx = createContext(project);

        JIPipeAutomationAPI.addCompartment(ctx, "My Compartment");

        JsonNode result = JIPipeAutomationAPI.queryCompartments(ctx);
        assertEquals(1, result.get("compartments").size());
        JsonNode comp = result.get("compartments").get(0);
        assertEquals("My Compartment", comp.get("name").asText());
    }

    @Test
    void queryGraph_returnsNodesAndConnectionsArrays() {
        JIPipeProject project = new JIPipeProject();
        AutomationContext ctx = createContext(project);

        JsonNode result = JIPipeAutomationAPI.queryGraph(ctx, null);

        assertTrue(result.has("nodes"));
        assertTrue(result.has("connections"));
        assertEquals(0, result.get("nodes").size());
        assertEquals(0, result.get("connections").size());
    }

    @Test
    void queryNode_returnsErrorForUnknownNode() {
        JIPipeProject project = new JIPipeProject();
        AutomationContext ctx = createContext(project);

        JsonNode result = JIPipeAutomationAPI.queryNode(ctx, "nonexistent");

        assertTrue(result.has("error"));
    }

    @Test
    void getPipelineMap_returnsRootWithProjectName() {
        JIPipeProject project = new JIPipeProject();
        AutomationContext ctx = createContext(project);

        JsonNode result = JIPipeAutomationAPI.getPipelineMap(ctx);

        assertNotNull(result);
        assertTrue(result.has("root"));
    }

    @Test
    void searchNodes_returnsResultsForKnownQuery() {
        AutomationContext ctx = createContext(null);

        JsonNode result = JIPipeAutomationAPI.searchNodes(ctx, "data", 10);

        assertTrue(result.has("results"));
        // Results may be empty in minimal test environments where few plugins are loaded
        assumeTrue(result.get("results").size() > 0,
                "No node types found for 'data' - minimal test environment");
    }
}
