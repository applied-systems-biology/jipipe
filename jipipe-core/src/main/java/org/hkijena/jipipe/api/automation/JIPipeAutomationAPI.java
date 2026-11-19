package org.hkijena.jipipe.api.automation;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.hkijena.jipipe.JIPipe;
import org.hkijena.jipipe.api.data.JIPipeData;
import org.hkijena.jipipe.api.data.JIPipeDataSlot;
import org.hkijena.jipipe.api.automation.events.*;
import org.hkijena.jipipe.api.automation.pipeline_map.PipelineMap;
import org.hkijena.jipipe.api.automation.pipeline_map.SegmentNode;
import org.hkijena.jipipe.api.automation.operations.AutomationOperationUtils;
import org.hkijena.jipipe.api.compartments.algorithms.JIPipeProjectCompartment;
import org.hkijena.jipipe.api.metadata.JIPipeAuthorMetadata;
import org.hkijena.jipipe.api.nodes.JIPipeGraph;
import org.hkijena.jipipe.api.nodes.JIPipeGraphNode;
import org.hkijena.jipipe.api.nodes.JIPipeNodeInfo;
import org.hkijena.jipipe.api.nodes.annotation.JIPipeGraphCanvasNote;
import org.hkijena.jipipe.api.parameters.JIPipeCustomTextDescriptionParameter;
import org.hkijena.jipipe.api.parameters.JIPipeParameterAccess;
import org.hkijena.jipipe.api.parameters.JIPipeParameterTree;
import org.hkijena.jipipe.api.project.JIPipeProject;
import org.hkijena.jipipe.api.project.JIPipeProjectMetadata;
import org.hkijena.jipipe.api.project.JIPipeProjectUserPaths;
import org.hkijena.jipipe.desktop.app.grapheditor.commons.canvas.JIPipeDesktopGraphCanvasGrid;
import org.hkijena.jipipe.plugins.core.nodes.JIPipeCommentNode;
import org.hkijena.jipipe.plugins.parameters.library.markup.HTMLText;
import org.hkijena.jipipe.plugins.publish.rocrate.CreateROCrateRun;
import org.hkijena.jipipe.plugins.publish.rocrate.ROCrateDockerSettings;
import org.hkijena.jipipe.utils.json.JsonUtils;

import java.awt.*;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.List;

/**
 * Stateless service providing typed Java methods for all built-in automation operations.
 * The operation implementations delegate to this class.
 */
public final class JIPipeAutomationAPI {

    private static final ObjectMapper mapper = new ObjectMapper();

    private JIPipeAutomationAPI() {
    }

    // ── Queries ──

    public static JsonNode queryCompartments(AutomationContext ctx) {
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

    /**
     * The canonical node fields of the query_graph response, in emission order.
     * Shared by the API emission, the operation schema enum, and the unknown-field refusal.
     */
    public static final List<String> QUERY_GRAPH_NODE_FIELDS = List.of("id", "name", "typeId", "compartmentId", "uuid", "x", "y");

    public static JsonNode queryGraph(AutomationContext ctx, String compartmentIdStr) {
        return queryGraph(ctx, compartmentIdStr, null, false, false);
    }

    public static JsonNode queryGraph(AutomationContext ctx, String compartmentIdStr, List<String> fields,
                                      boolean omitPositions, boolean connectionsOnly) {
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
            compartmentFilter = requireCompartment(project, compartmentIdStr).getProjectCompartmentUUID();
        }
        if (compartmentFilter != null) {
            data.put("compartmentId", compartmentFilter.toString());
        }
        ArrayNode nodes = data.putArray("nodes");
        ArrayNode connections = data.putArray("connections");
        // The emitted node fields are the whitelist entries selected by the
        // fields parameter, always in whitelist order (never the request order);
        // omitPositions additionally removes x and y
        Set<String> requested = fields != null ? new LinkedHashSet<>(fields) : null;
        List<String> emittedFields = new ArrayList<>();
        for (String field : QUERY_GRAPH_NODE_FIELDS) {
            if (requested != null && !requested.contains(field)) continue;
            if (omitPositions && ("x".equals(field) || "y".equals(field))) continue;
            emittedFields.add(field);
        }
        if (!connectionsOnly) {
            for (JIPipeGraphNode node : graph.getGraphNodes()) {
                UUID nodeCompartment = graph.getCompartmentUUIDOf(node);
                if (compartmentFilter != null && !compartmentFilter.equals(nodeCompartment)) continue;
                nodes.add(buildGraphNodeObject(graph, node, nodeCompartment, emittedFields));
            }
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

    public static JsonNode queryNode(AutomationContext ctx, String nodeIdStr) {
        return queryNode(ctx, nodeIdStr, false);
    }

    /**
     * Renders one node of the query_node operation. When includeDocs is true,
     * every entry of the parameters object additionally carries the name,
     * description, and important flag of its parameter access: name and
     * description render as empty strings when the access provides none or a
     * blank value (a parameter without a documentation annotation derives its
     * name from the key), and important is always present as a boolean.
     * Without includeDocs the entries carry only the normalized value fields
     * (value, type, raw?).
     */
    public static JsonNode queryNode(AutomationContext ctx, String nodeIdStr, boolean includeDocs) {
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
            ObjectNode entry = renderParameterValue(access);
            if (includeDocs) {
                String name = access.getName();
                String description = access.getDescription();
                entry.put("name", name != null && !name.isBlank() ? name : "");
                entry.put("description", description != null && !description.isBlank() ? description : "");
                entry.put("important", access.isImportant());
            }
            params.set(key, entry);
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

    /**
     * Renders one parameter of the query_node response as the normalized
     * object {"value", "type", "raw"?}: "value" is the effective readable
     * string (the text description when the value implements
     * {@link JIPipeCustomTextDescriptionParameter}, e.g. expressions without
     * the "Expression: " prefix, the plain toString() otherwise, the literal
     * string "null" for a null value), "type" is the field class simple name
     * ("?" when absent), and "raw" is the plain toString() form, present only
     * when it differs from "value". A RuntimeException from either rendering
     * path yields value = "error rendering value: <exception simple name>" and
     * no raw key.
     *
     * @param access the parameter access
     * @return the rendered parameter object
     */
    public static ObjectNode renderParameterValue(JIPipeParameterAccess access) {
        Object value = access.get(Object.class);
        String valueText = null;
        String rawText = null;
        String errorText = null;
        try {
            valueText = value instanceof JIPipeCustomTextDescriptionParameter
                    ? JIPipeCustomTextDescriptionParameter.getTextDescriptionOf(value)
                    : String.valueOf(value);
            rawText = String.valueOf(value);
        } catch (RuntimeException e) {
            errorText = "error rendering value: " + e.getClass().getSimpleName();
        }
        ObjectNode entry = mapper.createObjectNode();
        entry.put("value", errorText != null ? errorText : valueText);
        entry.put("type", access.getFieldClass() != null ? access.getFieldClass().getSimpleName() : "?");
        if (errorText == null && !rawText.equals(valueText)) {
            entry.put("raw", rawText);
        }
        return entry;
    }

    /**
     * Renders the changed-vs-default parameter diff of one node with the
     * project report's own machinery (the approach of
     * {@link JIPipeGraphNode#getTextDescription(StringBuilder)}): a fresh
     * reference instance of the node type is created via
     * {@code node.getInfo().newInstance()} (a new instance per call, never
     * mutated), a {@link JIPipeParameterTree} is built over the live node and
     * the reference, and the keys of the CURRENT tree are iterated (the
     * report's behavior; keys that only the reference has are out of scope).
     * Both sides are rendered through {@link #renderParameterValue}, so
     * "changed" is the rendered "value" string differing from the rendered
     * "defaultValue" string. The jipipe:node:name and jipipe:node:description
     * keys are skipped (the same exclusions the report applies). The
     * "defaultValue" of an entry is the rendered reference value, or the JSON
     * null value when the reference tree has no such key (a present-but-null
     * parameter renders the string "null", never JSON null). The parameters
     * array carries the changed entries only unless includeUnchanged is true;
     * changedCount counts the changed entries either way.
     *
     * @param ctx             the automation context
     * @param nodeIdStr       the node id (alias id or UUID)
     * @param includeUnchanged also list parameters that match the reference
     * @return the response object with nodeId, parameters, and changedCount
     */
    public static JsonNode getParameterDiff(AutomationContext ctx, String nodeIdStr, boolean includeUnchanged) {
        JIPipeProject project = ctx.getProject();
        if (project == null) return mapper.createObjectNode().put("error", "No project loaded");
        JIPipeGraph graph = ctx.getGraph();
        JIPipeGraphNode node = graph.findNode(nodeIdStr);
        if (node == null) return mapper.createObjectNode().put("error", "Node not found: " + nodeIdStr);
        JIPipeGraphNode referenceInstance = node.getInfo().newInstance();
        JIPipeParameterTree currentTree = new JIPipeParameterTree(node);
        JIPipeParameterTree referenceTree = new JIPipeParameterTree(referenceInstance);
        ObjectNode data = mapper.createObjectNode();
        data.put("nodeId", graph.getAliasIdOf(node));
        ArrayNode parameters = data.putArray("parameters");
        int changedCount = 0;
        for (String key : currentTree.getParameters().keySet()) {
            if ("jipipe:node:name".equals(key)) {
                continue;
            }
            if ("jipipe:node:description".equals(key)) {
                continue;
            }
            JIPipeParameterAccess currentAccess = currentTree.getParameterAccess(key);
            JIPipeParameterAccess referenceAccess = referenceTree.getParameterAccess(key);
            ObjectNode currentRendered = renderParameterValue(currentAccess);
            String value = currentRendered.get("value").asText();
            ObjectNode entry = mapper.createObjectNode();
            entry.put("key", key);
            entry.put("value", value);
            entry.put("type", currentRendered.get("type").asText());
            boolean changed;
            if (referenceAccess == null) {
                entry.putNull("defaultValue");
                changed = true;
            } else {
                String defaultValue = renderParameterValue(referenceAccess).get("value").asText();
                entry.put("defaultValue", defaultValue);
                changed = !value.equals(defaultValue);
            }
            entry.put("changed", changed);
            if (changed) {
                changedCount++;
            }
            if (changed || includeUnchanged) {
                parameters.add(entry);
            }
        }
        data.put("changedCount", changedCount);
        return data;
    }

    public static JsonNode queryData(AutomationContext ctx, String nodeIdStr, String slotName, int limit) {
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

    /**
     * Returns all comments and text boxes of the project in one response.
     * The "annotations" array holds one object per annotation node with the
     * fields id (the alias id, JSON null when unassigned), uuid, typeId,
     * kind ("comment" or "text-box", decided by the node type), compartmentId,
     * title, x, y and, for text boxes, content, width, and height (the grid
     * size times the canvas grid constants). Comment nodes carry their node
     * name as title and no content, width, or height fields. The text box
     * title and content are read from the "text-title" and "text-content"
     * parameters (empty strings when absent). Node types that are neither
     * comments nor canvas notes are skipped.
     *
     * @param ctx            the automation context
     * @param compartmentRef optional compartment filter (UUID, alias, or display name; null or blank for all compartments)
     * @param kind           optional kind filter ("comment" or "text-box"; null or blank for both kinds)
     * @return the response object with the "annotations" array
     * @throws IllegalArgumentException if the compartment reference cannot be resolved
     */
    public static JsonNode queryAnnotations(AutomationContext ctx, String compartmentRef, String kind) {
        ObjectNode data = mapper.createObjectNode();
        ArrayNode annotations = data.putArray("annotations");
        JIPipeProject project = ctx.getProject();
        if (project == null) return data;
        JIPipeGraph graph = project.getGraph();
        UUID compartmentFilter = null;
        if (compartmentRef != null && !compartmentRef.isBlank()) {
            compartmentFilter = requireCompartment(project, compartmentRef).getProjectCompartmentUUID();
        }
        boolean filterByKind = kind != null && !kind.isBlank();
        for (JIPipeGraphNode node : graph.getGraphNodes()) {
            String annotationKind;
            if (node instanceof JIPipeCommentNode) {
                annotationKind = "comment";
            } else if (node instanceof JIPipeGraphCanvasNote) {
                annotationKind = "text-box";
            } else {
                continue;
            }
            if (filterByKind && !kind.equals(annotationKind)) continue;
            UUID nodeCompartment = graph.getCompartmentUUIDOf(node);
            if (compartmentFilter != null && !compartmentFilter.equals(nodeCompartment)) continue;
            UUID uuid = graph.getUUIDOf(node);
            String alias = graph.getAliasIdOf(node);
            Point loc = node.getNodeUILocationWithin(nodeCompartment != null ? nodeCompartment.toString() : "");
            boolean textBox = node instanceof JIPipeGraphCanvasNote;
            ObjectNode n = annotations.addObject();
            n.put("id", alias);
            n.put("uuid", uuid != null ? uuid.toString() : null);
            n.put("typeId", node.getInfo() != null ? node.getInfo().getId() : "?");
            n.put("kind", annotationKind);
            n.put("compartmentId", nodeCompartment != null ? nodeCompartment.toString() : null);
            if (textBox) {
                Map<String, JIPipeParameterAccess> parameters = JIPipeParameterTree.getParameters(node);
                n.put("title", parameterStringValue(parameters, "text-title"));
                n.put("content", parameterStringValue(parameters, "text-content"));
            } else {
                n.put("title", node.getName());
            }
            n.put("x", loc != null ? loc.x : 0);
            n.put("y", loc != null ? loc.y : 0);
            if (textBox) {
                JIPipeGraphCanvasNote note = (JIPipeGraphCanvasNote) node;
                n.put("width", note.getGridWidth() * JIPipeDesktopGraphCanvasGrid.GRID_WIDTH);
                n.put("height", note.getGridHeight() * JIPipeDesktopGraphCanvasGrid.GRID_HEIGHT);
            }
        }
        return data;
    }

    /**
     * Reads one string-valued parameter of a node's parameter tree.
     *
     * @param parameters the node's parameter tree accesses
     * @param key        the parameter key
     * @return the string value, or an empty string when the parameter is absent or not a string
     */
    private static String parameterStringValue(Map<String, JIPipeParameterAccess> parameters, String key) {
        JIPipeParameterAccess access = parameters.get(key);
        if (access == null) return "";
        Object value = access.get(Object.class);
        return value instanceof String s ? s : "";
    }

    /**
     * Searches the nodes of the open project by name, node type id, and
     * compartment. All filters are optional and combined with AND: the query
     * matches case-insensitively as a substring of the node name, the type
     * filter matches case-insensitively as a substring of the registered node
     * type id, and the compartment reference (UUID, alias, or display name)
     * restricts the result to that compartment. Blank filters are treated as
     * omitted. The response holds the "matches" array of query_graph node
     * objects (all canonical fields in canonical order) and "totalCount", the
     * number of matches before the limit is applied.
     *
     * @param ctx            the automation context
     * @param query          optional node name substring filter (null or blank matches all node names)
     * @param typeId         optional node type id substring filter (null or blank matches all node types)
     * @param compartmentRef optional compartment filter (UUID, alias, or display name; null or blank matches all compartments)
     * @param limit          maximum number of matches to return
     * @return the response object with the "matches" array and "totalCount"
     * @throws IllegalArgumentException if the compartment reference cannot be resolved
     */
    public static JsonNode findNodes(AutomationContext ctx, String query, String typeId, String compartmentRef, int limit) {
        ObjectNode data = mapper.createObjectNode();
        ArrayNode matches = data.putArray("matches");
        JIPipeProject project = ctx.getProject();
        if (project == null) {
            data.put("totalCount", 0);
            return data;
        }
        JIPipeGraph graph = project.getGraph();
        String compartment = normalizeFilter(compartmentRef);
        UUID compartmentFilter = null;
        if (compartment != null) {
            compartmentFilter = requireCompartment(project, compartment).getProjectCompartmentUUID();
        }
        // Both matching sides are folded once: the filter values and each node
        // label (the searchNodes convention)
        String nameFilter = normalizeFilter(query);
        if (nameFilter != null) nameFilter = nameFilter.toLowerCase();
        String typeIdFilter = normalizeFilter(typeId);
        if (typeIdFilter != null) typeIdFilter = typeIdFilter.toLowerCase();
        int total = 0;
        for (JIPipeGraphNode node : graph.getGraphNodes()) {
            UUID nodeCompartment = graph.getCompartmentUUIDOf(node);
            if (compartmentFilter != null && !compartmentFilter.equals(nodeCompartment)) continue;
            String nodeTypeId = node.getInfo() != null ? node.getInfo().getId() : "?";
            if (typeIdFilter != null && !nodeTypeId.toLowerCase().contains(typeIdFilter)) continue;
            if (nameFilter != null && !node.getName().toLowerCase().contains(nameFilter)) continue;
            total++;
            if (total <= limit) {
                matches.add(buildGraphNodeObject(graph, node, nodeCompartment, QUERY_GRAPH_NODE_FIELDS));
            }
        }
        data.put("totalCount", total);
        return data;
    }

    // ── Execution (async) ──

    public static InstrumentationJob runNode(AutomationContext ctx, String nodeId, RunMode mode) {
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
        }, "automation-run-" + job.getId());
        runThread.setDaemon(true);
        runThread.start();
        return job;
    }

    public static InstrumentationJob runCompartment(AutomationContext ctx, String compartmentId) {
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
        }, "automation-run-" + job.getId());
        runThread.setDaemon(true);
        runThread.start();
        return job;
    }

    public static InstrumentationJob runPipeline(AutomationContext ctx, RunMode mode) {
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
        }, "automation-run-" + job.getId());
        runThread.setDaemon(true);
        runThread.start();
        return job;
    }

    // ── Modifications ──

    public static String addNode(AutomationContext ctx, String compartmentId, String nodeTypeId, int x, int y) {
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

    public static void removeNode(AutomationContext ctx, String nodeId) {
        JIPipeGraph graph = ctx.getGraph();
        if (graph == null) throw new IllegalStateException("No graph available");
        JIPipeGraphNode node = graph.findNode(nodeId);
        if (node == null) throw new IllegalArgumentException("Node not found: " + nodeId);
        graph.removeNode(node, true);
        ctx.getEventBus().publish(new NodeRemovedEvent(ctx.getProjectId(), nodeId));
    }

    public static void addConnection(AutomationContext ctx, String source, String sourceSlot, String target, String targetSlot) {
        JIPipeGraph graph = ctx.getGraph();
        if (graph == null) throw new IllegalStateException("No graph available");
        JIPipeGraphNode sourceNode = graph.findNode(source);
        JIPipeGraphNode targetNode = graph.findNode(target);
        if (sourceNode == null) throw new IllegalArgumentException("Source node not found: " + source);
        if (targetNode == null) throw new IllegalArgumentException("Target node not found: " + target);
        graph.connect(sourceNode.getOutputSlot(sourceSlot), targetNode.getInputSlot(targetSlot));
        ctx.getEventBus().publish(new ConnectionChangedEvent(ctx.getProjectId(), source, sourceSlot, target, targetSlot, "added"));
    }

    public static void removeConnection(AutomationContext ctx, String source, String sourceSlot, String target, String targetSlot) {
        JIPipeGraph graph = ctx.getGraph();
        if (graph == null) throw new IllegalStateException("No graph available");
        JIPipeGraphNode sourceNode = graph.findNode(source);
        JIPipeGraphNode targetNode = graph.findNode(target);
        if (sourceNode == null) throw new IllegalArgumentException("Source node not found: " + source);
        if (targetNode == null) throw new IllegalArgumentException("Target node not found: " + target);
        graph.disconnect(sourceNode.getOutputSlot(sourceSlot), targetNode.getInputSlot(targetSlot), true);
        ctx.getEventBus().publish(new ConnectionChangedEvent(ctx.getProjectId(), source, sourceSlot, target, targetSlot, "removed"));
    }

    /**
     * Sets a parameter on a node: the text value is converted to the field
     * class of the parameter access, the access is set, and the
     * ParameterChangedEvent is published. Returns the SAME access rendered
     * through {@link #renderParameterValue} AFTER the set, so callers (the
     * set_parameter operation) can verify the effective value after
     * conversion in the same call.
     *
     * @param ctx    the automation context
     * @param nodeId the node id (alias id or UUID)
     * @param key    the parameter key as reported by query_node
     * @param value  the new value as text
     * @return the rendered parameter entry (value, type, raw?)
     */
    public static ObjectNode setParameter(AutomationContext ctx, String nodeId, String key, String value) {
        JIPipeGraph graph = ctx.getGraph();
        if (graph == null) throw new IllegalStateException("No graph available");
        JIPipeGraphNode node = graph.findNode(nodeId);
        if (node == null) throw new IllegalArgumentException("Node not found: " + nodeId);
        JIPipeParameterAccess access = JIPipeParameterTree.getParameters(node).get(key);
        if (access == null) throw new IllegalArgumentException("Parameter not found: " + key);
        Object converted = convertParameterValue(value, access.getFieldClass());
        access.set(converted);
        ctx.getEventBus().publish(new ParameterChangedEvent(ctx.getProjectId(), nodeId, key, value));
        return renderParameterValue(access);
    }

    public static String addCompartment(AutomationContext ctx, String name) {
        JIPipeProject project = ctx.getProject();
        if (project == null) throw new IllegalStateException("No project available");
        var compartment = project.addCompartment(name);
        ctx.getEventBus().publish(new CompartmentChangedEvent(ctx.getProjectId(), compartment.getProjectCompartmentUUID().toString(), name, "added"));
        return compartment.getProjectCompartmentUUID().toString();
    }

    /**
     * Sets project metadata fields. A null argument keeps the current value;
     * a provided authors/acknowledgements list replaces the whole list (an empty
     * list is a replacement with an empty list). Blank strings are stored as-is:
     * clearing a field is a legitimate intent (the FAIR check reports it as missing).
     *
     * @param ctx              the automation context
     * @param name             the new project name (null keeps the current)
     * @param description      the new description as plain text (null keeps the current)
     * @param license          the new license (null keeps the current)
     * @param summary          the new summary as plain text (null keeps the current)
     * @param website          the new website (null keeps the current)
     * @param citation         the new citation (null keeps the current)
     * @param authors          the replacement author list (null keeps the current)
     * @param acknowledgements the replacement acknowledgements list (null keeps the current)
     */
    public static void setProjectMetadata(AutomationContext ctx, String name, String description,
                                          String license, String summary, String website, String citation,
                                          JIPipeAuthorMetadata.List authors, JIPipeAuthorMetadata.List acknowledgements) {
        JIPipeProject project = ctx.getProject();
        if (project == null) throw new IllegalStateException("No project available");
        if (name != null) project.getMetadata().setName(name);
        if (description != null) project.getMetadata().setDescription(new HTMLText(description));
        if (license != null) project.getMetadata().setLicense(license);
        if (summary != null) project.getMetadata().setSummary(new HTMLText(summary));
        if (website != null) project.getMetadata().setWebsite(website);
        if (citation != null) project.getMetadata().setCitation(citation);
        if (authors != null) {
            JIPipeProjectMetadata metadata = project.getMetadata();
            // Fire the authors-changed signal only on an actual change (feature #753
            // pattern), so an open project window recomputes the FAIR badge without
            // being spammed
            boolean changed = !JsonUtils.toJsonString(metadata.getAuthors()).equals(JsonUtils.toJsonString(authors));
            metadata.setAuthors(authors);
            if (changed) {
                metadata.getAuthorsChangedEventEmitter().emit(new JIPipeProjectMetadata.AuthorsChangedEvent(metadata));
            }
        }
        if (acknowledgements != null) {
            project.getMetadata().setAcknowledgements(acknowledgements);
        }
    }

    // ── Pipeline Map ──

    public static JsonNode getPipelineMap(AutomationContext ctx) {
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

    public static JsonNode getSegmentDetail(AutomationContext ctx, String segmentId) {
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

    public static JsonNode searchNodes(AutomationContext ctx, String query, int limit) {
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

    public static JsonNode createROCrate(AutomationContext ctx, String outputPath,
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

    private static Path getProjectSavePath(AutomationContext ctx) {
        org.hkijena.jipipe.desktop.app.JIPipeDesktopProjectWindow window =
                org.hkijena.jipipe.desktop.app.JIPipeDesktopProjectWindow.getWindowFor(ctx.getProject());
        if (window != null) {
            return window.getProjectSavePath();
        }
        return null;
    }

    // ── Helpers ──

    /**
     * Builds one node object of the query_graph response shape, emitting the
     * requested fields in the given order. Shared by queryGraph and findNodes
     * so both responses carry identical node objects.
     *
     * @param graph           the project graph
     * @param node            the node
     * @param nodeCompartment the node's compartment UUID (may be null)
     * @param fields          the fields to emit, in emission order
     * @return the node object
     */
    private static ObjectNode buildGraphNodeObject(JIPipeGraph graph, JIPipeGraphNode node,
                                                   UUID nodeCompartment, List<String> fields) {
        UUID uuid = graph.getUUIDOf(node);
        String alias = graph.getAliasIdOf(node);
        Point loc = node.getNodeUILocationWithin(nodeCompartment != null ? nodeCompartment.toString() : "");
        ObjectNode n = mapper.createObjectNode();
        for (String field : fields) {
            switch (field) {
                case "id" -> n.put("id", alias != null ? alias : (uuid != null ? uuid.toString() : "?"));
                case "name" -> n.put("name", node.getName());
                case "typeId" -> n.put("typeId", node.getInfo() != null ? node.getInfo().getId() : "?");
                case "compartmentId" -> n.put("compartmentId", nodeCompartment != null ? nodeCompartment.toString() : null);
                case "uuid" -> n.put("uuid", uuid != null ? uuid.toString() : null);
                case "x" -> n.put("x", loc != null ? loc.x : 0);
                case "y" -> n.put("y", loc != null ? loc.y : 0);
            }
        }
        return n;
    }

    /**
     * Normalizes an optional string filter: null stays null, blank values
     * (after trimming) are treated as omitted (null), other values are
     * trimmed.
     *
     * @param value the raw filter value (may be null)
     * @return the normalized filter value (null when omitted)
     */
    private static String normalizeFilter(String value) {
        if (value == null) return null;
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    /**
     * Resolves a compartment reference against the project's registered compartments.
     * Resolution order: (a) a well-formed UUID string that matches a registered
     * compartment, (b) the alias lookup of {@link JIPipeProject#findCompartment(String)},
     * (c) the first compartment whose display name equals the reference (case-sensitive).
     * Returns null for a null or blank reference. A well-formed UUID that matches no
     * compartment returns null (no alias or name fallback for UUID input).
     *
     * @param project the project (may be null, then null is returned)
     * @param ref     the compartment UUID, alias, or display name (may be null/blank)
     * @return the compartment or null if the reference could not be resolved
     */
    public static JIPipeProjectCompartment findCompartment(JIPipeProject project, String ref) {
        if (project == null || ref == null || ref.isBlank()) {
            return null;
        }
        UUID uuid = null;
        try {
            uuid = UUID.fromString(ref);
        } catch (IllegalArgumentException ignored) {
            // Not a UUID: resolve via alias and display name below
        }
        if (uuid != null) {
            return project.getCompartments().get(uuid);
        }
        JIPipeProjectCompartment resolved = project.findCompartment(ref);
        if (resolved != null) {
            return resolved;
        }
        for (var entry : project.getCompartments().entrySet()) {
            if (entry.getValue().getName() != null && entry.getValue().getName().equals(ref)) {
                return entry.getValue();
            }
        }
        return null;
    }

    /**
     * Resolves a compartment reference like {@link #findCompartment} but
     * throws the canonical fail-loud {@link IllegalArgumentException} (with
     * the list of known compartments) when the reference cannot be resolved.
     * Shared by the query operations so the error message contract stays
     * identical across them.
     *
     * @param project the project (must not be null)
     * @param ref     the compartment UUID, alias, or display name (must not be null or blank)
     * @return the compartment (never null)
     * @throws IllegalArgumentException if the reference cannot be resolved
     */
    private static JIPipeProjectCompartment requireCompartment(JIPipeProject project, String ref) {
        JIPipeProjectCompartment compartment = findCompartment(project, ref);
        if (compartment == null) {
            StringJoiner knownCompartments = new StringJoiner(", ");
            for (JIPipeProjectCompartment known : project.getCompartments().values()) {
                knownCompartments.add(String.valueOf(known.getName()));
            }
            throw new IllegalArgumentException("Compartment not found: '" + ref
                    + "'. Known compartments: " + knownCompartments);
        }
        return compartment;
    }

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
        Object composite = AutomationOperationUtils.parseCompositeValue(value, fieldClass);
        return composite != null ? composite : value;
    }

    private static UUID resolveCompartmentUUID(AutomationContext ctx, String compartmentId) {
        if (compartmentId == null || compartmentId.isBlank()) return null;
        try {
            return UUID.fromString(compartmentId);
        } catch (IllegalArgumentException e) {
            var comp = ctx.getProject().findCompartment(compartmentId);
            return comp != null ? comp.getProjectCompartmentUUID() : null;
        }
    }
}
