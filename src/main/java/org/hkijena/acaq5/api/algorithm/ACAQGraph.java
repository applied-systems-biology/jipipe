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

package org.hkijena.acaq5.api.algorithm;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.google.common.collect.BiMap;
import com.google.common.collect.HashBiMap;
import com.google.common.collect.ImmutableBiMap;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import com.google.common.eventbus.EventBus;
import com.google.common.eventbus.Subscribe;
import org.hkijena.acaq5.ACAQDependency;
import org.hkijena.acaq5.api.ACAQValidatable;
import org.hkijena.acaq5.api.ACAQValidityReport;
import org.hkijena.acaq5.api.compartments.algorithms.ACAQProjectCompartment;
import org.hkijena.acaq5.api.data.ACAQDataSlot;
import org.hkijena.acaq5.api.events.AlgorithmGraphChangedEvent;
import org.hkijena.acaq5.api.events.AlgorithmGraphConnectedEvent;
import org.hkijena.acaq5.api.events.AlgorithmGraphDisconnectedEvent;
import org.hkijena.acaq5.api.events.AlgorithmSlotsChangedEvent;
import org.hkijena.acaq5.api.events.ParameterStructureChangedEvent;
import org.hkijena.acaq5.api.exceptions.UserFriendlyRuntimeException;
import org.hkijena.acaq5.api.parameters.ACAQParameterTree;
import org.hkijena.acaq5.api.registries.ACAQAlgorithmRegistry;
import org.hkijena.acaq5.api.registries.ACAQDatatypeRegistry;
import org.hkijena.acaq5.extensions.settings.RuntimeSettings;
import org.hkijena.acaq5.utils.GraphUtils;
import org.hkijena.acaq5.utils.StringUtils;
import org.jgrapht.Graph;
import org.jgrapht.alg.cycle.CycleDetector;
import org.jgrapht.graph.DefaultDirectedGraph;
import org.jgrapht.traverse.GraphIterator;
import org.jgrapht.traverse.TopologicalOrderIterator;

import java.awt.Point;
import java.io.IOException;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Manages multiple {@link ACAQGraphNode} instances as graph
 */
@JsonSerialize(using = ACAQGraph.Serializer.class)
@JsonDeserialize(using = ACAQGraph.Deserializer.class)
public class ACAQGraph implements ACAQValidatable {

    public static final String COMPARTMENT_DEFAULT = "DEFAULT";

    private DefaultDirectedGraph<ACAQDataSlot, ACAQGraphEdge> graph = new DefaultDirectedGraph<>(ACAQGraphEdge.class);
    private BiMap<String, ACAQGraphNode> algorithms = HashBiMap.create();
    private Map<ACAQGraphNode, String> compartments = new HashMap<>();
    private List<ACAQDataSlot> traversedSlots;
    private List<ACAQGraphNode> traversedAlgorithms;
    private EventBus eventBus = new EventBus();
    /**
     * If this value is greater than one, no events are triggered
     */
    private int preventTriggerEvents = 0;

    /**
     * Creates a new algorithm graph
     */
    public ACAQGraph() {

    }

    /**
     * Creates a deep copy of the other algorithm graph
     *
     * @param other The original graph
     */
    public ACAQGraph(ACAQGraph other) {
        // Copy nodes
        for (Map.Entry<String, ACAQGraphNode> kv : other.algorithms.entrySet()) {
            ACAQGraphNode algorithm = kv.getValue().getDeclaration().clone(kv.getValue());
            algorithms.put(kv.getKey(), algorithm);
            algorithm.setGraph(this);
            algorithm.getEventBus().register(this);
        }
        repairGraph();

        // Copy edges
        for (Map.Entry<ACAQDataSlot, ACAQDataSlot> edge : other.getSlotEdges()) {
            String sourceAlgorithmName = other.algorithms.inverse().get(edge.getKey().getNode());
            String targetAlgorithmName = other.algorithms.inverse().get(edge.getValue().getNode());
            ACAQGraphNode sourceAlgorithm = algorithms.get(sourceAlgorithmName);
            ACAQGraphNode targetAlgorithm = algorithms.get(targetAlgorithmName);
            connect(sourceAlgorithm.getOutputSlotMap().get(edge.getKey().getName()),
                    targetAlgorithm.getInputSlotMap().get(edge.getValue().getName()));
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
    public String insertNode(String key, ACAQGraphNode algorithm, String compartment) {
        if (compartment == null)
            throw new NullPointerException("Compartment should not be null!");
        if (algorithms.containsKey(key))
            throw new UserFriendlyRuntimeException("Already contains algorithm with name " + key,
                    "Could not add an algorithm node into the graph!",
                    "Algorithm graph", "There already exists an algorithm with the same identifier.",
                    "If you are loading from a JSON project or plugin, check if the file is valid. Contact " +
                            "the ACAQ or plugin developers for further assistance.");
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
            eventBus.post(new AlgorithmGraphChangedEvent(this));
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
    public String insertNode(ACAQGraphNode algorithm, String compartment) {
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
        List<ACAQGraphNode> traversedAlgorithms = traverseAlgorithms();
        ImmutableBiMap<String, ACAQGraphNode> oldIds = ImmutableBiMap.copyOf(algorithms);
        algorithms.clear();
        for (ACAQGraphNode algorithm : traversedAlgorithms) {
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
    public boolean canUserDelete(ACAQGraphNode algorithm) {
        return algorithm.getCategory() != ACAQAlgorithmCategory.Internal || algorithm instanceof ACAQProjectCompartment;
    }

    /**
     * Returns true if the user can disconnect the slots
     *
     * @param source Source slot
     * @param target Target slot
     * @return True if the user can disconnect those slots. Returns false if the connection does not exist.
     */
    public boolean canUserDisconnect(ACAQDataSlot source, ACAQDataSlot target) {
        if (graph.containsEdge(source, target)) {
            ACAQGraphEdge edge = graph.getEdge(source, target);
            if (edge != null) {
                return edge.isUserDisconnectable();
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
        for (ACAQGraphNode algorithm : algorithms.values().stream().filter(a -> compartment.equals(compartments.get(a))).collect(Collectors.toSet())) {
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
    public void removeNode(ACAQGraphNode algorithm, boolean user) {
        if (user && algorithm.getCategory() == ACAQAlgorithmCategory.Internal)
            return;
        ++preventTriggerEvents;
        // Do regular disconnect
        for (ACAQDataSlot slot : algorithm.getInputSlots()) {
            disconnectAll(slot, false);
        }
        for (ACAQDataSlot slot : algorithm.getOutputSlots()) {
            disconnectAll(slot, false);
        }

        // Do internal remove operation
        algorithms.remove(getIdOf(algorithm));
        compartments.remove(algorithm);
        algorithm.getEventBus().unregister(this);
        for (ACAQDataSlot slot : algorithm.getInputSlots()) {
            graph.removeVertex(slot);
        }
        for (ACAQDataSlot slot : algorithm.getOutputSlots()) {
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
    public boolean canConnect(ACAQDataSlot source, ACAQDataSlot target, boolean user) {
        if (!canConnectFast(source, target, user))
            return false;
        Graph<ACAQDataSlot, ACAQGraphEdge> copy = (Graph<ACAQDataSlot, ACAQGraphEdge>) graph.clone();
        copy.addEdge(source, target);
        CycleDetector<ACAQDataSlot, ACAQGraphEdge> cycleDetector = new CycleDetector<>(copy);
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
    public boolean canConnectFast(ACAQDataSlot source, ACAQDataSlot target, boolean user) {
        if (!source.isOutput() || !target.isInput())
            return false;
        if (user && !ACAQDatatypeRegistry.getInstance().isConvertible(source.getAcceptedDataType(), target.getAcceptedDataType()))
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
    public void connect(ACAQDataSlot source, ACAQDataSlot target) {
        connect(source, target, true);
    }

    /**
     * Connects an output slot to an input slot.
     *
     * @param source            An output slot.
     * @param target            An input slot.
     * @param userCanDisconnect If true, users are allowed to disconnect this connection again
     */
    public void connect(ACAQDataSlot source, ACAQDataSlot target, boolean userCanDisconnect) {
        if (!canConnect(source, target, false))
            throw new UserFriendlyRuntimeException("Cannot connect data slots: " + source.getNameWithAlgorithmName() + " ==> " + target.getNameWithAlgorithmName(),
                    "Cannot create a connection between '" + source.getNameWithAlgorithmName() + "' and '" + target.getNameWithAlgorithmName() + "'!",
                    "Algorithm graph", "The connection is invalid, such as one that causes cycles in the graph, or a connection where a slot receives multiple inputs",
                    "Check if your pipeline contains complicated sections prone to cycles. Reorganize the graph by dragging the nodes around.");
        graph.addEdge(source, target, new ACAQGraphEdge(userCanDisconnect));
        postChangedEvent();
        getEventBus().post(new AlgorithmGraphConnectedEvent(this, source, target));
        source.getNode().onSlotConnected(new AlgorithmGraphConnectedEvent(this, source, target));
        target.getNode().onSlotConnected(new AlgorithmGraphConnectedEvent(this, source, target));
    }

    /**
     * Extracts all parameters as tree
     *
     * @return the tree
     */
    public ACAQParameterTree getParameterTree() {
        ACAQParameterTree tree = new ACAQParameterTree();
        for (Map.Entry<String, ACAQGraphNode> entry : getAlgorithmNodes().entrySet()) {
            ACAQParameterTree.Node node = tree.add(entry.getValue(), entry.getKey(), null);
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
        Set<ACAQDataSlot> toRemove = new HashSet<>();
        for (ACAQGraphNode algorithm : algorithms.values()) {
            for (ACAQDataSlot slot : graph.vertexSet()) {
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
        for (ACAQGraphNode algorithm : algorithms.values()) {

            // Add vertices
            for (ACAQDataSlot inputSlot : algorithm.getInputSlots()) {
                if (!graph.vertexSet().contains(inputSlot)) {
                    graph.addVertex(inputSlot);
                    inputSlot.getEventBus().register(this);
                    modified = true;
                }
            }
            for (ACAQDataSlot outputSlot : algorithm.getOutputSlots()) {
                if (!graph.vertexSet().contains(outputSlot)) {
                    graph.addVertex(outputSlot);
                    outputSlot.getEventBus().register(this);
                    modified = true;
                }
            }

            // Connect input -> output in the graph
            for (ACAQDataSlot inputSlot : algorithm.getInputSlots()) {
                for (ACAQDataSlot outputSlot : algorithm.getOutputSlots()) {
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
    public ACAQDataSlot getSourceSlot(ACAQDataSlot target) {
        if (target.isInput()) {
            Set<ACAQGraphEdge> edges = graph.incomingEdgesOf(target);
            if (edges.isEmpty())
                return null;
            if (edges.size() > 1)
                throw new UserFriendlyRuntimeException("Graph is illegal!", "The algorithm graph is invalid!",
                        "Algorithm graph", "There is at least one input slot with multiple inputs.",
                        "Open the project or JSON extension file and look for a section 'edges'. Ensure that each slot is only at most once on the right-hand side of ':'. " +
                                "You can also contact the ACAQ5 developers - after checking if you use the newest version - , as this should done automatically for you.");
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
    public Set<ACAQDataSlot> getTargetSlots(ACAQDataSlot source) {
        if (source.isOutput()) {
            Set<ACAQGraphEdge> edges = graph.outgoingEdgesOf(source);
            Set<ACAQDataSlot> result = new HashSet<>();
            for (ACAQGraphEdge edge : edges) {
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
    public Set<ACAQDataSlot> getAvailableSources(ACAQDataSlot target, boolean user, boolean fast) {
        if (getSourceSlot(target) != null)
            return Collections.emptySet();
        Set<ACAQDataSlot> result = new HashSet<>();
        for (ACAQDataSlot source : graph.vertexSet()) {
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
    public void disconnectAll(ACAQDataSlot slot, boolean user) {
        if (slot.isInput()) {
            ACAQDataSlot source = getSourceSlot(slot);
            if (source != null) {
                disconnect(source, slot, user);
            }
        } else if (slot.isOutput()) {
            for (ACAQDataSlot target : getTargetSlots(slot)) {
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
    public boolean disconnect(ACAQDataSlot source, ACAQDataSlot target, boolean user) {
        if (graph.containsEdge(source, target)) {
            if (user && !canUserDisconnect(source, target))
                return false;
            graph.removeEdge(source, target);
            getEventBus().post(new AlgorithmGraphDisconnectedEvent(this, source, target));
            postChangedEvent();
            source.getNode().onSlotDisconnected(new AlgorithmGraphDisconnectedEvent(this, source, target));
            target.getNode().onSlotDisconnected(new AlgorithmGraphDisconnectedEvent(this, source, target));
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
    public Set<ACAQDataSlot> getAvailableTargets(ACAQDataSlot source, boolean user, boolean fast) {
        if (source.isInput())
            return Collections.emptySet();
        Set<ACAQDataSlot> result = new HashSet<>();
        for (ACAQDataSlot target : graph.vertexSet()) {
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
     * Should be triggered when an {@link ACAQGraphNode}'s slots are changed.
     * Triggers a graph repair
     *
     * @param event The generated event
     */
    @Subscribe
    public void onAlgorithmSlotsChanged(AlgorithmSlotsChangedEvent event) {
        repairGraph();
    }

    /**
     * Should be triggered when an {@link ACAQGraphNode}'s parameter structure is changed.
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
    public Set<ACAQDependency> getDependencies() {
        Set<ACAQDependency> result = new HashSet<>();
        for (ACAQGraphNode algorithm : algorithms.values()) {
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
    public BiMap<String, ACAQGraphNode> getAlgorithmNodes() {
        return ImmutableBiMap.copyOf(algorithms);
    }

    /**
     * Returns true if this graph contains the algorithm
     *
     * @param algorithm The algorithm instance
     * @return True if the algorithm is part of this graph
     */
    public boolean containsNode(ACAQGraphNode algorithm) {
        return algorithms.containsValue(algorithm);
    }

    /**
     * Returns all edges as set of edges
     *
     * @return Set of entries where the key is an output slot and the value is an input slot
     */
    public Set<Map.Entry<ACAQDataSlot, ACAQDataSlot>> getSlotEdges() {
        Set<Map.Entry<ACAQDataSlot, ACAQDataSlot>> result = new HashSet<>();
        for (ACAQGraphEdge edge : graph.edgeSet()) {
            ACAQDataSlot source = graph.getEdgeSource(edge);
            ACAQDataSlot target = graph.getEdgeTarget(edge);
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
     */
    public void fromJson(JsonNode node) {
        if (!node.has("nodes"))
            return;

        for (Map.Entry<String, JsonNode> kv : ImmutableList.copyOf(node.get("nodes").fields())) {
            if (!algorithms.containsKey(kv.getKey())) {
                String declarationInfo = kv.getValue().get("acaq:algorithm-type").asText();
                ACAQAlgorithmDeclaration declaration = ACAQAlgorithmRegistry.getInstance().getDeclarationById(declarationInfo);
                ACAQGraphNode algorithm = declaration.newInstance();
                algorithm.fromJson(kv.getValue());
                insertNode(StringUtils.jsonify(kv.getKey()), algorithm, algorithm.getCompartment());
            }
        }

        // Load edges
        for (JsonNode edgeNode : ImmutableList.copyOf(node.get("edges").elements())) {
            String sourceAlgorithmName = edgeNode.get("source-algorithm").asText();
            String targetAlgorithmName = edgeNode.get("target-algorithm").asText();
            ACAQGraphNode sourceAlgorithm = algorithms.get(sourceAlgorithmName);
            ACAQGraphNode targetAlgorithm = algorithms.get(targetAlgorithmName);
            if (sourceAlgorithm == null) {
                System.err.println("Unable to find algorithm with ID '" + sourceAlgorithmName + "'. Skipping this instruction.");
                continue;
            }
            if (targetAlgorithm == null) {
                System.err.println("Unable to find algorithm with ID '" + targetAlgorithmName + "'. Skipping this instruction.");
                continue;
            }
            String sourceSlotName = edgeNode.get("source-slot").asText();
            String targetSlotName = edgeNode.get("target-slot").asText();
            ACAQDataSlot source = sourceAlgorithm.getOutputSlotMap().get(sourceSlotName);
            ACAQDataSlot target = targetAlgorithm.getInputSlotMap().get(targetSlotName);
            if (source == null) {
                System.err.println("Unable to find data slot '" + sourceSlotName + "' in algorithm '" + sourceAlgorithmName + "'. Skipping this instruction.");
                continue;
            }
            if (target == null) {
                System.err.println("Unable to find data slot '" + targetSlotName + "' in algorithm '" + targetAlgorithmName + "'. Skipping this instruction.");
                continue;
            }
            if (!graph.containsEdge(source, target) && graph.inDegreeOf(target) > 0) {
                System.err.println("Detected invalid edge from " + source.getNode().getIdInGraph() + "/" + source.getName() + " -> "
                        + target.getNode().getIdInGraph() + "/" + target.getName() + "! Input slot already has a source. Skipping this instruction.");
                continue;
            }
            if (!graph.containsEdge(source, target))
                connect(source, target);
        }
    }

    /**
     * Merges another graph into this graph
     * The input is not copied!
     *
     * @param otherGraph The other graph
     * @return A map from ID in source graph to algorithm in target graph
     */
    public Map<String, ACAQGraphNode> mergeWith(ACAQGraph otherGraph) {
        Map<String, ACAQGraphNode> insertedNodes = new HashMap<>();
        for (ACAQGraphNode algorithm : otherGraph.getAlgorithmNodes().values()) {
            String newId = insertNode(algorithm, algorithm.getCompartment());
            insertedNodes.put(newId, algorithm);
        }
        for (Map.Entry<ACAQDataSlot, ACAQDataSlot> edge : otherGraph.getSlotEdges()) {
            ACAQGraphNode copySource = insertedNodes.get(edge.getKey().getNode().getIdInGraph());
            ACAQGraphNode copyTarget = insertedNodes.get(edge.getValue().getNode().getIdInGraph());
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
    public ACAQGraph extract(Collection<ACAQGraphNode> nodes, boolean withInternal) {
        ACAQGraph graph = new ACAQGraph();
        for (ACAQGraphNode algorithm : nodes) {
            if (!withInternal && algorithm.getCategory() == ACAQAlgorithmCategory.Internal)
                continue;
            ACAQGraphNode copy = algorithm.getDeclaration().clone(algorithm);
            if (copy.getCompartment() != null) {
                Map<String, Point> map = copy.getLocations().get(copy.getCompartment());
                copy.getLocations().clear();
                copy.getLocations().put(ACAQGraph.COMPARTMENT_DEFAULT, map);
            }
            graph.insertNode(algorithm.getIdInGraph(), copy, ACAQGraph.COMPARTMENT_DEFAULT);
        }
        for (Map.Entry<ACAQDataSlot, ACAQDataSlot> edge : getSlotEdges()) {
            ACAQDataSlot source = edge.getKey();
            ACAQDataSlot target = edge.getValue();
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
    public List<ACAQDataSlot> traverseSlots() {
        if (traversedSlots != null)
            return Collections.unmodifiableList(traversedSlots);
        GraphIterator<ACAQDataSlot, ACAQGraphEdge> iterator = new TopologicalOrderIterator<>(graph);
        List<ACAQDataSlot> result = new ArrayList<>();
        while (iterator.hasNext()) {
            ACAQDataSlot slot = iterator.next();
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
    public List<ACAQGraphNode> getPredecessorAlgorithms(ACAQGraphNode target, List<ACAQGraphNode> traversed) {
        Set<ACAQGraphNode> predecessors = new HashSet<>();
        for (ACAQDataSlot inputSlot : target.getInputSlots()) {
            for (ACAQDataSlot predecessor : GraphUtils.getAllPredecessors(graph, inputSlot)) {
                predecessors.add(predecessor.getNode());
            }
        }
        predecessors.remove(target);
        List<ACAQGraphNode> output = new ArrayList<>();
        for (ACAQGraphNode node : traversed) {
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
    public Set<ACAQGraphNode> getDeactivatedAlgorithms() {
        Set<ACAQGraphNode> missing = new HashSet<>();
        for (ACAQGraphNode algorithm : traverseAlgorithms()) {
            if (algorithm instanceof ACAQAlgorithm) {
                if (!((ACAQAlgorithm) algorithm).isEnabled()) {
                    missing.add(algorithm);
                    continue;
                }
            }
            for (ACAQDataSlot inputSlot : algorithm.getInputSlots()) {
                ACAQDataSlot sourceSlot = getSourceSlot(inputSlot);
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
    public Set<ACAQGraphNode> getDeactivatedAlgorithms(Set<ACAQGraphNode> externallySatisfied) {
        Set<ACAQGraphNode> missing = new HashSet<>();
        for (ACAQGraphNode algorithm : traverseAlgorithms()) {
            if (externallySatisfied.contains(algorithm))
                continue;
            if (algorithm instanceof ACAQAlgorithm) {
                if (!((ACAQAlgorithm) algorithm).isEnabled()) {
                    missing.add(algorithm);
                    continue;
                }
            }
            for (ACAQDataSlot inputSlot : algorithm.getInputSlots()) {
                ACAQDataSlot sourceSlot = getSourceSlot(inputSlot);
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
    public List<ACAQGraphNode> traverseAlgorithms() {
        if (traversedAlgorithms != null)
            return Collections.unmodifiableList(traversedAlgorithms);

        Set<ACAQGraphNode> visited = new HashSet<>();
        List<ACAQGraphNode> result = new ArrayList<>();
        for (ACAQDataSlot slot : traverseSlots()) {
            if (slot.isOutput()) {
                if (!visited.contains(slot.getNode())) {
                    visited.add(slot.getNode());
                    result.add(slot.getNode());
                }
            }
        }
        for (ACAQGraphNode missing : algorithms.values()) {
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
    public Set<ACAQDataSlot> getSlotNodes() {
        return graph.vertexSet();
    }

    /**
     * Returns true if the slot is in this graph
     *
     * @param slot A slot
     * @return True if this graph contains the slot
     */
    public boolean containsNode(ACAQDataSlot slot) {
        return graph.vertexSet().contains(slot);
    }

    @Override
    public void reportValidity(ACAQValidityReport report) {
        for (Map.Entry<String, ACAQGraphNode> entry : algorithms.entrySet()) {
            if (entry.getValue() instanceof ACAQAlgorithm) {
                ACAQAlgorithm algorithm = (ACAQAlgorithm) entry.getValue();
                if (!algorithm.isEnabled() || (algorithm.canPassThrough() && algorithm.isPassThrough()))
                    continue;
            }
            report.forCategory(entry.getValue().getCompartment()).forCategory(entry.getValue().getName()).report(entry.getValue());
        }
        if (!RuntimeSettings.getInstance().isAllowSkipAlgorithmsWithoutInput()) {
            for (ACAQDataSlot slot : graph.vertexSet()) {
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
    public void reportValidity(ACAQValidityReport report, ACAQGraphNode targetNode) {
        reportValidity(report, targetNode, Collections.emptySet());
    }

    /**
     * Reports the validity for the target node and its dependencies
     *
     * @param report     the report
     * @param targetNode the target node
     * @param satisfied  all algorithms that are considered to have a satisfied input
     */
    public void reportValidity(ACAQValidityReport report, ACAQGraphNode targetNode, Set<ACAQGraphNode> satisfied) {
        List<ACAQGraphNode> predecessorAlgorithms = getPredecessorAlgorithms(targetNode, traverseAlgorithms());
        predecessorAlgorithms.add(targetNode);
        for (ACAQGraphNode node : predecessorAlgorithms) {
            if (satisfied.contains(node))
                continue;
            if (node instanceof ACAQAlgorithm) {
                ACAQAlgorithm algorithm = (ACAQAlgorithm) node;
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
            for (ACAQDataSlot slot : node.getInputSlots()) {
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
    public Graph<ACAQDataSlot, ACAQGraphEdge> getGraph() {
        return graph;
    }

    /**
     * Clears this graph
     */
    public void clear() {
        for (ACAQGraphNode algorithm : ImmutableSet.copyOf(algorithms.values())) {
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
    public String getIdOf(ACAQGraphNode algorithm) {
        return algorithms.inverse().get(algorithm);
    }

    /**
     * Returns true if this graph contains an algorithm with the same ID as the foreign algorithm in the foreign graph
     *
     * @param foreign      An algorithm
     * @param foreignGraph Graph that contains the foreign algorithm
     * @return True if this graph contains an equivalent algorithm (with the same ID)
     */
    public boolean containsEquivalentOf(ACAQGraphNode foreign, ACAQGraph foreignGraph) {
        return getAlgorithmNodes().containsKey(foreignGraph.getIdOf(foreign));
    }

    /**
     * Returns the algorithm that has the same ID as the foreign algorithm in the foreign graph.
     *
     * @param foreign An algorithm
     * @return Equivalent algorithm within this graph
     */
    public ACAQGraphNode getEquivalentAlgorithm(ACAQGraphNode foreign) {
        return getAlgorithmNodes().get(foreign.getIdInGraph());
    }

    /**
     * Returns the slot with the same name within the algorithm with the same ID.
     *
     * @param foreign A data slot
     * @return slot with the same name within the algorithm with the same ID
     */
    public ACAQDataSlot getEquivalentSlot(ACAQDataSlot foreign) {
        if (foreign.isInput())
            return getAlgorithmNodes().get(foreign.getNode().getIdInGraph()).getInputSlotMap().get(foreign.getName());
        else
            return getAlgorithmNodes().get(foreign.getNode().getIdInGraph()).getOutputSlotMap().get(foreign.getName());
    }

    /**
     * Returns the graph compartment ID of the algorithm
     * This graph must contain the algorithm
     *
     * @param algorithm An algorithm
     * @return The compartment ID
     */
    public String getCompartmentOf(ACAQGraphNode algorithm) {
        return compartments.get(algorithm);
    }

    /**
     * @return All unconnected slots
     */
    public List<ACAQDataSlot> getUnconnectedSlots() {
        List<ACAQDataSlot> result = new ArrayList<>();
        for (ACAQDataSlot slot : traverseSlots()) {
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
    public Set<ACAQGraphNode> getAlgorithmsWithCompartment(String compartmentId) {
        Set<ACAQGraphNode> result = new HashSet<>();
        for (ACAQGraphNode algorithm : algorithms.values()) {
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
    public void removeNodes(Set<ACAQGraphNode> nodes, boolean user) {
        ++preventTriggerEvents;
        for (ACAQGraphNode node : nodes) {
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
    public void replaceWith(ACAQGraph other) {
        ++preventTriggerEvents;
        this.algorithms.clear();
        this.compartments.clear();
        this.algorithms.putAll(other.algorithms);
        this.compartments.putAll(other.compartments);
        for (ACAQGraphNode node : this.algorithms.values()) {
            node.getEventBus().register(this);
        }
        this.graph = other.graph;
        --preventTriggerEvents;
        postChangedEvent();
    }

    /**
     * Serializes an {@link ACAQGraph}
     */
    public static class Serializer extends JsonSerializer<ACAQGraph> {
        @Override
        public void serialize(ACAQGraph algorithmGraph, JsonGenerator jsonGenerator, SerializerProvider serializerProvider) throws IOException, JsonProcessingException {
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

        private void serializeNodes(ACAQGraph algorithmGraph, JsonGenerator jsonGenerator) throws IOException {
            for (Map.Entry<String, ACAQGraphNode> kv : algorithmGraph.algorithms.entrySet()) {
                jsonGenerator.writeObjectField(StringUtils.jsonify(kv.getKey()), kv.getValue());
            }
        }

        private void serializeEdges(ACAQGraph graph, JsonGenerator jsonGenerator) throws IOException {
            for (Map.Entry<ACAQDataSlot, ACAQDataSlot> edge : graph.getSlotEdges()) {
                jsonGenerator.writeStartObject();
                jsonGenerator.writeStringField("source-algorithm", StringUtils.jsonify(graph.getIdOf(edge.getKey().getNode())));
                jsonGenerator.writeStringField("target-algorithm", StringUtils.jsonify(graph.getIdOf(edge.getValue().getNode())));
                jsonGenerator.writeStringField("source-slot", StringUtils.makeFilesystemCompatible(edge.getKey().getName()));
                jsonGenerator.writeStringField("target-slot", StringUtils.makeFilesystemCompatible(edge.getValue().getName()));
                jsonGenerator.writeEndObject();
            }
        }
    }

    /**
     * Deserializes an {@link ACAQGraph}
     */
    public static class Deserializer extends JsonDeserializer<ACAQGraph> {
        @Override
        public ACAQGraph deserialize(JsonParser jsonParser, DeserializationContext deserializationContext) throws IOException, JsonProcessingException {
            ACAQGraph graph = new ACAQGraph();
            graph.fromJson(jsonParser.readValueAsTree());
            return graph;
        }
    }
}
