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
import org.hkijena.jipipe.api.automation.JIPipeAutomationAPI;
import org.hkijena.jipipe.api.automation.events.NodeAddedEvent;
import org.hkijena.jipipe.api.compartments.algorithms.JIPipeProjectCompartment;
import org.hkijena.jipipe.api.nodes.JIPipeGraphNode;
import org.hkijena.jipipe.api.nodes.annotation.JIPipeGraphCanvasNote;
import org.hkijena.jipipe.api.nodes.infos.JIPipeJavaNodeInfo;
import org.hkijena.jipipe.api.parameters.JIPipeParameterAccess;
import org.hkijena.jipipe.api.parameters.JIPipeParameterTree;
import org.hkijena.jipipe.api.project.JIPipeProject;
import org.hkijena.jipipe.plugins.canvasnotes.nodes.TextBoxGraphCanvasNote;
import org.hkijena.jipipe.plugins.core.nodes.JIPipeCommentNode;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.awt.Point;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

/**
 * Tests the move_node operation: the stored grid-location write (the same write
 * the arrange_nodes operation applies per node), the response envelope (node,
 * uuid, compartment, x, y, and previous as an object or JSON null), the error
 * envelope for an unknown node, and that canvas notes move like any other node.
 * Also tests the create_note operation: the stored title/content/size/location
 * of the created note, the response envelope, the comment kind (title is the
 * node name), the defaults, the fail-fast refusals (comment with content or
 * width/height, unknown compartment), and the single NodeAddedEvent that
 * carries the final title.
 */
public class ModificationOperationsTest {

    private static final String COMMENT_NODE_ID = "jipipe:comment";
    private static final String TEXT_BOX_NODE_ID = "graph-annotation-text-box";
    private static final JIPipeMutableDependency TEST_DEPENDENCY =
            new JIPipeMutableDependency("modification-operations-test", "1.0.0", "modification-operations-test");

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

    private static JIPipeProject createProject() {
        JIPipeProject project = new JIPipeProject();
        project.addCompartment("Notes");
        return project;
    }

    private static JIPipeProjectCompartment onlyCompartment(JIPipeProject project) {
        return project.getCompartments().values().iterator().next();
    }

    private static ObjectNode moveNodeParams(String nodeId, int x, int y) {
        ObjectNode params = mapper.createObjectNode();
        params.put("node", nodeId);
        params.put("x", x);
        params.put("y", y);
        return params;
    }

    private static ObjectNode createNoteParams(String compartment) {
        ObjectNode params = mapper.createObjectNode();
        params.put("compartment", compartment);
        return params;
    }

    private static JIPipeGraphNode singleNodeInCompartment(JIPipeProject project, JIPipeProjectCompartment compartment) {
        Set<JIPipeGraphNode> nodes = project.getGraph().getNodesWithinCompartment(compartment.getProjectCompartmentUUID());
        assertEquals(1, nodes.size(), "the compartment must contain exactly the created note");
        return nodes.iterator().next();
    }

    @Test
    void smoke_commentNodeTypeIsRegistered() {
        assertTrue(hasNodeType(COMMENT_NODE_ID),
                COMMENT_NODE_ID + " is not registered: the moveNode behavioral tests would silently skip");
    }

    @Test
    void smoke_textBoxNodeTypeIsRegistered() {
        assertTrue(hasNodeType(TEXT_BOX_NODE_ID),
                TEXT_BOX_NODE_ID + " is not registered: the moveNode canvas note test would silently skip");
    }

    @Test
    void moveNodeWritesGridLocation() {
        assumeTrue(hasNodeType(COMMENT_NODE_ID), COMMENT_NODE_ID + " node type not registered - skipping");
        JIPipeProject project = createProject();
        AutomationContext ctx = createContext(project);
        JIPipeProjectCompartment compartment = onlyCompartment(project);
        // The UUID string is the canonical compartment reference: resolveCompartmentUUID
        // resolves UUIDs and aliases, and the generated alias of a compartment named
        // "Notes" is the lowercase "notes", so the display name would not resolve
        String alias = JIPipeAutomationAPI.addNode(ctx, compartment.getProjectCompartmentUUID().toString(), COMMENT_NODE_ID, 0, 0);

        JsonNode result = new ModificationOperations.MoveNode().execute(ctx, moveNodeParams(alias, 5, 7));

        JIPipeGraphNode node = project.getGraph().findNode(alias);
        assertNotNull(node, "the moved node must stay in the graph");
        Point stored = node.getNodeUILocationWithin(compartment.getProjectCompartmentUUID().toString());
        assertNotNull(stored, "the move must store a location");
        assertEquals(5, stored.x, "the stored grid location x");
        assertEquals(7, stored.y, "the stored grid location y");
        assertEquals(alias, result.get("node").asText(), "the response node must be the alias");
        assertNotNull(result.get("uuid"), "the response must carry the uuid");
        assertFalse(result.get("uuid").isNull(), "the uuid must not be JSON null");
        assertEquals(compartment.getProjectCompartmentUUID().toString(), result.get("compartment").asText());
        assertEquals(5, result.get("x").asInt());
        assertEquals(7, result.get("y").asInt());
        assertEquals(0, result.get("previous").get("x").asInt(), "previous.x must report the location before the move");
        assertEquals(0, result.get("previous").get("y").asInt(), "previous.y must report the location before the move");
    }

    @Test
    void moveNodeWithoutStoredLocationReportsNullPrevious() {
        assumeTrue(hasNodeType(COMMENT_NODE_ID), COMMENT_NODE_ID + " node type not registered - skipping");
        JIPipeProject project = createProject();
        AutomationContext ctx = createContext(project);
        JIPipeProjectCompartment compartment = onlyCompartment(project);
        JIPipeGraphNode node = JIPipe.createNode(COMMENT_NODE_ID);
        project.getGraph().insertNode(node, compartment.getProjectCompartmentUUID());
        assertNull(node.getNodeUILocationWithin(compartment.getProjectCompartmentUUID().toString()),
                "precondition: insertNode must not set a location");

        JsonNode result = JIPipeAutomationAPI.moveNode(ctx, project.getGraph().getAliasIdOf(node), 3, 4);

        assertTrue(result.get("previous").isNull(),
                "a node without a stored location must report previous as JSON null: " + result);
        Point stored = node.getNodeUILocationWithin(compartment.getProjectCompartmentUUID().toString());
        assertNotNull(stored, "the move must store a location");
        assertEquals(3, stored.x, "the stored grid location x");
        assertEquals(4, stored.y, "the stored grid location y");
        assertEquals(3, result.get("x").asInt());
        assertEquals(4, result.get("y").asInt());
    }

    @Test
    void moveNodeUnknownNodeReturnsError() {
        JIPipeProject project = createProject();
        AutomationContext ctx = createContext(project);

        JsonNode result = new ModificationOperations.MoveNode().execute(ctx, moveNodeParams("missing", 1, 2));

        assertTrue(result.has("error"), "an unknown node must yield the error envelope: " + result);
        assertEquals("Node not found: missing", result.get("error").asText());
    }

    @Test
    void moveNodeMovesCanvasNotes() {
        assumeTrue(hasNodeType(TEXT_BOX_NODE_ID), TEXT_BOX_NODE_ID + " node type not registered - skipping");
        JIPipeProject project = createProject();
        AutomationContext ctx = createContext(project);
        JIPipeProjectCompartment compartment = onlyCompartment(project);
        // The UUID string is the canonical compartment reference (see moveNodeWritesGridLocation)
        String alias = JIPipeAutomationAPI.addNode(ctx, compartment.getProjectCompartmentUUID().toString(), TEXT_BOX_NODE_ID, 10, 10);

        JsonNode result = JIPipeAutomationAPI.moveNode(ctx, alias, 12, 9);

        JIPipeGraphNode note = project.getGraph().findNode(alias);
        assertNotNull(note, "the moved note must stay in the graph");
        assertTrue(note instanceof JIPipeGraphCanvasNote, "the moved node must be a canvas note");
        Point stored = note.getNodeUILocationWithin(compartment.getProjectCompartmentUUID().toString());
        assertNotNull(stored, "the move must store a location");
        assertEquals(12, stored.x, "the stored grid location x of the note");
        assertEquals(9, stored.y, "the stored grid location y of the note");
        assertEquals(12, result.get("x").asInt());
        assertEquals(9, result.get("y").asInt());
        assertEquals(10, result.get("previous").get("x").asInt(), "previous.x must report the note location before the move");
        assertEquals(10, result.get("previous").get("y").asInt(), "previous.y must report the note location before the move");
    }

    @Test
    void createNoteTextBoxStoresEverything() {
        assumeTrue(hasNodeType(TEXT_BOX_NODE_ID), TEXT_BOX_NODE_ID + " node type not registered - skipping");
        JIPipeProject project = createProject();
        AutomationContext ctx = createContext(project);
        JIPipeProjectCompartment compartment = onlyCompartment(project);

        ObjectNode params = createNoteParams(compartment.getProjectCompartmentUUID().toString());
        params.put("kind", "text-box");
        params.put("title", "T");
        params.put("content", "C");
        params.put("x", 2);
        params.put("y", 3);
        params.put("width", 6);
        params.put("height", 5);

        JsonNode result = new ModificationOperations.CreateNote().execute(ctx, params);

        JIPipeGraphNode node = singleNodeInCompartment(project, compartment);
        assertEquals(TEXT_BOX_NODE_ID, node.getInfo().getId(), "the created node must be a text box");
        Map<String, JIPipeParameterAccess> parameters = JIPipeParameterTree.getParameters(node);
        assertEquals("T", parameters.get("text-title").get(String.class), "the stored text-title");
        assertEquals("C", parameters.get("text-content").get(String.class), "the stored text-content");
        JIPipeGraphCanvasNote note = (JIPipeGraphCanvasNote) node;
        assertEquals(6, note.getGridWidth(), "the stored grid-width");
        assertEquals(5, note.getGridHeight(), "the stored grid-height");
        Point stored = node.getNodeUILocationWithin(compartment.getProjectCompartmentUUID().toString());
        assertNotNull(stored, "the creation must store a location");
        assertEquals(2, stored.x, "the stored grid location x");
        assertEquals(3, stored.y, "the stored grid location y");
        assertEquals(project.getGraph().getAliasIdOf(node), result.get("nodeId").asText(), "the response nodeId must be the alias");
        assertEquals(project.getGraph().getUUIDOf(node).toString(), result.get("uuid").asText(), "the response uuid must be the node UUID");
        assertEquals("text-box", result.get("kind").asText());
        assertEquals("T", result.get("title").asText());
        assertEquals(2, result.get("x").asInt());
        assertEquals(3, result.get("y").asInt());
        assertEquals(6, result.get("width").asInt());
        assertEquals(5, result.get("height").asInt());
    }

    @Test
    void createNoteCommentUsesTitleAsName() {
        assumeTrue(hasNodeType(COMMENT_NODE_ID), COMMENT_NODE_ID + " node type not registered - skipping");
        JIPipeProject project = createProject();
        AutomationContext ctx = createContext(project);
        JIPipeProjectCompartment compartment = onlyCompartment(project);

        ObjectNode params = createNoteParams(compartment.getProjectCompartmentUUID().toString());
        params.put("kind", "comment");
        params.put("title", "Heads up");

        JsonNode result = new ModificationOperations.CreateNote().execute(ctx, params);

        JIPipeGraphNode node = singleNodeInCompartment(project, compartment);
        assertEquals(COMMENT_NODE_ID, node.getInfo().getId(), "the created node must be a comment");
        assertEquals("Heads up", node.getName(), "the comment title must be the node name");
        assertEquals("comment", result.get("kind").asText());
        assertEquals("Heads up", result.get("title").asText(), "the response title must be the node name");
        assertFalse(result.has("width"), "a comment response must not carry width");
        assertFalse(result.has("height"), "a comment response must not carry height");
    }

    @Test
    void createNoteDefaults() {
        assumeTrue(hasNodeType(TEXT_BOX_NODE_ID), TEXT_BOX_NODE_ID + " node type not registered - skipping");
        JIPipeProject project = createProject();
        AutomationContext ctx = createContext(project);
        JIPipeProjectCompartment compartment = onlyCompartment(project);

        JsonNode result = new ModificationOperations.CreateNote().execute(ctx,
                createNoteParams(compartment.getProjectCompartmentUUID().toString()));

        JIPipeGraphNode node = singleNodeInCompartment(project, compartment);
        Map<String, JIPipeParameterAccess> parameters = JIPipeParameterTree.getParameters(node);
        assertEquals("Text box", parameters.get("text-title").get(String.class), "the default text-title");
        assertEquals("", parameters.get("text-content").get(String.class), "the default text-content");
        JIPipeGraphCanvasNote note = (JIPipeGraphCanvasNote) node;
        assertEquals(4, note.getGridWidth(), "the default grid-width");
        assertEquals(3, note.getGridHeight(), "the default grid-height");
        Point stored = node.getNodeUILocationWithin(compartment.getProjectCompartmentUUID().toString());
        assertNotNull(stored, "the creation must store a location");
        assertEquals(0, stored.x, "the default grid location x");
        assertEquals(0, stored.y, "the default grid location y");
        assertEquals("text-box", result.get("kind").asText(), "the default kind");
        assertEquals("Text box", result.get("title").asText(), "the default title");
        assertEquals(0, result.get("x").asInt(), "the default x");
        assertEquals(0, result.get("y").asInt(), "the default y");
        assertEquals(4, result.get("width").asInt(), "the default width");
        assertEquals(3, result.get("height").asInt(), "the default height");
    }

    @Test
    void createNoteContentOnCommentFails() {
        JIPipeProject project = createProject();
        AutomationContext ctx = createContext(project);
        JIPipeProjectCompartment compartment = onlyCompartment(project);

        ObjectNode params = createNoteParams(compartment.getProjectCompartmentUUID().toString());
        params.put("kind", "comment");
        params.put("content", "nope");

        JsonNode result = new ModificationOperations.CreateNote().execute(ctx, params);

        assertTrue(result.has("error"), "content on a comment must yield the error envelope: " + result);
        String error = result.get("error").asText();
        assertTrue(error.contains("content"), "the error must name the offending parameter: " + error);
        assertTrue(error.contains("only supported for kind text-box"), "the error must state the restriction: " + error);
        assertTrue(project.getGraph().getNodesWithinCompartment(compartment.getProjectCompartmentUUID()).isEmpty(),
                "a refused call must not create a node");
    }

    @Test
    void createNoteWidthOnCommentFails() {
        JIPipeProject project = createProject();
        AutomationContext ctx = createContext(project);
        JIPipeProjectCompartment compartment = onlyCompartment(project);

        ObjectNode params = createNoteParams(compartment.getProjectCompartmentUUID().toString());
        params.put("kind", "comment");
        params.put("width", 5);

        JsonNode result = new ModificationOperations.CreateNote().execute(ctx, params);

        assertTrue(result.has("error"), "width on a comment must yield the error envelope: " + result);
        String error = result.get("error").asText();
        assertTrue(error.contains("width"), "the error must name the offending parameter: " + error);
        assertTrue(error.contains("only supported for kind text-box"), "the error must state the restriction: " + error);
        assertTrue(project.getGraph().getNodesWithinCompartment(compartment.getProjectCompartmentUUID()).isEmpty(),
                "a refused call must not create a node");
    }

    @Test
    void createNoteOptsOutOfSchemaDefaultApplication() {
        // The create_note schema declares width/height defaults (for the text-box kind), but
        // the execute body passes absent keys as null on purpose, so createNote can refuse
        // text-box-only parameters on a comment. If the registry injected the defaults, every
        // comment creation without width/height would fail with "width is only supported for
        // kind text-box". This pin guards the opt-out contract.
        assertFalse(new ModificationOperations.CreateNote().appliesSchemaDefaults(),
                "create_note must see parameter absence, not injected schema defaults");
    }

    @Test
    void createNoteUnknownCompartmentReturnsError() {
        JIPipeProject project = createProject();
        AutomationContext ctx = createContext(project);

        JsonNode result = new ModificationOperations.CreateNote().execute(ctx, createNoteParams("Nope"));

        assertTrue(result.has("error"), "an unknown compartment must yield the error envelope: " + result);
        assertEquals("Compartment not found: Nope", result.get("error").asText());
    }

    @Test
    void createNotePublishesOneEventWithFinalTitle() {
        assumeTrue(hasNodeType(TEXT_BOX_NODE_ID), TEXT_BOX_NODE_ID + " node type not registered - skipping");
        JIPipeProject project = createProject();
        AutomationContext ctx = createContext(project);
        JIPipeProjectCompartment compartment = onlyCompartment(project);
        List<NodeAddedEvent> events = new ArrayList<>();
        ctx.getEventBus().subscribe(NodeAddedEvent.class, events::add);

        ObjectNode params = createNoteParams(compartment.getProjectCompartmentUUID().toString());
        params.put("kind", "text-box");
        params.put("title", "Final title");

        JsonNode result = new ModificationOperations.CreateNote().execute(ctx, params);

        assertEquals(1, events.size(), "the creation must publish exactly one NodeAddedEvent");
        NodeAddedEvent event = events.get(0);
        assertEquals("Final title", event.getNodeName(),
                "the event must carry the final title, not the constructor default");
        assertEquals(compartment.getProjectCompartmentUUID().toString(), event.getCompartmentId(),
                "the event must name the compartment of the created note");
        assertEquals(TEXT_BOX_NODE_ID, event.getNodeTypeId(), "the event must name the node type");
        assertEquals(result.get("nodeId").asText(), event.getNodeId(), "the event node id must be the created note's alias");
    }
}
