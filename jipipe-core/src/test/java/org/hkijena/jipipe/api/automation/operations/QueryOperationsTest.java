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
import org.hkijena.jipipe.api.annotation.JIPipeTextAnnotation;
import org.hkijena.jipipe.api.annotation.JIPipeTextAnnotationMergeMode;
import org.hkijena.jipipe.api.automation.AutomationContext;
import org.hkijena.jipipe.api.automation.InstrumentationEventBus;
import org.hkijena.jipipe.api.automation.InstrumentationJobManager;
import org.hkijena.jipipe.api.automation.JIPipeAutomationAPI;
import org.hkijena.jipipe.api.compartments.algorithms.JIPipeProjectCompartment;
import org.hkijena.jipipe.api.data.JIPipeDataItemStore;
import org.hkijena.jipipe.api.data.JIPipeOutputDataSlot;
import org.hkijena.jipipe.api.nodes.JIPipeGraphNode;
import org.hkijena.jipipe.api.nodes.annotation.JIPipeGraphCanvasNote;
import org.hkijena.jipipe.api.nodes.infos.JIPipeJavaNodeInfo;
import org.hkijena.jipipe.api.parameters.JIPipeCustomTextDescriptionParameter;
import org.hkijena.jipipe.api.parameters.JIPipeParameterAccess;
import org.hkijena.jipipe.api.parameters.JIPipeParameterCollection;
import org.hkijena.jipipe.api.parameters.JIPipeParameterTree;
import org.hkijena.jipipe.api.project.JIPipeProject;
import org.hkijena.jipipe.plugins.canvasnotes.nodes.TextBoxGraphCanvasNote;
import org.hkijena.jipipe.plugins.core.nodes.JIPipeCommentNode;
import org.hkijena.jipipe.plugins.expressions.DataExportExpressionParameter;
import org.hkijena.jipipe.plugins.expressions.JIPipeExpressionParameter;
import org.hkijena.jipipe.plugins.strings.StringData;
import org.hkijena.jipipe.utils.json.JIPipePathMetadataStore;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.lang.annotation.Annotation;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
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
 * Also tests the query_notes operation: both note families in one
 * response, the kind filter, compartment filtering with the fail-loud
 * error, the position consistency with query_graph, and the grid-unit
 * reporting of text boxes (gridWidth/gridHeight in cells, width/height
 * in pixels).
 * Also tests the find_nodes operation: case-insensitive name and node type id
 * substring filters, compartment restriction with the fail-loud error, limit
 * with totalCount, and the no-filter default that matches every node.
 * Also tests the normalized query_node parameter values: the
 * renderParameterValue contract (effective text description, type, raw only
 * when it differs, null, and a throwing renderer) and the node-level wiring.
 * Also tests the query_data operation: data-annotation columns appended after
 * the text-annotation columns, the dataAnnotationTypes map (data-annotation
 * column to the registered data type id), per-row data-annotation values as
 * string representations (empty string when a cell is empty), and the
 * unchanged response for a table that has text annotations only.
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
        // Same wall as the node types: the data type id "string" is registered
        // by the StringsPlugin of the jipipe-plugin-strings module, which is
        // not on this module's test classpath. Register the class (it lives
        // in jipipe-core) under its production id so the query_data tests pin
        // the registered-id path of dataAnnotationTypes. The datatype registry
        // has no unregister, and the registration mirrors the production one,
        // so it is deliberately kept for the rest of the JVM.
        if (!JIPipe.getDataTypes().hasDatatypeWithId("string")) {
            JIPipe.getDataTypes().register("string", StringData.class, TEST_DEPENDENCY);
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
        // The "string" data type registration from ensureJIPipe is deliberately
        // kept: the datatype registry has no unregister and the registration
        // mirrors the production one.
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

    private static ObjectNode noteParams(String compartment, String kind) {
        ObjectNode params = mapper.createObjectNode();
        if (compartment != null) {
            params.put("compartment", compartment);
        }
        if (kind != null) {
            params.put("kind", kind);
        }
        return params;
    }

    private static ObjectNode findNodesParams(String query, String typeId, String compartment, Integer limit) {
        ObjectNode params = mapper.createObjectNode();
        if (query != null) {
            params.put("query", query);
        }
        if (typeId != null) {
            params.put("type-id", typeId);
        }
        if (compartment != null) {
            params.put("compartment", compartment);
        }
        if (limit != null) {
            params.put("limit", limit);
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

    private static ObjectNode queryNodeParams(String nodeId) {
        ObjectNode params = mapper.createObjectNode();
        params.put("node-id", nodeId);
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
    private record NoteFixture(JIPipeProject project, JIPipeProjectCompartment compartment,
                               JIPipeGraphNode comment, JIPipeGraphNode textBox) {
    }

    private static NoteFixture createNoteFixture() {
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
        return new NoteFixture(project, compartment, comment, textBox);
    }

    private static JsonNode findEntryByUuid(JsonNode result, String uuid) {
        for (JsonNode entry : result.get("notes")) {
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
                "graph-annotation-text-box is not registered: the queryNotes behavioral tests would silently skip");
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
    void queryGraph_schemaFieldEnumMatchesSharedConstant() {
        // Pins the cross-representation contract: the schema enum, the
        // emission whitelist, and the unknown-field refusal message all
        // derive from JIPipeAutomationAPI.QUERY_GRAPH_NODE_FIELDS.
        JsonNode schema = new QueryOperations.QueryGraph().getInputSchema();
        JsonNode enumNode = schema.get("properties").get("fields").get("items").get("enum");

        assertNotNull(enumNode, "the fields items schema carries no enum: " + schema);
        assertTrue(enumNode.isArray(), "the fields enum must be an array: " + enumNode);
        List<String> schemaFields = new ArrayList<>();
        for (JsonNode entry : enumNode) {
            schemaFields.add(entry.asText());
        }

        assertEquals(JIPipeAutomationAPI.QUERY_GRAPH_NODE_FIELDS, schemaFields,
                "the schema enum must equal the shared field whitelist, in order");
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
    void queryNotes_returnsBothFamiliesWithFields() {
        assumeTrue(hasNodeType(COMMENT_NODE_ID) && hasNodeType(TEXT_BOX_NODE_ID),
                COMMENT_NODE_ID + " or " + TEXT_BOX_NODE_ID + " node type not registered - skipping");
        NoteFixture fixture = createNoteFixture();
        AutomationContext ctx = createContext(fixture.project());
        String commentUuid = fixture.project().getGraph().getUUIDOf(fixture.comment()).toString();
        String textBoxUuid = fixture.project().getGraph().getUUIDOf(fixture.textBox()).toString();

        JsonNode result = new QueryOperations.QueryNotes().execute(ctx, mapper.createObjectNode());

        assertEquals(2, result.get("notes").size());
        JsonNode comment = findEntryByUuid(result, commentUuid);
        assertNotNull(comment, "the comment entry is missing: " + result);
        assertEquals("comment", comment.get("kind").asText());
        assertEquals(fixture.project().getGraph().getAliasIdOf(fixture.comment()), comment.get("id").asText(),
                "the comment entry id must be the graph alias");
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
        assertEquals(fixture.project().getGraph().getAliasIdOf(fixture.textBox()), textBox.get("id").asText(),
                "the text box entry id must be the graph alias");
        assertEquals(TEXT_BOX_NODE_ID, textBox.get("typeId").asText());
        assertEquals(fixture.compartment().getProjectCompartmentUUID().toString(),
                textBox.get("compartmentId").asText());
        assertEquals("Box title", textBox.get("title").asText());
        assertEquals("Box content", textBox.get("content").asText());
        assertEquals(75, textBox.get("width").asInt());
        assertEquals(50, textBox.get("height").asInt());
    }

    @Test
    void queryNotes_kindFilter() {
        assumeTrue(hasNodeType(COMMENT_NODE_ID) && hasNodeType(TEXT_BOX_NODE_ID),
                COMMENT_NODE_ID + " or " + TEXT_BOX_NODE_ID + " node type not registered - skipping");
        NoteFixture fixture = createNoteFixture();
        AutomationContext ctx = createContext(fixture.project());

        JsonNode comments = new QueryOperations.QueryNotes().execute(ctx, noteParams(null, "comment"));
        assertEquals(1, comments.get("notes").size());
        assertEquals("comment", comments.get("notes").get(0).get("kind").asText());

        JsonNode textBoxes = new QueryOperations.QueryNotes().execute(ctx, noteParams(null, "text-box"));
        assertEquals(1, textBoxes.get("notes").size());
        assertEquals("text-box", textBoxes.get("notes").get(0).get("kind").asText());
    }

    @Test
    void queryNotes_compartmentFilterByName() {
        assumeTrue(hasNodeType(COMMENT_NODE_ID) && hasNodeType(TEXT_BOX_NODE_ID),
                COMMENT_NODE_ID + " or " + TEXT_BOX_NODE_ID + " node type not registered - skipping");
        NoteFixture fixture = createNoteFixture();
        fixture.project().addCompartment("Empty");
        AutomationContext ctx = createContext(fixture.project());

        JsonNode filtered = new QueryOperations.QueryNotes().execute(ctx, noteParams("Empty", null));
        assertEquals(0, filtered.get("notes").size());

        JsonNode unknown = new QueryOperations.QueryNotes().execute(ctx, noteParams("Nope", null));
        assertTrue(unknown.has("error"), "an unknown compartment name must error: " + unknown);
        String error = unknown.get("error").asText();
        assertTrue(error.contains("Compartment not found: 'Nope'"), error);
        assertTrue(error.contains("Notes") && error.contains("Empty"), error);
        assertTrue(error.endsWith("(see query_compartments for ids)"), error);
    }

    @Test
    void queryNotes_emptyTextFieldsStayPresent() {
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

        JsonNode result = new QueryOperations.QueryNotes().execute(ctx, mapper.createObjectNode());

        assertEquals(1, result.get("notes").size());
        JsonNode entry = result.get("notes").get(0);
        assertTrue(entry.has("title"), "title must stay present: " + entry);
        assertTrue(entry.has("content"), "content must stay present: " + entry);
        assertEquals("", entry.get("title").asText());
        assertEquals("", entry.get("content").asText());
    }

    @Test
    void queryNotes_positionsMatchQueryGraph() {
        assumeTrue(hasNodeType(COMMENT_NODE_ID) && hasNodeType(TEXT_BOX_NODE_ID),
                COMMENT_NODE_ID + " or " + TEXT_BOX_NODE_ID + " node type not registered - skipping");
        NoteFixture fixture = createNoteFixture();
        fixture.textBox().setNodeUILocationWithin(fixture.compartment().getProjectCompartmentUUID(),
                new java.awt.Point(120, 340));
        AutomationContext ctx = createContext(fixture.project());
        String textBoxUuid = fixture.project().getGraph().getUUIDOf(fixture.textBox()).toString();

        JsonNode notes = new QueryOperations.QueryNotes().execute(ctx, noteParams(null, "text-box"));
        JsonNode graph = new QueryOperations.QueryGraph().execute(ctx, mapper.createObjectNode());

        JsonNode note = findEntryByUuid(notes, textBoxUuid);
        JsonNode graphNode = findGraphNodeByUuid(graph, textBoxUuid);
        assertNotNull(note, "the text box entry is missing: " + notes);
        assertNotNull(graphNode, "the text box node is missing from query_graph: " + graph);
        assertEquals(graphNode.get("x"), note.get("x"), "x must match query_graph");
        assertEquals(graphNode.get("y"), note.get("y"), "y must match query_graph");
        assertEquals(120, note.get("x").asInt());
        assertEquals(340, note.get("y").asInt());
    }

    @Test
    void queryNotes_reportsGridUnits() {
        assumeTrue(hasNodeType(TEXT_BOX_NODE_ID), TEXT_BOX_NODE_ID + " node type not registered - skipping");
        // The read/write unit contract: gridWidth/gridHeight mirror the
        // grid-width/grid-height parameters (grid cells, the unit move_node,
        // create_note, and set_parameter write), while width/height stay in
        // pixels (grid cells times the canvas grid constants).
        var textBoxInfo = JIPipe.getNodes().getInfoById(TEXT_BOX_NODE_ID);
        JIPipeProject project = new JIPipeProject();
        JIPipeProjectCompartment compartment = project.addCompartment("Notes");
        JIPipeGraphNode textBox = textBoxInfo.newInstance();
        JIPipeGraphCanvasNote note = (JIPipeGraphCanvasNote) textBox;
        note.setGridWidth(6);
        note.setGridHeight(5);
        project.getGraph().insertNode(textBox, compartment.getProjectCompartmentUUID());
        AutomationContext ctx = createContext(project);
        String textBoxUuid = project.getGraph().getUUIDOf(textBox).toString();

        JsonNode result = new QueryOperations.QueryNotes().execute(ctx, mapper.createObjectNode());

        JsonNode textBoxEntry = findEntryByUuid(result, textBoxUuid);
        assertNotNull(textBoxEntry, "the text box entry is missing: " + result);
        assertEquals(6, textBoxEntry.get("gridWidth").asInt(),
                "gridWidth must mirror the grid-width parameter (grid cells)");
        assertEquals(5, textBoxEntry.get("gridHeight").asInt(),
                "gridHeight must mirror the grid-height parameter (grid cells)");
        assertEquals(150, textBoxEntry.get("width").asInt(), "width stays pixel-based (6 cells x 25 px)");
        assertEquals(125, textBoxEntry.get("height").asInt(), "height stays pixel-based (5 cells x 25 px)");
    }

    @Test
    void queryNotes_isRegisteredAndAnnotationsIsGone() {
        // The clean break: the renamed id is registered, the old id is gone
        // (no deprecated alias; Query/read-only ops carry no persisted
        // permission state, so nothing holds the old id).
        assertTrue(JIPipe.getAutomation().getOperationRegistry().get("query_notes") != null,
                "query_notes must be registered");
        assertTrue(JIPipe.getAutomation().getOperationRegistry().get("query_annotations") == null,
                "query_annotations must be removed without an alias");
    }

    /**
     * A project with one compartment and one comment node whose live
     * "Comment" output slot carries the table rows for the query_data tests.
     * query_data reads the slot of the node directly (output slots first, then
     * input slots), so populating the live slot is the verified wiring; a
     * project-cache entry would not be read by query_data.
     */
    private record QueryDataFixture(JIPipeProject project, JIPipeGraphNode node) {
    }

    private static QueryDataFixture createQueryDataFixture() {
        var info = JIPipe.getNodes().getInfoById(COMMENT_NODE_ID);
        JIPipeProject project = new JIPipeProject();
        JIPipeProjectCompartment compartment = project.addCompartment("Notes");
        JIPipeGraphNode node = info.newInstance();
        project.getGraph().insertNode(node, compartment.getProjectCompartmentUUID());
        return new QueryDataFixture(project, node);
    }

    private static ObjectNode queryDataParams(String nodeId, String slotName) {
        ObjectNode params = mapper.createObjectNode();
        params.put("node-id", nodeId);
        params.put("slot-name", slotName);
        return params;
    }

    @Test
    void queryDataWithDataAnnotations() {
        assumeTrue(hasNodeType(COMMENT_NODE_ID), COMMENT_NODE_ID + " node type not registered - skipping");
        QueryDataFixture fixture = createQueryDataFixture();
        JIPipeOutputDataSlot slot = fixture.node().getOutputSlot("Comment");
        slot.addData(new StringData("row-1"),
                List.of(new JIPipeTextAnnotation("Tag", "t-1")),
                JIPipeTextAnnotationMergeMode.OverwriteExisting, new JIPipeProgressInfo());
        slot.addData(new StringData("row-2"),
                List.of(new JIPipeTextAnnotation("Tag", "t-2")),
                JIPipeTextAnnotationMergeMode.OverwriteExisting, new JIPipeProgressInfo());
        slot.setDataAnnotationItemStore(0, "Mask", new JIPipeDataItemStore(new StringData("m-1")));
        AutomationContext ctx = createContext(fixture.project());

        JsonNode result = new QueryOperations.QueryData().execute(ctx,
                queryDataParams(fixture.project().getGraph().getAliasIdOf(fixture.node()), "Comment"));

        // Column order: Name, text-annotation columns, then data-annotation columns
        JsonNode columns = result.get("columns");
        assertEquals(3, columns.size(), "columns must be Name, the text-annotation columns, then Mask: " + columns);
        assertEquals("Name", columns.get(0).asText());
        assertEquals("Tag", columns.get(1).asText());
        assertEquals("Mask", columns.get(2).asText());

        // dataAnnotationTypes maps the data-annotation column to the registered data type id
        JsonNode types = result.get("dataAnnotationTypes");
        assertNotNull(types, "dataAnnotationTypes must be present: " + result);
        assertEquals(1, types.size(), "only Mask is a data-annotation column: " + types);
        assertEquals("string", types.get("Mask").asText(),
                "dataAnnotationTypes must report the registered data type id of the column values");

        // Per-row values as string representations; an empty cell is the empty string
        assertEquals(2, result.get("rows").size());
        JsonNode row0 = result.get("rows").get(0);
        assertEquals("row-1", row0.get("Name").asText());
        assertEquals("t-1", row0.get("Tag").asText());
        assertEquals("m-1", row0.get("Mask").asText());
        JsonNode row1 = result.get("rows").get(1);
        assertEquals("row-2", row1.get("Name").asText());
        assertEquals("t-2", row1.get("Tag").asText());
        assertEquals("", row1.get("Mask").asText(), "a data-annotation cell without a value must be the empty string");
    }

    @Test
    void queryDataWithoutDataAnnotationsIsUnchanged() {
        assumeTrue(hasNodeType(COMMENT_NODE_ID), COMMENT_NODE_ID + " node type not registered - skipping");
        QueryDataFixture fixture = createQueryDataFixture();
        JIPipeOutputDataSlot slot = fixture.node().getOutputSlot("Comment");
        slot.addData(new StringData("row-1"),
                List.of(new JIPipeTextAnnotation("Tag", "t-1")),
                JIPipeTextAnnotationMergeMode.OverwriteExisting, new JIPipeProgressInfo());
        AutomationContext ctx = createContext(fixture.project());

        JsonNode result = new QueryOperations.QueryData().execute(ctx,
                queryDataParams(fixture.project().getGraph().getAliasIdOf(fixture.node()), "Comment"));

        // Columns and rows exactly as before: Name plus the text-annotation columns only
        JsonNode columns = result.get("columns");
        assertEquals(2, columns.size(), "columns must stay Name plus the text-annotation columns: " + columns);
        assertEquals("Name", columns.get(0).asText());
        assertEquals("Tag", columns.get(1).asText());

        // No dataAnnotationTypes entries for a table without data annotations
        JsonNode types = result.get("dataAnnotationTypes");
        assertNotNull(types, "dataAnnotationTypes must be present: " + result);
        assertEquals(0, types.size(), "dataAnnotationTypes must carry no entries: " + types);

        assertEquals(1, result.get("rows").size());
        JsonNode row0 = result.get("rows").get(0);
        assertEquals(Set.of("Name", "Tag"), fieldNames(row0),
                "the row must carry no data-annotation fields: " + row0);
        assertEquals("row-1", row0.get("Name").asText());
        assertEquals("t-1", row0.get("Tag").asText());
    }

    @Test
    void find_nodes_nameSubstringCaseInsensitive() {
        assumeTrue(hasNodeType(COMMENT_NODE_ID), COMMENT_NODE_ID + " node type not registered - skipping");
        var info = JIPipe.getNodes().getInfoById(COMMENT_NODE_ID);
        JIPipeProject project = new JIPipeProject();
        JIPipeProjectCompartment compartment = project.addCompartment("Notes");
        JIPipeGraphNode csv = info.newInstance();
        csv.setCustomName("Input CSV");
        project.getGraph().insertNode(csv, compartment.getProjectCompartmentUUID());
        JIPipeGraphNode filter = info.newInstance();
        filter.setCustomName("Filter");
        project.getGraph().insertNode(filter, compartment.getProjectCompartmentUUID());
        AutomationContext ctx = createContext(project);

        JsonNode csvMatch = new QueryOperations.FindNodes().execute(ctx, findNodesParams("csv", null, null, null));
        assertEquals(1, csvMatch.get("matches").size());
        assertEquals("Input CSV", csvMatch.get("matches").get(0).get("name").asText());

        JsonNode filterMatch = new QueryOperations.FindNodes().execute(ctx, findNodesParams("filter", null, null, null));
        assertEquals(1, filterMatch.get("matches").size());
        assertEquals("Filter", filterMatch.get("matches").get(0).get("name").asText());

        // The reverse direction: uppercase queries must match too (both sides folded)
        JsonNode upperQueryMatch = new QueryOperations.FindNodes().execute(ctx, findNodesParams("CSV", null, null, null));
        assertEquals(1, upperQueryMatch.get("matches").size(), "an uppercase query must match case-insensitively");
        assertEquals("Input CSV", upperQueryMatch.get("matches").get(0).get("name").asText());

        JsonNode upperTypeIdMatch = new QueryOperations.FindNodes().execute(ctx, findNodesParams(null, "COMMENT", null, null));
        assertEquals(2, upperTypeIdMatch.get("matches").size(), "an uppercase type-id must match case-insensitively");
    }

    @Test
    void find_nodes_typeIdFilter() {
        assumeTrue(hasNodeType(COMMENT_NODE_ID) && hasNodeType(TEXT_BOX_NODE_ID),
                COMMENT_NODE_ID + " or " + TEXT_BOX_NODE_ID + " node type not registered - skipping");
        var commentInfo = JIPipe.getNodes().getInfoById(COMMENT_NODE_ID);
        var textBoxInfo = JIPipe.getNodes().getInfoById(TEXT_BOX_NODE_ID);
        JIPipeProject project = new JIPipeProject();
        JIPipeProjectCompartment compartment = project.addCompartment("Notes");
        JIPipeGraphNode comment = commentInfo.newInstance();
        project.getGraph().insertNode(comment, compartment.getProjectCompartmentUUID());
        JIPipeGraphNode textBox = textBoxInfo.newInstance();
        project.getGraph().insertNode(textBox, compartment.getProjectCompartmentUUID());
        AutomationContext ctx = createContext(project);

        JsonNode result = new QueryOperations.FindNodes().execute(ctx, findNodesParams(null, "comment", null, null));

        assertEquals(1, result.get("matches").size());
        assertEquals(COMMENT_NODE_ID, result.get("matches").get(0).get("typeId").asText());
        assertEquals(project.getGraph().getUUIDOf(comment).toString(),
                result.get("matches").get(0).get("uuid").asText());
    }

    @Test
    void find_nodes_compartmentFilter() {
        assumeTrue(hasNodeType(COMMENT_NODE_ID), COMMENT_NODE_ID + " node type not registered - skipping");
        TwoCompartmentFixture fixture = createTwoCompartmentFixture("Preprocessing", "Analysis");
        AutomationContext ctx = createContext(fixture.project());

        JsonNode result = new QueryOperations.FindNodes().execute(ctx,
                findNodesParams(null, null, "Preprocessing", null));

        assertEquals(1, result.get("matches").size());
        assertEquals(fixture.project().getGraph().getUUIDOf(fixture.firstNode()).toString(),
                result.get("matches").get(0).get("uuid").asText());
        assertEquals(fixture.first().getProjectCompartmentUUID().toString(),
                result.get("matches").get(0).get("compartmentId").asText());
    }

    @Test
    void find_nodes_limitAndTotalCount() {
        assumeTrue(hasNodeType(COMMENT_NODE_ID), COMMENT_NODE_ID + " node type not registered - skipping");
        var info = JIPipe.getNodes().getInfoById(COMMENT_NODE_ID);
        JIPipeProject project = new JIPipeProject();
        JIPipeProjectCompartment compartment = project.addCompartment("Notes");
        for (int i = 0; i < 3; i++) {
            JIPipeGraphNode node = info.newInstance();
            node.setCustomName("Node " + i);
            project.getGraph().insertNode(node, compartment.getProjectCompartmentUUID());
        }
        AutomationContext ctx = createContext(project);

        JsonNode result = new QueryOperations.FindNodes().execute(ctx, findNodesParams(null, null, null, 2));

        assertEquals(2, result.get("matches").size(), "the limit must cap the matches array");
        assertEquals(3, result.get("totalCount").asInt(), "totalCount must count the matches before limiting");
    }

    @Test
    void find_nodes_noFiltersReturnsEverythingLimited() {
        assumeTrue(hasNodeType(COMMENT_NODE_ID), COMMENT_NODE_ID + " node type not registered - skipping");
        var info = JIPipe.getNodes().getInfoById(COMMENT_NODE_ID);
        JIPipeProject project = new JIPipeProject();
        JIPipeProjectCompartment compartment = project.addCompartment("Notes");
        for (int i = 0; i < 2; i++) {
            JIPipeGraphNode node = info.newInstance();
            project.getGraph().insertNode(node, compartment.getProjectCompartmentUUID());
        }
        AutomationContext ctx = createContext(project);

        JsonNode result = new QueryOperations.FindNodes().execute(ctx, mapper.createObjectNode());

        assertEquals(2, result.get("totalCount").asInt(), "no filters must match every node");
        assertTrue(result.get("matches").size() <= 50, "the default limit caps the matches at 50");
        assertEquals(Math.min(2, 50), result.get("matches").size());

        // Blank params are treated as omitted: the same call must match everything
        JsonNode blank = new QueryOperations.FindNodes().execute(ctx, findNodesParams("   ", " ", "", null));
        assertEquals(result.get("totalCount"), blank.get("totalCount"),
                "blank filters must behave like omitted filters");
        assertEquals(result.get("matches").size(), blank.get("matches").size(),
                "blank filters must behave like omitted filters");
    }

    @Test
    void find_nodes_unknownCompartmentErrors() {
        assumeTrue(hasNodeType(COMMENT_NODE_ID), COMMENT_NODE_ID + " node type not registered - skipping");
        TwoCompartmentFixture fixture = createTwoCompartmentFixture("Preprocessing", "Analysis");
        AutomationContext ctx = createContext(fixture.project());

        JsonNode result = new QueryOperations.FindNodes().execute(ctx, findNodesParams(null, null, "Nope", null));

        assertTrue(result.has("error"),
                "an unknown compartment name must error, not return an empty match list: " + result);
        String error = result.get("error").asText();
        assertTrue(error.contains("Compartment not found: 'Nope'."), error);
        assertTrue(error.contains("Preprocessing") && error.contains("Analysis"), error);
        assertTrue(error.endsWith("(see query_compartments for ids)"), error);
    }

    @Test
    void queryNode_parametersUseTheRenderer() {
        assumeTrue(hasNodeType(COMMENT_NODE_ID), COMMENT_NODE_ID + " node type not registered - skipping");
        var info = JIPipe.getNodes().getInfoById(COMMENT_NODE_ID);
        JIPipeProject project = new JIPipeProject();
        JIPipeProjectCompartment compartment = project.addCompartment("Notes");
        JIPipeGraphNode comment = info.newInstance();
        project.getGraph().insertNode(comment, compartment.getProjectCompartmentUUID());
        AutomationContext ctx = createContext(project);

        JsonNode result = new QueryOperations.QueryNode().execute(ctx,
                queryNodeParams(project.getGraph().getAliasIdOf(comment)));

        JsonNode parameters = result.get("parameters");
        JsonNode backgroundColor = parameters.get("background-color");
        assertTrue(backgroundColor.isObject(),
                "parameter entries must be normalized objects, not flat strings: " + backgroundColor);
        assertEquals(Set.of("value", "type"), fieldNames(backgroundColor),
                "both renderings of a Color agree, so there is no raw key: " + backgroundColor);
        assertEquals("Color", backgroundColor.get("type").asText());
        assertEquals(String.valueOf(((JIPipeCommentNode) comment).getBackgroundColor()),
                backgroundColor.get("value").asText());
        for (String key : fieldNames(parameters)) {
            assertFalse(key.startsWith("jipipe:") || key.startsWith("jipipe-ui:"),
                    "jipipe-prefixed keys must stay excluded, but '" + key + "' was emitted");
        }
    }

    /**
     * Minimal canned parameter access for the renderer-seam tests: the key,
     * value, and field class are fixed, everything else is inert.
     */
    private static final class CannedParameterAccess implements JIPipeParameterAccess {
        private final String key;
        private final Object value;
        private final Class<?> fieldClass;

        private CannedParameterAccess(String key, Object value, Class<?> fieldClass) {
            this.key = key;
            this.value = value;
            this.fieldClass = fieldClass;
        }

        @Override
        public String getKey() {
            return key;
        }

        @Override
        public String getName() {
            return key;
        }

        @Override
        public String getDescription() {
            return "";
        }

        @Override
        public boolean isHidden() {
            return false;
        }

        @Override
        public boolean isImportant() {
            return false;
        }

        @Override
        public <T extends Annotation> T getAnnotationOfType(Class<T> klass) {
            return null;
        }

        @Override
        public Collection<Annotation> getAnnotations() {
            return Collections.emptyList();
        }

        @Override
        public Class<?> getFieldClass() {
            return fieldClass;
        }

        @Override
        @SuppressWarnings("unchecked")
        public <T> T get(Class<T> klass) {
            return (T) value;
        }

        @Override
        public <T> boolean set(T value) {
            return false;
        }

        @Override
        public JIPipeParameterCollection getSource() {
            return null;
        }

        @Override
        public double getPriority() {
            return 0;
        }

        @Override
        public String getShortKey() {
            return key;
        }

        @Override
        public int getUIOrder() {
            return 0;
        }

        @Override
        public JIPipePathMetadataStore getMetadata() {
            return null;
        }
    }

    /**
     * A parameter value whose text description always fails (pins Review Focus 5).
     */
    private static final class ThrowingTextDescription implements JIPipeCustomTextDescriptionParameter {
        @Override
        public String getTextDescription() {
            throw new IllegalStateException("boom");
        }
    }

    @Test
    void renderParameterValue_stringHasNoRaw() {
        ObjectNode rendered = JIPipeAutomationAPI.renderParameterValue(
                new CannedParameterAccess("my-string", "hello", String.class));
        assertEquals(Set.of("value", "type"), fieldNames(rendered));
        assertEquals("hello", rendered.get("value").asText());
        assertEquals("String", rendered.get("type").asText());
    }

    @Test
    void renderParameterValue_expressionIsBare() {
        ObjectNode rendered = JIPipeAutomationAPI.renderParameterValue(new CannedParameterAccess(
                "expression", new JIPipeExpressionParameter("foo(bar)"), JIPipeExpressionParameter.class));
        assertEquals(Set.of("value", "type", "raw"), fieldNames(rendered));
        assertEquals("foo(bar)", rendered.get("value").asText(),
                "the text description must not carry the 'Expression: ' prefix");
        assertEquals("Expression: foo(bar)", rendered.get("raw").asText(), "raw carries the plain toString() form");
        assertEquals("JIPipeExpressionParameter", rendered.get("type").asText());
    }

    @Test
    void renderParameterValue_dataExportWrapperIsBare() {
        ObjectNode rendered = JIPipeAutomationAPI.renderParameterValue(new CannedParameterAccess(
                "export-path", new DataExportExpressionParameter("PATH_COMBINE(data_dir, \"x\")"),
                DataExportExpressionParameter.class));
        assertEquals(Set.of("value", "type", "raw"), fieldNames(rendered));
        assertEquals("PATH_COMBINE(data_dir, \"x\")", rendered.get("value").asText());
        assertEquals("Expression: PATH_COMBINE(data_dir, \"x\")", rendered.get("raw").asText());
        assertEquals("DataExportExpressionParameter", rendered.get("type").asText());
    }

    @Test
    void renderParameterValue_rawOnlyWhenDifferent() {
        // Absence: both renderings of a plain String agree
        ObjectNode stringRendered = JIPipeAutomationAPI.renderParameterValue(
                new CannedParameterAccess("my-string", "hello", String.class));
        assertFalse(stringRendered.has("raw"), "raw must be absent when both renderings agree: " + stringRendered);
        // Presence: raw carries the plain toString() form
        ObjectNode expressionRendered = JIPipeAutomationAPI.renderParameterValue(new CannedParameterAccess(
                "expression", new JIPipeExpressionParameter("foo(bar)"), JIPipeExpressionParameter.class));
        assertEquals("Expression: foo(bar)", expressionRendered.get("raw").asText());
    }

    @Test
    void renderParameterValue_throwingRendererDoesNotFail() {
        ObjectNode rendered = JIPipeAutomationAPI.renderParameterValue(new CannedParameterAccess(
                "broken", new ThrowingTextDescription(), ThrowingTextDescription.class));
        assertEquals(Set.of("value", "type"), fieldNames(rendered));
        assertTrue(rendered.get("value").asText().startsWith("error rendering value:"),
                "a failing rendering must yield the error placeholder: " + rendered);
        assertFalse(rendered.has("raw"), "the error case must not emit a raw key: " + rendered);
        assertEquals("ThrowingTextDescription", rendered.get("type").asText());
        // A healthy entry rendered after the failure still renders normally
        ObjectNode healthy = JIPipeAutomationAPI.renderParameterValue(
                new CannedParameterAccess("my-string", "hello", String.class));
        assertEquals("hello", healthy.get("value").asText());
        assertFalse(healthy.has("raw"));
    }

    @Test
    void renderParameterValue_nullValue() {
        ObjectNode rendered = JIPipeAutomationAPI.renderParameterValue(
                new CannedParameterAccess("empty", null, String.class));
        assertEquals(Set.of("value", "type"), fieldNames(rendered));
        assertEquals("null", rendered.get("value").asText(), "a null value renders the literal string 'null'");
        assertFalse(rendered.has("raw"));
        assertEquals("String", rendered.get("type").asText());
    }
}
