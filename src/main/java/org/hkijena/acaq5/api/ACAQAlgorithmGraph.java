package org.hkijena.acaq5.api;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.google.common.collect.*;
import com.google.common.eventbus.EventBus;
import com.google.common.eventbus.Subscribe;
import org.hkijena.acaq5.ACAQRegistryService;
import org.hkijena.acaq5.api.events.AlgorithmGraphChangedEvent;
import org.hkijena.acaq5.api.events.AlgorithmSlotsChangedEvent;
import org.jgrapht.Graph;
import org.jgrapht.alg.cycle.CycleDetector;
import org.jgrapht.graph.DefaultDirectedGraph;
import org.jgrapht.graph.DefaultEdge;
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
public class ACAQAlgorithmGraph {

    public static final String ACAQ_ALGORITHM_KEY_PREPROCESSING_OUTPUT = "acaq:preprocessing-output";
    private DefaultDirectedGraph<ACAQDataSlot<?>, DefaultEdge> graph = new DefaultDirectedGraph<>(DefaultEdge.class);
    private BiMap<String, ACAQAlgorithm> algorithms = HashBiMap.create();
    private EventBus eventBus = new EventBus();

    public ACAQAlgorithmGraph() {

    }

    public ACAQAlgorithmGraph(ACAQAlgorithmGraph other) {
        // Copy nodes
        for(Map.Entry<String, ACAQAlgorithm> kv : other.algorithms.entrySet()) {
            ACAQAlgorithm algorithm = ACAQAlgorithm.clone(kv.getValue());
            algorithms.put(kv.getKey(), algorithm);
            algorithm.getEventBus().register(this);
        }
        repairGraph();

        // Copy edges
        for(Map.Entry<ACAQDataSlot<?>, ACAQDataSlot<?>> edge : other.getSlotEdges()) {
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
     * @param key The unique ID
     * @param algorithm
     */
    public void insertNode(String key, ACAQAlgorithm algorithm) {
        if(algorithms.containsKey(key))
            throw new RuntimeException("Already contains algorithm with name " + key);
        algorithms.put(key, algorithm);
        algorithm.getEventBus().register(this);
        repairGraph();
    }

    /**
     * Inserts an algorithm into the graph.
     * The name is automatically generated
     * @param algorithm
     */
    public void insertNode(ACAQAlgorithm algorithm) {
       String name;
       if(algorithm instanceof ACAQPreprocessingOutput)
           name = ACAQ_ALGORITHM_KEY_PREPROCESSING_OUTPUT;
       else
           name = algorithm.getName().toLowerCase().replace(' ', '-');
       String uniqueName = name;
       for(int i = 2; algorithms.containsKey(uniqueName); ++i) {
           uniqueName = name + "-" + i;
       }
       insertNode(uniqueName, algorithm);
    }

    public void removeNode(ACAQAlgorithm algorithm) {
        algorithms.remove(algorithms.inverse().get(algorithm));
        algorithm.getEventBus().unregister(this);
        for(ACAQDataSlot<?> slot : algorithm.getInputSlots()) {
            graph.removeVertex(slot);
        }
        for(ACAQDataSlot<?> slot : algorithm.getOutputSlots()) {
            graph.removeVertex(slot);
        }
        getEventBus().post(new AlgorithmGraphChangedEvent(this));
    }

    /**
     * Slow implementation of connection checking that tests for graph cycles
     * @param source
     * @param target
     * @return
     */
    public boolean canConnect(ACAQDataSlot<?> source, ACAQDataSlot<?> target) {
        if(!canConnectFast(source, target))
            return false;
        Graph<ACAQDataSlot<?>, DefaultEdge> copy = (Graph<ACAQDataSlot<?>, DefaultEdge>)graph.clone();
        copy.addEdge(source, target);
        CycleDetector<ACAQDataSlot<?>, DefaultEdge> cycleDetector = new CycleDetector<>(copy);
        return !cycleDetector.detectCycles();
    }

    /**
     * Fast implementation of connection checking without copying the graph
     * @param source
     * @param target
     * @return
     */
    public boolean canConnectFast(ACAQDataSlot<?> source, ACAQDataSlot<?> target) {
        if (!source.isOutput() || !target.isInput())
            return false;
        if (!target.getAcceptedDataType().isAssignableFrom(source.getAcceptedDataType()))
            return false;
        return true;
    }

    public void connect(ACAQDataSlot<?> source, ACAQDataSlot<?> target) {
        if(!canConnect(source, target))
            throw new RuntimeException("Cannot connect data slots!");
        graph.addEdge(source, target);
        getEventBus().post(new AlgorithmGraphChangedEvent(this));
    }

    public void repairGraph() {

        boolean modified = false;

        // Remove deleted slots from the graph
        Set<ACAQDataSlot<?>> toRemove = new HashSet<>();
        for(ACAQAlgorithm algorithm : algorithms.values()) {
            for(ACAQDataSlot<?> slot : graph.vertexSet()) {
                if(slot.getAlgorithm() == algorithm && !algorithm.getInputSlots().contains(slot) &&
                        !algorithm.getOutputSlots().contains(slot)) {
                    toRemove.add(slot);
                    modified = true;
                }
            }
        }
        toRemove.forEach(graph::removeVertex);

        // Add missing slots
        for(ACAQAlgorithm algorithm : algorithms.values()) {

            // Add vertices
            for(ACAQDataSlot<?> inputSlot : algorithm.getInputSlots()) {
                if(!graph.vertexSet().contains(inputSlot)) {
                    graph.addVertex(inputSlot);
                    modified = true;
                }
            }
            for(ACAQDataSlot<?> outputSlot : algorithm.getOutputSlots()) {
                if (!graph.vertexSet().contains(outputSlot)) {
                    graph.addVertex(outputSlot);
                    modified = true;
                }
            }

            // Connect input -> output in the graph
            for(ACAQDataSlot<?> inputSlot : algorithm.getInputSlots()) {
                for(ACAQDataSlot<?> outputSlot : algorithm.getOutputSlots()) {
                    if(!graph.containsEdge(inputSlot, outputSlot)) {
                        graph.addEdge(inputSlot, outputSlot);
                        modified = true;
                    }
                }
            }
        }

        if(modified)
            getEventBus().post(new AlgorithmGraphChangedEvent(this));
    }

    /**
     * If exists, returns the output slot that provides data for the input slot
     * Returns null if target is an output or no slot exists
     * @return
     */
    public ACAQDataSlot<?> getSourceSlot(ACAQDataSlot<?> target) {
        if(target.isInput()) {
            Set<DefaultEdge> edges = graph.incomingEdgesOf(target);
            if(edges.isEmpty())
                return null;
            if(edges.size() > 1)
                throw new RuntimeException("Graph is illegal!");
            return graph.getEdgeSource(edges.iterator().next());
        }
        return null;
    }

    /**
     * Returns the list of input slots that are provided by the source slot
     * Returns an empty set if the target is an input or no slot exists
     * @param source
     * @return
     */
    public Set<ACAQDataSlot<?>> getTargetSlots(ACAQDataSlot<?> source) {
        if(source.isOutput()) {
            Set<DefaultEdge> edges = graph.outgoingEdgesOf(source);
            Set<ACAQDataSlot<?>> result = new HashSet<>();
            for(DefaultEdge edge : edges) {
                result.add(graph.getEdgeTarget(edge));
            }
            return result;
        }
        return Collections.emptySet();
    }

    /**
     * Returns all available sources for an input slot
     * @param target
     * @return
     */
    public Set<ACAQDataSlot<?>> getAvailableSources(ACAQDataSlot<?> target) {
        if(getSourceSlot(target) != null)
            return Collections.emptySet();
        Set<ACAQDataSlot<?>> result = new HashSet<>();
        for(ACAQDataSlot<?> source : graph.vertexSet()) {
            if(source == target)
                continue;
            if(!source.isOutput())
                continue;
            if(source.getAlgorithm() == target.getAlgorithm())
                continue;
            if(graph.containsEdge(source, target))
                continue;
            if(!canConnectFast(source, target))
                continue;
            result.add(source);
        }
        return result;
    }

    /**
     * Completely disconnects a slot
     * @param slot
     */
    public void disconnectAll(ACAQDataSlot<?> slot) {
        if(slot.isInput()) {
            ACAQDataSlot<?> source = getSourceSlot(slot);
            if(source != null) {
                graph.removeEdge(source, slot);
                getEventBus().post(new AlgorithmGraphChangedEvent(this));
            }
        }
        else if(slot.isOutput()) {
            boolean modified = false;
            for(ACAQDataSlot<?> target : getTargetSlots(slot)) {
                graph.removeEdge(slot, target);
                modified = true;
            }
            if(modified)
                getEventBus().post(new AlgorithmGraphChangedEvent(this));
        }
    }

    /**
     * Returns all available target for an output slot
     * @param source
     * @return
     */
    public Set<ACAQDataSlot<?>> getAvailableTargets(ACAQDataSlot<?> source) {
        if(source.isInput())
            return Collections.emptySet();
        Set<ACAQDataSlot<?>> result = new HashSet<>();
        for(ACAQDataSlot<?> target : graph.vertexSet()) {
            if(source == target)
                continue;
            if(!target.isInput())
                continue;
            if(target.getAlgorithm() == source.getAlgorithm())
                continue;
            if(graph.containsEdge(source, target))
                continue;
            if(getSourceSlot(target) != null)
                continue;
            if(!canConnectFast(source, target))
                continue;
            result.add(target);
        }
        return result;
    }

    @Subscribe
    public void onAlgorithmSlotsChanged(AlgorithmSlotsChangedEvent event) {
        repairGraph();
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

    public Set<Map.Entry<ACAQDataSlot<?>, ACAQDataSlot<?>>> getSlotEdges() {
        Set<Map.Entry<ACAQDataSlot<?>, ACAQDataSlot<?>>> result = new HashSet<>();
        for(DefaultEdge edge : graph.edgeSet()) {
            ACAQDataSlot<?> source = graph.getEdgeSource(edge);
            ACAQDataSlot<?> target = graph.getEdgeTarget(edge);
            if(source.getAlgorithm() == target.getAlgorithm())
                continue;
            result.add(new AbstractMap.SimpleImmutableEntry<>(source, target));
        }
        return result;
    }

    public void fromJson(JsonNode node) {
        if(!node.has("nodes"))
            return;

        for(Map.Entry<String, JsonNode> kv : ImmutableList.copyOf(node.get("nodes").fields())) {

            if(algorithms.containsKey(kv.getKey())) {
                // Load an existing algorithm (like the preprocessing output)
                algorithms.get(kv.getKey()).fromJson(kv.getValue());
            }
            else {
                String className = kv.getValue().get("acaq:algorithm-class").asText();
                ACAQAlgorithm algorithm = ACAQRegistryService.getInstance().getAlgorithmRegistry().createInstanceFromClassName(className);
                algorithm.fromJson(kv.getValue());
                insertNode(kv.getKey(), algorithm);
            }
        }

        // Load edges
        for(JsonNode edgeNode : ImmutableList.copyOf(node.get("edges").elements())) {
            ACAQAlgorithm sourceAlgorithm = algorithms.get(edgeNode.get("source-algorithm").asText());
            ACAQAlgorithm targetAlgorithm = algorithms.get(edgeNode.get("target-algorithm").asText());
            ACAQDataSlot<?> source = sourceAlgorithm.getSlots().get(edgeNode.get("source-slot").asText());
            ACAQDataSlot<?> target = targetAlgorithm.getSlots().get(edgeNode.get("target-slot").asText());
            connect(source, target);
        }
    }

    public int getAlgorithmCount() {
        return algorithms.size();
    }

    public int getSlotCount() {
        return graph.vertexSet().size();
    }

    public void exportDOT(Path fileName) {
        DOTExporter<ACAQDataSlot<?>, DefaultEdge> exporter = new DOTExporter<>();
        try {
            exporter.exportGraph(graph, fileName.toFile());
        } catch (ExportException e) {
            throw new RuntimeException(e);
        }
    }

    public List<ACAQDataSlot<?>> traverse() {
        GraphIterator<ACAQDataSlot<?>, DefaultEdge> iterator = new TopologicalOrderIterator<>(graph);
        List<ACAQDataSlot<?>> result = new ArrayList<>();
        while(iterator.hasNext()) {
            ACAQDataSlot<?> slot = iterator.next();
            result.add(slot);
        }
        return result;
    }

    public Set<ACAQDataSlot<?>> getSlotNodes() {
        return graph.vertexSet();
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
            int index = 0;
            for(Map.Entry<String, ACAQAlgorithm> kv : algorithmGraph.algorithms.entrySet()) {
                jsonGenerator.writeObjectField(kv.getKey(), kv.getValue());
            }
        }

        private void serializeEdges(ACAQAlgorithmGraph graph, JsonGenerator jsonGenerator) throws IOException {
            for(Map.Entry<ACAQDataSlot<?>, ACAQDataSlot<?>> edge : graph.getSlotEdges()) {
                jsonGenerator.writeStartObject();
                jsonGenerator.writeStringField("source-algorithm", graph.algorithms.inverse().get(edge.getKey().getAlgorithm()));
                jsonGenerator.writeStringField("target-algorithm", graph.algorithms.inverse().get(edge.getValue().getAlgorithm()));
                jsonGenerator.writeStringField("source-slot", edge.getKey().getName());
                jsonGenerator.writeStringField("target-slot", edge.getValue().getName());
                jsonGenerator.writeEndObject();
            }
        }
    }
}
