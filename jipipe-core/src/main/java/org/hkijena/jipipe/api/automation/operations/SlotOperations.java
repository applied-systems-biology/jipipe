package org.hkijena.jipipe.api.automation.operations;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.hkijena.jipipe.JIPipe;
import org.hkijena.jipipe.api.automation.AutomationContext;
import org.hkijena.jipipe.api.automation.AutomationOperation;
import org.hkijena.jipipe.api.automation.AutomationOperationCategory;
import org.hkijena.jipipe.api.automation.AutomationSchemaUtils;
import org.hkijena.jipipe.api.data.JIPipeData;
import org.hkijena.jipipe.api.data.JIPipeDataSlot;
import org.hkijena.jipipe.api.data.JIPipeDataSlotInfo;
import org.hkijena.jipipe.api.data.JIPipeMutableSlotConfiguration;
import org.hkijena.jipipe.api.data.JIPipeSlotType;
import org.hkijena.jipipe.api.nodes.JIPipeGraph;
import org.hkijena.jipipe.api.nodes.JIPipeGraphNode;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.StringJoiner;

/**
 * Built-in slot editing operations. Connections are never dropped implicitly:
 * add_connection and remove_connection are the connect/disconnect commands,
 * while these operations only create and delete slots and refuse to remove a
 * slot that still has connections.
 */
public class SlotOperations {

    private static final ObjectMapper mapper = new ObjectMapper();

    private static ObjectNode error(String message) {
        return mapper.createObjectNode().put("error", message);
    }

    /**
     * Parses the slot-type parameter. The schema restricts the value to the
     * input and output enum entries; the defensive envelope covers direct
     * calls that bypass the registry validation.
     */
    private static JIPipeSlotType parseSlotType(JsonNode params) {
        String text = params.get("slot-type").asText();
        if ("input".equals(text)) return JIPipeSlotType.Input;
        if ("output".equals(text)) return JIPipeSlotType.Output;
        return null;
    }

    /**
     * Returns whether a graph slot edge endpoint is the given slot of the
     * node: matched by node identity, slot name, and slot side.
     */
    private static boolean touches(JIPipeDataSlot slot, JIPipeGraphNode node, String name, JIPipeSlotType slotType) {
        return slot.getNode() == node && slot.getName().equals(name) && slot.getSlotType() == slotType;
    }

    /**
     * Renders one connection for the remove_slot refusal: node names (not
     * UUIDs) with the slot names in parentheses.
     */
    private static String renderConnection(JIPipeDataSlot source, JIPipeDataSlot target) {
        return source.getNode().getName() + " (" + source.getName() + ") -> "
                + target.getNode().getName() + " (" + target.getName() + ")";
    }

    /**
     * Returns the refused slot name followed by the existing slot names of
     * the side (the list tells the agent what can be removed instead).
     */
    private static String unknownSlotMessage(String name, JIPipeSlotType slotType, JIPipeMutableSlotConfiguration configuration) {
        String side = slotType == JIPipeSlotType.Input ? "input" : "output";
        Map<String, JIPipeDataSlotInfo> slots = slotType == JIPipeSlotType.Input
                ? configuration.getInputSlots() : configuration.getOutputSlots();
        return "no " + side + " slot named '" + name + "' on this node. Existing " + side + " slots: "
                + (slots.isEmpty() ? "(none)" : String.join(", ", slots.keySet()));
    }

    /**
     * Returns the refusal message for a failed canRemoveInputSlot/canRemoveOutputSlot
     * gate: the not-user-modifiable case (the slot itself is locked) is
     * distinguished from the sealed or not-allowed side (the configuration
     * refuses changes of that side).
     */
    private static String notRemovableMessage(String name, JIPipeSlotType slotType, JIPipeMutableSlotConfiguration configuration) {
        String side = slotType == JIPipeSlotType.Input ? "input" : "output";
        JIPipeDataSlotInfo info = slotType == JIPipeSlotType.Input
                ? configuration.getInputSlots().get(name) : configuration.getOutputSlots().get(name);
        if (info != null && !info.isUserModifiable()) {
            return "slot '" + name + "' is not user-modifiable; it cannot be removed";
        }
        boolean allows = slotType == JIPipeSlotType.Input
                ? configuration.allowsInputSlots() : configuration.allowsOutputSlots();
        if (!allows) {
            return "the " + side + " slots of this node do not allow changes; slots cannot be removed";
        }
        boolean sealed = slotType == JIPipeSlotType.Input
                ? configuration.isInputSlotsSealed() : configuration.isOutputSlotsSealed();
        if (sealed) {
            return "the " + side + " slots of this node are sealed; slots cannot be removed";
        }
        return "slot '" + name + "' cannot be removed";
    }

    /**
     * Returns the refusal message for an unknown data-type parameter: the
     * template-id refusal style with the registered ids. The registry map is
     * read directly because the getById accessor throws for unknown ids,
     * while the gates must produce error envelopes.
     */
    private static String unknownDataTypeMessage(String dataTypeId) {
        StringJoiner known = new StringJoiner(", ");
        for (String id : JIPipe.getDataTypes().getRegisteredDataTypes().keySet()) {
            known.add(id);
        }
        return "Unknown data-type '" + dataTypeId + "'. Registered data type ids: " + known;
    }

    public static class AddSlot implements AutomationOperation {
        @Override
        public String getId() {
            return "add_slot";
        }

        @Override
        public boolean requiresEdt() {
            return true;
        }

        @Override
        public String getDescription() {
            return "Add a slot to a node. Slot operations never disconnect anything: add_connection and remove_connection are the connect/disconnect commands.";
        }

        @Override
        public JsonNode getInputSchema() {
            return AutomationSchemaUtils.parseJson("""
                    {
                      "type": "object",
                      "required": ["node-id", "slot-type", "name", "data-type"],
                      "properties": {
                        "node-id": {"type": "string", "description": "The node id as used by query_graph (alias id or UUID)"},
                        "slot-type": {"type": "string", "enum": ["input", "output"], "description": "Side of the node that receives the slot"},
                        "name": {"type": "string", "description": "Unique slot name on that side (non-blank); an existing name on that side is refused"},
                        "data-type": {"type": "string", "description": "A registered data type id; an unknown id is refused with the registered data type ids"},
                        "description": {"type": "string", "description": "Optional description of the slot"}
                      },
                      "additionalProperties": false
                    }""");
        }

        @Override
        public AutomationOperationCategory getCategory() {
            return AutomationOperationCategory.Modify;
        }

        @Override
        public boolean isReadOnly() {
            return false;
        }

        @Override
        public JsonNode execute(AutomationContext ctx, JsonNode params) {
            JIPipeGraph graph = ctx.getGraph();
            if (graph == null) return error("No graph available");
            String nodeId = params.get("node-id").asText();
            JIPipeGraphNode node = graph.findNode(nodeId);
            if (node == null) return error("Node not found: " + nodeId);
            if (!(node.getSlotConfiguration() instanceof JIPipeMutableSlotConfiguration configuration)) {
                return error("node does not support slot editing");
            }
            JIPipeSlotType slotType = parseSlotType(params);
            if (slotType == null) return error("slot-type must be 'input' or 'output'");
            String name = params.get("name").asText().trim();
            if (name.isBlank()) return error("name must not be blank");
            String dataTypeId = params.get("data-type").asText().trim();
            Class<? extends JIPipeData> dataClass = JIPipe.getDataTypes().getRegisteredDataTypes().get(dataTypeId);
            if (dataClass == null) return error(unknownDataTypeMessage(dataTypeId));

            // Gate order per the slot editing ruling: every gate is its own
            // error envelope, and all gates run BEFORE addSlot (the mutable
            // configuration throws a JIPipeValidationRuntimeException for a
            // user add on a sealed side, which must never surface raw)
            String side = slotType == JIPipeSlotType.Input ? "input" : "output";
            boolean allows = slotType == JIPipeSlotType.Input
                    ? configuration.allowsInputSlots() : configuration.allowsOutputSlots();
            if (!allows) {
                return error("the " + side + " slots of this node do not allow changes: the node does not allow " + side + " slots");
            }
            boolean sealed = slotType == JIPipeSlotType.Input
                    ? configuration.isInputSlotsSealed() : configuration.isOutputSlotsSealed();
            if (sealed) {
                return error("the " + side + " slots of this node are sealed; slots cannot be added");
            }
            boolean canModify = slotType == JIPipeSlotType.Input
                    ? configuration.canModifyInputSlots() : configuration.canModifyOutputSlots();
            if (!canModify) {
                return error("the " + side + " slots of this node cannot be modified");
            }
            boolean canAdd = slotType == JIPipeSlotType.Input
                    ? configuration.canAddInputSlot() : configuration.canAddOutputSlot();
            if (!canAdd) {
                return error("cannot add more " + side + " slots to this node: the slot limit is reached");
            }
            boolean canCreateCompatible = slotType == JIPipeSlotType.Input
                    ? configuration.canCreateCompatibleInputSlot(dataClass)
                    : configuration.canCreateCompatibleOutputSlot(dataClass);
            if (!canCreateCompatible) {
                return error("the " + side + " slots of this node do not accept the data type '" + dataTypeId + "'");
            }
            boolean nameUsed = slotType == JIPipeSlotType.Input
                    ? configuration.hasInputSlot(name) : configuration.hasOutputSlot(name);
            if (nameUsed) {
                return error("slot '" + name + "' already exists on the " + side + " side");
            }

            String description = params.has("description") ? params.get("description").asText() : "";
            configuration.addSlot(name, new JIPipeDataSlotInfo(dataClass, slotType, name, description), true);
            ObjectNode result = mapper.createObjectNode();
            result.put("added", true);
            result.put("name", name);
            result.put("slotType", side);
            return result;
        }
    }

    public static class RemoveSlot implements AutomationOperation {
        @Override
        public String getId() {
            return "remove_slot";
        }

        @Override
        public boolean requiresEdt() {
            return true;
        }

        @Override
        public String getDescription() {
            return "Remove a slot from a node. Refused while the slot still has connections: nothing is ever disconnected implicitly, disconnect each connection with remove_connection first. add_connection and remove_connection are the connect/disconnect commands; slot operations never disconnect anything.";
        }

        @Override
        public JsonNode getInputSchema() {
            return AutomationSchemaUtils.parseJson("""
                    {
                      "type": "object",
                      "required": ["node-id", "slot-type", "name"],
                      "properties": {
                        "node-id": {"type": "string", "description": "The node id as used by query_graph (alias id or UUID)"},
                        "slot-type": {"type": "string", "enum": ["input", "output"], "description": "Side of the node that loses the slot"},
                        "name": {"type": "string", "description": "Name of the slot to remove; an unknown name is refused with the existing slot names of that side"}
                      },
                      "additionalProperties": false
                    }""");
        }

        @Override
        public AutomationOperationCategory getCategory() {
            return AutomationOperationCategory.Modify;
        }

        @Override
        public boolean isReadOnly() {
            return false;
        }

        @Override
        public JsonNode execute(AutomationContext ctx, JsonNode params) {
            JIPipeGraph graph = ctx.getGraph();
            if (graph == null) return error("No graph available");
            String nodeId = params.get("node-id").asText();
            JIPipeGraphNode node = graph.findNode(nodeId);
            if (node == null) return error("Node not found: " + nodeId);
            if (!(node.getSlotConfiguration() instanceof JIPipeMutableSlotConfiguration configuration)) {
                return error("node does not support slot editing");
            }
            JIPipeSlotType slotType = parseSlotType(params);
            if (slotType == null) return error("slot-type must be 'input' or 'output'");
            String name = params.get("name").asText().trim();
            if (name.isBlank()) return error("name must not be blank");
            String side = slotType == JIPipeSlotType.Input ? "input" : "output";

            boolean exists = slotType == JIPipeSlotType.Input
                    ? configuration.hasInputSlot(name) : configuration.hasOutputSlot(name);
            if (!exists) {
                return error(unknownSlotMessage(name, slotType, configuration));
            }
            boolean canRemove = slotType == JIPipeSlotType.Input
                    ? configuration.canRemoveInputSlot(name, true) : configuration.canRemoveOutputSlot(name, true);
            if (!canRemove) {
                return error(notRemovableMessage(name, slotType, configuration));
            }

            // The connection check (the user ruling): collect every slot edge
            // whose source OR target is this slot (node identity, slot name,
            // and slot side), scanned over the whole graph without a
            // compartment filter. Any touching edge refuses the removal and
            // nothing is disconnected: the agent must call remove_connection
            // first, then repeat remove_slot
            List<String> connections = new ArrayList<>();
            for (Map.Entry<JIPipeDataSlot, JIPipeDataSlot> edge : graph.getSlotEdges()) {
                if (touches(edge.getKey(), node, name, slotType) || touches(edge.getValue(), node, name, slotType)) {
                    connections.add(renderConnection(edge.getKey(), edge.getValue()));
                }
            }
            if (!connections.isEmpty()) {
                return error("unable to remove slot '" + name + "': it has " + connections.size()
                        + " connection(s): " + String.join("; ", connections)
                        + "; disconnect them first with remove_connection");
            }

            if (slotType == JIPipeSlotType.Input) {
                configuration.removeInputSlot(name, true);
            } else {
                configuration.removeOutputSlot(name, true);
            }
            ObjectNode result = mapper.createObjectNode();
            result.put("removed", true);
            result.put("name", name);
            result.put("slotType", side);
            return result;
        }
    }
}
