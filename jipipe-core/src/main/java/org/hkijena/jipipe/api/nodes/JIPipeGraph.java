/*
 * Copyright by Zoltán Cseresnyés, Ruman Gerst
 *
 * Research Group Applied Systems Biology - Head: Prof. Dr. Marc Thilo Figge
 * https://www.leibniz-hki.de/en/applied-systems-biology.html
 * HKI-Center for Systems Biology of Infection
 * Leibniz Institute for Natural Product Research and Infection Biology - Hans Knöll Institute (HKI)
 * Adolf-Reichwein-Straße 23, 07745 Jena, Germany
 *
 * The project code is licensed under BSD 2-Clause.
 * See the LICENSE file provided with the code for the full license.
 */

package org.hkijena.jipipe.api.nodes;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.*;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.google.common.collect.*;
import com.google.common.eventbus.EventBus;
import com.google.common.eventbus.Subscribe;
import org.hkijena.jipipe.JIPipeDependency;
import org.hkijena.jipipe.api.JIPipeValidatable;
import org.hkijena.jipipe.api.JIPipeValidityReport;
import org.hkijena.jipipe.api.data.JIPipeDataSlot;
import org.hkijena.jipipe.api.events.*;
import org.hkijena.jipipe.api.exceptions.UserFriendlyRuntimeException;
import org.hkijena.jipipe.api.parameters.JIPipeParameterTree;
import org.hkijena.jipipe.api.registries.JIPipeDatatypeRegistry;
import org.hkijena.jipipe.api.registries.JIPipeNodeRegistry;
import org.hkijena.jipipe.extensions.settings.RuntimeSettings;
import org.hkijena.jipipe.utils.GraphUtils;
import org.hkijena.jipipe.utils.JsonUtils;
import org.hkijena.jipipe.utils.StringUtils;
import org.jgrapht.Graph;
import org.jgrapht.alg.cycle.CycleDetector;
import org.jgrapht.graph.DefaultDirectedGraph;
import org.jgrapht.traverse.GraphIterator;
import org.jgrapht.traverse.TopologicalOrderIterator;

import java.awt.*;
import java.io.IOException;
import java.util.List;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Manages multiple {@link JIPipeGraphNode} instances as graph
 */
@JsonSerialize(using = JIPipeGraph.Serializer.class)
@JsonDeserialize(using = JIPipeGraph.Deserializer.class)
public class JIPipeGraph implements JIPipeValidatable {

    public static final String COMPARTMENT_DEFAULT = "DEFAULT";

    private DefaultDirectedGraph<JIPipeDataSlot, JIPipeGraphEdge> graph = new DefaultDirectedGraph<>(JIPipeGraphEdge.class);
    private BiMap<String, JIPipeGraphNode> algorithms = HashBiMap.create();
    private Map<JIPipeGraphNode, String> compartments = new HashMap<>();
    private List<JIPipeDataSlot> traversedSlots;
    private List<JIPipeGraphNode> traversedAlgorithms;
    private EventBus eventBus = new EventBus();
    /**
     * If this value is greater than one, no events are triggered
     */
    private int preventTriggerEvents = 0;

    /**
     * Creates a new node type graph
     */
    public JIPipeGraph() {
    }

    /**
     * Creates a deep copy of the other algorithm graph
     *
     * @param other The original graph
     */
    public JIPipeGraph(JIPipeGraph other) {
        // Copy nodes
        for (Map.Entry<String, JIPipeGraphNode> kv : other.algorithms.entrySet()) {
            JIPipeGraphNode algorithm = kv.getValue().getInfo().clone(kv.getValue());
            algorithms.put(kv.getKey(), algorithm);
            algorithm.setGraph(this);
            algorithm.getEventBus().register(this);
        }
        repairGraph();

        // Copy edges
        for (Map.Entry<JIPipeDataSlot, JIPipeDataSlot> edge : other.getSlotEdges()) {
            String sourceAlgorithmName = other.algorithms.inverse().get(edge.getKey().getNode());
            String targetAlgorithmName = other.algorithms.inverse().get(edge.getValue().getNode());
            JIPipeGraphNode sourceAlgorithm = algorithms.get(sourceAlgorithmName);
            JIPipeGraphNode targetAlgorithm = algorithms.get(targetAlgorithmName);
            JIPipeDataSlot source = sourceAlgorithm.getOutputSlotMap().get(edge.getKey().getName());
            JIPipeDataSlot target = targetAlgorithm.getInputSlotMap().get(edge.getValue().getName());
            connect(source, target);
            JIPipeGraphEdge copyEdge = graph.getEdge(source, target);
            JIPipeGraphEdge originalEdge = other.graph.getEdge(edge.getKey(), edge.getValue());
            copyEdge.setMetadataFrom(originalEdge);
        }
    }

    /**
     * Inserts an algorithm into the graph
     *
     * @param key         The unique ID
     * @param algorithm   The algorithm
     * @param compartment The compartment where the algorithm will be placed
     * @return the ID of the inserted node
     */
    public String insertNode(String key, JIPipeGraphNode algorithm, String compartment) {
        if (compartment == null)
            throw new NullPointerException("Compartment should not be null!");
        if (algorithms.containsKey(key))
            throw new UserFriendlyRuntimeException("Already contains algorithm with name " + key,
                    "Could not add an algorithm node into the graph!",
                    "Algorithm graph", "There already exists an algorithm with the same identifier.",
                    "If you are loading from a JSON project or plugin, check if the file is valid. Contact " +
                            "the JIPipe or plugin developers for further assistance.");
        algorithm.setCompartment(compartment);
        algorithm.setGraph(this);
        algorithms.put(key, algorithm);
        compartments.put(algorithm, compartment);
        algorithm.getEventBus().register(this);
        ++preventTriggerEvents;
        repairGraph();
        --preventTriggerEvents;

        // Sometimes we have algorithms with no slots, so trigger manually
        postChangedEvent();

        return key;
    }

    private void postChangedEvent() {
        traversedAlgorithms = null;
        traversedSlots = null;
        if (preventTriggerEvents <= 0) {
            preventTriggerEvents = 0;
            eventBus.post(new GraphChangedEvent(this));
        }
    }

    /**
     * Inserts an algorithm into the graph.
     * The name is automatically generated
     *
     * @param algorithm   The algorithm
     * @param compartment The compartment where the algorithm will be placed
     * @return the ID of the inserted node
     */
    public String insertNode(JIPipeGraphNode algorithm, String compartment) {
        String name;
        if (Objects.equals(compartment, COMPARTMENT_DEFAULT))
            name = algorithm.getName();
        else
            name = compartment + "-" + algorithm.getName();
        String uniqueName = StringUtils.makeUniqueString(StringUtils.jsonify(name), " ", algorithms.keySet());
        return insertNode(uniqueName, algorithm, compartment);
    }

    /**
     * Re-assigns new Ids to the all nodes
     *
     * @return how old Ids are assigned to new Ids
     */
    public Map<String, String> cleanupIds() {
        Map<String, String> renaming = new HashMap<>();
        List<JIPipeGraphNode> traversedAlgorithms = traverseAlgorithms();
        ImmutableBiMap<String, JIPipeGraphNode> oldIds = ImmutableBiMap.copyOf(algorithms);
        algorithms.clear();
        for (JIPipeGraphNode algorithm : traversedAlgorithms) {
            String compartment = algorithm.getCompartment();
            String name;
            if (Objects.equals(compartment, COMPARTMENT_DEFAULT))
                name = algorithm.getName();
            else
                name = compartment + "-" + algorithm.getName();
            String newId = StringUtils.makeUniqueString(StringUtils.jsonify(name), " ", algorithms.keySet());
            algorithms.put(newId, algorithm);
            String oldId = oldIds.inverse().get(algorithm);
            renaming.put(oldId, newId);
        }
        postChangedEvent();
        return renaming;
    }

    /**
     * Returns true if the user can delete the algorithm
     *
     * @param algorithm The algorithm
     * @return True if the user can delete the algorithm
     */
    public boolean canUserDelete(JIPipeGraphNode algorithm) {
        return algorithm.getCategory().userCanDelete();
    }

    /**
     * Returns true if the user can disconnect the slots
     *
     * @param source Source slot
     * @param target Target slot
     * @return True if the user can disconnect those slots. Returns false if the connection does not exist.
     */
    public boolean canUserDisconnect(JIPipeDataSlot source, JIPipeDataSlot target) {
        if (graph.containsEdge(source, target)) {
            JIPipeGraphEdge edge = graph.getEdge(source, target);
            if (edge != null) {
                return edge.isUserCanDisconnect();
            }
        }
        return false;
    }

    /**
     * Removes a whole compartment from the graph.
     * Will fail silently if the ID does not exist
     *
     * @param compartment The compartment ID
     */
    public void removeCompartment(String compartment) {
        Set<String> ids = algorithms.keySet().stream().filter(id -> compartment.equals(compartments.get(algorithms.get(id)))).collect(Collectors.toSet());
        for (String id : ids) {
            removeNode(algorithms.get(id), false);
        }
    }

    /**
     * Renames a compartment
     *
     * @param compartment    Original compartment ID
     * @param newCompartment New compartment ID
     */
    public void renameCompartment(String compartment, String newCompartment) {
        for (JIPipeGraphNode algorithm : algorithms.values().stream().filter(a -> compartment.equals(compartments.get(a))).collect(Collectors.toSet())) {
            compartments.put(algorithm, newCompartment);
        }
        postChangedEvent();
    }

    /**
     * Removes an algorithm.
     * The algorithm should exist within the graph.
     *
     * @param algorithm The algorithm
     * @param user      if a user triggered the operation. If true, will not remove internal nodes
     */
    public void removeNode(JIPipeGraphNode algorithm, boolean user) {
        if (user && !algorithm.getCategory().userCanDelete())
            return;
        ++preventTriggerEvents;
        // Do regular disconnect
        for (JIPipeDataSlot slot : algorithm.getInputSlots()) {
            disconnectAll(slot, false);
        }
        for (JIPipeDataSlot slot : algorithm.getOutputSlots()) {
            disconnectAll(slot, false);
        }

        // Do internal remove operation
        algorithms.remove(getIdOf(algorithm));
        compartments.remove(algorithm);
        algorithm.getEventBus().unregister(this);
        for (JIPipeDataSlot slot : algorithm.getInputSlots()) {
            graph.removeVertex(slot);
        }
        for (JIPipeDataSlot slot : algorithm.getOutputSlots()) {
            graph.removeVertex(slot);
        }
        algorithm.setGraph(null);
        --preventTriggerEvents;
        postChangedEvent();
    }

    /**
     * Slow implementation of connection checking that tests for graph cycles
     *
     * @param source Source slot. Must be an output.
     * @param target Target slot. Must be an input.
     * @param user   If true, the user is generating the connection
     * @return True if the connection could be possible
     */
    public boolean canConnect(JIPipeDataSlot source, JIPipeDataSlot target, boolean user) {
        if (!canConnectFast(source, target, user))
            return false;
        Graph<JIPipeDataSlot, JIPipeGraphEdge> copy = (Graph<JIPipeDataSlot, JIPipeGraphEdge>) graph.clone();
        copy.addEdge(source, target);
        CycleDetector<JIPipeDataSlot, JIPipeGraphEdge> cycleDetector = new CycleDetector<>(copy);
        return !cycleDetector.detectCycles();
    }

    /**
     * Fast implementation of connection checking without copying the graph
     *
     * @param source Source slot. Must be an output.
     * @param target Target slot. Must be an input.
     * @param user   If triggered by a user
     * @return True if the connection could be possible. Please check again with canConnect()
     */
    public boolean canConnectFast(JIPipeDataSlot source, JIPipeDataSlot target, boolean user) {
        if (!source.isOutput() || !target.isInput())
            return false;
        if (user && !JIPipeDatatypeRegistry.getInstance().isConvertible(source.getAcceptedDataType(), target.getAcceptedDataType()))
            return false;
        if (graph.inDegreeOf(target) > 0)
            return false;
        return true;
    }

    /**
     * Connects an output slot to an input slot.
     * Allows users to disconnect this connection
     *
     * @param source An output slot
     * @param target An input slot
     */
    public void connect(JIPipeDataSlot source, JIPipeDataSlot target) {
        connect(source, target, true);
    }

    /**
     * Connects an output slot to an input slot.
     *
     * @param source            An output slot.
     * @param target            An input slot.
     * @param userCanDisconnect If true, users are allowed to disconnect this connection again
     */
    public void connect(JIPipeDataSlot source, JIPipeDataSlot target, boolean userCanDisconnect) {
        if (!canConnect(source, target, false))
            throw new UserFriendlyRuntimeException("Cannot connect data slots: " + source.getDisplayName() + " ==> " + target.getDisplayName(),
                    "Cannot create a connection between '" + source.getDisplayName() + "' and '" + target.getDisplayName() + "'!",
                    "Algorithm graph", "The connection is invalid, such as one that causes cycles in the graph, or a connection where a slot receives multiple inputs",
                    "Check if your pipeline contains complicated sections prone to cycles. Reorganize the graph by dragging the nodes around.");
        graph.addEdge(source, target, new JIPipeGraphEdge(userCanDisconnect));
        postChangedEvent();
        getEventBus().post(new NodeConnectedEvent(this, source, target));
        source.getNode().onSlotConnected(new NodeConnectedEvent(this, source, target));
        target.getNode().onSlotConnected(new NodeConnectedEvent(this, source, target));
    }

    /**
     * Extracts all parameters as tree
     *
     * @return the tree
     */
    public JIPipeParameterTree getParameterTree() {
        JIPipeParameterTree tree = new JIPipeParameterTree();
        for (Map.Entry<String, JIPipeGraphNode> entry : getNodes().entrySet()) {
            JIPipeParameterTree.Node node = tree.add(entry.getValue(), entry.getKey(), null);
            node.setName(entry.getValue().getName());
            node.setDescription(entry.getValue().getCustomDescription());
        }
        return tree;
    }

    /**
     * Repairs the graph by removing old nodes and adding missing ones
     */
    public void repairGraph() {

        boolean modified = false;

        // Remove deleted slots from the graph
        Set<JIPipeDataSlot> toRemove = new HashSet<>();
        for (JIPipeGraphNode algorithm : algorithms.values()) {
            for (JIPipeDataSlot slot : graph.vertexSet()) {
                if (slot.getNode() == algorithm && !algorithm.getInputSlots().contains(slot) &&
                        !algorithm.getOutputSlots().contains(slot)) {
                    toRemove.add(slot);
                    slot.getEventBus().unregister(this);
                    modified = true;
                }
            }
        }
        toRemove.forEach(graph::removeVertex);

        // Add missing slots
        for (JIPipeGraphNode algorithm : algorithms.values()) {

            // Add vertices
            for (JIPipeDataSlot inputSlot : algorithm.getInputSlots()) {
                if (!graph.vertexSet().contains(inputSlot)) {
                    graph.addVertex(inputSlot);
                    inputSlot.getEventBus().register(this);
                    modified = true;
                }
            }
            for (JIPipeDataSlot outputSlot : algorithm.getOutputSlots()) {
                if (!graph.vertexSet().contains(outputSlot)) {
                    graph.addVertex(outputSlot);
                    outputSlot.getEventBus().register(this);
                    modified = true;
                }
            }

            // Connect input -> output in the graph
            for (JIPipeDataSlot inputSlot : algorithm.getInputSlots()) {
                for (JIPipeDataSlot outputSlot : algorithm.getOutputSlots()) {
                    if (!graph.containsEdge(inputSlot, outputSlot)) {
                        graph.addEdge(inputSlot, outputSlot);
                        modified = true;
                    }
                }
            }
        }

        if (modified)
            postChangedEvent();
    }

    /**
     * If exists, returns the output slot that provides data for the input slot
     * Returns null if target is an output or no slot exists
     *
     * @param target The input slot
     * @return The output slot that generates data for the input. Null if no source exists.
     */
    public JIPipeDataSlot getSourceSlot(JIPipeDataSlot target) {
        if (target.isInput()) {
            Set<JIPipeGraphEdge> edges = graph.incomingEdgesOf(target);
            if (edges.isEmpty())
                return null;
            if (edges.size() > 1)
                throw new UserFriendlyRuntimeException("Graph is illegal!", "The algorithm graph is invalid!",
                        "Algorithm graph", "There is at least one input slot with multiple inputs.",
                        "Open the project or JSON extension file and look for a section 'edges'. Ensure that each slot is only at most once on the right-hand side of ':'. " +
                                "You can also contact the JIPipe developers - after checking if you use the newest version - , as this should done automatically for you.");
            return graph.getEdgeSource(edges.iterator().next());
        }
        return null;
    }

    /**
     * Returns the list of input slots that are provided by the source slot
     * Returns an empty set if the target is an input or no slot exists
     *
     * @param source An output slot
     * @return All slots that receive data from the output slot
     */
    public Set<JIPipeDataSlot> getTargetSlots(JIPipeDataSlot source) {
        if (source.isOutput()) {
            Set<JIPipeGraphEdge> edges = graph.outgoingEdgesOf(source);
            Set<JIPipeDataSlot> result = new HashSet<>();
            for (JIPipeGraphEdge edge : edges) {
                result.add(graph.getEdgeTarget(edge));
            }
            return result;
        }
        return Collections.emptySet();
    }

    /**
     * Returns all available sources for an input slot.
     * This function does not check for cycles.
     *
     * @param target An input slot
     * @param user   If true, only connections that can be created by a user are shown
     * @param fast   If true, use canConnectFast instead of canConnect
     * @return Set of potential sources
     */
    public Set<JIPipeDataSlot> getAvailableSources(JIPipeDataSlot target, boolean user, boolean fast) {
        if (getSourceSlot(target) != null)
            return Collections.emptySet();
        Set<JIPipeDataSlot> result = new HashSet<>();
        for (JIPipeDataSlot source : graph.vertexSet()) {
            if (source == target)
                continue;
            if (!source.isOutput())
                continue;
            if (source.getNode() == target.getNode())
                continue;
            if (graph.containsEdge(source, target))
                continue;
            if (fast && !canConnectFast(source, target, user))
                continue;
            if (!fast && !canConnect(source, target, user))
                continue;
            result.add(source);
        }
        return result;
    }

    /**
     * Completely disconnects a slot
     *
     * @param slot An input or output slot
     * @param user If true, indicates that is disconnect was issued by a user
     */
    public void disconnectAll(JIPipeDataSlot slot, boolean user) {
        if (slot.isInput()) {
            JIPipeDataSlot source = getSourceSlot(slot);
            if (source != null) {
                disconnect(source, slot, user);
            }
        } else if (slot.isOutput()) {
            for (JIPipeDataSlot target : getTargetSlots(slot)) {
                disconnect(slot, target, user);
            }
        }
    }

    /**
     * Disconnects an input and an output slot
     *
     * @param source An output slot
     * @param target An input slot
     * @param user   Indicates if the command was issued by a user
     * @return True if the disconnect was successful, otherwise false
     */
    public boolean disconnect(JIPipeDataSlot source, JIPipeDataSlot target, boolean user) {
        if (graph.containsEdge(source, target)) {
            if (user && !canUserDisconnect(source, target))
                return false;
            graph.removeEdge(source, target);
            getEventBus().post(new NodeDisconnectedEvent(this, source, target));
            postChangedEvent();
            source.getNode().onSlotDisconnected(new NodeDisconnectedEvent(this, source, target));
            target.getNode().onSlotDisconnected(new NodeDisconnectedEvent(this, source, target));
            return true;
        }
        return false;
    }

    /**
     * Returns all available target for an output slot
     * Does not check for cycles.
     *
     * @param source An output slot
     * @param user   Indicates that a user issues the connection
     * @param fast   If true, use canConnectFast instead of canConnect
     * @return A list of all available input slots
     */
    public Set<JIPipeDataSlot> getAvailableTargets(JIPipeDataSlot source, boolean user, boolean fast) {
        if (source.isInput())
            return Collections.emptySet();
        Set<JIPipeDataSlot> result = new HashSet<>();
        for (JIPipeDataSlot target : graph.vertexSet()) {
            if (source == target)
                continue;
            if (!target.isInput())
                continue;
            if (target.getNode() == source.getNode())
                continue;
            if (graph.containsEdge(source, target))
                continue;
            if (getSourceSlot(target) != null)
                continue;
            if (fast && !canConnectFast(source, target, user))
                continue;
            if (!fast && !canConnect(source, target, user))
                continue;
            result.add(target);
        }
        return result;
    }

    /**
     * Should be triggered when an {@link JIPipeGraphNode}'s slots are changed.
     * Triggers a graph repair
     *
     * @param event The generated event
     */
    @Subscribe
    public void onAlgorithmSlotsChanged(NodeSlotsChangedEvent event) {
        repairGraph();
    }

    /**
     * Should be triggered when an {@link JIPipeGraphNode}'s parameter structure is changed.
     * This event is re-triggered into getEventBus()
     *
     * @param event Generated event
     */
    @Subscribe
    public void onParameterStructureChanged(ParameterStructureChangedEvent event) {
        eventBus.post(event);
    }

    /**
     * Gets all dependencies of all algorithms
     *
     * @return Set of dependencies
     */
    public Set<JIPipeDependency> getDependencies() {
        Set<JIPipeDependency> result = new HashSet<>();
        for (JIPipeGraphNode algorithm : algorithms.values()) {
            result.addAll(algorithm.getDependencies());
        }
        return result;
    }

    /**
     * Returns the even bus
     *
     * @return Event bus instance
     */
    public EventBus getEventBus() {
        return eventBus;
    }

    /**
     * Returns all algorithms
     *
     * @return Map from algorithm instance ID to algorithm instance
     */
    public BiMap<String, JIPipeGraphNode> getNodes() {
        return ImmutableBiMap.copyOf(algorithms);
    }

    /**
     * Returns true if this graph contains the algorithm
     *
     * @param algorithm The algorithm instance
     * @return True if the algorithm is part of this graph
     */
    public boolean containsNode(JIPipeGraphNode algorithm) {
        return algorithms.containsValue(algorithm);
    }

    /**
     * Returns all edges as set of edges
     *
     * @return Set of entries where the key is an output slot and the value is an input slot
     */
    public Set<Map.Entry<JIPipeDataSlot, JIPipeDataSlot>> getSlotEdges() {
        Set<Map.Entry<JIPipeDataSlot, JIPipeDataSlot>> result = new HashSet<>();
        for (JIPipeGraphEdge edge : graph.edgeSet()) {
            JIPipeDataSlot source = graph.getEdgeSource(edge);
            JIPipeDataSlot target = graph.getEdgeTarget(edge);
            if (source.getNode() == target.getNode())
                continue;
            result.add(new AbstractMap.SimpleImmutableEntry<>(source, target));
        }
        return result;
    }

    /**
     * Loads this graph from JSON
     *
     * @param node JSON data
     * @param issues
     */
    public void fromJson(JsonNode node, JIPipeValidityReport issues) {
        if (!node.has("nodes"))
            return;

        for (Map.Entry<String, JsonNode> kv : ImmutableList.copyOf(node.get("nodes").fields())) {
            if (!algorithms.containsKey(kv.getKey())) {
                String id = kv.getValue().get("jipipe:node-info-id").asText();
                if (!JIPipeNodeRegistry.getInstance().hasNodeInfoWithId(id)) {
                    System.err.println("Unable to find node with ID '" + id + "'. Skipping.");
                    issues.forCategory("Nodes").forCategory(id).reportIsInvalid("Unable to find node type '" + id + "'!",
                            "The JSON data requested to load a node of type '" + id + "', but it is not known to JIPipe.",
                            "Please check if all extensions are are correctly loaded.",
                            node);
                    continue;
                }
                JIPipeNodeInfo info = JIPipeNodeRegistry.getInstance().getInfoById(id);
                JIPipeGraphNode algorithm = info.newInstance();
                algorithm.fromJson(kv.getValue());
                insertNode(StringUtils.jsonify(kv.getKey()), algorithm, algorithm.getCompartment());
            }
        }

        // Load edges
        for (JsonNode edgeNode : ImmutableList.copyOf(node.get("edges").elements())) {
            String sourceAlgorithmName = edgeNode.get("source-node").asText();
            String targetAlgorithmName = edgeNode.get("target-node").asText();
            JIPipeGraphNode sourceAlgorithm = algorithms.get(sourceAlgorithmName);
            JIPipeGraphNode targetAlgorithm = algorithms.get(targetAlgorithmName);
            if (sourceAlgorithm == null) {
                issues.forCategory("Edges").forCategory("Source").forCategory(sourceAlgorithmName).reportIsInvalid("Unable to find node '" + sourceAlgorithmName + "'!",
                        "The JSON data requested to create an edge between the nodes '" + sourceAlgorithmName + "' and '" + targetAlgorithmName + "', but the source does not exist. " +
                                "This might have been caused by a previous error.",
                        "Please check if all extensions are are correctly loaded.",
                        node);
                System.err.println("Unable to find node with ID '" + sourceAlgorithmName + "'. Skipping this instruction.");
                continue;
            }
            if (targetAlgorithm == null) {
                issues.forCategory("Edges").forCategory("Target").forCategory(sourceAlgorithmName).reportIsInvalid("Unable to find node '" + targetAlgorithmName + "'!",
                        "The JSON data requested to create an edge between the nodes '" + sourceAlgorithmName + "' and '" + targetAlgorithmName + "', but the source does not exist. " +
                                "This might have been caused by a previous error.",
                        "Please check if all extensions are are correctly loaded.",
                        node);
                System.err.println("Unable to find node with ID '" + targetAlgorithmName + "'. Skipping this instruction.");
                continue;
            }
            String sourceSlotName = edgeNode.get("source-slot").asText();
            String targetSlotName = edgeNode.get("target-slot").asText();
            JIPipeDataSlot source = sourceAlgorithm.getOutputSlotMap().get(sourceSlotName);
            JIPipeDataSlot target = targetAlgorithm.getInputSlotMap().get(targetSlotName);
            if (source == null) {
                issues.forCategory("Edges").forCategory("Output slots").forCategory(sourceAlgorithmName).reportIsInvalid("Unable to find output slot '" + sourceSlotName + "' in node '" + sourceAlgorithmName + "'!",
                        "The JSON data requested to create an edge between the nodes '" + sourceAlgorithmName + "' and '" + targetAlgorithmName + "', but the source slot does not exist. " +
                                "This might have been caused by a previous error.",
                        "Please check if all extensions are are correctly loaded.",
                        node);
                System.err.println("Unable to find data slot '" + sourceSlotName + "' in algorithm '" + sourceAlgorithmName + "'. Skipping this instruction.");
                continue;
            }
            if (target == null) {
                issues.forCategory("Edges").forCategory("Input slots").forCategory(sourceAlgorithmName).reportIsInvalid("Unable to find input slot '" + targetSlotName + "' in node '" + targetAlgorithmName + "'!",
                        "The JSON data requested to create an edge between the nodes '" + sourceAlgorithmName + "' and '" + targetAlgorithmName + "', but the target slot does not exist. " +
                                "This might have been caused by a previous error.",
                        "Please check if all extensions are are correctly loaded.",
                        node);
                System.err.println("Unable to find data slot '" + targetSlotName + "' in algorithm '" + targetAlgorithmName + "'. Skipping this instruction.");
                continue;
            }
            if (!graph.containsEdge(source, target) && graph.inDegreeOf(target) > 0) {
                issues.forCategory("Edges").forCategory("Validation").forCategory(sourceAlgorithmName).reportIsInvalid("Invalid edge found!",
                        "The JSON data requested to create an edge between the nodes '" + sourceAlgorithmName + "' and '" + targetAlgorithmName + "', but the edge is invalid.",
                        "Please check the JSON data manually or ignore this error.",
                        node);
                System.err.println("Detected invalid edge from " + source.getNode().getIdInGraph() + "/" + source.getName() + " -> "
                        + target.getNode().getIdInGraph() + "/" + target.getName() + "! Input slot already has a source. Skipping this instruction.");
                continue;
            }
            if (!graph.containsEdge(source, target))
                connect(source, target);
            JsonNode metadataNode = edgeNode.path("metadata");
            if (!metadataNode.isMissingNode()) {
                JIPipeGraphEdge edgeInstance = graph.getEdge(source, target);
                try {
                    JsonUtils.getObjectMapper().readerForUpdating(edgeInstance).readValue(metadataNode);
                } catch (IOException e) {
                    issues.forCategory("Metadata").reportIsInvalid("Unable to deserialize graph metadata!",
                            "The JSON data contains some metadata, but it could not be recovered.",
                            "Metadata does not contain critical information. You can ignore this message.",
                            node);
                    System.err.println("Cannot deserialize edge metadata!");
                    e.printStackTrace();
                }
            }
        }
    }

    /**
     * Merges another graph into this graph
     * The input is not copied!
     *
     * @param otherGraph The other graph
     * @return A map from ID in source graph to algorithm in target graph
     */
    public Map<String, JIPipeGraphNode> mergeWith(JIPipeGraph otherGraph) {
        Map<String, JIPipeGraphNode> insertedNodes = new HashMap<>();
        for (JIPipeGraphNode algorithm : otherGraph.getNodes().values()) {
            String newId = insertNode(algorithm, algorithm.getCompartment());
            insertedNodes.put(newId, algorithm);
        }
        for (Map.Entry<JIPipeDataSlot, JIPipeDataSlot> edge : otherGraph.getSlotEdges()) {
            JIPipeGraphNode copySource = insertedNodes.get(edge.getKey().getNode().getIdInGraph());
            JIPipeGraphNode copyTarget = insertedNodes.get(edge.getValue().getNode().getIdInGraph());
            connect(copySource.getOutputSlotMap().get(edge.getKey().getName()), copyTarget.getInputSlotMap().get(edge.getValue().getName()));
        }
        return insertedNodes;
    }

    /**
     * Copies the selected algorithms into a new graph
     * Connections between the nodes are kept
     *
     * @param nodes        the nodes
     * @param withInternal also copy internal algorithms
     * @return graph that only contains the selected algorithms
     */
    public JIPipeGraph extract(Collection<JIPipeGraphNode> nodes, boolean withInternal) {
        JIPipeGraph graph = new JIPipeGraph();
        for (JIPipeGraphNode algorithm : nodes) {
            if (!withInternal && !algorithm.getCategory().canExtract())
                continue;
            JIPipeGraphNode copy = algorithm.getInfo().clone(algorithm);
            if (copy.getCompartment() != null) {
                Map<String, Point> map = copy.getLocations().get(copy.getCompartment());
                copy.getLocations().clear();
                copy.getLocations().put(JIPipeGraph.COMPARTMENT_DEFAULT, map);
            }
            graph.insertNode(algorithm.getIdInGraph(), copy, JIPipeGraph.COMPARTMENT_DEFAULT);
        }
        for (Map.Entry<JIPipeDataSlot, JIPipeDataSlot> edge : getSlotEdges()) {
            JIPipeDataSlot source = edge.getKey();
            JIPipeDataSlot target = edge.getValue();
            if (nodes.contains(source.getNode()) && nodes.contains(target.getNode())) {
                graph.connect(graph.getEquivalentSlot(source), graph.getEquivalentSlot(target));
            }
        }
        return graph;
    }

    /**
     * @return The number of all algorithms
     */
    public int getAlgorithmCount() {
        return algorithms.size();
    }

    /**
     * @return The number of all algorithm slots in this graph
     */
    public int getSlotCount() {
        return graph.vertexSet().size();
    }

    /**
     * Traverses the graph in topological order.
     * The order guarantees that input is always available
     *
     * @return Sorted list of data slots
     */
    public List<JIPipeDataSlot> traverseSlots() {
        if (traversedSlots != null)
            return Collections.unmodifiableList(traversedSlots);
        GraphIterator<JIPipeDataSlot, JIPipeGraphEdge> iterator = new TopologicalOrderIterator<>(graph);
        List<JIPipeDataSlot> result = new ArrayList<>();
        while (iterator.hasNext()) {
            JIPipeDataSlot slot = iterator.next();
            result.add(slot);
        }
        this.traversedSlots = result;
        return Collections.unmodifiableList(result);
    }

    /**
     * Returns all predecessor algorithms of an algorithm. The predecessors are ordered according to the list of traversed algorithms (topological order)
     *
     * @param target    the target algorithm
     * @param traversed list of algorithms to sort by (usually this is in topological order)
     * @return predecessors in topological order
     */
    public List<JIPipeGraphNode> getPredecessorAlgorithms(JIPipeGraphNode target, List<JIPipeGraphNode> traversed) {
        Set<JIPipeGraphNode> predecessors = new HashSet<>();
        for (JIPipeDataSlot inputSlot : target.getInputSlots()) {
            for (JIPipeDataSlot predecessor : GraphUtils.getAllPredecessors(graph, inputSlot)) {
                predecessors.add(predecessor.getNode());
            }
        }
        predecessors.remove(target);
        List<JIPipeGraphNode> output = new ArrayList<>();
        for (JIPipeGraphNode node : traversed) {
            if (predecessors.contains(node))
                output.add(node);
        }
        return output;
    }

    /**
     * Gets all algorithms and all dependent algorithms that are missing inputs or are deactivated by the user
     *
     * @return list of algorithms
     */
    public Set<JIPipeGraphNode> getDeactivatedAlgorithms() {
        Set<JIPipeGraphNode> missing = new HashSet<>();
        for (JIPipeGraphNode algorithm : traverseAlgorithms()) {
            if (algorithm instanceof JIPipeAlgorithm) {
                if (!((JIPipeAlgorithm) algorithm).isEnabled()) {
                    missing.add(algorithm);
                    continue;
                }
            }
            for (JIPipeDataSlot inputSlot : algorithm.getInputSlots()) {
                JIPipeDataSlot sourceSlot = getSourceSlot(inputSlot);
                if (sourceSlot == null) {
                    missing.add(algorithm);
                    break;
                }
                if (missing.contains(sourceSlot.getNode())) {
                    missing.add(algorithm);
                    break;
                }
            }
        }
        return missing;
    }

    /**
     * Gets all algorithms and all dependent algorithms that are missing inputs or are deactivated by the user
     *
     * @param externallySatisfied list of algorithms that have their input set externally
     * @return list of algorithms
     */
    public Set<JIPipeGraphNode> getDeactivatedAlgorithms(Set<JIPipeGraphNode> externallySatisfied) {
        Set<JIPipeGraphNode> missing = new HashSet<>();
        for (JIPipeGraphNode algorithm : traverseAlgorithms()) {
            if (externallySatisfied.contains(algorithm))
                continue;
            if (algorithm instanceof JIPipeAlgorithm) {
                if (!((JIPipeAlgorithm) algorithm).isEnabled()) {
                    missing.add(algorithm);
                    continue;
                }
            }
            for (JIPipeDataSlot inputSlot : algorithm.getInputSlots()) {
                JIPipeDataSlot sourceSlot = getSourceSlot(inputSlot);
                if (sourceSlot == null) {
                    missing.add(algorithm);
                    break;
                }
                if (missing.contains(sourceSlot.getNode())) {
                    missing.add(algorithm);
                    break;
                }
            }
        }
        return missing;
    }

    /**
     * Traverses the graph in topological order.
     * The order guarantees that input is always available
     *
     * @return Sorted list of algorithms
     */
    public List<JIPipeGraphNode> traverseAlgorithms() {
        if (traversedAlgorithms != null)
            return Collections.unmodifiableList(traversedAlgorithms);

        Set<JIPipeGraphNode> visited = new HashSet<>();
        List<JIPipeGraphNode> result = new ArrayList<>();
        for (JIPipeDataSlot slot : traverseSlots()) {
            if (slot.isOutput()) {
                if (!visited.contains(slot.getNode())) {
                    visited.add(slot.getNode());
                    result.add(slot.getNode());
                }
            }
        }
        for (JIPipeGraphNode missing : algorithms.values()) {
            if (!visited.contains(missing)) {
                result.add(missing);
            }
        }
        this.traversedAlgorithms = result;
        return Collections.unmodifiableList(result);
    }

    /**
     * @return All data slots
     */
    public Set<JIPipeDataSlot> getSlotNodes() {
        return graph.vertexSet();
    }

    /**
     * Returns true if the slot is in this graph
     *
     * @param slot A slot
     * @return True if this graph contains the slot
     */
    public boolean containsNode(JIPipeDataSlot slot) {
        return graph.vertexSet().contains(slot);
    }

    @Override
    public void reportValidity(JIPipeValidityReport report) {
        for (Map.Entry<String, JIPipeGraphNode> entry : algorithms.entrySet()) {
            if (entry.getValue() instanceof JIPipeAlgorithm) {
                JIPipeAlgorithm algorithm = (JIPipeAlgorithm) entry.getValue();
                if (!algorithm.isEnabled() || (algorithm.canPassThrough() && algorithm.isPassThrough()))
                    continue;
            }
            report.forCategory(entry.getValue().getCompartment()).forCategory(entry.getValue().getName()).report(entry.getValue());
        }
        if (!RuntimeSettings.getInstance().isAllowSkipAlgorithmsWithoutInput()) {
            for (JIPipeDataSlot slot : graph.vertexSet()) {
                if (slot.isInput()) {
                    if (graph.incomingEdgesOf(slot).isEmpty()) {
                        report.forCategory(slot.getNode().getCompartment()).forCategory(slot.getNode().getName())
                                .forCategory("Slot: " + slot.getName()).reportIsInvalid("An input slot has no incoming data!",
                                "Input slots must always be provided with input data.",
                                "Please connect the slot to an output of another algorithm.",
                                this);
                    }
                }
            }
        }
    }

    /**
     * Reports the validity for the target node and its dependencies
     *
     * @param report     the report
     * @param targetNode the target node
     */
    public void reportValidity(JIPipeValidityReport report, JIPipeGraphNode targetNode) {
        reportValidity(report, targetNode, Collections.emptySet());
    }

    /**
     * Reports the validity for the target node and its dependencies
     *
     * @param report     the report
     * @param targetNode the target node
     * @param satisfied  all algorithms that are considered to have a satisfied input
     */
    public void reportValidity(JIPipeValidityReport report, JIPipeGraphNode targetNode, Set<JIPipeGraphNode> satisfied) {
        List<JIPipeGraphNode> predecessorAlgorithms = getPredecessorAlgorithms(targetNode, traverseAlgorithms());
        predecessorAlgorithms.add(targetNode);
        for (JIPipeGraphNode node : predecessorAlgorithms) {
            if (satisfied.contains(node))
                continue;
            if (node instanceof JIPipeAlgorithm) {
                JIPipeAlgorithm algorithm = (JIPipeAlgorithm) node;
                if (algorithm.canPassThrough() && algorithm.isPassThrough()) {
                    continue;
                }
                if (!algorithm.isEnabled()) {
                    report.forCategory(node.getCompartment()).forCategory(node.getName()).reportIsInvalid(
                            "Dependency algorithm is deactivated!",
                            "A dependency algorithm is not enabled. It blocks the execution of all following algorithms.",
                            "Check if all dependency algorithms are enabled. If you just want to skip the processing, try 'Pass through'.",
                            algorithm
                    );
                    return;
                }
            }
            for (JIPipeDataSlot slot : node.getInputSlots()) {
                if (graph.incomingEdgesOf(slot).isEmpty()) {
                    report.forCategory(slot.getNode().getCompartment()).forCategory(slot.getNode().getName())
                            .forCategory("Slot: " + slot.getName()).reportIsInvalid("An input slot has no incoming data!",
                            "Input slots must always be provided with input data.",
                            "Please connect the slot to an output of another algorithm.",
                            this);
                    return;
                }
            }

            report.forCategory(node.getCompartment()).forCategory(node.getName()).report(node);
        }
    }

    /**
     * @return The underlying graph
     */
    public Graph<JIPipeDataSlot, JIPipeGraphEdge> getGraph() {
        return graph;
    }

    /**
     * Clears this graph
     */
    public void clear() {
        for (JIPipeGraphNode algorithm : ImmutableSet.copyOf(algorithms.values())) {
            removeNode(algorithm, false);
        }
    }

    /**
     * Returns the ID of the algorithm within this graph.
     * The algorithm must be a part of this graph.
     *
     * @param algorithm The algorithm
     * @return The ID of this algorithm within the graph.
     */
    public String getIdOf(JIPipeGraphNode algorithm) {
        return algorithms.inverse().get(algorithm);
    }

    /**
     * Returns true if this graph contains an algorithm with the same ID as the foreign algorithm in the foreign graph
     *
     * @param foreign      An algorithm
     * @param foreignGraph Graph that contains the foreign algorithm
     * @return True if this graph contains an equivalent algorithm (with the same ID)
     */
    public boolean containsEquivalentOf(JIPipeGraphNode foreign, JIPipeGraph foreignGraph) {
        return getNodes().containsKey(foreignGraph.getIdOf(foreign));
    }

    /**
     * Returns the algorithm that has the same ID as the foreign algorithm in the foreign graph.
     *
     * @param foreign An algorithm
     * @return Equivalent algorithm within this graph
     */
    public JIPipeGraphNode getEquivalentAlgorithm(JIPipeGraphNode foreign) {
        return getNodes().get(foreign.getIdInGraph());
    }

    /**
     * Returns the slot with the same name within the algorithm with the same ID.
     *
     * @param foreign A data slot
     * @return slot with the same name within the algorithm with the same ID
     */
    public JIPipeDataSlot getEquivalentSlot(JIPipeDataSlot foreign) {
        if (foreign.isInput())
            return getNodes().get(foreign.getNode().getIdInGraph()).getInputSlotMap().get(foreign.getName());
        else
            return getNodes().get(foreign.getNode().getIdInGraph()).getOutputSlotMap().get(foreign.getName());
    }

    /**
     * Returns the graph compartment ID of the algorithm
     * This graph must contain the algorithm
     *
     * @param algorithm An algorithm
     * @return The compartment ID
     */
    public String getCompartmentOf(JIPipeGraphNode algorithm) {
        return compartments.get(algorithm);
    }

    /**
     * @return All unconnected slots
     */
    public List<JIPipeDataSlot> getUnconnectedSlots() {
        List<JIPipeDataSlot> result = new ArrayList<>();
        for (JIPipeDataSlot slot : traverseSlots()) {
            if (slot.isInput()) {
                if (getSourceSlot(slot) == null)
                    result.add(slot);
            } else if (slot.isOutput()) {
                if (getTargetSlots(slot).isEmpty())
                    result.add(slot);
            }
        }

        return result;
    }

    /**
     * Returns the algorithms within the specified compartment ID.
     *
     * @param compartmentId The compartment ID
     * @return Algorithms that have the specified compartment ID. Empty set if the compartment does not exist.
     */
    public Set<JIPipeGraphNode> getAlgorithmsWithCompartment(String compartmentId) {
        Set<JIPipeGraphNode> result = new HashSet<>();
        for (JIPipeGraphNode algorithm : algorithms.values()) {
            if (algorithm.getCompartment().equals(compartmentId))
                result.add(algorithm);
        }
        return result;
    }

    /**
     * Removes multiple nodes at once
     *
     * @param nodes list of nodes
     * @param user  if the operation is done by a user
     */
    public void removeNodes(Set<JIPipeGraphNode> nodes, boolean user) {
        ++preventTriggerEvents;
        for (JIPipeGraphNode node : nodes) {
            removeNode(node, user);
        }
        --preventTriggerEvents;
        postChangedEvent();
    }

    /**
     * Replaces all contents with the ones in the other graph
     * Does not apply copying!
     *
     * @param other the other graph
     */
    public void replaceWith(JIPipeGraph other) {
        ++preventTriggerEvents;
        this.algorithms.clear();
        this.compartments.clear();
        this.algorithms.putAll(other.algorithms);
        this.compartments.putAll(other.compartments);
        for (JIPipeGraphNode node : this.algorithms.values()) {
            node.getEventBus().register(this);
        }
        this.graph = other.graph;
        --preventTriggerEvents;
        postChangedEvent();
    }

    /**
     * Gets all edges between two algorithm nodes
     *
     * @param source the source
     * @param target the target
     * @return all edges
     */
    public Set<Map.Entry<JIPipeDataSlot, JIPipeDataSlot>> getEdgesBetween(JIPipeGraphNode source, JIPipeGraphNode target) {
        Set<Map.Entry<JIPipeDataSlot, JIPipeDataSlot>> result = new HashSet<>();
        for (JIPipeDataSlot outputSlot : source.getOutputSlots()) {
            for (JIPipeDataSlot targetSlot : getTargetSlots(outputSlot)) {
                if (targetSlot.getNode() == target) {
                    result.add(new AbstractMap.SimpleEntry<>(outputSlot, targetSlot));
                }
            }
        }
        return result;
    }

    /**
     * Serializes an {@link JIPipeGraph}
     */
    public static class Serializer extends JsonSerializer<JIPipeGraph> {
        @Override
        public void serialize(JIPipeGraph algorithmGraph, JsonGenerator jsonGenerator, SerializerProvider serializerProvider) throws IOException, JsonProcessingException {
            jsonGenerator.writeStartObject();

            jsonGenerator.writeFieldName("nodes");
            jsonGenerator.writeStartObject();
            serializeNodes(algorithmGraph, jsonGenerator);
            jsonGenerator.writeEndObject();

            jsonGenerator.writeFieldName("edges");
            jsonGenerator.writeStartArray();
            serializeEdges(algorithmGraph, jsonGenerator);
            jsonGenerator.writeEndArray();

            jsonGenerator.writeEndObject();
        }

        private void serializeNodes(JIPipeGraph algorithmGraph, JsonGenerator jsonGenerator) throws IOException {
            for (Map.Entry<String, JIPipeGraphNode> kv : algorithmGraph.algorithms.entrySet()) {
                jsonGenerator.writeObjectField(StringUtils.jsonify(kv.getKey()), kv.getValue());
            }
        }

        private void serializeEdges(JIPipeGraph graph, JsonGenerator jsonGenerator) throws IOException {
            for (Map.Entry<JIPipeDataSlot, JIPipeDataSlot> edge : graph.getSlotEdges()) {
                jsonGenerator.writeStartObject();
                jsonGenerator.writeStringField("source-node", StringUtils.jsonify(graph.getIdOf(edge.getKey().getNode())));
                jsonGenerator.writeStringField("target-node", StringUtils.jsonify(graph.getIdOf(edge.getValue().getNode())));
                jsonGenerator.writeStringField("source-slot", StringUtils.makeFilesystemCompatible(edge.getKey().getName()));
                jsonGenerator.writeStringField("target-slot", StringUtils.makeFilesystemCompatible(edge.getValue().getName()));
                jsonGenerator.writeObjectField("metadata", graph.getGraph().getEdge(edge.getKey(), edge.getValue()));
                jsonGenerator.writeEndObject();
            }
        }
    }

    /**
     * Deserializes an {@link JIPipeGraph}
     */
    public static class Deserializer extends JsonDeserializer<JIPipeGraph> {
        @Override
        public JIPipeGraph deserialize(JsonParser jsonParser, DeserializationContext deserializationContext) throws IOException, JsonProcessingException {
            JIPipeGraph graph = new JIPipeGraph();
            graph.fromJson(jsonParser.readValueAsTree(), new JIPipeValidityReport());
            return graph;
        }
    }
}
