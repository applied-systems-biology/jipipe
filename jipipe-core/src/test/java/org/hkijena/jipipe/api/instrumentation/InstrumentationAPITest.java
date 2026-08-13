package org.hkijena.jipipe.api.instrumentation;

import com.fasterxml.jackson.databind.JsonNode;
import org.hkijena.jipipe.JIPipe;
import org.hkijena.jipipe.api.JIPipeProgressInfo;
import org.hkijena.jipipe.api.project.JIPipeProject;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

class InstrumentationAPITest {

    @TempDir
    Path tempDir;

    @BeforeAll
    static void ensureJIPipe() {
        JIPipe.ensureInstance();
    }

    private InstrumentationContext createContext(JIPipeProject project) {
        InstrumentationEventBus eventBus = new InstrumentationEventBus();
        InstrumentationJobManager jobManager = new InstrumentationJobManager(eventBus, "test");
        return new InstrumentationContext(project, "test", new JIPipeProgressInfo(), eventBus, jobManager);
    }

    @Test
    void queryCompartments_returnsEmptyArrayForNewProject() {
        JIPipeProject project = new JIPipeProject();
        InstrumentationContext ctx = createContext(project);

        JsonNode result = InstrumentationAPI.queryCompartments(ctx);

        assertTrue(result.has("compartments"));
        assertEquals(0, result.get("compartments").size());
    }

    @Test
    void queryCompartments_returnsCompartmentsAfterAdd() {
        JIPipeProject project = new JIPipeProject();
        InstrumentationContext ctx = createContext(project);

        InstrumentationAPI.addCompartment(ctx, "My Compartment");

        JsonNode result = InstrumentationAPI.queryCompartments(ctx);
        assertEquals(1, result.get("compartments").size());
        JsonNode comp = result.get("compartments").get(0);
        assertEquals("My Compartment", comp.get("name").asText());
    }

    @Test
    void queryGraph_returnsNodesAndConnectionsArrays() {
        JIPipeProject project = new JIPipeProject();
        InstrumentationContext ctx = createContext(project);

        JsonNode result = InstrumentationAPI.queryGraph(ctx, null);

        assertTrue(result.has("nodes"));
        assertTrue(result.has("connections"));
        assertEquals(0, result.get("nodes").size());
        assertEquals(0, result.get("connections").size());
    }

    @Test
    void queryNode_returnsErrorForUnknownNode() {
        JIPipeProject project = new JIPipeProject();
        InstrumentationContext ctx = createContext(project);

        JsonNode result = InstrumentationAPI.queryNode(ctx, "nonexistent");

        assertTrue(result.has("error"));
    }

    @Test
    void getPipelineMap_returnsRootWithProjectName() {
        JIPipeProject project = new JIPipeProject();
        InstrumentationContext ctx = createContext(project);

        JsonNode result = InstrumentationAPI.getPipelineMap(ctx);

        assertNotNull(result);
        assertTrue(result.has("root"));
    }

    @Test
    void searchNodes_returnsResultsForKnownQuery() {
        InstrumentationContext ctx = createContext(null);

        JsonNode result = InstrumentationAPI.searchNodes(ctx, "data", 10);

        assertTrue(result.has("results"));
        assertTrue(result.get("results").size() > 0);
    }
}
