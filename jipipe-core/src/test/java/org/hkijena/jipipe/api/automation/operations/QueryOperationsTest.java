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
import com.fasterxml.jackson.databind.node.ArrayNode;
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
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

/**
 * Tests the query_graph operation: compartment filtering by name, the fail-loud
 * error envelope for unresolvable compartment filters (including UUID-shaped
 * filters without name fallback), stable resolution of duplicate compartment
 * names, the per-node compartmentId attribution, and the response shaping
 * parameters (fields whitelist, omit-positions, connections-only).
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

    private static ObjectNode shapingParams(List<String> fields, boolean omitPositions, boolean connectionsOnly) {
        ObjectNode params = mapper.createObjectNode();
        if (fields != null) {
            ArrayNode array = params.putArray("fields");
            for (String field : fields) {
                array.add(field);
            }
        }
        params.put("omit-positions", omitPositions);
        params.put("connections-only", connectionsOnly);
        return params;
    }

    private static Set<String> fieldNames(JsonNode node) {
        Set<String> names = new LinkedHashSet<>();
        node.fieldNames().forEachRemaining(names::add);
        return names;
    }

    /**
     * A two-compartment fixture whose first node is connected to the second
     * node across the compartment boundary.
     */
    private static TwoCompartmentFixture createCrossCompartmentConnectedFixture(String firstName, String secondName) {
        TwoCompartmentFixture fixture = createTwoCompartmentFixture(firstName, secondName);
        fixture.project().getGraph().connect(fixture.firstNode().getOutputSlot("Comment"),
                fixture.secondNode().getInputSlot("Comment"));
        return fixture;
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

    @Test
    void queryGraph_fieldsWhitelist() {
        assumeTrue(hasNodeType(COMMENT_NODE_ID), COMMENT_NODE_ID + " node type not registered - skipping");
        TwoCompartmentFixture fixture = createTwoCompartmentFixture("Preprocessing", "Analysis");
        AutomationContext ctx = createContext(fixture.project());

        JsonNode result = new QueryOperations.QueryGraph().execute(ctx,
                shapingParams(List.of("name", "typeId"), false, false));

        assertEquals(2, result.get("nodes").size());
        for (JsonNode node : result.get("nodes")) {
            assertEquals(Set.of("name", "typeId"), fieldNames(node), "node objects must carry exactly the requested fields");
        }
    }

    @Test
    void queryGraph_omitPositions() {
        assumeTrue(hasNodeType(COMMENT_NODE_ID), COMMENT_NODE_ID + " node type not registered - skipping");
        TwoCompartmentFixture fixture = createTwoCompartmentFixture("Preprocessing", "Analysis");
        AutomationContext ctx = createContext(fixture.project());

        JsonNode shaped = new QueryOperations.QueryGraph().execute(ctx, shapingParams(null, true, false));
        assertEquals(2, shaped.get("nodes").size());
        for (JsonNode node : shaped.get("nodes")) {
            assertFalse(node.has("x"), "omit-positions must drop x: " + node);
            assertFalse(node.has("y"), "omit-positions must drop y: " + node);
        }

        JsonNode unshaped = new QueryOperations.QueryGraph().execute(ctx, mapper.createObjectNode());
        assertEquals(2, unshaped.get("nodes").size());
        for (JsonNode node : unshaped.get("nodes")) {
            assertTrue(node.has("x") && node.has("y"), "the default response keeps positions: " + node);
        }
    }

    @Test
    void queryGraph_connectionsOnly() {
        assumeTrue(hasNodeType(COMMENT_NODE_ID), COMMENT_NODE_ID + " node type not registered - skipping");
        TwoCompartmentFixture fixture = createCrossCompartmentConnectedFixture("Preprocessing", "Analysis");
        AutomationContext ctx = createContext(fixture.project());

        JsonNode shaped = new QueryOperations.QueryGraph().execute(ctx, shapingParams(null, false, true));

        assertEquals(0, shaped.get("nodes").size(), "connections-only must return an empty nodes array");
        assertEquals(1, shaped.get("connections").size(), "connections-only must keep the connections");

        JsonNode unshaped = new QueryOperations.QueryGraph().execute(ctx, mapper.createObjectNode());
        assertEquals(unshaped.get("connections"), shaped.get("connections"),
                "connections-only must not change the connection objects");
    }

    @Test
    void queryGraph_omitPositionsBeatsFields() {
        assumeTrue(hasNodeType(COMMENT_NODE_ID), COMMENT_NODE_ID + " node type not registered - skipping");
        TwoCompartmentFixture fixture = createTwoCompartmentFixture("Preprocessing", "Analysis");
        AutomationContext ctx = createContext(fixture.project());

        JsonNode result = new QueryOperations.QueryGraph().execute(ctx,
                shapingParams(List.of("id", "x"), true, false));

        assertEquals(2, result.get("nodes").size());
        for (JsonNode node : result.get("nodes")) {
            assertEquals(Set.of("id"), fieldNames(node), "omit-positions must win over the fields whitelist");
        }
    }

    @Test
    void queryGraph_unknownFieldErrors() {
        assumeTrue(hasNodeType(COMMENT_NODE_ID), COMMENT_NODE_ID + " node type not registered - skipping");
        TwoCompartmentFixture fixture = createTwoCompartmentFixture("Preprocessing", "Analysis");
        AutomationContext ctx = createContext(fixture.project());

        JsonNode result = new QueryOperations.QueryGraph().execute(ctx,
                shapingParams(List.of("id", "bogus"), false, false));

        assertTrue(result.has("error"), "an unknown fields entry must be refused: " + result);
        String error = result.get("error").asText();
        assertTrue(error.contains("bogus"), error);
        assertTrue(error.contains("id, name, typeId, compartmentId, uuid, x, y"), error);
    }

    @Test
    void queryGraph_nullAliasConnectionUnchanged() {
        assumeTrue(hasNodeType(COMMENT_NODE_ID), COMMENT_NODE_ID + " node type not registered - skipping");
        // A connection whose endpoint node was never explicitly aliased: the
        // graph assigns the alias lazily at query time. The shaping path must
        // leave the connection emission untouched, whatever the endpoint alias
        // resolves to (the JSON-null shape included).
        TwoCompartmentFixture fixture = createCrossCompartmentConnectedFixture("Preprocessing", "Analysis");
        AutomationContext ctx = createContext(fixture.project());

        JsonNode unshaped = new QueryOperations.QueryGraph().execute(ctx, mapper.createObjectNode());
        JsonNode shaped = new QueryOperations.QueryGraph().execute(ctx, shapingParams(null, false, true));

        assertEquals(unshaped.get("connections"), shaped.get("connections"),
                "shaping must not change the connection objects");
        assertEquals(1, shaped.get("connections").size());
        JsonNode connection = shaped.get("connections").get(0);
        assertEquals(Set.of("source", "sourceSlot", "target", "targetSlot"), fieldNames(connection),
                "connection objects keep their shape");
        assertEquals(unshaped.get("connections").get(0).get("target"), connection.get("target"),
                "the target endpoint alias is the same value as before shaping");
    }
}
