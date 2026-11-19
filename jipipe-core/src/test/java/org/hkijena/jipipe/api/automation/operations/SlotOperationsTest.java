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

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.hkijena.jipipe.JIPipe;
import org.hkijena.jipipe.JIPipeMutableDependency;
import org.hkijena.jipipe.api.data.JIPipeData;
import org.hkijena.jipipe.api.data.JIPipeDataSlot;
import org.hkijena.jipipe.api.data.JIPipeDataSlotInfo;
import org.hkijena.jipipe.api.data.JIPipeDefaultMutableSlotConfiguration;
import org.hkijena.jipipe.api.data.JIPipeMutableSlotConfiguration;
import org.hkijena.jipipe.api.data.JIPipeSlotConfiguration;
import org.hkijena.jipipe.api.data.JIPipeSlotType;
import org.hkijena.jipipe.api.automation.AutomationContext;
import org.hkijena.jipipe.api.automation.InstrumentationEventBus;
import org.hkijena.jipipe.api.automation.InstrumentationJobManager;
import org.hkijena.jipipe.api.JIPipeProgressInfo;
import org.hkijena.jipipe.api.nodes.JIPipeGraphNode;
import org.hkijena.jipipe.api.nodes.JIPipeGraphNodeRunContext;
import org.hkijena.jipipe.api.nodes.JIPipeGraph;
import org.hkijena.jipipe.api.nodes.JIPipeNodeInfo;
import org.hkijena.jipipe.api.nodes.infos.JIPipeJavaNodeInfo;
import org.hkijena.jipipe.api.compartments.algorithms.JIPipeProjectCompartment;
import org.hkijena.jipipe.api.project.JIPipeProject;
import org.hkijena.jipipe.plugins.core.nodes.JIPipeCommentNode;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Tests the connection-safe slot editing operations. add_slot and remove_slot
 * edit the slot configuration of a node through the gate order of the user
 * ruling, and connections are never dropped implicitly: a remove_slot that
 * would touch a connected slot is refused with the connection endpoints and
 * the pointer to remove_connection, so disconnecting is always an explicit
 * agent step. The refusals are error envelopes, in particular the sealed
 * configuration case, which must be caught before any mutation (the mutable
 * configuration itself would throw a JIPipeValidationRuntimeException).
 */
public class SlotOperationsTest {

    private static final String COMMENT_NODE_ID = "jipipe:comment";
    private static final String FIXED_SLOT_NODE_ID = "slot-operations-test:fixed-slots";
    private static final String SEALED_SLOT_NODE_ID = "slot-operations-test:sealed-slots";
    private static final String DATA_TYPE_ID = "jipipe:data";
    private static final JIPipeMutableDependency TEST_DEPENDENCY =
            new JIPipeMutableDependency("slot-operations-test", "1.0.0",
                    "slot-operations-test");

    private static final ObjectMapper mapper = new ObjectMapper();

    private static boolean registeredCommentNodeHere;
    private static boolean registeredFixedSlotNodeHere;
    private static boolean registeredSealedSlotNodeHere;

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
        // The fixed-slots fixture node is a test-only type: its slot
        // configuration is a non-mutable JIPipeSlotConfiguration stub, which
        // pins the "node does not support slot editing" refusal of both slot
        // operations. It extends JIPipeGraphNode directly because the fixture
        // needs the slot-configuration constructor of the graph node (the
        // comment node does not provide one); the id cannot collide with a
        // real registration.
        if (!JIPipe.getNodes().hasNodeInfoWithId(FIXED_SLOT_NODE_ID)) {
            JIPipe.getNodes().register(new JIPipeJavaNodeInfo(FIXED_SLOT_NODE_ID, FixedSlotNode.class), TEST_DEPENDENCY);
            registeredFixedSlotNodeHere = true;
        }
        // The sealed-slots fixture node is a test-only type: its mutable
        // configuration is sealed on both sides (the setters setInputSealed
        // and setOutputSealed of JIPipeDefaultMutableSlotConfiguration), which
        // pins that add_slot refuses a sealed side BEFORE any mutation (the
        // configuration itself would throw on a user add). It extends
        // JIPipeGraphNode directly because the fixture needs the
        // slot-configuration constructor of the graph node (the comment node
        // does not provide one); the id cannot collide with a real
        // registration.
        if (!JIPipe.getNodes().hasNodeInfoWithId(SEALED_SLOT_NODE_ID)) {
            JIPipe.getNodes().register(new JIPipeJavaNodeInfo(SEALED_SLOT_NODE_ID, SealedSlotNode.class), TEST_DEPENDENCY);
            registeredSealedSlotNodeHere = true;
        }
    }

    @AfterAll
    static void cleanup() {
        if (registeredCommentNodeHere) {
            JIPipe.getNodes().unregister(COMMENT_NODE_ID);
            registeredCommentNodeHere = false;
        }
        if (registeredFixedSlotNodeHere) {
            JIPipe.getNodes().unregister(FIXED_SLOT_NODE_ID);
            registeredFixedSlotNodeHere = false;
        }
        if (registeredSealedSlotNodeHere) {
            JIPipe.getNodes().unregister(SEALED_SLOT_NODE_ID);
            registeredSealedSlotNodeHere = false;
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

    private static ObjectNode addSlotParams(String nodeId, String slotType, String name, String dataType) {
        ObjectNode params = mapper.createObjectNode();
        params.put("node-id", nodeId);
        params.put("slot-type", slotType);
        params.put("name", name);
        params.put("data-type", dataType);
        return params;
    }

    private static ObjectNode removeSlotParams(String nodeId, String slotType, String name) {
        ObjectNode params = mapper.createObjectNode();
        params.put("node-id", nodeId);
        params.put("slot-type", slotType);
        params.put("name", name);
        return params;
    }

    private static ObjectNode removeConnectionParams(String source, String sourceSlot, String target, String targetSlot) {
        ObjectNode params = mapper.createObjectNode();
        params.put("source", source);
        params.put("source-slot", sourceSlot);
        params.put("target", target);
        params.put("target-slot", targetSlot);
        return params;
    }

    /**
     * Creates a comment node whose slot configuration is editable: the
     * annotation-built configuration of the comment node is mutable (a
     * JIPipeDefaultMutableSlotConfiguration), but JIPipeGraphNode seals it
     * after building it from the annotations, so the fixture unseals both
     * sides to model a node type whose slots are editable (the state that
     * nodes with a custom unsealed configuration provide, e.g. the input side
     * of parameters-merge).
     */
    private static JIPipeGraphNode newEditableCommentNode() {
        JIPipeGraphNode node = JIPipe.getNodes().getInfoById(COMMENT_NODE_ID).newInstance();
        JIPipeDefaultMutableSlotConfiguration configuration =
                (JIPipeDefaultMutableSlotConfiguration) node.getSlotConfiguration();
        configuration.setInputSealed(false);
        configuration.setOutputSealed(false);
        return node;
    }

    /**
     * Creates a project with one compartment and inserts the node into it.
     */
    private static JIPipeProject projectWithNode(JIPipeGraphNode node) {
        JIPipeProject project = new JIPipeProject();
        JIPipeProjectCompartment compartment = project.addCompartment("Test");
        project.getGraph().insertNode(node, compartment.getProjectCompartmentUUID());
        return project;
    }

    private static boolean edgeExists(JIPipeGraph graph, JIPipeGraphNode source, String sourceSlot,
                                      JIPipeGraphNode target, String targetSlot) {
        for (Map.Entry<JIPipeDataSlot, JIPipeDataSlot> edge : graph.getSlotEdges()) {
            if (edge.getKey().getNode() == source && edge.getKey().getName().equals(sourceSlot)
                    && edge.getValue().getNode() == target && edge.getValue().getName().equals(targetSlot)) {
                return true;
            }
        }
        return false;
    }

    @Test
    void smoke_requiredNodeTypesAndDataTypesAreRegistered() {
        assertTrue(hasNodeType(COMMENT_NODE_ID),
                COMMENT_NODE_ID + " is not registered: the slot editing tests would silently skip");
        assertTrue(hasNodeType(FIXED_SLOT_NODE_ID),
                FIXED_SLOT_NODE_ID + " is not registered: the non-mutable refusal tests would silently skip");
        assertTrue(hasNodeType(SEALED_SLOT_NODE_ID),
                SEALED_SLOT_NODE_ID + " is not registered: the sealed refusal tests would silently skip");
        assertTrue(JIPipe.getDataTypes().hasDatatypeWithId(DATA_TYPE_ID),
                DATA_TYPE_ID + " is not registered: the slot data type tests would silently skip");
    }

    @Test
    void addSlot_onMutableNodeAddsSlotAndListShowsIt() {
        JIPipeGraphNode comment = newEditableCommentNode();
        JIPipeProject project = projectWithNode(comment);
        AutomationContext ctx = createContext(project);
        String alias = project.getGraph().getAliasIdOf(comment);

        JsonNode result = new SlotOperations.AddSlot().execute(ctx,
                addSlotParams(alias, "output", "extra", DATA_TYPE_ID));

        assertFalse(result.has("error"), "the add must succeed: " + result);
        assertTrue(result.get("added").asBoolean(), "the response reports added: " + result);
        assertEquals("extra", result.get("name").asText(), "the response reports the name: " + result);
        assertEquals("output", result.get("slotType").asText(), "the response reports the side: " + result);
        JIPipeMutableSlotConfiguration configuration = (JIPipeMutableSlotConfiguration) comment.getSlotConfiguration();
        assertTrue(configuration.hasOutputSlot("extra"), "the configuration contains the slot afterwards");
        assertTrue(comment.getOutputSlots().stream().anyMatch(slot -> slot.getName().equals("extra")),
                "the node slot list shows the added slot");
        assertEquals(DATA_TYPE_ID, JIPipe.getDataTypes().getIdOf(comment.getOutputSlot("extra").getAcceptedDataType()),
                "the added slot accepts the requested data type");
    }

    @Test
    void addSlot_duplicateNameRefused() {
        JIPipeGraphNode comment = newEditableCommentNode();
        JIPipeProject project = projectWithNode(comment);
        AutomationContext ctx = createContext(project);
        String alias = project.getGraph().getAliasIdOf(comment);
        JsonNode first = new SlotOperations.AddSlot().execute(ctx,
                addSlotParams(alias, "output", "extra", DATA_TYPE_ID));
        assertFalse(first.has("error"), "the first add must succeed: " + first);

        JsonNode second = new SlotOperations.AddSlot().execute(ctx,
                addSlotParams(alias, "output", "extra", DATA_TYPE_ID));

        assertTrue(second.has("error"), "the duplicate add must be refused: " + second);
        assertTrue(second.get("error").asText().contains("extra"),
                "the refusal names the slot name: " + second);
        assertTrue(second.get("error").asText().contains("already exists on the output side"),
                "the refusal names the side: " + second);
    }

    @Test
    void addSlot_unknownDataTypeRefusedListingKnown() {
        JIPipeGraphNode comment = newEditableCommentNode();
        JIPipeProject project = projectWithNode(comment);
        AutomationContext ctx = createContext(project);
        String alias = project.getGraph().getAliasIdOf(comment);

        JsonNode result = new SlotOperations.AddSlot().execute(ctx,
                addSlotParams(alias, "output", "extra", "nope"));

        assertTrue(result.has("error"), "the unknown data type must be refused: " + result);
        String error = result.get("error").asText();
        assertTrue(error.contains("nope"), "the refusal names the unknown id: " + error);
        assertTrue(error.contains(DATA_TYPE_ID), "the refusal lists known data type ids: " + error);
        assertFalse(comment.getSlotConfiguration().hasOutputSlot("extra"),
                "nothing was added for an unknown data type");
    }

    @Test
    void addSlot_onNonMutableConfigurationRefused() {
        JIPipeGraphNode fixed = JIPipe.getNodes().getInfoById(FIXED_SLOT_NODE_ID).newInstance();
        JIPipeProject project = projectWithNode(fixed);
        AutomationContext ctx = createContext(project);
        String alias = project.getGraph().getAliasIdOf(fixed);
        assertFalse(fixed.getSlotConfiguration() instanceof JIPipeMutableSlotConfiguration,
                "fixture precondition: the fixed configuration is not mutable");

        JsonNode result = new SlotOperations.AddSlot().execute(ctx,
                addSlotParams(alias, "output", "extra", DATA_TYPE_ID));

        assertTrue(result.has("error"), "the non-mutable configuration must be refused: " + result);
        assertEquals("node does not support slot editing", result.get("error").asText(),
                "the refusal is the slot editing refusal: " + result);
    }

    @Test
    void addSlot_sealedConfigurationRefusedBeforeAnyMutation() {
        JIPipeGraphNode sealed = JIPipe.getNodes().getInfoById(SEALED_SLOT_NODE_ID).newInstance();
        JIPipeProject project = projectWithNode(sealed);
        AutomationContext ctx = createContext(project);
        String alias = project.getGraph().getAliasIdOf(sealed);

        JsonNode result = new SlotOperations.AddSlot().execute(ctx,
                addSlotParams(alias, "output", "extra", DATA_TYPE_ID));

        assertTrue(result.has("error"), "the sealed configuration must be refused: " + result);
        String error = result.get("error").asText();
        assertTrue(error.contains("output"), "the refusal names the sealed side: " + error);
        assertTrue(error.contains("sealed"), "the refusal names the reason: " + error);
        assertFalse(sealed.getSlotConfiguration().hasOutputSlot("extra"),
                "the configuration is unchanged: no slot was added before the refusal");
        assertFalse(sealed.getOutputSlots().stream().anyMatch(slot -> slot.getName().equals("extra")),
                "the node has no added slot after the refusal");
    }

    @Test
    void addSlot_sealedStockNodeRefused() {
        // A fresh comment node keeps its stock state: the annotation-built
        // slot configuration is sealed by the JIPipeGraphNode constructor
        // after building it from the annotations, so the stock node refuses
        // add_slot. No unsealing step: the stock sealed state is the point.
        JIPipeGraphNode comment = JIPipe.getNodes().getInfoById(COMMENT_NODE_ID).newInstance();
        JIPipeProject project = projectWithNode(comment);
        AutomationContext ctx = createContext(project);
        String alias = project.getGraph().getAliasIdOf(comment);

        JsonNode result = new SlotOperations.AddSlot().execute(ctx,
                addSlotParams(alias, "output", "extra", DATA_TYPE_ID));

        assertTrue(result.has("error"), "the sealed stock node must be refused: " + result);
        assertTrue(result.get("error").asText().contains("sealed"),
                "the refusal names the sealed state: " + result);
        assertFalse(comment.getSlotConfiguration().hasOutputSlot("extra"),
                "the configuration is unchanged: the output slot map has no extra slot");
    }

    @Test
    void removeSlot_existingWithoutConnectionsSucceeds() {
        JIPipeGraphNode comment = newEditableCommentNode();
        JIPipeProject project = projectWithNode(comment);
        AutomationContext ctx = createContext(project);
        String alias = project.getGraph().getAliasIdOf(comment);
        JsonNode added = new SlotOperations.AddSlot().execute(ctx,
                addSlotParams(alias, "output", "extra", DATA_TYPE_ID));
        assertFalse(added.has("error"), "precondition: the slot was added: " + added);

        JsonNode result = new SlotOperations.RemoveSlot().execute(ctx,
                removeSlotParams(alias, "output", "extra"));

        assertFalse(result.has("error"), "the removal must succeed: " + result);
        assertTrue(result.get("removed").asBoolean(), "the response reports removed: " + result);
        assertEquals("extra", result.get("name").asText(), "the response reports the name: " + result);
        assertEquals("output", result.get("slotType").asText(), "the response reports the side: " + result);
        assertFalse(comment.getSlotConfiguration().hasOutputSlot("extra"), "the slot is gone");
        assertFalse(comment.getOutputSlots().stream().anyMatch(slot -> slot.getName().equals("extra")),
                "the node slot list no longer shows the slot");
    }

    @Test
    void removeSlot_unknownSlotListsExisting() {
        JIPipeGraphNode comment = newEditableCommentNode();
        JIPipeProject project = projectWithNode(comment);
        AutomationContext ctx = createContext(project);
        String alias = project.getGraph().getAliasIdOf(comment);

        JsonNode result = new SlotOperations.RemoveSlot().execute(ctx,
                removeSlotParams(alias, "output", "nope"));

        assertTrue(result.has("error"), "the unknown slot must be refused: " + result);
        String error = result.get("error").asText();
        assertTrue(error.contains("nope"), "the refusal names the unknown slot: " + error);
        assertTrue(error.contains("Comment"), "the refusal lists the existing output slot names: " + error);
    }

    @Test
    void removeSlot_connectedRefusesListingConnections() {
        JIPipeGraphNode source = newEditableCommentNode();
        JIPipeGraphNode target = newEditableCommentNode();
        source.setCustomName("Source");
        target.setCustomName("Target");
        JIPipeProject project = projectWithNode(source);
        JIPipeProjectCompartment compartment = project.addCompartment("Second");
        project.getGraph().insertNode(target, compartment.getProjectCompartmentUUID());
        project.getGraph().connect(source.getOutputSlot("Comment"), target.getInputSlot("Comment"));
        AutomationContext ctx = createContext(project);
        String sourceAlias = project.getGraph().getAliasIdOf(source);
        assertTrue(edgeExists(project.getGraph(), source, "Comment", target, "Comment"),
                "precondition: the edge exists");

        JsonNode result = new SlotOperations.RemoveSlot().execute(ctx,
                removeSlotParams(sourceAlias, "output", "Comment"));

        assertTrue(result.has("error"), "the connected slot must be refused: " + result);
        String error = result.get("error").asText();
        assertTrue(error.contains("unable to remove slot 'Comment'"), "the refusal names the slot: " + error);
        assertTrue(error.contains("Source (Comment) -> Target (Comment)"),
                "the refusal lists the endpoints as node names with slot names: " + error);
        assertTrue(error.contains("remove_connection"), "the refusal points to remove_connection: " + error);
        assertTrue(source.getSlotConfiguration().hasOutputSlot("Comment"),
                "the slot still exists after the failed call");
        assertTrue(edgeExists(project.getGraph(), source, "Comment", target, "Comment"),
                "the edge still exists after the failed call: nothing was disconnected");
    }

    @Test
    void removeSlot_disconnectThenRemoveSucceeds() {
        JIPipeGraphNode source = newEditableCommentNode();
        JIPipeGraphNode target = newEditableCommentNode();
        JIPipeProject project = projectWithNode(source);
        JIPipeProjectCompartment compartment = project.addCompartment("Second");
        project.getGraph().insertNode(target, compartment.getProjectCompartmentUUID());
        project.getGraph().connect(source.getOutputSlot("Comment"), target.getInputSlot("Comment"));
        AutomationContext ctx = createContext(project);
        String sourceAlias = project.getGraph().getAliasIdOf(source);
        String targetAlias = project.getGraph().getAliasIdOf(target);
        JsonNode disconnected = new ModificationOperations.RemoveConnection().execute(ctx,
                removeConnectionParams(sourceAlias, "Comment", targetAlias, "Comment"));
        assertFalse(disconnected.has("error"), "precondition: the disconnect succeeds: " + disconnected);

        JsonNode result = new SlotOperations.RemoveSlot().execute(ctx,
                removeSlotParams(sourceAlias, "output", "Comment"));

        assertFalse(result.has("error"), "the removal succeeds after the disconnect: " + result);
        assertTrue(result.get("removed").asBoolean(), "the response reports removed: " + result);
        assertFalse(source.getSlotConfiguration().hasOutputSlot("Comment"), "the slot is gone");
        assertFalse(edgeExists(project.getGraph(), source, "Comment", target, "Comment"),
                "no edge remains");
    }

    @Test
    void removeSlot_sameNameOnBothSidesDisambiguated() {
        // The comment node carries an input slot "Comment" and an output slot
        // "Comment": the same name on both sides (Review Focus 5)
        JIPipeGraphNode comment = newEditableCommentNode();
        JIPipeProject project = projectWithNode(comment);
        AutomationContext ctx = createContext(project);
        String alias = project.getGraph().getAliasIdOf(comment);
        assertTrue(comment.getSlotConfiguration().hasInputSlot("Comment")
                        && comment.getSlotConfiguration().hasOutputSlot("Comment"),
                "fixture precondition: the name exists on both sides");

        JsonNode result = new SlotOperations.RemoveSlot().execute(ctx,
                removeSlotParams(alias, "input", "Comment"));

        assertFalse(result.has("error"), "the removal must succeed: " + result);
        assertEquals("input", result.get("slotType").asText(), "the response reports the removed side: " + result);
        assertFalse(comment.getSlotConfiguration().hasInputSlot("Comment"), "the input slot is gone");
        assertTrue(comment.getSlotConfiguration().hasOutputSlot("Comment"),
                "the output slot with the same name remains");
    }

    /**
     * A fixed, non-mutable slot configuration: implements the
     * {@link JIPipeSlotConfiguration} interface with one input and one output
     * slot and no edit operations.
     */
    public static class FixedSlotConfiguration implements JIPipeSlotConfiguration {

        private final Map<String, JIPipeDataSlotInfo> inputSlots = new LinkedHashMap<>();
        private final Map<String, JIPipeDataSlotInfo> outputSlots = new LinkedHashMap<>();
        private final SlotConfigurationChangedEventEmitter emitter = new SlotConfigurationChangedEventEmitter();

        public FixedSlotConfiguration() {
            inputSlots.put("Input", new JIPipeDataSlotInfo(JIPipeData.class, JIPipeSlotType.Input, "Input", "Fixed fixture input"));
            outputSlots.put("Output", new JIPipeDataSlotInfo(JIPipeData.class, JIPipeSlotType.Output, "Output", "Fixed fixture output"));
        }

        @Override
        public JIPipeSlotConfiguration duplicate() {
            return new FixedSlotConfiguration();
        }

        @Override
        public List<String> getInputSlotOrder() {
            return new ArrayList<>(inputSlots.keySet());
        }

        @Override
        public List<String> getOutputSlotOrder() {
            return new ArrayList<>(outputSlots.keySet());
        }

        @Override
        public SlotConfigurationChangedEventEmitter getSlotConfigurationChangedEventEmitter() {
            return emitter;
        }

        @Override
        public Map<String, JIPipeDataSlotInfo> getInputSlots() {
            return inputSlots;
        }

        @Override
        public Map<String, JIPipeDataSlotInfo> getOutputSlots() {
            return outputSlots;
        }

        @Override
        public void setTo(JIPipeSlotConfiguration configuration) {
            throw new UnsupportedOperationException("The fixture configuration is fixed");
        }

        @Override
        public void fromJson(JsonNode jsonNode) {
            throw new UnsupportedOperationException("The fixture configuration is fixed");
        }

        @Override
        public void toJson(JsonGenerator jsonGenerator) {
            throw new UnsupportedOperationException("The fixture configuration is fixed");
        }
    }

    /**
     * Test-only node type whose slot configuration is the fixed, non-mutable
     * {@link FixedSlotConfiguration}: pins the "node does not support slot
     * editing" refusal. Extends JIPipeGraphNode directly because the fixture
     * needs the slot-configuration constructor of the graph node (the comment
     * node does not provide one); run() is empty like the comment node's.
     */
    public static class FixedSlotNode extends JIPipeGraphNode {

        public FixedSlotNode(JIPipeNodeInfo info) {
            super(info, new FixedSlotConfiguration());
        }

        public FixedSlotNode(FixedSlotNode other) {
            super(other);
        }

        @Override
        public void run(JIPipeGraphNodeRunContext runContext, JIPipeProgressInfo progressInfo) {
        }
    }

    /**
     * Test-only node type whose slot configuration is a mutable
     * {@link JIPipeDefaultMutableSlotConfiguration} sealed on both sides (via
     * the setInputSealed/setOutputSealed setters): pins that add_slot refuses
     * a sealed side before any mutation. Extends JIPipeGraphNode directly
     * because the fixture needs the slot-configuration constructor of the
     * graph node (the comment node does not provide one); run() is empty like
     * the comment node's.
     */
    public static class SealedSlotNode extends JIPipeGraphNode {

        public SealedSlotNode(JIPipeNodeInfo info) {
            super(info, sealedConfiguration());
        }

        public SealedSlotNode(SealedSlotNode other) {
            super(other);
        }

        @Override
        public void run(JIPipeGraphNodeRunContext runContext, JIPipeProgressInfo progressInfo) {
        }

        private static JIPipeDefaultMutableSlotConfiguration sealedConfiguration() {
            JIPipeDefaultMutableSlotConfiguration configuration = JIPipeDefaultMutableSlotConfiguration.builder()
                    .addInputSlot("Input", "Sealed fixture input", JIPipeData.class)
                    .addOutputSlot("Output", "Sealed fixture output", JIPipeData.class)
                    .build();
            configuration.setInputSealed(true);
            configuration.setOutputSealed(true);
            return configuration;
        }
    }
}
