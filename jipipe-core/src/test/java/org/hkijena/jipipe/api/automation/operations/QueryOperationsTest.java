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
import org.hkijena.jipipe.api.nodes.annotation.JIPipeGraphCanvasNote;
import org.hkijena.jipipe.api.nodes.infos.JIPipeJavaNodeInfo;
import org.hkijena.jipipe.api.parameters.JIPipeParameterTree;
import org.hkijena.jipipe.api.project.JIPipeProject;
import org.hkijena.jipipe.plugins.canvasnotes.nodes.TextBoxGraphCanvasNote;
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
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

/**
 * Tests the query_graph operation: compartment filtering by name, the fail-loud
 * error envelope for unresolvable compartment filters (including UUID-shaped
 * filters without name fallback), stable resolution of duplicate compartment
 * names, the per-node compartmentId attribution, and the response shaping
 * parameters (fields whitelist, omit-positions, connections-only).
 * Also tests the query_annotations operation: both annotation families in one
 * response, the kind filter, compartment filtering with the fail-loud error,
 * and the position consistency with query_graph.
 */
public class QueryOperationsTest {

    private static final String COMMENT_NODE_ID = "jipipe:comment";
    private static final String TEXT_BOX_NODE_ID = "graph-annotation-text-box";
    private static final JIPipeMutableDependency TEST_DEPENDENCY =
            new JIPipeMutableDependency("query-operations-test", "1.0.0", "query-operations-test");

    private static final ObjectMapper mapper = new ObjectMapper();

    private static boolean registeredCommentNodeHere;
    private static boolean registeredTextBoxNodeHere;

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
        // Same wall as jipipe:comment: the id graph-annotation-text-box is
        // registered by the CanvasNotesPlugin, whose registration is not
        // active in this module's test JVM (the class itself lives in
        // jipipe-core). Where the plugin is loaded, its registration is kept.
        if (!JIPipe.getNodes().hasNodeInfoWithId(TEXT_BOX_NODE_ID)) {
            JIPipe.getNodes().register(new JIPipeJavaNodeInfo(TEXT_BOX_NODE_ID, TextBoxGraphCanvasNote.class), TEST_DEPENDENCY);
            registeredTextBoxNodeHere = true;
        }
    }

    @AfterAll
    static void cleanup() {
        if (registeredCommentNodeHere) {
            JIPipe.getNodes().unregister(COMMENT_NODE_ID);
            registeredCommentNodeHere = false;
        }
        if (registeredTextBoxNodeHere) {
            JIPipe.getNodes().unregister(TEXT_BOX_NODE_ID);
            registeredTextBoxNodeHere = false;
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

    private static ObjectNode annotationParams(String compartment, String kind) {
        ObjectNode params = mapper.createObjectNode();
        if (compartment != null) {
            params.put("compartment", compartment);
        }
        if (kind != null) {
            params.put("kind", kind);
        }
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

    /**
     * A project with one compartment holding one comment node named
     * "My note" and one text box canvas note with a set title and content
     * and a 3x2 grid size (75x50 pixels).
     */
    private record AnnotationFixture(JIPipeProject project, JIPipeProjectCompartment compartment,
                                     JIPipeGraphNode comment, JIPipeGraphNode textBox) {
    }

    private static AnnotationFixture createAnnotationFixture() {
        var commentInfo = JIPipe.getNodes().getInfoById(COMMENT_NODE_ID);
        var textBoxInfo = JIPipe.getNodes().getInfoById(TEXT_BOX_NODE_ID);
        JIPipeProject project = new JIPipeProject();
        JIPipeProjectCompartment compartment = project.addCompartment("Notes");
        JIPipeGraphNode comment = commentInfo.newInstance();
        comment.setCustomName("My note");
        project.getGraph().insertNode(comment, compartment.getProjectCompartmentUUID());
        JIPipeGraphNode textBox = textBoxInfo.newInstance();
        JIPipeParameterTree.getParameters(textBox).get("text-title").set("Box title");
        JIPipeParameterTree.getParameters(textBox).get("text-content").set("Box content");
        JIPipeGraphCanvasNote note = (JIPipeGraphCanvasNote) textBox;
        note.setGridWidth(3);
        note.setGridHeight(2);
        project.getGraph().insertNode(textBox, compartment.getProjectCompartmentUUID());
        return new AnnotationFixture(project, compartment, comment, textBox);
    }

    private static JsonNode findEntryByUuid(JsonNode result, String uuid) {
        for (JsonNode entry : result.get("annotations")) {
            if (entry.get("uuid").asText().equals(uuid)) {
                return entry;
            }
        }
        return null;
    }

    private static JsonNode findGraphNodeByUuid(JsonNode result, String uuid) {
        for (JsonNode node : result.get("nodes")) {
            if (node.get("uuid").asText().equals(uuid)) {
                return node;
            }
        }
        return null;
    }

    @Test
    void smoke_commentNodeTypeIsRegistered() {
        assertTrue(hasNodeType(COMMENT_NODE_ID),
                "jipipe:comment is not registered: the queryGraph behavioral tests would silently skip");
    }

    @Test
    void smoke_textBoxNodeTypeIsRegistered() {
        assertTrue(hasNodeType(TEXT_BOX_NODE_ID),
                "graph-annotation-text-box is not registered: the queryAnnotations behavioral tests would silently skip");
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

    @Test
    void queryAnnotations_returnsBothFamiliesWithFields() {
        assumeTrue(hasNodeType(COMMENT_NODE_ID) && hasNodeType(TEXT_BOX_NODE_ID),
                COMMENT_NODE_ID + " or " + TEXT_BOX_NODE_ID + " node type not registered - skipping");
        AnnotationFixture fixture = createAnnotationFixture();
        AutomationContext ctx = createContext(fixture.project());
        String commentUuid = fixture.project().getGraph().getUUIDOf(fixture.comment()).toString();
        String textBoxUuid = fixture.project().getGraph().getUUIDOf(fixture.textBox()).toString();

        JsonNode result = new QueryOperations.QueryAnnotations().execute(ctx, mapper.createObjectNode());

        assertEquals(2, result.get("annotations").size());
        JsonNode comment = findEntryByUuid(result, commentUuid);
        assertNotNull(comment, "the comment entry is missing: " + result);
        assertEquals("comment", comment.get("kind").asText());
        assertEquals("My note", comment.get("title").asText());
        assertEquals(COMMENT_NODE_ID, comment.get("typeId").asText());
        assertEquals(fixture.compartment().getProjectCompartmentUUID().toString(),
                comment.get("compartmentId").asText());
        assertFalse(comment.has("content"), "comment entries carry no content: " + comment);
        assertFalse(comment.has("width"), "comment entries carry no width: " + comment);
        assertFalse(comment.has("height"), "comment entries carry no height: " + comment);

        JsonNode textBox = findEntryByUuid(result, textBoxUuid);
        assertNotNull(textBox, "the text box entry is missing: " + result);
        assertEquals("text-box", textBox.get("kind").asText());
        assertEquals(TEXT_BOX_NODE_ID, textBox.get("typeId").asText());
        assertEquals(fixture.compartment().getProjectCompartmentUUID().toString(),
                textBox.get("compartmentId").asText());
        assertEquals("Box title", textBox.get("title").asText());
        assertEquals("Box content", textBox.get("content").asText());
        assertEquals(75, textBox.get("width").asInt());
        assertEquals(50, textBox.get("height").asInt());
    }

    @Test
    void queryAnnotations_kindFilter() {
        assumeTrue(hasNodeType(COMMENT_NODE_ID) && hasNodeType(TEXT_BOX_NODE_ID),
                COMMENT_NODE_ID + " or " + TEXT_BOX_NODE_ID + " node type not registered - skipping");
        AnnotationFixture fixture = createAnnotationFixture();
        AutomationContext ctx = createContext(fixture.project());

        JsonNode comments = new QueryOperations.QueryAnnotations().execute(ctx, annotationParams(null, "comment"));
        assertEquals(1, comments.get("annotations").size());
        assertEquals("comment", comments.get("annotations").get(0).get("kind").asText());

        JsonNode textBoxes = new QueryOperations.QueryAnnotations().execute(ctx, annotationParams(null, "text-box"));
        assertEquals(1, textBoxes.get("annotations").size());
        assertEquals("text-box", textBoxes.get("annotations").get(0).get("kind").asText());
    }

    @Test
    void queryAnnotations_compartmentFilterByName() {
        assumeTrue(hasNodeType(COMMENT_NODE_ID) && hasNodeType(TEXT_BOX_NODE_ID),
                COMMENT_NODE_ID + " or " + TEXT_BOX_NODE_ID + " node type not registered - skipping");
        AnnotationFixture fixture = createAnnotationFixture();
        fixture.project().addCompartment("Empty");
        AutomationContext ctx = createContext(fixture.project());

        JsonNode filtered = new QueryOperations.QueryAnnotations().execute(ctx, annotationParams("Empty", null));
        assertEquals(0, filtered.get("annotations").size());

        JsonNode unknown = new QueryOperations.QueryAnnotations().execute(ctx, annotationParams("Nope", null));
        assertTrue(unknown.has("error"), "an unknown compartment name must error: " + unknown);
        String error = unknown.get("error").asText();
        assertTrue(error.contains("Compartment not found: 'Nope'"), error);
        assertTrue(error.contains("Notes") && error.contains("Empty"), error);
        assertTrue(error.endsWith("(see query_compartments for ids)"), error);
    }

    @Test
    void queryAnnotations_emptyTextFieldsStayPresent() {
        assumeTrue(hasNodeType(TEXT_BOX_NODE_ID), TEXT_BOX_NODE_ID + " node type not registered - skipping");
        // Review Focus 4: a text box with empty title and content must still
        // emit both keys (as empty strings), never omit them.
        var textBoxInfo = JIPipe.getNodes().getInfoById(TEXT_BOX_NODE_ID);
        JIPipeProject project = new JIPipeProject();
        JIPipeProjectCompartment compartment = project.addCompartment("Notes");
        JIPipeGraphNode textBox = textBoxInfo.newInstance();
        JIPipeParameterTree.getParameters(textBox).get("text-title").set("");
        JIPipeParameterTree.getParameters(textBox).get("text-content").set("");
        project.getGraph().insertNode(textBox, compartment.getProjectCompartmentUUID());
        AutomationContext ctx = createContext(project);

        JsonNode result = new QueryOperations.QueryAnnotations().execute(ctx, mapper.createObjectNode());

        assertEquals(1, result.get("annotations").size());
        JsonNode entry = result.get("annotations").get(0);
        assertTrue(entry.has("title"), "title must stay present: " + entry);
        assertTrue(entry.has("content"), "content must stay present: " + entry);
        assertEquals("", entry.get("title").asText());
        assertEquals("", entry.get("content").asText());
    }

    @Test
    void queryAnnotations_positionsMatchQueryGraph() {
        assumeTrue(hasNodeType(COMMENT_NODE_ID) && hasNodeType(TEXT_BOX_NODE_ID),
                COMMENT_NODE_ID + " or " + TEXT_BOX_NODE_ID + " node type not registered - skipping");
        AnnotationFixture fixture = createAnnotationFixture();
        fixture.textBox().setNodeUILocationWithin(fixture.compartment().getProjectCompartmentUUID(),
                new java.awt.Point(120, 340));
        AutomationContext ctx = createContext(fixture.project());
        String textBoxUuid = fixture.project().getGraph().getUUIDOf(fixture.textBox()).toString();

        JsonNode annotations = new QueryOperations.QueryAnnotations().execute(ctx, annotationParams(null, "text-box"));
        JsonNode graph = new QueryOperations.QueryGraph().execute(ctx, mapper.createObjectNode());

        JsonNode annotation = findEntryByUuid(annotations, textBoxUuid);
        JsonNode graphNode = findGraphNodeByUuid(graph, textBoxUuid);
        assertNotNull(annotation, "the text box entry is missing: " + annotations);
        assertNotNull(graphNode, "the text box node is missing from query_graph: " + graph);
        assertEquals(graphNode.get("x"), annotation.get("x"), "x must match query_graph");
        assertEquals(graphNode.get("y"), annotation.get("y"), "y must match query_graph");
        assertEquals(120, annotation.get("x").asInt());
        assertEquals(340, annotation.get("y").asInt());
    }
}
