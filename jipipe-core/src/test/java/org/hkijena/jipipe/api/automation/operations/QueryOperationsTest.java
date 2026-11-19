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
import org.hkijena.jipipe.JIPipeMutableDependency;
import org.hkijena.jipipe.api.JIPipeProgressInfo;
import org.hkijena.jipipe.api.automation.AutomationContext;
import org.hkijena.jipipe.api.automation.InstrumentationEventBus;
import org.hkijena.jipipe.api.automation.InstrumentationJobManager;
import org.hkijena.jipipe.api.compartments.algorithms.JIPipeProjectCompartment;
import org.hkijena.jipipe.api.nodes.JIPipeGraphNode;
import org.hkijena.jipipe.api.nodes.infos.JIPipeJavaNodeInfo;
import org.hkijena.jipipe.api.project.JIPipeProject;
import org.hkijena.jipipe.plugins.core.nodes.JIPipeCommentNode;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

/**
 * Tests the query_graph operation: compartment filtering by name, the fail-loud
 * error envelope for unresolvable compartment filters (including UUID-shaped
 * filters without name fallback), stable resolution of duplicate compartment
 * names, and the per-node compartmentId attribution.
 */
public class QueryOperationsTest {

    private static final String COMMENT_NODE_ID = "jipipe:comment";
    private static final JIPipeMutableDependency TEST_DEPENDENCY =
            new JIPipeMutableDependency("query-operations-test", "1.0.0", "query-operations-test");

    private static final ObjectMapper mapper = new ObjectMapper();

    private static boolean registeredCommentNodeHere;

    @BeforeAll
    static void ensureJIPipe() {
        JIPipe.ensureInstance();
        // The jipipe-core test JVM loads only the core plugins: the id
        // jipipe:comment is registered by the UtilitiesPlugin of the
        // jipipe-plugin-utils module, which is not on this module's test
        // classpath. Register the node (the class itself lives in jipipe-core)
        // when absent so the behavioral tests below run instead of skipping,
        // following the same approach as the UI controller tests. Where the
        // UtilitiesPlugin is loaded, its registration is kept.
        if (!JIPipe.getNodes().hasNodeInfoWithId(COMMENT_NODE_ID)) {
            JIPipe.getNodes().register(new JIPipeJavaNodeInfo(COMMENT_NODE_ID, JIPipeCommentNode.class), TEST_DEPENDENCY);
            registeredCommentNodeHere = true;
        }
    }

    @AfterAll
    static void cleanup() {
        if (registeredCommentNodeHere) {
            JIPipe.getNodes().unregister(COMMENT_NODE_ID);
            registeredCommentNodeHere = false;
        }
    }

    private static boolean hasNodeType(String id) {
        try {
            JIPipe.getNodes().getInfoById(id);
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    private static AutomationContext createContext(JIPipeProject project) {
        InstrumentationEventBus eventBus = new InstrumentationEventBus();
        InstrumentationJobManager jobManager = new InstrumentationJobManager(eventBus, "test");
        return new AutomationContext(project, "test", new JIPipeProgressInfo(), eventBus, jobManager);
    }

    /**
     * A project with two named compartments, each containing one comment node.
     */
    private record TwoCompartmentFixture(JIPipeProject project, JIPipeProjectCompartment first,
                                         JIPipeProjectCompartment second, JIPipeGraphNode firstNode,
                                         JIPipeGraphNode secondNode) {
    }

    private static TwoCompartmentFixture createTwoCompartmentFixture(String firstName, String secondName) {
        var info = JIPipe.getNodes().getInfoById(COMMENT_NODE_ID);
        JIPipeProject project = new JIPipeProject();
        JIPipeProjectCompartment first = project.addCompartment(firstName);
        JIPipeProjectCompartment second = project.addCompartment(secondName);
        JIPipeGraphNode firstNode = info.newInstance();
        project.getGraph().insertNode(firstNode, first.getProjectCompartmentUUID());
        JIPipeGraphNode secondNode = info.newInstance();
        project.getGraph().insertNode(secondNode, second.getProjectCompartmentUUID());
        return new TwoCompartmentFixture(project, first, second, firstNode, secondNode);
    }

    private static ObjectNode compartmentParams(String ref) {
        ObjectNode params = mapper.createObjectNode();
        params.put("compartment-id", ref);
        return params;
    }

    @Test
    void smoke_commentNodeTypeIsRegistered() {
        assertTrue(hasNodeType(COMMENT_NODE_ID),
                "jipipe:comment is not registered: the queryGraph behavioral tests would silently skip");
    }

    @Test
    void queryGraph_filterByNameReturnsOnlyThatCompartment() {
        assumeTrue(hasNodeType(COMMENT_NODE_ID), COMMENT_NODE_ID + " node type not registered - skipping");
        TwoCompartmentFixture fixture = createTwoCompartmentFixture("Preprocessing", "Analysis");
        AutomationContext ctx = createContext(fixture.project());

        JsonNode result = new QueryOperations.QueryGraph().execute(ctx, compartmentParams("Preprocessing"));

        assertEquals(1, result.get("nodes").size());
        assertEquals(fixture.project().getGraph().getUUIDOf(fixture.firstNode()).toString(),
                result.get("nodes").get(0).get("uuid").asText());
        assertEquals(fixture.first().getProjectCompartmentUUID().toString(), result.get("compartmentId").asText());
    }

    @Test
    void queryGraph_unknownFilterErrorsWithKnownCompartments() {
        assumeTrue(hasNodeType(COMMENT_NODE_ID), COMMENT_NODE_ID + " node type not registered - skipping");
        TwoCompartmentFixture fixture = createTwoCompartmentFixture("Preprocessing", "Analysis");
        AutomationContext ctx = createContext(fixture.project());

        JsonNode result = new QueryOperations.QueryGraph().execute(ctx, compartmentParams("Nope"));

        assertTrue(result.has("error"));
        String error = result.get("error").asText();
        assertTrue(error.contains("Compartment not found: 'Nope'"), error);
        assertTrue(error.contains("Preprocessing") && error.contains("Analysis"), error);
        assertTrue(error.endsWith("(see query_compartments for ids)"), error);
    }

    @Test
    void queryGraph_unknownUuidFilterDoesNotFallBackToName() {
        assumeTrue(hasNodeType(COMMENT_NODE_ID), COMMENT_NODE_ID + " node type not registered - skipping");
        TwoCompartmentFixture fixture = createTwoCompartmentFixture("Preprocessing", "Analysis");
        AutomationContext ctx = createContext(fixture.project());

        JsonNode result = new QueryOperations.QueryGraph().execute(ctx,
                compartmentParams(UUID.randomUUID().toString()));

        assertTrue(result.has("error"),
                "a well-formed UUID that matches no compartment must error, not return nodes: " + result);
    }

    @Test
    void queryGraph_duplicateNamesResolveStably() {
        assumeTrue(hasNodeType(COMMENT_NODE_ID), COMMENT_NODE_ID + " node type not registered - skipping");
        TwoCompartmentFixture fixture = createTwoCompartmentFixture("Dup", "Dup");
        AutomationContext ctx = createContext(fixture.project());
        Set<String> allowed = Set.of(fixture.first().getProjectCompartmentUUID().toString(),
                fixture.second().getProjectCompartmentUUID().toString());
        Set<String> resolved = new LinkedHashSet<>();

        for (int i = 0; i < 3; i++) {
            JsonNode result = new QueryOperations.QueryGraph().execute(ctx, compartmentParams("Dup"));
            assertEquals(1, result.get("nodes").size());
            resolved.add(result.get("compartmentId").asText());
        }

        assertEquals(1, resolved.size(), "duplicate names must resolve to the same compartment across calls");
        assertTrue(allowed.contains(resolved.iterator().next()));
    }

    @Test
    void queryGraph_nodesCarryCompartmentId() {
        assumeTrue(hasNodeType(COMMENT_NODE_ID), COMMENT_NODE_ID + " node type not registered - skipping");
        TwoCompartmentFixture fixture = createTwoCompartmentFixture("Preprocessing", "Analysis");
        AutomationContext ctx = createContext(fixture.project());
        Map<String, String> expected = new LinkedHashMap<>();
        expected.put(fixture.project().getGraph().getUUIDOf(fixture.firstNode()).toString(),
                fixture.first().getProjectCompartmentUUID().toString());
        expected.put(fixture.project().getGraph().getUUIDOf(fixture.secondNode()).toString(),
                fixture.second().getProjectCompartmentUUID().toString());

        JsonNode result = new QueryOperations.QueryGraph().execute(ctx, mapper.createObjectNode());

        assertEquals(2, result.get("nodes").size());
        for (JsonNode node : result.get("nodes")) {
            String uuid = node.get("uuid").asText();
            assertTrue(expected.containsKey(uuid), "unexpected node " + uuid);
            assertTrue(node.has("compartmentId"), "node " + uuid + " carries no compartmentId");
            assertEquals(expected.get(uuid), node.get("compartmentId").asText(), "node " + uuid);
        }
    }
}
