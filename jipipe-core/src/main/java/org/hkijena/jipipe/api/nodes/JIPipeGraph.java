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
import com.google.common.collect.BiMap;
import com.google.common.collect.HashBiMap;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import org.hkijena.jipipe.JIPipe;
import org.hkijena.jipipe.JIPipeDependency;
import org.hkijena.jipipe.api.JIPipeFunctionallyComparable;
import org.hkijena.jipipe.api.JIPipeGraphType;
import org.hkijena.jipipe.api.JIPipeProject;
import org.hkijena.jipipe.api.compartments.algorithms.JIPipeProjectCompartment;
import org.hkijena.jipipe.api.data.JIPipeDataSlot;
import org.hkijena.jipipe.api.events.AbstractJIPipeEvent;
import org.hkijena.jipipe.api.events.JIPipeEventEmitter;
import org.hkijena.jipipe.api.grouping.GraphWrapperAlgorithm;
import org.hkijena.jipipe.api.looping.LoopEndNode;
import org.hkijena.jipipe.api.looping.LoopGroup;
import org.hkijena.jipipe.api.looping.LoopStartNode;
import org.hkijena.jipipe.api.notifications.JIPipeNotificationInbox;
import org.hkijena.jipipe.api.parameters.JIPipeParameterCollection;
import org.hkijena.jipipe.api.parameters.JIPipeParameterTree;
import org.hkijena.jipipe.api.validation.*;
import org.hkijena.jipipe.api.validation.contexts.GraphNodeSlotValidationReportContext;
import org.hkijena.jipipe.api.validation.contexts.GraphNodeValidationReportContext;
import org.hkijena.jipipe.api.validation.contexts.GraphValidationReportContext;
import org.hkijena.jipipe.api.validation.contexts.UnspecifiedValidationReportContext;
import org.hkijena.jipipe.extensions.settings.RuntimeSettings;
import org.hkijena.jipipe.utils.GraphUtils;
import org.hkijena.jipipe.utils.ParameterUtils;
import org.hkijena.jipipe.utils.ReflectionUtils;
import org.hkijena.jipipe.utils.StringUtils;
import org.hkijena.jipipe.utils.json.JsonUtils;
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
public class JIPipeGraph implements JIPipeValidatable, JIPipeFunctionallyComparable, JIPipeGraphNode.NodeSlotsChangedEventListener, JIPipeParameterCollection.ParameterStructureChangedEventListener {

    private final BiMap<UUID, String> nodeAliasIds = HashBiMap.create();
    private final Map<UUID, UUID> nodeCompartmentUUIDs = new HashMap<>();
    private final Map<UUID, Set<UUID>> nodeVisibleCompartmentUUIDs = new HashMap<>();
    private final Map<UUID, String> nodeLegacyCompartmentIDs = new HashMap<>();
    private final BiMap<UUID, JIPipeGraphNode> nodeUUIDs = HashBiMap.create();
    private final JIPipeParameterCollection.ParameterStructureChangedEventEmitter parameterStructureChangedEventEmitter = new JIPipeParameterCollection.ParameterStructureChangedEventEmitter();
    private final GraphChangedEventEmitter graphChangedEventEmitter = new GraphChangedEventEmitter();

    private final NodeAddedEventEmitter nodeAddedEventEmitter = new NodeAddedEventEmitter();
    private final NodeRemovedEventEmitter nodeRemovedEventEmitter = new NodeRemovedEventEmitter();
    private final NodeConnectedEventEmitter nodeConnectedEventEmitter = new NodeConnectedEventEmitter();
    private final NodeDisconnectedEventEmitter nodeDisconnectedEventEmitter = new NodeDisconnectedEventEmitter();
    private final JIPipeGraphNode.NodeSlotsChangedEventEmitter nodeSlotsChangedEventEmitter = new JIPipeGraphNode.NodeSlotsChangedEventEmitter();
    private DefaultDirectedGraph<JIPipeDataSlot, JIPipeGraphEdge> graph = new DefaultDirectedGraph<>(JIPipeGraphEdge.class);
    private List<JIPipeDataSlot> traversedSlots;
    private List<JIPipeGraphNode> traversedAlgorithms;
    private Map<Class<?>, Object> attachments = new HashMap<>();
    private Map<String, Object> additionalMetadata = new HashMap<>();
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
     * Creates a deep copy of the other algorithm graph.
     * UUIDs are copied.
     * Will not copy the additional metadata
     *
     * @param other The original graph
     */
    public JIPipeGraph(JIPipeGraph other) {
        // Copy nodes
        for (Map.Entry<UUID, JIPipeGraphNode> kv : other.nodeUUIDs.entrySet()) {
            JIPipeGraphNode algorithm = kv.getValue().getInfo().duplicate(kv.getValue());
            nodeUUIDs.put(kv.getKey(), algorithm);
            nodeAliasIds.put(kv.getKey(), other.getAliasIdOf(kv.getValue()));
            nodeCompartmentUUIDs.put(kv.getKey(), other.getCompartmentUUIDOf(kv.getValue()));
            nodeVisibleCompartmentUUIDs.put(kv.getKey(), new HashSet<>(other.getVisibleCompartmentUUIDsOf(kv.getValue())));
            algorithm.setParentGraph(this);
            registerNodeEvents(algorithm);
        }
        repairGraph();

        // Copy edges
        for (Map.Entry<JIPipeDataSlot, JIPipeDataSlot> edge : other.getSlotEdges()) {
            UUID sourceAlgorithmName = other.nodeUUIDs.inverse().get(edge.getKey().getNode());
            UUID targetAlgorithmName = other.nodeUUIDs.inverse().get(edge.getValue().getNode());
            JIPipeGraphNode sourceAlgorithm = getNodeByUUID(sourceAlgorithmName);
            JIPipeGraphNode targetAlgorithm = nodeUUIDs.get(targetAlgorithmName);
            JIPipeDataSlot source = sourceAlgorithm.getOutputSlotMap().get(edge.getKey().getName());
            JIPipeDataSlot target = targetAlgorithm.getInputSlotMap().get(edge.getValue().getName());
            connect(source, target);
            JIPipeGraphEdge copyEdge = graph.getEdge(source, target);
            JIPipeGraphEdge originalEdge = other.graph.getEdge(edge.getKey(), edge.getValue());
            copyEdge.setMetadataFrom(originalEdge);
        }
    }

    public NodeAddedEventEmitter getNodeAddedEventEmitter() {
        return nodeAddedEventEmitter;
    }

    public NodeRemovedEventEmitter getNodeRemovedEventEmitter() {
        return nodeRemovedEventEmitter;
    }

    public GraphChangedEventEmitter getGraphChangedEventEmitter() {
        return graphChangedEventEmitter;
    }

    public NodeConnectedEventEmitter getNodeConnectedEventEmitter() {
        return nodeConnectedEventEmitter;
    }

    public NodeDisconnectedEventEmitter getNodeDisconnectedEventEmitter() {
        return nodeDisconnectedEventEmitter;
    }

    public JIPipeGraphNode.NodeSlotsChangedEventEmitter getNodeSlotsChangedEventEmitter() {
        return nodeSlotsChangedEventEmitter;
    }

    public JIPipeParameterCollection.ParameterStructureChangedEventEmitter getParameterStructureChangedEventEmitter() {
        return parameterStructureChangedEventEmitter;
    }

    /**
     * Generates an alias ID for a node
     *
     * @param node the node
     * @return a unique alias ID for the node
     */
    public String generateAliasIdFor(JIPipeGraphNode node) {
        String jsonify = StringUtils.safeJsonify(node.getName());
        // Limit length for filesystem compatibility
        if (jsonify.length() > 30) {
            jsonify = jsonify.substring(0, 31);
        }
        return StringUtils.makeUniqueString(jsonify, "-", id -> nodeAliasIds.values().contains(id));
    }

    /**
     * Gets the alias ID of a node
     * A unique alias ID is generated if none is assigned yet
     *
     * @param node the node
     * @return the alias ID or null if the node is not contained within this graph.
     */
    public String getAliasIdOf(JIPipeGraphNode node) {
        UUID uuid = getUUIDOf(node);
        String aliasId = nodeAliasIds.getOrDefault(uuid, null);
        if (aliasId == null) {
            aliasId = generateAliasIdFor(node);
            nodeAliasIds.put(uuid, aliasId);
        }
        return aliasId;
    }

    /**
     * Gets the UUID of a node
     *
     * @param node the node
     * @return the UUID or null if the node is not contained within this graph.
     */
    public UUID getUUIDOf(JIPipeGraphNode node) {
        return nodeUUIDs.inverse().getOrDefault(node, null);
    }

    /**
     * Gets a node by its UUID
     *
     * @param uuid the UUID
     * @return the node assigned to this UUID
     */
    public JIPipeGraphNode getNodeByUUID(UUID uuid) {
        return nodeUUIDs.getOrDefault(uuid, null);
    }

    /**
     * Changes the UUID of the node
     *
     * @param node the node
     * @param uuid the uuid
     */
    public void setUUIDOf(JIPipeGraphNode node, UUID uuid) {
        UUID originalUUID = getUUIDOf(node);
        String aliasId = getAliasIdOf(node);
        UUID compartmentUUID = getCompartmentUUIDOf(node);
        nodeUUIDs.remove(originalUUID);
        nodeAliasIds.remove(originalUUID);
        nodeCompartmentUUIDs.remove(originalUUID);
        nodeUUIDs.put(uuid, node);
        nodeAliasIds.put(uuid, aliasId);
        nodeCompartmentUUIDs.put(uuid, compartmentUUID);
    }

    /**
     * Gets the UUID of the compartment
     *
     * @param node the node
     * @return the UUID of the compartment
     */
    public UUID getCompartmentUUIDOf(JIPipeGraphNode node) {
        return nodeCompartmentUUIDs.get(getUUIDOf(node));
    }

    /**
     * Gets the compartment UUIDs where the node is also visible inside
     *
     * @param node the node
     * @return set of compartment UUIDs (writeable)
     */
    public Set<UUID> getVisibleCompartmentUUIDsOf(JIPipeGraphNode node) {
        UUID uuid = getUUIDOf(node);
        Set<UUID> result = nodeVisibleCompartmentUUIDs.getOrDefault(uuid, null);
        if (result == null) {
            result = new HashSet<>();
            nodeVisibleCompartmentUUIDs.put(uuid, result);
        }
        return result;
    }

    /**
     * Finds a node UUID by its UUID (as string) or alias ID.
     * Prefers UUID.
     *
     * @param uuidOrAlias the UUID string or alias ID
     * @return the node or null if it could not be found
     */
    public UUID findNodeUUID(String uuidOrAlias) {
        if (StringUtils.isNullOrEmpty(uuidOrAlias))
            return null;
        try {
            JIPipeGraphNode node = nodeUUIDs.getOrDefault(UUID.fromString(uuidOrAlias), null);
            if (node != null)
                return node.getUUIDInParentGraph();
            else
                return null;
        } catch (IllegalArgumentException e) {
            return nodeAliasIds.inverse().getOrDefault(uuidOrAlias, null);
        }
    }

    /**
     * Finds a node by its UUID (as string) or alias ID.
     * Prefers UUID.
     *
     * @param uuidOrAlias the UUID string or alias ID
     * @return the node or null if it could not be found
     */
    public JIPipeGraphNode findNode(String uuidOrAlias) {
        if (StringUtils.isNullOrEmpty(uuidOrAlias))
            return null;
        try {
            return nodeUUIDs.getOrDefault(UUID.fromString(uuidOrAlias), null);
        } catch (IllegalArgumentException e) {
            UUID uuid = nodeAliasIds.inverse().getOrDefault(uuidOrAlias, null);
            if (uuid != null) {
                return getNodeByUUID(uuid);
            } else {
                return null;
            }
        }
    }

    public Map<String, Object> getAdditionalMetadata() {
        return additionalMetadata;
    }

    public void setAdditionalMetadata(Map<String, Object> additionalMetadata) {
        this.additionalMetadata = additionalMetadata;
    }

    /**
     * Inserts an algorithm into the graph
     *
     * @param uuid        The unique ID
     * @param node        The node
     * @param compartment The compartment where the algorithm will be placed. Can be null.
     * @return the ID of the inserted node
     */
    public UUID insertNode(UUID uuid, JIPipeGraphNode node, UUID compartment) {
        if (nodeUUIDs.containsKey(uuid))
            throw new JIPipeValidationRuntimeException(new JIPipeValidationReportEntry(JIPipeValidationReportEntryLevel.Error,
                    new GraphNodeValidationReportContext(node),
                    "Already contains algorithm with UUID " + uuid,
                    "Could not add an algorithm node into the graph!",
                    "There already exists an algorithm with the same identifier.",
                    "If you are loading from a JSON project or plugin, check if the file is valid. Contact " +
                            "the JIPipe or plugin developers for further assistance."));
        node.setParentGraph(this);
        nodeUUIDs.put(uuid, node);
        nodeCompartmentUUIDs.put(uuid, compartment);
        getAliasIdOf(node); // Use the side effect of creating an alias
        registerNodeEvents(node);
        ++preventTriggerEvents;
        repairGraph();
        --preventTriggerEvents;

        // Sometimes we have algorithms with no slots, so trigger manually
        postChangedEvent();
        nodeAddedEventEmitter.emit(new NodeAddedEvent(this, node));

        return uuid;
    }

    private void registerNodeEvents(JIPipeGraphNode node) {
        node.getNodeSlotsChangedEventEmitter().subscribe(this);
        node.getParameterStructureChangedEventEmitter().subscribe(this);
    }

    private void postChangedEvent() {
        traversedAlgorithms = null;
        traversedSlots = null;
        if (preventTriggerEvents <= 0) {
            preventTriggerEvents = 0;
            graphChangedEventEmitter.emit(new GraphChangedEvent(this));
        }
    }

    /**
     * Inserts a node into the graph.
     * Its compartment is null.
     *
     * @param node The node
     * @return the ID of the inserted node
     */
    public UUID insertNode(JIPipeGraphNode node) {
        return insertNode(UUID.randomUUID(), node, null);
    }

    /**
     * Inserts a node into the graph.
     *
     * @param node        The node
     * @param compartment The compartment where the algorithm will be placed
     * @return the ID of the inserted node
     */
    public UUID insertNode(JIPipeGraphNode node, UUID compartment) {
        return insertNode(UUID.randomUUID(), node, compartment);
    }

    /**
     * Returns an attachment or null if it does not exist
     *
     * @param klass the attachment class
     * @param <T>   the attachment class
     * @return the attachment or null
     */
    public <T> T getAttachment(Class<T> klass) {
        Object result = attachments.getOrDefault(klass, null);
        if (result != null)
            return (T) result;
        else
            return null;
    }

    /**
     * Returns additional metadata or null if it does not exist
     *
     * @param klass returned metadata class
     * @param key   metadata key
     * @param <T>   returned metadata class
     * @return the object or null
     */
    public <T> T getAdditionalMetadata(Class<T> klass, String key) {
        Object result = additionalMetadata.getOrDefault(key, null);
        if (result != null)
            return (T) result;
        else
            return null;
    }

    /**
     * Attaches additional metadata (persistent)
     *
     * @param key    the key
     * @param object JSON-serializable object
     */
    public void attachAdditionalMetadata(String key, Object object) {
        additionalMetadata.put(key, object);
    }

    /**
     * Attaches an object as the specified type
     * Warning: Attachments are not serialized
     *
     * @param klass      the type the attachment is attached as
     * @param attachment the attachment
     */
    public void attach(Class<?> klass, Object attachment) {
        if (!klass.isAssignableFrom(attachment.getClass())) {
            throw new IllegalArgumentException("Attachment object must be of given attachment type.");
        }
        attachments.put(klass, attachment);
    }

    /**
     * Attaches the object with its class as key.
     * Warning: Attachments are not serialized
     *
     * @param attachment the attachment
     */
    public void attach(Object attachment) {
        attach(attachment.getClass(), attachment);
    }

    /**
     * Removes an attachment
     * Warning: Attachments are not serialized
     *
     * @param klass the class
     */
    public void removeAttachment(Class<?> klass) {
        attachments.remove(klass);
    }

    /**
     * Returns true if there is an attachment of given type
     *
     * @param klass the attachment class
     * @return if there is an attachment
     */
    public boolean hasAttachmentOfType(Class<?> klass) {
        return attachments.containsKey(klass);
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
     * Sets the compartment of a node
     *
     * @param nodeUUID        the node
     * @param compartmentUUID the compartment
     */
    public void setCompartment(UUID nodeUUID, UUID compartmentUUID) {
        if (!nodeUUIDs.containsKey(nodeUUID)) {
            throw new IllegalArgumentException("Graph does not contain a node with UUID " + nodeUUID);
        }
        nodeCompartmentUUIDs.put(nodeUUID, compartmentUUID);
    }

    /**
     * Removes a whole compartment from the graph.
     * Will fail silently if the ID does not exist
     *
     * @param compartmentUUID The compartment ID
     */
    public void removeCompartment(UUID compartmentUUID) {
        Set<UUID> toRemove = new HashSet<>();
        for (Map.Entry<UUID, UUID> entry : nodeCompartmentUUIDs.entrySet()) {
            if (Objects.equals(entry.getValue(), compartmentUUID)) {
                toRemove.add(entry.getKey());
            }
        }
        for (UUID uuid : toRemove) {
            removeNode(getNodeByUUID(uuid), false);
        }
    }

    /**
     * Removes an algorithm.
     * The algorithm should exist within the graph.
     *
     * @param node The node to be removed
     * @param user if a user triggered the operation. If true, will not remove internal nodes
     */
    public void removeNode(JIPipeGraphNode node, boolean user) {
        if (user && !node.getCategory().userCanDelete())
            return;
        ++preventTriggerEvents;
        // Do regular disconnect
        for (JIPipeDataSlot slot : node.getInputSlots()) {
            disconnectAll(slot, false);
        }
        for (JIPipeDataSlot slot : node.getOutputSlots()) {
            disconnectAll(slot, false);
        }

        // Do internal remove operation
        UUID uuid = getUUIDOf(node);
        nodeUUIDs.remove(uuid);
        nodeAliasIds.remove(uuid);
        nodeCompartmentUUIDs.remove(uuid);
        node.getNodeSlotsChangedEventEmitter().unsubscribe(this);
        node.getParameterStructureChangedEventEmitter().unsubscribe(this);
        for (JIPipeDataSlot slot : node.getInputSlots()) {
            graph.removeVertex(slot);
        }
        for (JIPipeDataSlot slot : node.getOutputSlots()) {
            graph.removeVertex(slot);
        }
        node.setParentGraph(null);
        --preventTriggerEvents;
        postChangedEvent();
        nodeRemovedEventEmitter.emit(new NodeRemovedEvent(this, node));
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
        if (user && !JIPipe.getDataTypes().isConvertible(source.getAcceptedDataType(), target.getAcceptedDataType()))
            return false;
        if (graph.containsEdge(source, target))
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
        if (graph.containsEdge(source, target)) {
            return;
        }
        if (!canConnect(source, target, false))
            throw new JIPipeValidationRuntimeException(new JIPipeValidationReportEntry(JIPipeValidationReportEntryLevel.Error,
                    new GraphNodeValidationReportContext(source.getNode()),
                    "Cannot connect data slots: " + source.getDisplayName() + " ==> " + target.getDisplayName(),
                    "Cannot create a connection between '" + source.getDisplayName() + "' and '" + target.getDisplayName() + "'!",
                    "The connection is invalid, such as one that causes cycles in the graph, or a connection where a slot receives multiple inputs",
                    "Check if your pipeline contains complicated sections prone to cycles. Reorganize the graph by dragging the nodes around."));
        graph.addEdge(source, target, new JIPipeGraphEdge(userCanDisconnect));
        postChangedEvent();
        nodeConnectedEventEmitter.emit(new NodeConnectedEvent(this, source, target));
    }

    /**
     * Extracts all parameters as tree.
     *
     * @param useAliasIds            if enabled, paths will use alias Ids instead of UUIDs
     * @param restrictToCompartments if not null or empty, restrict to specific compartments
     * @return the tree
     */
    public JIPipeParameterTree getParameterTree(boolean useAliasIds, Set<UUID> restrictToCompartments) {
        JIPipeParameterTree tree = new JIPipeParameterTree();
        for (Map.Entry<UUID, JIPipeGraphNode> entry : nodeUUIDs.entrySet()) {
            if (restrictToCompartments != null && !restrictToCompartments.isEmpty()) {
                UUID compartmentUUID = entry.getValue().getCompartmentUUIDInParentGraph();
                if (!restrictToCompartments.contains(compartmentUUID))
                    continue;
            }
            String id;
            if (useAliasIds)
                id = getAliasIdOf(entry.getValue());
            else
                id = entry.getKey().toString();
            JIPipeParameterTree.Node node = tree.add(entry.getValue(), id, null);
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
        for (JIPipeGraphNode node : nodeUUIDs.values()) {
            for (JIPipeDataSlot slot : graph.vertexSet()) {
                if (slot.getNode() == node && !node.getInputSlots().contains(slot) &&
                        !node.getOutputSlots().contains(slot)) {
                    toRemove.add(slot);
//                    slot.getEventBus().unregister(this);
                    modified = true;
                }
            }
        }
        toRemove.forEach(graph::removeVertex);

        // Add missing slots
        for (JIPipeGraphNode node : nodeUUIDs.values()) {

            // Add vertices
            for (JIPipeDataSlot inputSlot : node.getInputSlots()) {
                if (!graph.vertexSet().contains(inputSlot)) {
                    graph.addVertex(inputSlot);
//                    inputSlot.getEventBus().register(this);
                    modified = true;
                }
            }
            for (JIPipeDataSlot outputSlot : node.getOutputSlots()) {
                if (!graph.vertexSet().contains(outputSlot)) {
                    graph.addVertex(outputSlot);
//                    outputSlot.getEventBus().register(this);
                    modified = true;
                }
            }

            // Connect input -> output in the graph
            for (JIPipeDataSlot inputSlot : node.getInputSlots()) {
                for (JIPipeDataSlot outputSlot : node.getOutputSlots()) {
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
     * If exists, returns the output slots that provide data for the input slot
     * Returns and empty list if there are none
     *
     * @param target The input slot
     * @return The output slot that generates data for the input. Null if no source exists.
     */
    public Set<JIPipeDataSlot> getInputIncomingSourceSlots(JIPipeDataSlot target) {
        if (!graph.containsVertex(target))
            return Collections.emptySet();
        if (target.isInput()) {
            Set<JIPipeGraphEdge> edges = graph.incomingEdgesOf(target);
            Set<JIPipeDataSlot> result = new HashSet<>();
            for (JIPipeGraphEdge edge : edges) {
                result.add(graph.getEdgeSource(edge));
            }
            return result;
        }
        return Collections.emptySet();
    }

    /**
     * Returns the list of input slots that are provided by the source slot
     * Returns an empty set if the target is an input or no slot exists
     *
     * @param source An output slot
     * @return All slots that receive data from the output slot
     */
    public Set<JIPipeDataSlot> getOutputOutgoingTargetSlots(JIPipeDataSlot source) {
        if (!graph.containsVertex(source))
            return Collections.emptySet();
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
        result.removeAll(getInputIncomingSourceSlots(target));
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
            for (JIPipeDataSlot source : getInputIncomingSourceSlots(slot)) {
                disconnect(source, slot, user);
            }
        } else if (slot.isOutput()) {
            for (JIPipeDataSlot target : getOutputOutgoingTargetSlots(slot)) {
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
            nodeDisconnectedEventEmitter.emit(new NodeDisconnectedEvent(this, source, target));
            postChangedEvent();
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
    @Override
    public void onNodeSlotsChanged(JIPipeGraphNode.NodeSlotsChangedEvent event) {
        repairGraph();
    }

    /**
     * Should be triggered when an {@link JIPipeGraphNode}'s parameter structure is changed.
     * This event is re-triggered into getEventBus()
     *
     * @param event Generated event
     */
    @Override
    public void onParameterStructureChanged(JIPipeParameterCollection.ParameterStructureChangedEvent event) {
        if (event.getVisitors().contains(this))
            return;
        event.getVisitors().add(this);
        parameterStructureChangedEventEmitter.emit(event);
    }

    /**
     * Gets all dependencies of all algorithms
     *
     * @return Set of dependencies
     */
    public Set<JIPipeDependency> getDependencies() {
        Set<JIPipeDependency> result = new HashSet<>();
        for (JIPipeGraphNode algorithm : nodeUUIDs.values()) {
            result.addAll(algorithm.getDependencies());
        }
        return result;
    }

    /**
     * Returns the set of node UUIDs.
     * This is an immutable set. Not safe for modification.
     *
     * @return the UUIDs
     */
    public Set<UUID> getGraphNodeUUIDs() {
        return nodeUUIDs.keySet();
    }

    /**
     * Returns the set of node instances.
     * This is an immutable set. Not safe for modification.
     *
     * @return the node instances
     */
    public Set<JIPipeGraphNode> getGraphNodes() {
        return nodeUUIDs.values();
    }

    /**
     * Returns true if this graph contains the algorithm
     *
     * @param node The algorithm instance
     * @return True if the algorithm is part of this graph
     */
    public boolean containsNode(JIPipeGraphNode node) {
        return nodeUUIDs.containsValue(node);
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
     * @param jsonNode      JSON data
     * @param context       the context for the issue reporting
     * @param issues        issues reported during deserializing
     * @param notifications notifications for the user
     */
    public void fromJson(JsonNode jsonNode, JIPipeValidationReportContext context, JIPipeValidationReport issues, JIPipeNotificationInbox notifications) {
        if (!jsonNode.has("nodes"))
            return;

        // Load nodes
        nodesFromJson(jsonNode, context, issues, notifications);

        // Load edges
        edgesFromJson(jsonNode, context, issues);

        // Deserialize additional metadata
        JsonNode additionalMetadataNode = jsonNode.path("additional-metadata");
        for (Map.Entry<String, JsonNode> metadataEntry : ImmutableList.copyOf(additionalMetadataNode.fields())) {
            try {
                Class<?> metadataClass = JsonUtils.getObjectMapper().readerFor(Class.class).readValue(metadataEntry.getValue().get("jipipe:type"));
                if (JIPipeParameterCollection.class.isAssignableFrom(metadataClass)) {
                    JIPipeParameterCollection metadata = (JIPipeParameterCollection) ReflectionUtils.newInstance(metadataClass);
                    ParameterUtils.deserializeParametersFromJson(metadata, metadataEntry.getValue(), context, issues);
                    additionalMetadata.put(metadataEntry.getKey(), metadata);
                } else {
                    Object data = JsonUtils.getObjectMapper().readerFor(metadataClass).readValue(metadataEntry.getValue().get("data"));
                    if (data != null) {
                        additionalMetadata.put(metadataEntry.getKey(), data);
                    }
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    public void edgesFromJson(JsonNode jsonNode, JIPipeValidationReportContext context, JIPipeValidationReport issues) {
        for (JsonNode edgeNode : ImmutableList.copyOf(jsonNode.get("edges").elements())) {
            String sourceAlgorithmName = edgeNode.get("source-node").asText();
            String targetAlgorithmName = edgeNode.get("target-node").asText();
            JIPipeGraphNode sourceAlgorithm = findNode(sourceAlgorithmName);
            JIPipeGraphNode targetAlgorithm = findNode(targetAlgorithmName);
            if (sourceAlgorithm == null) {
                issues.add(new JIPipeValidationReportEntry(JIPipeValidationReportEntryLevel.Error, context,
                        "Unable to find node '" + sourceAlgorithmName + "'!",
                        "The JSON data requested to create an edge between the nodes '" + sourceAlgorithmName + "' and '" + targetAlgorithmName + "', but the source does not exist. " +
                                "This might have been caused by a previous error.",
                        "Please check if all extensions are are correctly loaded.",
                        JsonUtils.toPrettyJsonString(jsonNode)));
                System.err.println("Unable to find node with ID '" + sourceAlgorithmName + "'. Skipping this instruction.");
                continue;
            }
            if (targetAlgorithm == null) {
                issues.add(new JIPipeValidationReportEntry(JIPipeValidationReportEntryLevel.Error, context,
                        "Unable to find node '" + targetAlgorithmName + "'!",
                        "The JSON data requested to create an edge between the nodes '" + sourceAlgorithmName + "' and '" + targetAlgorithmName + "', but the source does not exist. " +
                                "This might have been caused by a previous error.",
                        "Please check if all extensions are are correctly loaded.",
                        JsonUtils.toPrettyJsonString(jsonNode)));
                System.err.println("Unable to find node with ID '" + targetAlgorithmName + "'. Skipping this instruction.");
                continue;
            }
            String sourceSlotName = edgeNode.get("source-slot").asText();
            String targetSlotName = edgeNode.get("target-slot").asText();
            JIPipeDataSlot source = sourceAlgorithm.getOutputSlotMap().get(sourceSlotName);
            JIPipeDataSlot target = targetAlgorithm.getInputSlotMap().get(targetSlotName);
            if (source == null) {
                issues.add(new JIPipeValidationReportEntry(JIPipeValidationReportEntryLevel.Error,
                        context,
                        "Unable to find output slot '" + sourceSlotName + "' in node '" + sourceAlgorithmName + "'!",
                        "The JSON data requested to create an edge between the nodes '" + sourceAlgorithmName + "' and '" + targetAlgorithmName + "', but the source slot does not exist. " +
                                "This might have been caused by a previous error.",
                        "Please check if all extensions are are correctly loaded.",
                        JsonUtils.toPrettyJsonString(jsonNode)));
                System.err.println("Unable to find data slot '" + sourceSlotName + "' in algorithm '" + sourceAlgorithmName + "'. Skipping this instruction.");
                continue;
            }
            if (target == null) {
                issues.add(new JIPipeValidationReportEntry(JIPipeValidationReportEntryLevel.Error,
                        context,
                        "Unable to find input slot '" + targetSlotName + "' in node '" + targetAlgorithmName + "'!",
                        "The JSON data requested to create an edge between the nodes '" + sourceAlgorithmName + "' and '" + targetAlgorithmName + "', but the target slot does not exist. " +
                                "This might have been caused by a previous error.",
                        "Please check if all extensions are are correctly loaded.",
                        JsonUtils.toPrettyJsonString(jsonNode)));
                System.err.println("Unable to find data slot '" + targetSlotName + "' in algorithm '" + targetAlgorithmName + "'. Skipping this instruction.");
                continue;
            }
//            if (!graph.containsEdge(source, target) && graph.inDegreeOf(target) > 0) {
//                issues.forCategory("Edges").forCategory("Validation").forCategory(sourceAlgorithmName).reportIsInvalid("Invalid edge found!",
//                        "The JSON data requested to create an edge between the nodes '" + sourceAlgorithmName + "' and '" + targetAlgorithmName + "', but the edge is invalid.",
//                        "Please check the JSON data manually or ignore this error.",
//                        node);
//                System.err.println("Detected invalid edge from " + source.getNode().getIdInGraph() + "/" + source.getName() + " -> "
//                        + target.getNode().getIdInGraph() + "/" + target.getName() + "! Input slot already has a source. Skipping this instruction.");
//                continue;
//            }
            if (!graph.containsEdge(source, target))
                connect(source, target);
            JsonNode metadataNode = edgeNode.path("metadata");
            if (!metadataNode.isMissingNode()) {
                JIPipeGraphEdge edgeInstance = graph.getEdge(source, target);
                try {
                    JsonUtils.getObjectMapper().readerForUpdating(edgeInstance).readValue(metadataNode);
                } catch (IOException e) {
                    issues.add(new JIPipeValidationReportEntry(JIPipeValidationReportEntryLevel.Warning,
                            context,
                            "Unable to deserialize graph metadata!",
                            "The JSON data contains some metadata, but it could not be recovered.",
                            "Metadata does not contain critical information. You can ignore this message.",
                            JsonUtils.toPrettyJsonString(jsonNode)));
                    System.err.println("Cannot deserialize edge metadata!");
                    e.printStackTrace();
                }
            }
        }
    }

    public void nodesFromJson(JsonNode jsonNode, JIPipeValidationReportContext context, JIPipeValidationReport issues, JIPipeNotificationInbox notifications) {
        for (Map.Entry<String, JsonNode> entry : ImmutableList.copyOf(jsonNode.get("nodes").fields())) {
            JsonNode currentNodeJson = entry.getValue();

            UUID nodeUUID;
            String aliasId = null;
            try {
                nodeUUID = UUID.fromString(entry.getKey());
            } catch (IllegalArgumentException e) {
                // Assign new UUID and store it in the legacy assignments
                nodeUUID = UUID.randomUUID();
                aliasId = entry.getKey();
            }

            if (!nodeUUIDs.containsKey(nodeUUID)) {
                String id = currentNodeJson.get("jipipe:node-info-id").asText();
                if (!JIPipe.getNodes().hasNodeInfoWithId(id)) {
                    System.err.println("Unable to find node type with ID '" + id + "'. Skipping.");
                    issues.add(new JIPipeValidationReportEntry(JIPipeValidationReportEntryLevel.Error,
                            context,
                            "Unable to find node type '" + id + "'!",
                            "The JSON data requested to load a node of type '" + id + "', but it is not known to JIPipe.",
                            "Please check if all extensions are are correctly loaded.",
                            JsonUtils.toPrettyJsonString(jsonNode)));
                    continue;
                }

                String compartmentString = currentNodeJson.get("jipipe:graph-compartment").asText();
                UUID compartmentUUID;
                try {
                    compartmentUUID = UUID.fromString(compartmentString);
                } catch (IllegalArgumentException e) {
                    // Set to default compartment and store into legacy compartment
                    compartmentUUID = null;
                    if (!"DEFAULT".equals(compartmentString))
                        nodeLegacyCompartmentIDs.put(nodeUUID, compartmentString);
                }

                JsonNode aliasIdNode = currentNodeJson.path("jipipe:alias-id");
                if (!aliasIdNode.isMissingNode()) {
                    aliasId = aliasIdNode.asText();
                }

                JIPipeNodeInfo info = JIPipe.getNodes().getInfoById(id);
                JIPipeGraphNode node = info.newInstance();
                node.fromJson(currentNodeJson, context, issues, notifications);
                insertNode(nodeUUID, node, compartmentUUID);
//                System.out.println("Insert: " + algorithm + " alias " + aliasId + " in compartment " + compartmentUUID);
                if (aliasId != null) {
                    nodeAliasIds.remove(nodeUUID);
                    nodeAliasIds.put(nodeUUID, aliasId);
                }

                // Move DEFAULT location to empty string (UUID-based)
                if (node.getLocations().containsKey("DEFAULT")) {
                    node.getLocations().put("", node.getLocations().get("DEFAULT"));
                    node.getLocations().remove("DEFAULT");
                }
            }
        }
    }

    /**
     * Merges another graph into this graph
     * The input is not copied!
     *
     * @param otherGraph The other graph
     * @return A map from old UUID in source graph to the node in the target graph
     */
    public Map<UUID, JIPipeGraphNode> mergeWith(JIPipeGraph otherGraph) {
        Map<UUID, JIPipeGraphNode> oldToNewMapping = new HashMap<>();
        Map<UUID, JIPipeGraphNode> newToNewMapping = new HashMap<>();
        for (JIPipeGraphNode node : otherGraph.getGraphNodes()) {
            UUID oldUUID = node.getUUIDInParentGraph();
            UUID newId = insertNode(node, otherGraph.getCompartmentUUIDOf(node));
            oldToNewMapping.put(oldUUID, node);
            newToNewMapping.put(newId, node);
        }
        for (Map.Entry<JIPipeDataSlot, JIPipeDataSlot> edge : otherGraph.getSlotEdges()) {
            // Info: the parent graph was now set by the target graph
            JIPipeGraphNode copySource = newToNewMapping.get(edge.getKey().getNode().getUUIDInParentGraph());
            JIPipeGraphNode copyTarget = newToNewMapping.get(edge.getValue().getNode().getUUIDInParentGraph());
            connect(copySource.getOutputSlotMap().get(edge.getKey().getName()), copyTarget.getInputSlotMap().get(edge.getValue().getName()));
        }
        return oldToNewMapping;
    }

    /**
     * Copies the selected algorithms into a new graph
     * Connections between the nodes are kept
     * UUIDs are kept
     *
     * @param nodes        the nodes
     * @param withInternal also copy internal algorithms
     * @return graph that only contains the selected algorithms, UUIDs are the same between the original and copies
     */
    public JIPipeGraph extract(Collection<JIPipeGraphNode> nodes, boolean withInternal) {
        return extract(nodes, withInternal, false);
    }

    /**
     * Copies the selected algorithms into a new graph
     * Connections between the nodes are kept
     * UUIDs are kept
     *
     * @param nodes        the nodes
     * @param withInternal also copy internal algorithms
     * @param skipLocked   skip locked nodes
     * @return graph that only contains the selected algorithms, UUIDs are the same between the original and copies
     */
    public JIPipeGraph extract(Collection<JIPipeGraphNode> nodes, boolean withInternal, boolean skipLocked) {
        JIPipeGraph graph = new JIPipeGraph();
        for (JIPipeGraphNode node : nodes) {
            if (!withInternal && !node.getCategory().canExtract())
                continue;
            if (node.isUiLocked() && skipLocked)
                continue;
            String compartment = StringUtils.nullToEmpty(getCompartmentUUIDOf(node));
            JIPipeGraphNode copy = node.getInfo().duplicate(node);
            Map<String, Point> map = copy.getLocations().getOrDefault(compartment, new HashMap<>());
            copy.getLocations().clear();
            copy.getLocations().put("", map);
            graph.insertNode(node.getUUIDInParentGraph(), copy, null);
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
    public int getNodeCount() {
        return nodeUUIDs.size();
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

    public JIPipeGraphConnection getConnection(JIPipeDataSlot source, JIPipeDataSlot target) {
        JIPipeGraphEdge edge = graph.getEdge(source, target);
        return new JIPipeGraphConnection(this, source, target, edge);
    }

    /**
     * Returns ALL predecessor algorithms of an algorithm. The predecessors are ordered according to the list of traversed algorithms (topological order)
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
     * @param cascading if predecessors are also checked.
     * @return set of algorithms
     */
    public Set<JIPipeGraphNode> getDeactivatedAlgorithms(boolean cascading) {
        Set<JIPipeGraphNode> missing = new HashSet<>();
        if (cascading) {
            for (JIPipeGraphNode algorithm : traverse()) {
                if (!algorithm.getInfo().isRunnable())
                    continue;
                if (algorithm instanceof JIPipeAlgorithm) {
                    if (!((JIPipeAlgorithm) algorithm).isEnabled()) {
                        missing.add(algorithm);
                        continue;
                    }
                }
                for (JIPipeDataSlot inputSlot : algorithm.getInputSlots()) {
                    if (inputSlot.getInfo().isOptional())
                        continue;
                    Set<JIPipeDataSlot> sourceSlots = getInputIncomingSourceSlots(inputSlot);
                    if (sourceSlots.isEmpty()) {
                        missing.add(algorithm);
                        break;
                    }
                    for (JIPipeDataSlot sourceSlot : sourceSlots) {
                        if (missing.contains(sourceSlot.getNode())) {
                            missing.add(algorithm);
                            break;
                        }
                    }
                }
            }
        } else {
            for (JIPipeGraphNode node : getGraphNodes()) {
                if (node instanceof JIPipeAlgorithm) {
                    if (!((JIPipeAlgorithm) node).isEnabled()) {
                        missing.add(node);
                    }
                }
            }
        }
        return missing;
    }

    /**
     * Gets all algorithms and all dependent algorithms that are missing inputs or are deactivated by the user
     *
     * @param externallySatisfied list of algorithms that have their input set externally
     * @return set of algorithms
     */
    public Set<JIPipeGraphNode> getDeactivatedAlgorithms(Set<JIPipeGraphNode> externallySatisfied) {
        Set<JIPipeGraphNode> missing = new HashSet<>();
        for (JIPipeGraphNode algorithm : traverse()) {
            if (!algorithm.getInfo().isRunnable())
                continue;
            if (externallySatisfied.contains(algorithm))
                continue;
            if (algorithm instanceof JIPipeAlgorithm) {
                if (!((JIPipeAlgorithm) algorithm).isEnabled()) {
                    missing.add(algorithm);
                    continue;
                }
            }
            for (JIPipeDataSlot inputSlot : algorithm.getInputSlots()) {
                if (inputSlot.getInfo().isOptional())
                    continue;
                Set<JIPipeDataSlot> sourceSlots = getInputIncomingSourceSlots(inputSlot);
                if (sourceSlots.isEmpty()) {
                    missing.add(algorithm);
                    break;
                }
                for (JIPipeDataSlot sourceSlot : sourceSlots) {
                    if (missing.contains(sourceSlot.getNode())) {
                        missing.add(algorithm);
                        break;
                    }
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
    public List<JIPipeGraphNode> traverse() {
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
        for (JIPipeGraphNode missing : getGraphNodes()) {
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

    /**
     * Gets the {@link JIPipeProject} if this is a project or project compartment graph.
     * Otherwise will return null
     *
     * @return the project or null if no project was attached to this graph
     */
    public JIPipeProject getProject() {
        return getAttachment(JIPipeProject.class);
    }

    /**
     * Returns a human-readable name of the compartment.
     *
     * @param node the node
     * @return the compartment name, human-readable
     */
    public String getCompartmentDisplayNameOf(JIPipeGraphNode node) {
        UUID compartmentUUID = getCompartmentUUIDOf(node);
        if (compartmentUUID == null)
            return "[No compartment]";
        else {
            JIPipeProject project = getProject();
            if (project != null) {
                JIPipeGraphType graphType = getAttachment(JIPipeGraphType.class);
                if (graphType == JIPipeGraphType.Project) {
                    JIPipeProjectCompartment projectCompartment = project.getCompartments().getOrDefault(compartmentUUID, null);
                    if (projectCompartment != null)
                        return projectCompartment.getName();
                }
            }
            return compartmentUUID.toString();
        }
    }

    @Override
    public void reportValidity(JIPipeValidationReportContext context, JIPipeValidationReport report) {
        for (Map.Entry<UUID, JIPipeGraphNode> entry : nodeUUIDs.entrySet()) {
            JIPipeGraphNode node = entry.getValue();
            if (node instanceof JIPipeAlgorithm) {
                JIPipeAlgorithm algorithm = (JIPipeAlgorithm) node;
                if (!algorithm.isEnabled() || (algorithm.canPassThrough() && algorithm.isPassThrough()))
                    continue;
            }
            report.report(new GraphNodeValidationReportContext(context, node), node);
        }
        if (!RuntimeSettings.getInstance().isAllowSkipAlgorithmsWithoutInput()) {
            for (JIPipeDataSlot slot : graph.vertexSet()) {
                if (!slot.getNode().getInfo().isRunnable())
                    continue;
                if (slot.isInput()) {
                    if (!slot.getInfo().isOptional() && graph.incomingEdgesOf(slot).isEmpty()) {
                        report.add(new JIPipeValidationReportEntry(JIPipeValidationReportEntryLevel.Error,
                                new GraphNodeSlotValidationReportContext(context, slot.getNode(), slot.getName(), slot.getSlotType()),
                                "An input slot has no incoming data!",
                                "Input slots must always be provided with input data.",
                                "Please connect the slot to an output of another algorithm."));
                    }
                }
            }
        }
    }

    public Map<Class<?>, Object> getAttachments() {
        return attachments;
    }

    /**
     * Sets the attachments map
     * Warning: Attachments are not serialized
     *
     * @param attachments attachments
     */
    public void setAttachments(Map<Class<?>, Object> attachments) {
        this.attachments = attachments;
    }

    /**
     * Reports the validity for the target node and its dependencies
     *
     * @param context    the context
     * @param report     the report
     * @param targetNode the target node
     */
    public void reportValidity(JIPipeValidationReportContext context, JIPipeValidationReport report, JIPipeGraphNode targetNode) {
        reportValidity(context, report, targetNode, Collections.emptySet());
    }

    /**
     * Reports the validity for the target node and its dependencies
     *
     * @param context    the context
     * @param report     the report
     * @param targetNode the target node
     * @param satisfied  all algorithms that are considered to have a satisfied input
     */
    public void reportValidity(JIPipeValidationReportContext context, JIPipeValidationReport report, JIPipeGraphNode targetNode, Set<JIPipeGraphNode> satisfied) {
        List<JIPipeGraphNode> predecessorAlgorithms = getPredecessorAlgorithms(targetNode, traverse());
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
                    report.add(new JIPipeValidationReportEntry(JIPipeValidationReportEntryLevel.Error,
                            new GraphNodeValidationReportContext(context, algorithm),
                            "Dependency algorithm is deactivated!",
                            "A dependency algorithm is not enabled. It blocks the execution of all following algorithms.",
                            "Check if all dependency algorithms are enabled. If you just want to skip the processing, try 'Pass through'."));
                    return;
                }
            }
            for (JIPipeDataSlot slot : node.getInputSlots()) {
                if (!slot.getNode().getInfo().isRunnable())
                    continue;
                if (!slot.getInfo().isOptional() && graph.incomingEdgesOf(slot).isEmpty()) {
                    report.add(new JIPipeValidationReportEntry(JIPipeValidationReportEntryLevel.Error,
                            new GraphNodeSlotValidationReportContext(context, slot.getNode(), slot.getName(), slot.getSlotType()),
                            "An input slot has no incoming data!",
                            "Input slots must always be provided with input data.",
                            "Please connect the slot to an output of another algorithm."));
                    return;
                }
            }

            report.report(new GraphNodeValidationReportContext(context, node), node);
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
        for (JIPipeGraphNode algorithm : ImmutableSet.copyOf(nodeUUIDs.values())) {
            removeNode(algorithm, false);
        }
    }

    /**
     * Replaces a node with another.
     *
     * @param target      the target node
     * @param replacement the replacement node
     */
    public void replaceNode(JIPipeGraphNode target, JIPipeGraphNode replacement) {
        UUID compartment = getCompartmentUUIDOf(target);
        insertNode(replacement, compartment);
        for (Map.Entry<String, JIPipeDataSlot> entry : target.getInputSlotMap().entrySet()) {
            JIPipeDataSlot replacementInput = replacement.getInputSlotMap().getOrDefault(entry.getKey(), null);
            if (replacementInput == null) {
                System.err.println("Could not find input slot " + entry.getKey());
                continue;
            }
            for (JIPipeDataSlot slot : getInputIncomingSourceSlots(entry.getValue())) {
                connect(slot, replacementInput);
            }
        }
        for (Map.Entry<String, JIPipeDataSlot> entry : target.getOutputSlotMap().entrySet()) {
            JIPipeDataSlot replacementOutput = replacement.getOutputSlotMap().getOrDefault(entry.getKey(), null);
            if (replacementOutput == null) {
                System.err.println("Could not find output slot " + entry.getKey());
                continue;
            }
            for (JIPipeDataSlot slot : getOutputOutgoingTargetSlots(entry.getValue())) {
                connect(replacementOutput, slot);
            }
        }
        replacement.setLocations(target.getLocations());
        removeNode(target, false);
    }

    /**
     * Returns the algorithm that has the same UUID as the foreign algorithm in the foreign graph.
     *
     * @param foreign An algorithm
     * @return Equivalent algorithm within this graph
     */
    public JIPipeGraphNode getEquivalentAlgorithm(JIPipeGraphNode foreign) {
        return getNodeByUUID(foreign.getUUIDInParentGraph());
    }

    /**
     * Returns the slot with the same name within the algorithm with the same ID.
     *
     * @param foreign A data slot
     * @return slot with the same name within the algorithm with the same ID
     */
    public JIPipeDataSlot getEquivalentSlot(JIPipeDataSlot foreign) {
        JIPipeGraphNode here = getNodeByUUID(foreign.getNode().getUUIDInParentGraph());
        if (foreign.isInput())
            return here.getInputSlotMap().get(foreign.getName());
        else
            return here.getOutputSlotMap().get(foreign.getName());
    }

    /**
     * @return All unconnected slots
     */
    public List<JIPipeDataSlot> getUnconnectedSlots() {
        List<JIPipeDataSlot> result = new ArrayList<>();
        for (JIPipeDataSlot slot : traverseSlots()) {
            if (slot.isInput()) {
                if (getInputIncomingSourceSlots(slot).isEmpty())
                    result.add(slot);
            } else if (slot.isOutput()) {
                if (getOutputOutgoingTargetSlots(slot).isEmpty())
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
    public Set<JIPipeGraphNode> getNodesWithinCompartment(UUID compartmentId) {
        Set<JIPipeGraphNode> result = new HashSet<>();
        for (Map.Entry<UUID, UUID> entry : nodeCompartmentUUIDs.entrySet()) {
            if (Objects.equals(entry.getValue(), compartmentId))
                result.add(getNodeByUUID(entry.getKey()));
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
        this.nodeUUIDs.clear();
        this.nodeCompartmentUUIDs.clear();
        this.nodeAliasIds.clear();
        this.nodeUUIDs.putAll(other.nodeUUIDs);
        this.nodeCompartmentUUIDs.putAll(other.nodeCompartmentUUIDs);
        this.nodeAliasIds.putAll(other.nodeAliasIds);
        for (JIPipeGraphNode node : this.nodeUUIDs.values()) {
            node.setParentGraph(this);
            registerNodeEvents(node);
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
            for (JIPipeDataSlot targetSlot : getOutputOutgoingTargetSlots(outputSlot)) {
                if (targetSlot.getNode() == target) {
                    result.add(new AbstractMap.SimpleEntry<>(outputSlot, targetSlot));
                }
            }
        }
        return result;
    }

    public Map<UUID, String> getNodeLegacyCompartmentIDs() {
        return nodeLegacyCompartmentIDs;
    }

    /**
     * Recreates the alias Ids based on their name
     *
     * @param force force rebuilding
     */
    public void rebuildAliasIds(boolean force) {
        for (Map.Entry<UUID, JIPipeGraphNode> entry : nodeUUIDs.entrySet()) {
            UUID uuid = entry.getKey();
            JIPipeGraphNode node = entry.getValue();
            String aliasId;
            if (force) {
                aliasId = null;
            } else {
                aliasId = nodeAliasIds.getOrDefault(uuid, null);
                String jsonifiedName = StringUtils.safeJsonify(node.getName());
                if (jsonifiedName.length() > 30) {
                    jsonifiedName = jsonifiedName.substring(0, 31);
                }
                if (aliasId != null && !aliasId.startsWith(jsonifiedName)) {
                    aliasId = null;
                }
            }
            if (aliasId == null) {
                // None assigned or name changed -> Create one
                aliasId = generateAliasIdFor(node);
                nodeAliasIds.put(uuid, aliasId);
            }
        }
    }

    public boolean isEmpty() {
        return nodeUUIDs.isEmpty();
    }

    /**
     * Converts loop structures into equivalent group nodes
     * Please note that nested loops will be put into the group; only order 1 loops will be converted.
     *
     * @param additionalLoopEnds nodes that are marked as loop ends
     */
    public List<LoopGroup> extractLoopGroups(Set<JIPipeGraphNode> additionalLoopEnds, Set<JIPipeGraphNode> deactivatedNodes) {

        // Collect all valid loop starts and ends
        Set<JIPipeGraphNode> allValidLoopStarts = new HashSet<>();
        Set<JIPipeGraphNode> allValidLoopEnds = new HashSet<>();
        findLoopStartsAndEnds(additionalLoopEnds, deactivatedNodes, allValidLoopStarts, allValidLoopEnds);

        // Optimization if there are no loops
        if (allValidLoopStarts.isEmpty()) {
            return new ArrayList<>();
        }

        // Find predecessor sets
        Map<JIPipeGraphNode, Set<JIPipeGraphNode>> predecessorSets = new IdentityHashMap<>();
        for (JIPipeGraphNode node : traverse()) {
            Set<JIPipeGraphNode> predecessors = new HashSet<>();
            for (JIPipeDataSlot inputSlot : node.getInputSlots()) {
                predecessors.add(inputSlot.getNode());
                for (JIPipeDataSlot sourceSlot : getInputIncomingSourceSlots(inputSlot)) {
                    predecessors.addAll(predecessorSets.get(sourceSlot.getNode()));
                }
            }
            predecessorSets.put(node, predecessors);
        }

        // Find start points for the loop search
        Set<JIPipeGraphNode> startNodes = new HashSet<>();
        for (JIPipeGraphNode node : getGraphNodes()) {
            if (node.getInputSlots().isEmpty()) {
                startNodes.add(node);
            } else {
                for (JIPipeDataSlot slot : node.getInputSlots()) {
                    if (graph.inDegreeOf(slot) == 0) {
                        startNodes.add(node);
                        break;
                    }
                }
            }
        }

        // We need this to distinguish null from "no loop"
        final LoopStartNode dummyLoopStart = new LoopStartNode(new JIPipeJavaNodeInfo("loop:start", LoopStartNode.class));

        // Iterate from start points to assign loop starts
        // Nested loops will be ignored (but tracked)
        Map<JIPipeGraphNode, LoopStartNode> detectedLoopStarts = new HashMap<>();
        Map<JIPipeGraphNode, Integer> detectedLoopDepths = new HashMap<>();
        for (JIPipeGraphNode startNode : startNodes) {
            // Initialize the start nodes
            if (allValidLoopStarts.contains(startNode)) {
                detectedLoopStarts.put(startNode, (LoopStartNode) startNode);
                detectedLoopDepths.put(startNode, 1);
            } else {
                detectedLoopStarts.put(startNode, dummyLoopStart);
                detectedLoopDepths.put(startNode, 0);
            }
        }
        for (JIPipeGraphNode targetNode : traverse()) {
            JIPipeGraphNode currentLoopStart = detectedLoopStarts.getOrDefault(targetNode, null);
            // Must be null -> Otherwise it might already be assigned
            if (currentLoopStart == null) {
                Set<JIPipeGraphNode> previousLoopStarts = new HashSet<>();
                int previousLoopDepth = 0;
                boolean previousConnectedToEnd = false;
                for (JIPipeDataSlot target : targetNode.getInputSlots()) {
                    for (JIPipeDataSlot source : getInputIncomingSourceSlots(target)) {
                        JIPipeGraphNode sourceNode = source.getNode();
                        if (sourceNode instanceof JIPipeAlgorithm) {
                            if (!allValidLoopEnds.contains(sourceNode)) {
                                JIPipeGraphNode edgeLoopStart = detectedLoopStarts.getOrDefault(sourceNode, dummyLoopStart);
                                previousLoopStarts.add(edgeLoopStart);
                            } else {
                                previousConnectedToEnd = true;
                            }
                            previousLoopDepth = Math.max(previousLoopDepth, detectedLoopDepths.get(sourceNode));
                        }
                    }
                }
                // Do we have a dummy loop start (branch merge?)
                if (previousLoopStarts.size() > 1 && previousLoopStarts.contains(dummyLoopStart)) {
                    JIPipeGraphNode correctLoopStart = previousLoopStarts.stream().filter(start -> start != dummyLoopStart).findAny().get();
                    // We encounter a branch merge -> All predecessors that have a dummy loop start will be included into the current loop
                    for (JIPipeDataSlot target : targetNode.getInputSlots()) {
                        for (JIPipeDataSlot source : getInputIncomingSourceSlots(target)) {
                            JIPipeGraphNode sourceNode = source.getNode();
                            if (sourceNode instanceof JIPipeAlgorithm) {
                                if (detectedLoopStarts.get(sourceNode) == dummyLoopStart) {
                                    detectedLoopStarts.put(sourceNode, (LoopStartNode) correctLoopStart);
                                    detectedLoopDepths.put(sourceNode, previousLoopDepth);
                                    for (JIPipeGraphNode predecessor : predecessorSets.get(sourceNode)) {
                                        detectedLoopStarts.put(predecessor, (LoopStartNode) correctLoopStart);
                                        detectedLoopDepths.put(predecessor, previousLoopDepth);
                                    }
                                }
                            }
                        }
                    }
                    previousLoopStarts.remove(dummyLoopStart);
                }

                // Determine the depth
                if (allValidLoopStarts.contains(targetNode)) {
                    previousLoopDepth += 1;
                } else if (previousConnectedToEnd) {
                    previousLoopDepth = Math.max(0, previousLoopDepth - 1);
                }
                if (previousLoopDepth == 0) {
                    previousLoopStarts.clear();
                }
                if (previousLoopStarts.isEmpty()) {
                    previousLoopStarts.add(dummyLoopStart);
                }
                if (previousLoopStarts.size() == 1) {
                    JIPipeGraphNode previousLoopStart = previousLoopStarts.iterator().next();
                    if (previousLoopStart == dummyLoopStart && allValidLoopStarts.contains(targetNode)) {
                        detectedLoopStarts.put(targetNode, (LoopStartNode) targetNode);
                        detectedLoopDepths.put(targetNode, previousLoopDepth);
                    } else {
                        detectedLoopStarts.put(targetNode, (LoopStartNode) previousLoopStart);
                        detectedLoopDepths.put(targetNode, previousLoopDepth);
                    }
                } else {
                    throw new JIPipeValidationRuntimeException(new JIPipeValidationReportEntry(JIPipeValidationReportEntryLevel.Error, new GraphValidationReportContext(this),
                            "Invalid loop detected: The node '" + targetNode.getDisplayName() + "' is rooted in different loops: "
                                    + previousLoopStarts.stream().map(JIPipeGraphNode::getDisplayName).collect(Collectors.joining(", ")),
                            "Invalid loop detected: " + "Node '" + targetNode.getDisplayName() + "', loop start nodes " + previousLoopStarts.stream().map(JIPipeGraphNode::getDisplayName).collect(Collectors.joining(", ")),
                            "You have created a loop section that has more than one loop starts. JIPipe does not know how to resolve this.",
                            "Check the affected node and trace back the loop start nodes. You can nest loops, but you cannot have multiple loop starts with equal depths."));
                }
            }
        }

        // Collect loops
        List<LoopGroup> result = new ArrayList<>();
        for (LoopStartNode loopStartNode : new HashSet<>(detectedLoopStarts.values())) {
            if (loopStartNode != dummyLoopStart) {
                LoopGroup loopGroup = new LoopGroup(this);
                loopGroup.setLoopStartNode(loopStartNode);
                loopGroup.getNodes().add(loopStartNode);
                for (Map.Entry<JIPipeGraphNode, LoopStartNode> entry : detectedLoopStarts.entrySet()) {
                    if (entry.getValue() == loopStartNode) {
                        loopGroup.getNodes().add(entry.getKey());
                        if (allValidLoopEnds.contains(entry.getKey())) {
                            loopGroup.getLoopEndNodes().add(entry.getKey());
                        }
                    }
                }
                result.add(loopGroup);
            }
        }

        return result;
    }

    private void findLoopStartsAndEnds(Set<JIPipeGraphNode> additionalLoopEnds, Set<JIPipeGraphNode> deactivatedNodes, Set<JIPipeGraphNode> loopStarts, Set<JIPipeGraphNode> loopEnds) {
        for (JIPipeGraphNode node : getGraphNodes()) {
            if ((node instanceof LoopEndNode && !((LoopEndNode) node).isPassThrough()) || additionalLoopEnds.contains(node)) {
                loopEnds.add(node);
            } else if (node instanceof LoopStartNode && ((LoopStartNode) node).getIterationMode() != GraphWrapperAlgorithm.IterationMode.PassThrough && !((LoopStartNode) node).isPassThrough()) {
                loopStarts.add(node);
            } else if (node.getOutputSlots().isEmpty()) {
                loopEnds.add(node);
            } else {
                boolean isConnected = false;
                for (JIPipeDataSlot outputSlot : node.getOutputSlots()) {
                    for (JIPipeGraphEdge edge : graph.outgoingEdgesOf(outputSlot)) {
                        JIPipeDataSlot target = graph.getEdgeTarget(edge);
                        if (!deactivatedNodes.contains(target.getNode())) {
                            isConnected = true;
                            break;
                        }
                    }
                    if (isConnected)
                        break;
                }
                if (!isConnected)
                    loopEnds.add(node);
            }
        }
    }

    public boolean containsNode(UUID nodeUUID) {
        return this.nodeUUIDs.containsKey(nodeUUID);
    }

    @Override
    public boolean functionallyEquals(Object other) {
        if (other instanceof JIPipeGraph) {
            JIPipeGraph otherGraph = (JIPipeGraph) other;
            if (graph.vertexSet().size() != otherGraph.graph.vertexSet().size())
                return false;
            if (graph.edgeSet().size() != otherGraph.graph.edgeSet().size())
                return false;

            // Compare nodes by UUID (if we find the same UUID, we can be sure) as UUIDs are unlikely to collide
            for (Map.Entry<UUID, JIPipeGraphNode> hereEntry : nodeUUIDs.entrySet()) {
                JIPipeGraphNode thisNode = hereEntry.getValue();
                JIPipeGraphNode otherNode = otherGraph.getNodeByUUID(hereEntry.getKey());
                if (otherNode != null && !thisNode.functionallyEquals(otherNode)) {
                    return false;
                }
            }

            // TODO: Comparison of edges (outside the edge set)

            return true;
        }
        return false;
    }

    public void disconnect(JIPipeGraphConnection connection, boolean user) {
        disconnect(connection.getSource(), connection.getTarget(), user);
    }

    public void disconnect(JIPipeGraphConnection connection) {
        disconnect(connection.getSource(), connection.getTarget(), true);
    }

    public interface GraphChangedEventListener {
        void onGraphChanged(GraphChangedEvent event);
    }

    public interface NodeConnectedEventListener {
        void onNodeConnected(NodeConnectedEvent event);
    }

    public interface NodeDisconnectedEventListener {
        void onNodeDisconnected(NodeDisconnectedEvent event);
    }

    public interface NodeAddedEventListener {
        void onNodeAdded(NodeAddedEvent event);
    }

    public interface NodeRemovedEventListener {
        void onNodeRemoved(NodeRemovedEvent event);
    }

    /**
     * Serializes an {@link JIPipeGraph}
     */
    public static class Serializer extends JsonSerializer<JIPipeGraph> {
        @Override
        public void serialize(JIPipeGraph algorithmGraph, JsonGenerator generator, SerializerProvider serializerProvider) throws IOException, JsonProcessingException {
            generator.writeStartObject();

            generator.writeFieldName("nodes");
            generator.writeStartObject();
            serializeNodes(algorithmGraph, generator);
            generator.writeEndObject();

            generator.writeFieldName("edges");
            generator.writeStartArray();
            serializeEdges(algorithmGraph, generator);
            generator.writeEndArray();

            if (!algorithmGraph.additionalMetadata.isEmpty()) {
                generator.writeObjectFieldStart("additional-metadata");
                for (Map.Entry<String, Object> entry : algorithmGraph.additionalMetadata.entrySet()) {
                    if (entry.getValue() instanceof JIPipeParameterCollection) {
                        generator.writeObjectFieldStart(entry.getKey());
                        generator.writeObjectField("jipipe:type", entry.getValue().getClass());
                        ParameterUtils.serializeParametersToJson((JIPipeParameterCollection) entry.getValue(), generator);
                        generator.writeEndObject();
                    } else {
                        generator.writeObjectFieldStart(entry.getKey());
                        generator.writeObjectField("jipipe:type", entry.getValue().getClass());
                        generator.writeObjectField("data", entry.getValue());
                        generator.writeEndObject();
                    }
                }
                generator.writeEndObject();
            }

            generator.writeEndObject();
        }

        private void serializeNodes(JIPipeGraph algorithmGraph, JsonGenerator jsonGenerator) throws IOException {
            for (Map.Entry<UUID, JIPipeGraphNode> kv : algorithmGraph.nodeUUIDs.entrySet()) {
                jsonGenerator.writeObjectField(kv.getKey().toString(), kv.getValue());
            }
        }

        private void serializeEdges(JIPipeGraph graph, JsonGenerator jsonGenerator) throws IOException {
            for (Map.Entry<JIPipeDataSlot, JIPipeDataSlot> edge : graph.getSlotEdges()) {
                jsonGenerator.writeStartObject();
                jsonGenerator.writeStringField("source-node", graph.getUUIDOf(edge.getKey().getNode()).toString());
                jsonGenerator.writeStringField("target-node", graph.getUUIDOf(edge.getValue().getNode()).toString());
                jsonGenerator.writeStringField("source-slot", edge.getKey().getName());
                jsonGenerator.writeStringField("target-slot", edge.getValue().getName());
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
            graph.fromJson(jsonParser.readValueAsTree(), new UnspecifiedValidationReportContext(), new JIPipeValidationReport(), new JIPipeNotificationInbox());
            return graph;
        }
    }

    /**
     * Event is triggered when algorithm graph is changed
     */
    public static class GraphChangedEvent extends AbstractJIPipeEvent {
        private final JIPipeGraph graph;

        /**
         * @param graph the graph
         */
        public GraphChangedEvent(JIPipeGraph graph) {
            super(graph);
            this.graph = graph;
        }

        public JIPipeGraph getGraph() {
            return graph;
        }
    }

    public static class GraphChangedEventEmitter extends JIPipeEventEmitter<GraphChangedEvent, GraphChangedEventListener> {

        @Override
        protected void call(GraphChangedEventListener graphChangedEventListener, GraphChangedEvent event) {
            graphChangedEventListener.onGraphChanged(event);
        }
    }

    /**
     * Generated when a connection was made in {@link JIPipeGraph}
     */
    public static class NodeConnectedEvent extends AbstractJIPipeEvent {
        private final JIPipeGraph graph;
        private final JIPipeDataSlot source;
        private final JIPipeDataSlot target;

        /**
         * @param graph  the graph
         * @param source the source slot
         * @param target the target slot
         */
        public NodeConnectedEvent(JIPipeGraph graph, JIPipeDataSlot source, JIPipeDataSlot target) {
            super(graph);
            this.graph = graph;
            this.source = source;
            this.target = target;
        }

        public JIPipeGraph getGraph() {
            return graph;
        }

        public JIPipeDataSlot getSource() {
            return source;
        }

        public JIPipeDataSlot getTarget() {
            return target;
        }
    }

    public static class NodeConnectedEventEmitter extends JIPipeEventEmitter<NodeConnectedEvent, NodeConnectedEventListener> {
        @Override
        protected void call(NodeConnectedEventListener nodeConnectedEventListener, NodeConnectedEvent event) {
            nodeConnectedEventListener.onNodeConnected(event);
        }
    }

    /**
     * Generated when slots are disconnected
     */
    public static class NodeDisconnectedEvent extends AbstractJIPipeEvent {
        private final JIPipeGraph graph;
        private final JIPipeDataSlot source;
        private final JIPipeDataSlot target;

        /**
         * @param graph  the graph
         * @param source the source slot
         * @param target the target slot
         */
        public NodeDisconnectedEvent(JIPipeGraph graph, JIPipeDataSlot source, JIPipeDataSlot target) {
            super(graph);
            this.graph = graph;
            this.source = source;
            this.target = target;
        }

        public JIPipeGraph getGraph() {
            return graph;
        }

        public JIPipeDataSlot getSource() {
            return source;
        }

        public JIPipeDataSlot getTarget() {
            return target;
        }
    }

    public static class NodeDisconnectedEventEmitter extends JIPipeEventEmitter<NodeDisconnectedEvent, NodeDisconnectedEventListener> {

        @Override
        protected void call(NodeDisconnectedEventListener nodeDisconnectedEventListener, NodeDisconnectedEvent event) {
            nodeDisconnectedEventListener.onNodeDisconnected(event);
        }
    }

    public static class NodeAddedEvent extends AbstractJIPipeEvent {
        private final JIPipeGraph graph;
        private final JIPipeGraphNode node;

        public NodeAddedEvent(JIPipeGraph graph, JIPipeGraphNode node) {
            super(graph);
            this.graph = graph;
            this.node = node;
        }

        public JIPipeGraph getGraph() {
            return graph;
        }

        public JIPipeGraphNode getNode() {
            return node;
        }
    }

    public static class NodeAddedEventEmitter extends JIPipeEventEmitter<NodeAddedEvent, NodeAddedEventListener> {
        @Override
        protected void call(NodeAddedEventListener nodeAddedEventListener, NodeAddedEvent event) {
            nodeAddedEventListener.onNodeAdded(event);
        }
    }

    public static class NodeRemovedEvent extends AbstractJIPipeEvent {
        private final JIPipeGraph graph;
        private final JIPipeGraphNode node;

        public NodeRemovedEvent(JIPipeGraph graph, JIPipeGraphNode node) {
            super(graph);
            this.graph = graph;
            this.node = node;
        }

        public JIPipeGraph getGraph() {
            return graph;
        }

        public JIPipeGraphNode getNode() {
            return node;
        }
    }

    public static class NodeRemovedEventEmitter extends JIPipeEventEmitter<NodeRemovedEvent, NodeRemovedEventListener> {

        @Override
        protected void call(NodeRemovedEventListener nodeRemovedEventListener, NodeRemovedEvent event) {
            nodeRemovedEventListener.onNodeRemoved(event);
        }
    }

}
