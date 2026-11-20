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
import org.hkijena.jipipe.api.compartments.algorithms.JIPipeProjectCompartment;
import org.hkijena.jipipe.api.nodes.JIPipeGraphNode;
import org.hkijena.jipipe.api.nodes.JIPipeNodeInfo;
import org.hkijena.jipipe.api.nodes.infos.JIPipeJavaNodeInfo;
import org.hkijena.jipipe.api.nodes.util.DummyAlgorithm;
import org.hkijena.jipipe.api.project.JIPipeProject;
import org.hkijena.jipipe.plugins.canvasnotes.nodes.TextBoxGraphCanvasNote;
import org.hkijena.jipipe.plugins.core.nodes.JIPipeCommentNode;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.awt.Point;
import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

/**
 * Tests the arrange_nodes guards: the preview default (apply absent or false
 * returns every computed position but writes no stored location), the apply
 * mode (the returned position of each algorithm node equals its stored
 * location), and the exclusion of canvas notes and comments from the layout
 * (absent from the positions, stored location untouched). The compartment
 * holds two connected algorithm nodes at hand positions plus a text box note
 * and a comment at their own positions.
 */
public class ArrangeOperationsTest {

    private static final String COMMENT_NODE_ID = "jipipe:comment";
    private static final String TEXT_BOX_NODE_ID = "graph-annotation-text-box";
    private static final String DUMMY_ALGORITHM_NODE_ID = "jipipe:graph-annotation-dummy-algorithm";
    private static final JIPipeMutableDependency TEST_DEPENDENCY =
            new JIPipeMutableDependency("arrange-operations-test", "1.0.0", "arrange-operations-test");

    private static final ObjectMapper mapper = new ObjectMapper();

    private static boolean registeredCommentNodeHere;
    private static boolean registeredTextBoxNodeHere;
    private static boolean registeredDummyAlgorithmHere;

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
        // Two algorithm nodes connected into a chain are the smallest layout
        // that exercises a two-column topological layering. No production node
        // type on this test classpath is guaranteed to have both an input and
        // an output slot, so the test registers a minimal concrete algorithm
        // for that purpose (see the DummyAlgorithm in the production tree).
        if (!JIPipe.getNodes().hasNodeInfoWithId(DUMMY_ALGORITHM_NODE_ID)) {
            JIPipe.getNodes().register(new JIPipeJavaNodeInfo(DUMMY_ALGORITHM_NODE_ID, DummyAlgorithm.class), TEST_DEPENDENCY);
            registeredDummyAlgorithmHere = true;
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
        if (registeredDummyAlgorithmHere) {
            JIPipe.getNodes().unregister(DUMMY_ALGORITHM_NODE_ID);
            registeredDummyAlgorithmHere = false;
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
     * A compartment with two connected algorithm nodes placed at hand
     * positions, a text box note, and a comment, each at their own position.
     */
    private record Fixture(JIPipeProject project, AutomationContext ctx, JIPipeProjectCompartment compartment,
                           String firstAlias, String secondAlias, String noteAlias, String commentAlias) {
    }

    private static Fixture createFixture() {
        JIPipeProject project = new JIPipeProject();
        JIPipeProjectCompartment compartment = project.addCompartment("Arrange Guard Test");
        AutomationContext ctx = createContext(project);
        String compartmentId = compartment.getProjectCompartmentUUID().toString();
        String firstAlias = JIPipeAutomationAPI.addNode(ctx, compartmentId, DUMMY_ALGORITHM_NODE_ID, 0, 0);
        String secondAlias = JIPipeAutomationAPI.addNode(ctx, compartmentId, DUMMY_ALGORITHM_NODE_ID, 10, 0);
        String noteAlias = JIPipeAutomationAPI.addNode(ctx, compartmentId, TEXT_BOX_NODE_ID, 20, 20);
        String commentAlias = JIPipeAutomationAPI.addNode(ctx, compartmentId, COMMENT_NODE_ID, 30, 30);
        JIPipeGraphNode first = project.getGraph().findNode(firstAlias);
        JIPipeGraphNode second = project.getGraph().findNode(secondAlias);
        project.getGraph().connect(first.getOutputSlots().get(0), second.getInputSlots().get(0));
        return new Fixture(project, ctx, compartment, firstAlias, secondAlias, noteAlias, commentAlias);
    }

    private static ObjectNode arrangeParams(String compartment, Boolean apply) {
        ObjectNode params = mapper.createObjectNode();
        params.put("compartment", compartment);
        if (apply != null) {
            params.put("apply", apply);
        }
        return params;
    }

    private static String compartmentRef(Fixture fixture) {
        return fixture.compartment().getProjectCompartmentUUID().toString();
    }

    /**
     * The stored grid location of a node in the fixture compartment
     */
    private static Point storedLocation(Fixture fixture, String alias) {
        JIPipeGraphNode node = fixture.project().getGraph().findNode(alias);
        assertNotNull(node, "the node " + alias + " must stay in the graph");
        return node.getNodeUILocationWithin(compartmentRef(fixture));
    }

    /**
     * The position returned for a node alias, or null when the alias is not
     * part of the response
     */
    private static JsonNode returnedPosition(JsonNode result, String alias) {
        for (JsonNode position : result.get("positions")) {
            if (alias.equals(position.get("node").asText())) {
                return position;
            }
        }
        return null;
    }

    /**
     * Snapshot of the stored location of every node in the fixture
     */
    private static Map<String, Point> storedLocations(Fixture fixture) {
        Map<String, Point> locations = new HashMap<>();
        for (String alias : new String[]{fixture.firstAlias(), fixture.secondAlias(),
                fixture.noteAlias(), fixture.commentAlias()}) {
            locations.put(alias, storedLocation(fixture, alias));
        }
        return locations;
    }

    @Test
    void smoke_nodeTypesAreRegistered() {
        assertTrue(hasNodeType(COMMENT_NODE_ID),
                COMMENT_NODE_ID + " is not registered: the arrange guard tests would silently skip");
        assertTrue(hasNodeType(TEXT_BOX_NODE_ID),
                TEXT_BOX_NODE_ID + " is not registered: the arrange guard tests would silently skip");
        assertTrue(hasNodeType(DUMMY_ALGORITHM_NODE_ID),
                DUMMY_ALGORITHM_NODE_ID + " is not registered: the arrange guard tests would silently skip");
    }

    @Test
    void arrangePreviewWritesNothing() {
        assumeTrue(hasNodeType(COMMENT_NODE_ID) && hasNodeType(TEXT_BOX_NODE_ID)
                        && hasNodeType(DUMMY_ALGORITHM_NODE_ID),
                "node types not registered - skipping");

        Fixture fixture = createFixture();
        Map<String, Point> before = storedLocations(fixture);

        JsonNode result = new ArrangeOperations.ArrangeNodes().execute(fixture.ctx(),
                arrangeParams(compartmentRef(fixture), null));

        assertFalse(result.has("error"), () -> "a valid compartment must not produce an error: " + result);
        assertFalse(result.get("applied").asBoolean(), "a preview (apply absent) must report applied == false");
        JsonNode positions = result.get("positions");
        assertNotNull(positions, "a preview must still return the computed positions");
        assertEquals(2, positions.size(), "only the two algorithm nodes may appear in the positions");
        assertNotNull(returnedPosition(result, fixture.firstAlias()), "the first algorithm node must have a position");
        assertNotNull(returnedPosition(result, fixture.secondAlias()), "the second algorithm node must have a position");

        // No stored location may change: the preview is not allowed to write
        // a single location
        for (Map.Entry<String, Point> entry : storedLocations(fixture).entrySet()) {
            Point expected = before.get(entry.getKey());
            assertNotNull(entry.getValue(), "the preview must not remove a stored location of " + entry.getKey());
            assertEquals(expected.x, entry.getValue().x, "the preview must not move " + entry.getKey() + " in x");
            assertEquals(expected.y, entry.getValue().y, "the preview must not move " + entry.getKey() + " in y");
        }

        // The returned positions must be the computed layout, not a restatement
        // of the hand positions: at least one node must be proposed elsewhere
        boolean differs = false;
        for (Map.Entry<String, Point> entry : before.entrySet()) {
            JsonNode position = returnedPosition(result, entry.getKey());
            if (position != null && (position.get("x").asInt() != entry.getValue().x
                    || position.get("y").asInt() != entry.getValue().y)) {
                differs = true;
            }
        }
        assertTrue(differs, "the preview must propose positions different from the hand positions");
    }

    @Test
    void arrangePreviewApplyFalseWritesNothing() {
        assumeTrue(hasNodeType(COMMENT_NODE_ID) && hasNodeType(TEXT_BOX_NODE_ID)
                        && hasNodeType(DUMMY_ALGORITHM_NODE_ID),
                "node types not registered - skipping");

        Fixture fixture = createFixture();
        Map<String, Point> before = storedLocations(fixture);

        JsonNode result = new ArrangeOperations.ArrangeNodes().execute(fixture.ctx(),
                arrangeParams(compartmentRef(fixture), false));

        assertFalse(result.has("error"), () -> "a valid compartment must not produce an error: " + result);
        assertFalse(result.get("applied").asBoolean(), "apply == false must report applied == false");
        for (Map.Entry<String, Point> entry : storedLocations(fixture).entrySet()) {
            Point expected = before.get(entry.getKey());
            assertEquals(expected.x, entry.getValue().x, "apply == false must not move " + entry.getKey() + " in x");
            assertEquals(expected.y, entry.getValue().y, "apply == false must not move " + entry.getKey() + " in y");
        }
    }

    @Test
    void arrangeApplyWritesPositions() {
        assumeTrue(hasNodeType(COMMENT_NODE_ID) && hasNodeType(TEXT_BOX_NODE_ID)
                        && hasNodeType(DUMMY_ALGORITHM_NODE_ID),
                "node types not registered - skipping");

        Fixture fixture = createFixture();

        JsonNode result = new ArrangeOperations.ArrangeNodes().execute(fixture.ctx(),
                arrangeParams(compartmentRef(fixture), true));

        assertFalse(result.has("error"), () -> "a valid compartment must not produce an error: " + result);
        assertTrue(result.get("applied").asBoolean(), "apply == true must report applied == true");
        JsonNode positions = result.get("positions");
        assertNotNull(positions, "apply == true must return the positions");

        // Each algorithm node's stored location must equal its returned
        // position: two connected nodes must end up in two columns
        for (String alias : new String[]{fixture.firstAlias(), fixture.secondAlias()}) {
            JsonNode position = returnedPosition(result, alias);
            assertNotNull(position, "the algorithm node " + alias + " must have a returned position");
            Point stored = storedLocation(fixture, alias);
            assertNotNull(stored, "the apply must store a location of " + alias);
            assertEquals(position.get("x").asInt(), stored.x, "the stored location x of " + alias);
            assertEquals(position.get("y").asInt(), stored.y, "the stored location y of " + alias);
        }
        int xFirst = returnedPosition(result, fixture.firstAlias()).get("x").asInt();
        int xSecond = returnedPosition(result, fixture.secondAlias()).get("x").asInt();
        assertTrue(xFirst < xSecond, "the chain source must be left of its successor");
    }

    @Test
    void arrangeExcludesNotes() {
        assumeTrue(hasNodeType(COMMENT_NODE_ID) && hasNodeType(TEXT_BOX_NODE_ID)
                        && hasNodeType(DUMMY_ALGORITHM_NODE_ID),
                "node types not registered - skipping");

        Fixture fixture = createFixture();
        Point noteBefore = storedLocation(fixture, fixture.noteAlias());
        Point commentBefore = storedLocation(fixture, fixture.commentAlias());

        JsonNode result = new ArrangeOperations.ArrangeNodes().execute(fixture.ctx(),
                arrangeParams(compartmentRef(fixture), true));

        assertFalse(result.has("error"), () -> "a valid compartment must not produce an error: " + result);
        assertTrue(result.get("applied").asBoolean(), "apply == true must report applied == true");

        // The note and the comment are absent from the positions ...
        assertEquals(2, result.get("positions").size(),
                "only the two algorithm nodes may appear in the positions");
        assertNull(returnedPosition(result, fixture.noteAlias()),
                "the text box note must not appear in the positions");
        assertNull(returnedPosition(result, fixture.commentAlias()),
                "the comment must not appear in the positions");

        // ... and keep their stored locations even in apply mode
        Point noteAfter = storedLocation(fixture, fixture.noteAlias());
        Point commentAfter = storedLocation(fixture, fixture.commentAlias());
        assertNotNull(noteAfter, "the note must keep its stored location");
        assertEquals(noteBefore.x, noteAfter.x, "the text box note must keep its stored location x");
        assertEquals(noteBefore.y, noteAfter.y, "the text box note must keep its stored location y");
        assertNotNull(commentAfter, "the comment must keep its stored location");
        assertEquals(commentBefore.x, commentAfter.x, "the comment must keep its stored location x");
        assertEquals(commentBefore.y, commentAfter.y, "the comment must keep its stored location y");
    }

    @Test
    void arrangeUnknownCompartmentReturnsError() {
        Fixture fixture = createFixture();
        JsonNode result = new ArrangeOperations.ArrangeNodes().execute(fixture.ctx(),
                arrangeParams("No Such Compartment", true));
        assertTrue(result.has("error"), "an unknown compartment must return an error field");
    }
}
