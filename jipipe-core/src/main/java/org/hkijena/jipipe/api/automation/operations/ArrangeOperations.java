package org.hkijena.jipipe.api.automation.operations;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.hkijena.jipipe.api.automation.AutomationContext;
import org.hkijena.jipipe.api.automation.AutomationOperation;
import org.hkijena.jipipe.api.automation.AutomationOperationCategory;
import org.hkijena.jipipe.api.automation.AutomationSchemaUtils;
import org.hkijena.jipipe.api.data.JIPipeDataSlot;
import org.hkijena.jipipe.api.nodes.JIPipeGraph;
import org.hkijena.jipipe.api.nodes.JIPipeGraphNode;
import org.hkijena.jipipe.api.project.JIPipeProject;

import java.awt.*;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Deque;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * Built-in graph arrangement operations.
 * <p>
 * The existing auto-layout implementations (Sugiyama, MST) consume a live
 * {@link org.hkijena.jipipe.desktop.app.grapheditor.commons.JIPipeDesktopGraphCanvasUI}
 * and are not callable from headless automation contexts. arrange_nodes therefore applies
 * a deterministic layered fallback: columns by topological depth, constant vertical spacing.
 */
public class ArrangeOperations {

    private static final ObjectMapper mapper = new ObjectMapper();

    /**
     * Horizontal distance between depth columns (pure gap between node tiles).
     */
    private static final int COLUMN_SPACING = 380;

    /**
     * Vertical distance between nodes within a column (pure gap between node tiles).
     */
    private static final int ROW_SPACING = 160;

    /**
     * Distance of the first column and row from the canvas origin.
     */
    private static final int MARGIN = 60;

    public static class ArrangeNodes implements AutomationOperation {
        @Override
        public String getId() {
            return "arrange_nodes";
        }

        @Override
        public boolean requiresEdt() {
            return true;
        }

        @Override
        public String getDescription() {
            return "Arrange the nodes of a compartment in topological layers and return the new positions";
        }

        @Override
        public JsonNode getInputSchema() {
            return AutomationSchemaUtils.parseJson("""
                    {
                      "type": "object",
                      "required": ["compartment"],
                      "properties": {
                        "compartment": {"type": "string", "description": "Compartment whose nodes are arranged (UUID, alias, or name)"}
                      },
                      "additionalProperties": false
                    }""");
        }

        @Override
        public AutomationOperationCategory getCategory() {
            return AutomationOperationCategory.Execute;
        }

        @Override
        public boolean isReadOnly() {
            return false;
        }

        @Override
        public JsonNode execute(AutomationContext ctx, JsonNode params) {
            JIPipeProject project = ctx.getProject();
            if (project == null) {
                return mapper.createObjectNode().put("error", "No project loaded");
            }
            var compartment = AutomationOperationUtils.findCompartment(project, params.get("compartment").asText());
            if (compartment == null) {
                return mapper.createObjectNode().put("error",
                        "Compartment not found: " + params.get("compartment").asText());
            }
            JIPipeGraph graph = ctx.getGraph();
            UUID compartmentUUID = compartment.getProjectCompartmentUUID();
            List<JIPipeGraphNode> nodes = new ArrayList<>(graph.getNodesWithinCompartment(compartmentUUID));
            // Deterministic order: alias, then UUID as a tiebreaker
            nodes.sort(Comparator.comparing((JIPipeGraphNode node) -> {
                String alias = graph.getAliasIdOf(node);
                return alias != null ? alias : String.valueOf(graph.getUUIDOf(node));
            }).thenComparing(node -> String.valueOf(graph.getUUIDOf(node))));

            Map<JIPipeGraphNode, Integer> depths = computeDepths(graph, nodes);
            Map<Integer, Integer> rowCounter = new HashMap<>();
            ArrayNode positions = mapper.createObjectNode().putArray("positions");
            for (JIPipeGraphNode node : nodes) {
                int column = depths.getOrDefault(node, 0);
                int row = rowCounter.merge(column, 1, Integer::sum) - 1;
                Point location = new Point(MARGIN + column * COLUMN_SPACING, MARGIN + row * ROW_SPACING);
                node.setNodeUILocationWithin(compartmentUUID.toString(), location);
                ObjectNode position = positions.addObject();
                String alias = graph.getAliasIdOf(node);
                position.put("node", alias != null ? alias : String.valueOf(graph.getUUIDOf(node)));
                UUID uuid = graph.getUUIDOf(node);
                if (uuid != null) {
                    position.put("uuid", uuid.toString());
                }
                position.put("x", location.x);
                position.put("y", location.y);
            }
            ObjectNode data = mapper.createObjectNode();
            data.set("positions", positions);
            return data;
        }

        /**
         * Layers the nodes by longest-path depth over the slot connections inside the
         * compartment. Nodes on cycles never reach in-degree zero and are placed in the
         * column after the deepest computed layer.
         */
        private Map<JIPipeGraphNode, Integer> computeDepths(JIPipeGraph graph, List<JIPipeGraphNode> nodes) {
            Set<JIPipeGraphNode> nodeSet = new HashSet<>(nodes);
            Map<JIPipeGraphNode, Set<JIPipeGraphNode>> predecessors = new HashMap<>();
            Map<JIPipeGraphNode, Set<JIPipeGraphNode>> successors = new HashMap<>();
            for (JIPipeGraphNode node : nodes) {
                predecessors.put(node, new HashSet<>());
                successors.put(node, new HashSet<>());
            }
            for (JIPipeGraphNode node : nodes) {
                for (JIPipeDataSlot inputSlot : node.getInputSlots()) {
                    for (JIPipeDataSlot incomingSlot : graph.getInputIncomingSourceSlots(inputSlot)) {
                        JIPipeGraphNode source = incomingSlot.getNode();
                        if (source == node || !nodeSet.contains(source)) {
                            // Skip self loops and connections that leave the compartment
                            continue;
                        }
                        if (predecessors.get(node).add(source)) {
                            successors.get(source).add(node);
                        }
                    }
                }
            }
            Map<JIPipeGraphNode, Integer> depths = new HashMap<>();
            Map<JIPipeGraphNode, Integer> remainingInputs = new HashMap<>();
            Deque<JIPipeGraphNode> queue = new ArrayDeque<>();
            for (JIPipeGraphNode node : nodes) {
                int inDegree = predecessors.get(node).size();
                remainingInputs.put(node, inDegree);
                if (inDegree == 0) {
                    depths.put(node, 0);
                    queue.add(node);
                }
            }
            while (!queue.isEmpty()) {
                JIPipeGraphNode node = queue.poll();
                for (JIPipeGraphNode successor : successors.get(node)) {
                    depths.merge(successor, depths.getOrDefault(node, 0) + 1, Math::max);
                    if (remainingInputs.merge(successor, -1, Integer::sum) == 0) {
                        queue.add(successor);
                    }
                }
            }
            int maxDepth = depths.values().stream().max(Integer::compare).orElse(0);
            for (JIPipeGraphNode node : nodes) {
                depths.putIfAbsent(node, maxDepth + 1);
            }
            return depths;
        }
    }
}
