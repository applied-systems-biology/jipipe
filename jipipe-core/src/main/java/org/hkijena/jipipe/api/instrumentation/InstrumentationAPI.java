package org.hkijena.jipipe.api.instrumentation;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.hkijena.jipipe.JIPipe;
import org.hkijena.jipipe.api.JIPipeProgressInfo;
import org.hkijena.jipipe.api.data.JIPipeData;
import org.hkijena.jipipe.api.data.JIPipeDataSlot;
import org.hkijena.jipipe.api.nodes.JIPipeGraph;
import org.hkijena.jipipe.api.nodes.JIPipeGraphNode;
import org.hkijena.jipipe.api.nodes.JIPipeNodeInfo;
import org.hkijena.jipipe.api.parameters.JIPipeParameterAccess;
import org.hkijena.jipipe.api.parameters.JIPipeParameterTree;
import org.hkijena.jipipe.api.project.JIPipeProject;
import org.hkijena.jipipe.api.project.JIPipeProjectUserPaths;
import org.hkijena.jipipe.api.instrumentation.events.*;
import org.hkijena.jipipe.api.instrumentation.pipeline_map.PipelineMap;
import org.hkijena.jipipe.api.instrumentation.pipeline_map.SegmentNode;
import org.hkijena.jipipe.plugins.parameters.library.markup.HTMLText;
import org.hkijena.jipipe.plugins.publish.rocrate.CreateROCrateRun;
import org.hkijena.jipipe.plugins.publish.rocrate.ROCrateDockerSettings;

import java.awt.Point;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Stateless service providing typed Java methods for all built-in instrumentation operations.
 * Both the AI agent and the WebSocket operation wrappers delegate to this class.
 */
public final class InstrumentationAPI {

    private static final ObjectMapper mapper = new ObjectMapper();

    private InstrumentationAPI() {
    }

    // ── Queries ──

    public static JsonNode queryCompartments(InstrumentationContext ctx) {
        ObjectNode data = mapper.createObjectNode();
        ArrayNode compartments = data.putArray("compartments");
        JIPipeProject project = ctx.getProject();
        if (project == null) return data;
        for (var entry : project.getCompartments().entrySet()) {
            ObjectNode comp = compartments.addObject();
            comp.put("id", entry.getKey().toString());
            comp.put("name", entry.getValue().getName());
            comp.put("nodeCount", project.getGraph().getNodesWithinCompartment(entry.getKey()).size());
        }
        return data;
    }

    public static JsonNode queryGraph(InstrumentationContext ctx, String compartmentIdStr) {
        ObjectNode data = mapper.createObjectNode();
        JIPipeProject project = ctx.getProject();
        if (project == null) {
            data.putArray("nodes");
            data.putArray("connections");
            return data;
        }
        JIPipeGraph graph = project.getGraph();
        UUID compartmentFilter = null;
        if (compartmentIdStr != null && !compartmentIdStr.isBlank()) {
            try {
                compartmentFilter = UUID.fromString(compartmentIdStr);
            } catch (IllegalArgumentException e) {
                var comp = project.findCompartment(compartmentIdStr);
                if (comp != null) {
                    compartmentFilter = comp.getProjectCompartmentUUID();
                }
            }
        }
        if (compartmentFilter != null) {
            data.put("compartmentId", compartmentFilter.toString());
        }
        ArrayNode nodes = data.putArray("nodes");
        ArrayNode connections = data.putArray("connections");
        for (JIPipeGraphNode node : graph.getGraphNodes()) {
            UUID nodeCompartment = graph.getCompartmentUUIDOf(node);
            if (compartmentFilter != null && !compartmentFilter.equals(nodeCompartment)) continue;
            UUID uuid = graph.getUUIDOf(node);
            String alias = graph.getAliasIdOf(node);
            Point loc = node.getNodeUILocationWithin(nodeCompartment != null ? nodeCompartment.toString() : "");
            ObjectNode n = nodes.addObject();
            n.put("id", alias != null ? alias : (uuid != null ? uuid.toString() : "?"));
            n.put("uuid", uuid != null ? uuid.toString() : null);
            n.put("name", node.getName());
            n.put("typeId", node.getInfo() != null ? node.getInfo().getId() : "?");
            n.put("x", loc != null ? loc.x : 0);
            n.put("y", loc != null ? loc.y : 0);
        }
        for (Map.Entry<JIPipeDataSlot, JIPipeDataSlot> edge : graph.getSlotEdges()) {
            JIPipeDataSlot source = edge.getKey();
            JIPipeDataSlot target = edge.getValue();
            UUID sourceComp = graph.getCompartmentUUIDOf(source.getNode());
            UUID targetComp = graph.getCompartmentUUIDOf(target.getNode());
            if (compartmentFilter != null && !compartmentFilter.equals(sourceComp) && !compartmentFilter.equals(targetComp))
                continue;
            ObjectNode c = connections.addObject();
            c.put("source", graph.getAliasIdOf(source.getNode()));
            c.put("sourceSlot", source.getName());
            c.put("target", graph.getAliasIdOf(target.getNode()));
            c.put("targetSlot", target.getName());
        }
        return data;
    }

    public static JsonNode queryNode(InstrumentationContext ctx, String nodeIdStr) {
        JIPipeProject project = ctx.getProject();
        if (project == null) return mapper.createObjectNode().put("error", "No project loaded");
        JIPipeGraph graph = project.getGraph();
        JIPipeGraphNode node = graph.findNode(nodeIdStr);
        if (node == null) return mapper.createObjectNode().put("error", "Node not found: " + nodeIdStr);
        ObjectNode data = mapper.createObjectNode();
        data.put("id", graph.getAliasIdOf(node));
        data.put("uuid", graph.getUUIDOf(node) != null ? graph.getUUIDOf(node).toString() : null);
        data.put("name", node.getName());
        data.put("typeId", node.getInfo() != null ? node.getInfo().getId() : "?");
        UUID compartment = graph.getCompartmentUUIDOf(node);
        data.put("compartmentId", compartment != null ? compartment.toString() : null);
        ObjectNode params = data.putObject("parameters");
        for (JIPipeParameterAccess access : JIPipeParameterTree.getParameters(node).values()) {
            String key = access.getKey();
            if (key == null || key.startsWith("jipipe:") || key.startsWith("jipipe-ui:")) continue;
            Object value = access.get(Object.class);
            params.put(key, value != null ? value.toString() : "null");
        }
        ArrayNode inputSlots = data.putArray("inputSlots");
        for (JIPipeDataSlot slot : node.getInputSlots()) {
            ObjectNode s = inputSlots.addObject();
            s.put("name", slot.getName());
            s.put("dataType", JIPipeData.getNameOf(slot.getAcceptedDataType()));
        }
        ArrayNode outputSlots = data.putArray("outputSlots");
        for (JIPipeDataSlot slot : node.getOutputSlots()) {
            ObjectNode s = outputSlots.addObject();
            s.put("name", slot.getName());
            s.put("dataType", JIPipeData.getNameOf(slot.getAcceptedDataType()));
        }
        return data;
    }

    public static JsonNode queryData(InstrumentationContext ctx, String nodeIdStr, String slotName, int limit) {
        JIPipeProject project = ctx.getProject();
        if (project == null) return mapper.createObjectNode().put("error", "No project loaded");
        JIPipeGraph graph = project.getGraph();
        JIPipeGraphNode node = graph.findNode(nodeIdStr);
        if (node == null) return mapper.createObjectNode().put("error", "Node not found: " + nodeIdStr);
        JIPipeDataSlot slot = node.getOutputSlot(slotName);
        if (slot == null) slot = node.getInputSlot(slotName);
        if (slot == null) return mapper.createObjectNode().put("error", "Slot not found: " + slotName);
        ObjectNode data = mapper.createObjectNode();
        data.put("nodeId", graph.getAliasIdOf(node));
        data.put("slotName", slot.getName());
        data.put("rowCount", slot.getRowCount());
        ArrayNode columns = data.putArray("columns");
        columns.add("Name");
        for (String col : slot.getTextAnnotationColumnNames()) columns.add(col);
        int maxRows = Math.min(slot.getRowCount(), limit);
        ArrayNode rows = data.putArray("rows");
        for (int row = 0; row < maxRows; row++) {
            ObjectNode r = rows.addObject();
            try {
                String strRep = slot.getDataItemStore(row).getStringRepresentation();
                r.put("Name", strRep != null ? strRep : "row-" + row);
            } catch (Exception e) {
                r.put("Name", "row-" + row);
            }
            for (String col : slot.getTextAnnotationColumnNames()) {
                var ann = slot.getTextAnnotationMap(row).get(col);
                r.put(col, ann != null ? ann.getValue() : "");
            }
        }
        return data;
    }

    // ── Execution (async) ──

    public static InstrumentationJob runNode(InstrumentationContext ctx, String nodeId, RunMode mode) {
        JIPipeProject project = ctx.getProject();
        if (project == null) throw new IllegalStateException("No project available");
        JIPipeGraphNode node = project.getGraph().findNode(nodeId);
        if (node == null) throw new IllegalArgumentException("No node found for ID '" + nodeId + "'");
        InstrumentationJob job = ctx.getJobManager().createJob("run_node", "Run: " + node.getName(), ctx.getProgressInfo());
        Thread runThread = new Thread(() -> {
            try {
                InstrumentationRunEngine engine = new InstrumentationRunEngine(project, ctx.getProgressInfo());
                engine.runNodes(List.of(node), mode, job);
                job.complete(null);
            } catch (Exception e) {
                job.fail(e.getMessage());
            }
        }, "instrumentation-run-" + job.getId());
        runThread.setDaemon(true);
        runThread.start();
        return job;
    }

    public static InstrumentationJob runCompartment(InstrumentationContext ctx, String compartmentId) {
        JIPipeProject project = ctx.getProject();
        if (project == null) throw new IllegalStateException("No project available");
        var compartment = project.findCompartment(compartmentId);
        if (compartment == null) throw new IllegalArgumentException("Compartment not found: " + compartmentId);
        List<JIPipeGraphNode> outputNodes = project.getGraph().getNodesWithinCompartment(compartment.getProjectCompartmentUUID())
                .stream().filter(n -> n.getInfo() != null && n.getInfo().isRunnable()).toList();
        InstrumentationJob job = ctx.getJobManager().createJob("run_compartment", "Run compartment: " + compartment.getName(), ctx.getProgressInfo());
        Thread runThread = new Thread(() -> {
            try {
                InstrumentationRunEngine engine = new InstrumentationRunEngine(project, ctx.getProgressInfo());
                engine.runNodes(outputNodes, RunMode.PIPELINE_FILESYSTEM, job);
                job.complete(null);
            } catch (Exception e) {
                job.fail(e.getMessage());
            }
        }, "instrumentation-run-" + job.getId());
        runThread.setDaemon(true);
        runThread.start();
        return job;
    }

    public static InstrumentationJob runPipeline(InstrumentationContext ctx, RunMode mode) {
        JIPipeProject project = ctx.getProject();
        if (project == null) throw new IllegalStateException("No project available");
        InstrumentationJob job = ctx.getJobManager().createJob("run_pipeline", "Run pipeline", ctx.getProgressInfo());
        Thread runThread = new Thread(() -> {
            try {
                InstrumentationRunEngine engine = new InstrumentationRunEngine(project, ctx.getProgressInfo());
                engine.runPipeline(mode, job);
                job.complete(null);
            } catch (Exception e) {
                job.fail(e.getMessage());
            }
        }, "instrumentation-run-" + job.getId());
        runThread.setDaemon(true);
        runThread.start();
        return job;
    }

    // ── Modifications ──

    public static String addNode(InstrumentationContext ctx, String compartmentId, String nodeTypeId, int x, int y) {
        JIPipeGraph graph = ctx.getGraph();
        if (graph == null) throw new IllegalStateException("No graph available");
        JIPipeNodeInfo info = JIPipe.getNodes().getInfoById(nodeTypeId);
        if (info == null) throw new IllegalArgumentException("Unknown node type: " + nodeTypeId);
        JIPipeGraphNode node = info.newInstance();
        UUID compartmentUuid = resolveCompartmentUUID(ctx, compartmentId);
        graph.insertNode(node, compartmentUuid);
        node.setNodeUILocationWithin(compartmentUuid != null ? compartmentUuid.toString() : "", new Point(x, y));
        String alias = graph.getAliasIdOf(node);
        ctx.getEventBus().publish(new NodeAddedEvent(ctx.getProjectId(), compartmentUuid != null ? compartmentUuid.toString() : null, alias, nodeTypeId, node.getName()));
        return alias;
    }

    public static void removeNode(InstrumentationContext ctx, String nodeId) {
        JIPipeGraph graph = ctx.getGraph();
        if (graph == null) throw new IllegalStateException("No graph available");
        JIPipeGraphNode node = graph.findNode(nodeId);
        if (node == null) throw new IllegalArgumentException("Node not found: " + nodeId);
        graph.removeNode(node, true);
        ctx.getEventBus().publish(new NodeRemovedEvent(ctx.getProjectId(), nodeId));
    }

    public static void addConnection(InstrumentationContext ctx, String source, String sourceSlot, String target, String targetSlot) {
        JIPipeGraph graph = ctx.getGraph();
        if (graph == null) throw new IllegalStateException("No graph available");
        JIPipeGraphNode sourceNode = graph.findNode(source);
        JIPipeGraphNode targetNode = graph.findNode(target);
        if (sourceNode == null) throw new IllegalArgumentException("Source node not found: " + source);
        if (targetNode == null) throw new IllegalArgumentException("Target node not found: " + target);
        graph.connect(sourceNode.getOutputSlot(sourceSlot), targetNode.getInputSlot(targetSlot));
        ctx.getEventBus().publish(new ConnectionChangedEvent(ctx.getProjectId(), source, sourceSlot, target, targetSlot, "added"));
    }

    public static void removeConnection(InstrumentationContext ctx, String source, String sourceSlot, String target, String targetSlot) {
        JIPipeGraph graph = ctx.getGraph();
        if (graph == null) throw new IllegalStateException("No graph available");
        JIPipeGraphNode sourceNode = graph.findNode(source);
        JIPipeGraphNode targetNode = graph.findNode(target);
        if (sourceNode == null) throw new IllegalArgumentException("Source node not found: " + source);
        if (targetNode == null) throw new IllegalArgumentException("Target node not found: " + target);
        graph.disconnect(sourceNode.getOutputSlot(sourceSlot), targetNode.getInputSlot(targetSlot), true);
        ctx.getEventBus().publish(new ConnectionChangedEvent(ctx.getProjectId(), source, sourceSlot, target, targetSlot, "removed"));
    }

    public static void setParameter(InstrumentationContext ctx, String nodeId, String key, String value) {
        JIPipeGraph graph = ctx.getGraph();
        if (graph == null) throw new IllegalStateException("No graph available");
        JIPipeGraphNode node = graph.findNode(nodeId);
        if (node == null) throw new IllegalArgumentException("Node not found: " + nodeId);
        JIPipeParameterAccess access = JIPipeParameterTree.getParameters(node).get(key);
        if (access == null) throw new IllegalArgumentException("Parameter not found: " + key);
        Object converted = convertParameterValue(value, access.getFieldClass());
        access.set(converted);
        ctx.getEventBus().publish(new ParameterChangedEvent(ctx.getProjectId(), nodeId, key, value));
    }

    public static String addCompartment(InstrumentationContext ctx, String name) {
        JIPipeProject project = ctx.getProject();
        if (project == null) throw new IllegalStateException("No project available");
        var compartment = project.addCompartment(name);
        ctx.getEventBus().publish(new CompartmentChangedEvent(ctx.getProjectId(), compartment.getProjectCompartmentUUID().toString(), name, "added"));
        return compartment.getProjectCompartmentUUID().toString();
    }

    public static void setProjectMetadata(InstrumentationContext ctx, String name, String description) {
        JIPipeProject project = ctx.getProject();
        if (project == null) throw new IllegalStateException("No project available");
        if (name != null) project.getMetadata().setName(name);
        if (description != null) project.getMetadata().setDescription(new HTMLText(description));
    }

    // ── Pipeline Map ──

    public static JsonNode getPipelineMap(InstrumentationContext ctx) {
        JIPipeProject project = ctx.getProject();
        if (project == null) return mapper.createObjectNode();
        SegmentNode root = new SegmentNode("root", project.getMetadata().getName() != null ? project.getMetadata().getName() : "Project");
        for (var entry : project.getCompartments().entrySet()) {
            String compId = entry.getKey().toString();
            String compName = entry.getValue().getName();
            SegmentNode segment = new SegmentNode(compId, compName);
            segment.setCompartmentId(compId);
            var nodes = project.getGraph().getNodesWithinCompartment(entry.getKey());
            for (JIPipeGraphNode node : nodes) {
                String alias = project.getGraph().getAliasIdOf(node);
                segment.getNodeIds().add(alias != null ? alias : node.getName());
            }
            segment.setSummary(nodes.size() + " nodes");
            if (root.getChildren() == null) root.setChildren(new java.util.ArrayList<>());
            root.getChildren().add(segment);
        }
        PipelineMap map = new PipelineMap(root);
        return mapper.valueToTree(map);
    }

    public static JsonNode getSegmentDetail(InstrumentationContext ctx, String segmentId) {
        JIPipeProject project = ctx.getProject();
        if (project == null) return mapper.createObjectNode().put("error", "No project loaded");
        try {
            UUID compUuid = UUID.fromString(segmentId);
            var compartment = project.getCompartments().get(compUuid);
            if (compartment == null) return mapper.createObjectNode().put("error", "Segment not found: " + segmentId);
            ObjectNode data = mapper.createObjectNode();
            data.put("segmentId", segmentId);
            data.put("name", compartment.getName());
            ArrayNode nodes = data.putArray("nodes");
            for (JIPipeGraphNode node : project.getGraph().getNodesWithinCompartment(compUuid)) {
                ObjectNode n = nodes.addObject();
                n.put("id", project.getGraph().getAliasIdOf(node));
                n.put("name", node.getName());
                n.put("typeId", node.getInfo() != null ? node.getInfo().getId() : "?");
            }
            return data;
        } catch (IllegalArgumentException e) {
            return mapper.createObjectNode().put("error", "Invalid segment ID: " + segmentId);
        }
    }

    // ── Node Search ──

    public static JsonNode searchNodes(InstrumentationContext ctx, String query, int limit) {
        ObjectNode data = mapper.createObjectNode();
        ArrayNode results = data.putArray("results");
        var nodeInfos = JIPipe.getNodes().getRegisteredNodeInfos().values();
        String lowerQuery = query.toLowerCase();
        int count = 0;
        for (JIPipeNodeInfo info : nodeInfos) {
            if (count >= limit) break;
            String name = info.getName() != null ? info.getName() : info.getId();
            String desc = info.getDescription() != null ? info.getDescription().toPlainText() : "";
            if (name.toLowerCase().contains(lowerQuery) || desc.toLowerCase().contains(lowerQuery) || info.getId().toLowerCase().contains(lowerQuery)) {
                ObjectNode r = results.addObject();
                r.put("id", info.getId());
                r.put("name", name);
                r.put("description", desc);
                count++;
            }
        }
        return data;
    }

    // ── RO-Crate ──

    public static JsonNode createROCrate(InstrumentationContext ctx, String outputPath,
                                         ROCrateDockerSettings dockerSettings,
                                         Map<String, JIPipeProjectUserPaths.Role> userPathOverrides) {
        JIPipeProject project = ctx.getProject();
        if (project == null) {
            throw new RuntimeException("No project selected");
        }

        Path roCrateFile = Path.of(outputPath);
        Path projectFile = getProjectSavePath(ctx);

        CreateROCrateRun run = new CreateROCrateRun(project, projectFile, roCrateFile,
                userPathOverrides != null ? userPathOverrides : new HashMap<>(),
                dockerSettings);
        run.setProgressInfo(ctx.getProgressInfo().resolveAndLog("Create RO-Crate"));
        run.run();

        ObjectNode result = mapper.createObjectNode();
        result.put("path", roCrateFile.toAbsolutePath().toString());
        try {
            result.put("sizeBytes", Files.size(roCrateFile));
        } catch (IOException e) {
            result.put("sizeBytes", -1);
        }
        return result;
    }

    private static Path getProjectSavePath(InstrumentationContext ctx) {
        org.hkijena.jipipe.desktop.app.JIPipeDesktopProjectWindow window =
                org.hkijena.jipipe.desktop.app.JIPipeDesktopProjectWindow.getWindowFor(ctx.getProject());
        if (window != null) {
            return window.getProjectSavePath();
        }
        return null;
    }

    // ── Helpers ──

    @SuppressWarnings({"unchecked", "rawtypes"})
    private static Object convertParameterValue(String value, Class<?> fieldClass) {
        if (fieldClass == String.class) {
            return value;
        }
        if (fieldClass == int.class || fieldClass == Integer.class) {
            try {
                return Integer.parseInt(value.trim());
            } catch (NumberFormatException e) {
                throw new IllegalArgumentException("Invalid integer value: '" + value + "'");
            }
        }
        if (fieldClass == long.class || fieldClass == Long.class) {
            try {
                return Long.parseLong(value.trim());
            } catch (NumberFormatException e) {
                throw new IllegalArgumentException("Invalid long value: '" + value + "'");
            }
        }
        if (fieldClass == double.class || fieldClass == Double.class) {
            try {
                return Double.parseDouble(value.trim());
            } catch (NumberFormatException e) {
                throw new IllegalArgumentException("Invalid double value: '" + value + "'");
            }
        }
        if (fieldClass == float.class || fieldClass == Float.class) {
            try {
                return Float.parseFloat(value.trim());
            } catch (NumberFormatException e) {
                throw new IllegalArgumentException("Invalid float value: '" + value + "'");
            }
        }
        if (fieldClass == boolean.class || fieldClass == Boolean.class) {
            return Boolean.parseBoolean(value);
        }
        if (Enum.class.isAssignableFrom(fieldClass)) {
            try {
                return Enum.valueOf((Class<Enum>) fieldClass, value.trim().toUpperCase());
            } catch (IllegalArgumentException e) {
                throw new IllegalArgumentException(
                        "Invalid enum value '" + value + "'. Valid values: " +
                                Arrays.toString(fieldClass.getEnumConstants()));
            }
        }
        if (Path.class.isAssignableFrom(fieldClass)) {
            return Path.of(value);
        }
        return value;
    }

    private static UUID resolveCompartmentUUID(InstrumentationContext ctx, String compartmentId) {
        if (compartmentId == null || compartmentId.isBlank()) return null;
        try {
            return UUID.fromString(compartmentId);
        } catch (IllegalArgumentException e) {
            var comp = ctx.getProject().findCompartment(compartmentId);
            return comp != null ? comp.getProjectCompartmentUUID() : null;
        }
    }
}
