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
import org.hkijena.jipipe.api.automation.InstrumentationJobManager;
import org.hkijena.jipipe.api.nodes.JIPipeGraphNode;
import org.hkijena.jipipe.api.nodes.JIPipeNodeInfo;
import org.hkijena.jipipe.api.parameters.JIPipeParameterTree;
import org.hkijena.jipipe.api.project.JIPipeProject;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

/**
 * Tests the info operations: get_project_info, list_algorithms, and get_node_docs.
 */
public class InfoOperationsTest {

    private static final ObjectMapper mapper = new ObjectMapper();

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
     * Returns the ID of any registered node type that has visible parameters, or null.
     */
    private static String findNodeTypeIdWithParameters() {
        for (JIPipeNodeInfo info : JIPipe.getNodes().getRegisteredNodeInfos().values()) {
            try {
                JIPipeGraphNode node = info.newInstance();
                boolean hasParameter = JIPipeParameterTree.getParameters(node).keySet().stream()
                        .anyMatch(key -> !key.startsWith("jipipe:") && !key.startsWith("jipipe-ui:"));
                if (hasParameter) {
                    return info.getId();
                }
            } catch (Exception ignored) {
                // Uninstantiable node types are skipped
            }
        }
        return null;
    }

    /**
     * Returns the ID of any registered node type, or null.
     */
    private static String findAnyNodeTypeId() {
        for (JIPipeNodeInfo info : JIPipe.getNodes().getRegisteredNodeInfos().values()) {
            return info.getId();
        }
        return null;
    }

    @Test
    void getNodeDocsReturnsNameAndParametersForNodeWithParameters() {
        String typeId = findNodeTypeIdWithParameters();
        assumeTrue(typeId != null, "no node type with parameters found in this environment");

        ObjectNode params = mapper.createObjectNode();
        params.put("node-id", typeId);
        JsonNode result = new InfoOperations.GetNodeDocs().execute(createContext(null), params);

        assertFalse(result.has("error"), "a registered node id must not produce an error");
        assertNotNull(result.get("name").asText(), "name must be present");
        assertFalse(result.get("name").asText().isEmpty(), "name must be non-empty");
        assertTrue(result.has("parameters"), "parameters array must be present");
        assertTrue(result.get("parameters").isArray());
        assertTrue(result.get("parameters").size() > 0, "a node with parameters must list them");
        JsonNode first = result.get("parameters").get(0);
        assertFalse(first.get("key").asText().isEmpty(), "parameter entries must have a key");
        assertNotNull(first.get("type").asText(), "parameter entries must have a type");
        assertFalse(result.get("description").asText().contains("<"), "description must be plain text");
    }

    @Test
    void getNodeDocsReturnsErrorForUnknownId() {
        ObjectNode params = mapper.createObjectNode();
        params.put("node-id", "no-such-node-type-exists");
        JsonNode result = new InfoOperations.GetNodeDocs().execute(createContext(null), params);

        assertTrue(result.has("error"), "an unknown node id must return an error field");
    }

    @Test
    void listAlgorithmsWithQueryReturnsKnownNodeAndNonEmptyIds() {
        String typeId = findAnyNodeTypeId();
        assumeTrue(typeId != null, "no node types registered in this environment");
        JIPipeNodeInfo info = JIPipe.getNodes().getInfoById(typeId);
        String name = info.getName() != null ? info.getName() : typeId;
        String query = name.substring(0, Math.min(4, name.length())).toLowerCase();

        ObjectNode params = mapper.createObjectNode();
        params.put("query", query);
        JsonNode result = new InfoOperations.ListAlgorithms().execute(createContext(null), params);

        assertTrue(result.has("algorithms"), "result must wrap the catalog in an algorithms array");
        assertTrue(result.get("algorithms").isArray());
        assertTrue(result.has("truncated"), "result must carry the truncation marker");
        boolean found = false;
        for (JsonNode entry : result.get("algorithms")) {
            assertFalse(entry.get("id").asText().isEmpty(), "every entry must have a non-empty id");
            if (entry.get("id").asText().equals(typeId)) {
                found = true;
            }
        }
        assertTrue(found, "the query '" + query + "' must match the node named '" + name + "'");
    }

    @Test
    void listAlgorithmsWithoutQueryReturnsEntries() {
        String typeId = findAnyNodeTypeId();
        assumeTrue(typeId != null, "no node types registered in this environment");

        JsonNode result = new InfoOperations.ListAlgorithms().execute(createContext(null),
                mapper.createObjectNode());

        assertTrue(result.has("algorithms"));
        assertTrue(result.get("algorithms").size() > 0, "an unfiltered catalog must return entries");
        for (JsonNode entry : result.get("algorithms")) {
            assertFalse(entry.get("id").asText().isEmpty());
        }
    }

    @Test
    void getProjectInfoReturnsErrorForNullProject() {
        JsonNode result = new InfoOperations.GetProjectInfo().execute(createContext(null),
                mapper.createObjectNode());

        assertTrue(result.has("error"), "a null-project context must return an error field");
    }

    @Test
    void getProjectInfoReturnsCompartmentsAndCountsForMinimalProject() {
        assumeTrue(JIPipe.getNodes().getInfoById("jipipe:project-compartment") != null,
                "jipipe:project-compartment node type not registered - skipping");
        JIPipeProject project = new JIPipeProject();
        project.addCompartment("Info Test Compartment");

        JsonNode result = new InfoOperations.GetProjectInfo().execute(createContext(project),
                mapper.createObjectNode());

        assertFalse(result.has("error"));
        assertTrue(result.has("compartments"));
        assertEquals(1, result.get("compartments").size());
        assertEquals("Info Test Compartment", result.get("compartments").get(0).get("name").asText());
        assertTrue(result.get("compartments").get(0).get("nodeCount").isInt());
        assertTrue(result.has("nodeCount"));
        assertTrue(result.get("nodeCount").isInt());
        assertTrue(result.has("metadata"), "metadata summary must be present");
        assertTrue(result.has("cache"), "cache summary must be present");
    }

    @Test
    void infoOperationsAreRegistered() {
        AutomationOperationRegistry registry = JIPipe.getInstance().getAutomation().getOperationRegistry();
        assertTrue(registry.has("get_project_info"), "get_project_info must be registered");
        assertTrue(registry.has("list_algorithms"), "list_algorithms must be registered");
        assertTrue(registry.has("get_node_docs"), "get_node_docs must be registered");
    }
}
