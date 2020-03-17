package org.hkijena.acaq5.api.algorithm;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.*;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.google.common.collect.*;
import com.google.common.eventbus.EventBus;
import com.google.common.eventbus.Subscribe;
import org.hkijena.acaq5.api.ACAQAlgorithmGraphEdge;
import org.hkijena.acaq5.api.ACAQValidatable;
import org.hkijena.acaq5.api.ACAQValidityReport;
import org.hkijena.acaq5.api.data.ACAQDataSlot;
import org.hkijena.acaq5.api.events.*;
import org.hkijena.acaq5.api.registries.ACAQAlgorithmRegistry;
import org.hkijena.acaq5.api.traits.ACAQTraitDeclaration;
import org.hkijena.acaq5.utils.StringUtils;
import org.jgrapht.Graph;
import org.jgrapht.alg.cycle.CycleDetector;
import org.jgrapht.graph.DefaultDirectedGraph;
import org.jgrapht.io.DOTExporter;
import org.jgrapht.io.ExportException;
import org.jgrapht.traverse.GraphIterator;
import org.jgrapht.traverse.TopologicalOrderIterator;

import java.io.IOException;
import java.nio.file.Path;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Manages multiple {@link ACAQAlgorithm} instances as graph
 */
@JsonSerialize(using = ACAQAlgorithmGraph.Serializer.class)
@JsonDeserialize(using = ACAQAlgorithmGraph.Deserializer.class)
public class ACAQAlgorithmGraph implements ACAQValidatable {

    public static final String COMPARTMENT_DEFAULT = "DEFAULT";

    private DefaultDirectedGraph<ACAQDataSlot, ACAQAlgorithmGraphEdge> graph = new DefaultDirectedGraph<>(ACAQAlgorithmGraphEdge.class);
    private BiMap<String, ACAQAlgorithm> algorithms = HashBiMap.create();
    private Map<ACAQAlgorithm, String> compartments = new HashMap<>();
    private EventBus eventBus = new EventBus();

    public ACAQAlgorithmGraph() {

    }

    public ACAQAlgorithmGraph(ACAQAlgorithmGraph other) {
        // Copy nodes
        for (Map.Entry<String, ACAQAlgorithm> kv : other.algorithms.entrySet()) {
            ACAQAlgorithm algorithm = kv.getValue().getDeclaration().clone(kv.getValue());
            algorithms.put(kv.getKey(), algorithm);
            algorithm.setGraph(this);
            algorithm.getEventBus().register(this);
        }
        repairGraph();

        // Copy edges
        for (Map.Entry<ACAQDataSlot, ACAQDataSlot> edge : other.getSlotEdges()) {
            String sourceAlgorithmName = other.algorithms.inverse().get(edge.getKey().getAlgorithm());
            String targetAlgorithmName = other.algorithms.inverse().get(edge.getValue().getAlgorithm());
            ACAQAlgorithm sourceAlgorithm = algorithms.get(sourceAlgorithmName);
            ACAQAlgorithm targetAlgorithm = algorithms.get(targetAlgorithmName);
            connect(sourceAlgorithm.getSlots().get(edge.getKey().getName()),
                    targetAlgorithm.getSlots().get(edge.getValue().getName()));
        }
    }

    /**
     * Inserts an algorithm into the graph
     *
     * @param key       The unique ID
     * @param algorithm
     */
    public void insertNode(String key, ACAQAlgorithm algorithm, String compartment) {
        if (compartment == null)
            throw new NullPointerException("Compartment should not be null!");
        if (algorithms.containsKey(key))
            throw new RuntimeException("Already contains algorithm with name " + key);
        algorithm.setCompartment(compartment);
        algorithm.setGraph(this);
        algorithms.put(key, algorithm);
        compartments.put(algorithm, compartment);
        algorithm.getEventBus().register(this);
        algorithm.getTraitConfiguration().getEventBus().register(this);
        repairGraph();

        // Sometimes we have algorithms with no slots, so trigger manually
        if (algorithm.getSlots().isEmpty()) {
            getEventBus().post(new AlgorithmGraphChangedEvent(this));
        }
    }

    /**
     * Inserts an algorithm into the graph.
     * The name is automatically generated
     *
     * @param algorithm
     */
    public void insertNode(ACAQAlgorithm algorithm, String compartment) {
        String uniqueName = StringUtils.makeUniqueString(StringUtils.jsonify(compartment + "-" + algorithm.getName()), " ", algorithms.keySet());
        insertNode(uniqueName, algorithm, compartment);
    }

    public boolean canUserDelete(ACAQAlgorithm algorithm) {
        return algorithm.getCategory() != ACAQAlgorithmCategory.Internal;
    }

    public boolean canUserDisconnect(ACAQDataSlot source, ACAQDataSlot target) {
        if (graph.containsEdge(source, target)) {
            ACAQAlgorithmGraphEdge edge = graph.getEdge(source, target);
            if (edge != null) {
                return edge.isUserDisconnectable();
            }
        }
        return false;
    }

    public void removeCompartment(String compartment) {
        Set<String> ids = algorithms.keySet().stream().filter(id -> compartment.equals(compartments.get(algorithms.get(id)))).collect(Collectors.toSet());
        for (String id : ids) {
            removeNode(algorithms.get(id));
        }
    }

    public void renameCompartment(String compartment, String newCompartment) {
        for (ACAQAlgorithm algorithm : algorithms.values().stream().filter(a -> compartment.equals(compartments.get(a))).collect(Collectors.toSet())) {
            compartments.put(algorithm, newCompartment);
        }
        getEventBus().post(new AlgorithmGraphChangedEvent(this));
    }

    public void removeNode(ACAQAlgorithm algorithm) {
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
        algorithm.getTraitConfiguration().getEventBus().unregister(this);
        for (ACAQDataSlot slot : algorithm.getInputSlots()) {
            graph.removeVertex(slot);
        }
        for (ACAQDataSlot slot : algorithm.getOutputSlots()) {
            graph.removeVertex(slot);
        }
        algorithm.setGraph(null);
        getEventBus().post(new AlgorithmGraphChangedEvent(this));
    }

    /**
     * Slow implementation of connection checking that tests for graph cycles
     *
     * @param source
     * @param target
     * @return
     */
    public boolean canConnect(ACAQDataSlot source, ACAQDataSlot target) {
        if (!canConnectFast(source, target))
            return false;
        Graph<ACAQDataSlot, ACAQAlgorithmGraphEdge> copy = (Graph<ACAQDataSlot, ACAQAlgorithmGraphEdge>) graph.clone();
        copy.addEdge(source, target);
        CycleDetector<ACAQDataSlot, ACAQAlgorithmGraphEdge> cycleDetector = new CycleDetector<>(copy);
        return !cycleDetector.detectCycles();
    }

    /**
     * Fast implementation of connection checking without copying the graph
     *
     * @param source
     * @param target
     * @return
     */
    public boolean canConnectFast(ACAQDataSlot source, ACAQDataSlot target) {
        if (!source.isOutput() || !target.isInput())
            return false;
        if (!target.getAcceptedDataType().isAssignableFrom(source.getAcceptedDataType()))
            return false;
        return true;
    }

    public void connect(ACAQDataSlot source, ACAQDataSlot target) {
        connect(source, target, true);
    }

    public void connect(ACAQDataSlot source, ACAQDataSlot target, boolean userDisconnectable) {
        if (!canConnect(source, target))
            throw new RuntimeException("Cannot connect data slots!");
        graph.addEdge(source, target, new ACAQAlgorithmGraphEdge(userDisconnectable));
        getEventBus().post(new AlgorithmGraphChangedEvent(this));
        getEventBus().post(new AlgorithmGraphConnectedEvent(this, source, target));
        updateDataSlotTraits();
    }

    public void repairGraph() {

        boolean modified = false;

        // Remove deleted slots from the graph
        Set<ACAQDataSlot> toRemove = new HashSet<>();
        for (ACAQAlgorithm algorithm : algorithms.values()) {
            for (ACAQDataSlot slot : graph.vertexSet()) {
                if (slot.getAlgorithm() == algorithm && !algorithm.getInputSlots().contains(slot) &&
                        !algorithm.getOutputSlots().contains(slot)) {
                    toRemove.add(slot);
                    slot.getEventBus().unregister(this);
                    modified = true;
                }
            }
        }
        toRemove.forEach(graph::removeVertex);

        // Add missing slots
        for (ACAQAlgorithm algorithm : algorithms.values()) {

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
            getEventBus().post(new AlgorithmGraphChangedEvent(this));
    }

    /**
     * If exists, returns the output slot that provides data for the input slot
     * Returns null if target is an output or no slot exists
     *
     * @return
     */
    public ACAQDataSlot getSourceSlot(ACAQDataSlot target) {
        if (target.isInput()) {
            Set<ACAQAlgorithmGraphEdge> edges = graph.incomingEdgesOf(target);
            if (edges.isEmpty())
                return null;
            if (edges.size() > 1)
                throw new RuntimeException("Graph is illegal!");
            return graph.getEdgeSource(edges.iterator().next());
        }
        return null;
    }

    /**
     * Returns the list of input slots that are provided by the source slot
     * Returns an empty set if the target is an input or no slot exists
     *
     * @param source
     * @return
     */
    public Set<ACAQDataSlot> getTargetSlots(ACAQDataSlot source) {
        if (source.isOutput()) {
            Set<ACAQAlgorithmGraphEdge> edges = graph.outgoingEdgesOf(source);
            Set<ACAQDataSlot> result = new HashSet<>();
            for (ACAQAlgorithmGraphEdge edge : edges) {
                result.add(graph.getEdgeTarget(edge));
            }
            return result;
        }
        return Collections.emptySet();
    }

    /**
     * Returns all available sources for an input slot
     *
     * @param target
     * @return
     */
    public Set<ACAQDataSlot> getAvailableSources(ACAQDataSlot target) {
        if (getSourceSlot(target) != null)
            return Collections.emptySet();
        Set<ACAQDataSlot> result = new HashSet<>();
        for (ACAQDataSlot source : graph.vertexSet()) {
            if (source == target)
                continue;
            if (!source.isOutput())
                continue;
            if (source.getAlgorithm() == target.getAlgorithm())
                continue;
            if (graph.containsEdge(source, target))
                continue;
            if (!canConnectFast(source, target))
                continue;
            result.add(source);
        }
        return result;
    }

    /**
     * Completely disconnects a slot
     *
     * @param slot
     * @param user
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

    public boolean disconnect(ACAQDataSlot source, ACAQDataSlot target, boolean user) {
        if (graph.containsEdge(source, target)) {
            if (user && !canUserDisconnect(source, target))
                return false;
            graph.removeEdge(source, target);
            getEventBus().post(new AlgorithmGraphDisconnectedEvent(this, source, target));
            getEventBus().post(new AlgorithmGraphChangedEvent(this));
            updateDataSlotTraits();
            return true;
        }
        return false;
    }

    /**
     * Returns all available target for an output slot
     *
     * @param source
     * @return
     */
    public Set<ACAQDataSlot> getAvailableTargets(ACAQDataSlot source) {
        if (source.isInput())
            return Collections.emptySet();
        Set<ACAQDataSlot> result = new HashSet<>();
        for (ACAQDataSlot target : graph.vertexSet()) {
            if (source == target)
                continue;
            if (!target.isInput())
                continue;
            if (target.getAlgorithm() == source.getAlgorithm())
                continue;
            if (graph.containsEdge(source, target))
                continue;
            if (getSourceSlot(target) != null)
                continue;
            if (!canConnectFast(source, target))
                continue;
            result.add(target);
        }
        return result;
    }

    @Subscribe
    public void onAlgorithmSlotsChanged(AlgorithmSlotsChangedEvent event) {
        repairGraph();
    }

    @Subscribe
    public void onTraitsChanged(TraitConfigurationChangedEvent event) {
        updateDataSlotTraits();
        eventBus.post(new AlgorithmGraphChangedEvent(this));
    }

    public void updateDataSlotTraits() {
        Set<ACAQAlgorithm> executedAlgorithms = new HashSet<>();
        for (ACAQDataSlot slot : traverse()) {
            if (slot.isInput()) {
                slot.clearSlotAnnotations();
            }
        }
        for (ACAQDataSlot slot : traverse()) {
            if (slot.isInput()) {
                // Execute trait configuration
                if (!executedAlgorithms.contains(slot.getAlgorithm())) {
                    slot.getAlgorithm().getTraitConfiguration().apply();
                    executedAlgorithms.add(slot.getAlgorithm());
                }
            } else {
                // Transfer traits from output to input
                for (ACAQDataSlot targetSlot : getTargetSlots(slot)) {
                    targetSlot.clearSlotAnnotations();
                    for (ACAQTraitDeclaration slotAnnotation : slot.getSlotAnnotations()) {
//                        System.out.println("GRAPHTRANSFER " + slotAnnotation.getName() + " FROM " + slot.getNameWithAlgorithmName() + " TO " + targetSlot.getNameWithAlgorithmName());
                        targetSlot.addSlotAnnotation(slotAnnotation);
                    }
                }
            }
        }
    }

    public EventBus getEventBus() {
        return eventBus;
    }

    public BiMap<String, ACAQAlgorithm> getAlgorithmNodes() {
        return ImmutableBiMap.copyOf(algorithms);
    }

    public boolean containsNode(ACAQAlgorithm algorithm) {
        return algorithms.containsValue(algorithm);
    }

    /**
     * Returns all edges as set of edges
     *
     * @return
     */
    public Set<Map.Entry<ACAQDataSlot, ACAQDataSlot>> getSlotEdges() {
        Set<Map.Entry<ACAQDataSlot, ACAQDataSlot>> result = new HashSet<>();
        for (ACAQAlgorithmGraphEdge edge : graph.edgeSet()) {
            ACAQDataSlot source = graph.getEdgeSource(edge);
            ACAQDataSlot target = graph.getEdgeTarget(edge);
            if (source.getAlgorithm() == target.getAlgorithm())
                continue;
            result.add(new AbstractMap.SimpleImmutableEntry<>(source, target));
        }
        return result;
    }

    public void fromJson(JsonNode node) {
        if (!node.has("nodes"))
            return;

        for (Map.Entry<String, JsonNode> kv : ImmutableList.copyOf(node.get("nodes").fields())) {
            if (!algorithms.containsKey(kv.getKey())) {
                String declarationInfo = kv.getValue().get("acaq:algorithm-type").asText();
                ACAQAlgorithmDeclaration declaration = ACAQAlgorithmRegistry.getInstance().getDeclarationById(declarationInfo);
                ACAQAlgorithm algorithm = declaration.newInstance();
                algorithm.fromJson(kv.getValue());
                insertNode(StringUtils.jsonify(kv.getKey()), algorithm, algorithm.getCompartment());
            }
        }

        // Load edges
        for (JsonNode edgeNode : ImmutableList.copyOf(node.get("edges").elements())) {
            ACAQAlgorithm sourceAlgorithm = algorithms.get(StringUtils.jsonify(edgeNode.get("source-algorithm").asText()));
            ACAQAlgorithm targetAlgorithm = algorithms.get(StringUtils.jsonify(edgeNode.get("target-algorithm").asText()));
            ACAQDataSlot source = sourceAlgorithm.getSlots().get(StringUtils.makeFilesystemCompatible(edgeNode.get("source-slot").asText()));
            ACAQDataSlot target = targetAlgorithm.getSlots().get(StringUtils.makeFilesystemCompatible(edgeNode.get("target-slot").asText()));
            if (!graph.containsEdge(source, target))
                connect(source, target);
        }
    }

    /**
     * Merges another graph into this graph
     *
     * @param otherGraph
     * @return A map from ID in source graph to algorithm in target graph
     */
    public Map<String, ACAQAlgorithm> mergeWith(ACAQAlgorithmGraph otherGraph) {
        Map<String, ACAQAlgorithm> copies = new HashMap<>();
        for (ACAQAlgorithm algorithm : otherGraph.getAlgorithmNodes().values()) {
            ACAQAlgorithm copy = algorithm.getDeclaration().clone(algorithm);
            insertNode(copy, copy.getCompartment());
            copies.put(algorithm.getIdInGraph(), copy);
        }
        for (Map.Entry<ACAQDataSlot, ACAQDataSlot> edge : otherGraph.getSlotEdges()) {
            ACAQAlgorithm copySource = copies.get(edge.getKey().getAlgorithm().getIdInGraph());
            ACAQAlgorithm copyTarget = copies.get(edge.getValue().getAlgorithm().getIdInGraph());
            connect(copySource.getSlots().get(edge.getKey().getName()), copyTarget.getSlots().get(edge.getValue().getName()));
        }
        return copies;
    }

    public int getAlgorithmCount() {
        return algorithms.size();
    }

    public int getSlotCount() {
        return graph.vertexSet().size();
    }

    public void exportDOT(Path fileName) {
        DOTExporter<ACAQDataSlot, ACAQAlgorithmGraphEdge> exporter = new DOTExporter<>();
        try {
            exporter.exportGraph(graph, fileName.toFile());
        } catch (ExportException e) {
            throw new RuntimeException(e);
        }
    }

    public List<ACAQDataSlot> traverse() {
        GraphIterator<ACAQDataSlot, ACAQAlgorithmGraphEdge> iterator = new TopologicalOrderIterator<>(graph);
        List<ACAQDataSlot> result = new ArrayList<>();
        while (iterator.hasNext()) {
            ACAQDataSlot slot = iterator.next();
            result.add(slot);
        }
        return result;
    }

    public List<ACAQAlgorithm> traverseAlgorithms() {
        Set<ACAQAlgorithm> visited = new HashSet<>();
        List<ACAQAlgorithm> result = new ArrayList<>();
        for (ACAQDataSlot slot : traverse()) {
            if (slot.isOutput()) {
                if (!visited.contains(slot.getAlgorithm())) {
                    visited.add(slot.getAlgorithm());
                    result.add(slot.getAlgorithm());
                }
            }
        }
        for (ACAQAlgorithm missing : algorithms.values()) {
            if (!visited.contains(missing)) {
                result.add(missing);
            }
        }
        return result;
    }

    public Set<ACAQDataSlot> getSlotNodes() {
        return graph.vertexSet();
    }

    public boolean containsNode(ACAQDataSlot slot) {
        return graph.vertexSet().contains(slot);
    }

    @Override
    public void reportValidity(ACAQValidityReport report) {
        for (Map.Entry<String, ACAQAlgorithm> entry : algorithms.entrySet()) {
            report.forCategory(entry.getValue().getCompartment()).forCategory(entry.getValue().getName()).report(entry.getValue());
        }
        for (ACAQDataSlot slot : graph.vertexSet()) {
            if (slot.isInput()) {
                if (graph.incomingEdgesOf(slot).isEmpty()) {
                    report.forCategory(slot.getAlgorithm().getCompartment()).forCategory(slot.getAlgorithm().getName())
                            .forCategory("Slot: " + slot.getName()).reportIsInvalid("An input slot has no incoming data! " +
                            "Please connect the slot to an output of another algorithm.");
                }
            }
        }
    }

    public Graph<ACAQDataSlot, ACAQAlgorithmGraphEdge> getGraph() {
        return graph;
    }

    public void clear() {
        for (ACAQAlgorithm algorithm : ImmutableSet.copyOf(algorithms.values())) {
            removeNode(algorithm);
        }
    }

    public String getIdOf(ACAQAlgorithm algorithm) {
        return algorithms.inverse().get(algorithm);
    }

    public boolean containsEquivalentOf(ACAQAlgorithm foreign, ACAQAlgorithmGraph foreignGraph) {
        return getAlgorithmNodes().containsKey(foreignGraph.getIdOf(foreign));
    }

    public ACAQAlgorithm getEquivalentOf(ACAQAlgorithm foreign, ACAQAlgorithmGraph foreignGraph) {
        return getAlgorithmNodes().get(foreignGraph.getIdOf(foreign));
    }

    public String getCompartmentOf(ACAQAlgorithm algorithm) {
        return compartments.get(algorithm);
    }

    public List<ACAQDataSlot> getUnconnectedSlots() {
        List<ACAQDataSlot> result = new ArrayList<>();
        for (ACAQDataSlot slot : traverse()) {
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

    public Set<ACAQAlgorithm> getAlgorithmsWithCompartment(String compartmentId) {
        Set<ACAQAlgorithm> result = new HashSet<>();
        for (ACAQAlgorithm algorithm : algorithms.values()) {
            if(algorithm.getCompartment().equals(compartmentId))
                result.add(algorithm);
        }
        return result;
    }

    public static class Serializer extends JsonSerializer<ACAQAlgorithmGraph> {
        @Override
        public void serialize(ACAQAlgorithmGraph algorithmGraph, JsonGenerator jsonGenerator, SerializerProvider serializerProvider) throws IOException, JsonProcessingException {
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

        private void serializeNodes(ACAQAlgorithmGraph algorithmGraph, JsonGenerator jsonGenerator) throws IOException {
            for (Map.Entry<String, ACAQAlgorithm> kv : algorithmGraph.algorithms.entrySet()) {
                jsonGenerator.writeObjectField(StringUtils.jsonify(kv.getKey()), kv.getValue());
            }
        }

        private void serializeEdges(ACAQAlgorithmGraph graph, JsonGenerator jsonGenerator) throws IOException {
            for (Map.Entry<ACAQDataSlot, ACAQDataSlot> edge : graph.getSlotEdges()) {
                jsonGenerator.writeStartObject();
                jsonGenerator.writeStringField("source-algorithm", StringUtils.jsonify(graph.getIdOf(edge.getKey().getAlgorithm())));
                jsonGenerator.writeStringField("target-algorithm", StringUtils.jsonify(graph.getIdOf(edge.getValue().getAlgorithm())));
                jsonGenerator.writeStringField("source-slot", StringUtils.makeFilesystemCompatible(edge.getKey().getName()));
                jsonGenerator.writeStringField("target-slot", StringUtils.makeFilesystemCompatible(edge.getValue().getName()));
                jsonGenerator.writeEndObject();
            }
        }
    }

    public static class Deserializer extends JsonDeserializer<ACAQAlgorithmGraph> {
        @Override
        public ACAQAlgorithmGraph deserialize(JsonParser jsonParser, DeserializationContext deserializationContext) throws IOException, JsonProcessingException {
            ACAQAlgorithmGraph graph = new ACAQAlgorithmGraph();
            graph.fromJson(jsonParser.readValueAsTree());
            return graph;
        }
    }
}
