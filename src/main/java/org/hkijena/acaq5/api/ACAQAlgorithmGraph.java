package org.hkijena.acaq5.api;

import com.google.common.eventbus.EventBus;
import com.google.common.eventbus.Subscribe;
import org.hkijena.acaq5.api.events.AlgorithmGraphChangedEvent;
import org.hkijena.acaq5.api.events.AlgorithmSlotsChangedEvent;
import org.jgrapht.Graph;
import org.jgrapht.alg.cycle.CycleDetector;
import org.jgrapht.graph.DefaultDirectedGraph;
import org.jgrapht.graph.DefaultEdge;

import java.util.*;

/**
 * Manages multiple {@link ACAQAlgorithm} instances as graph
 */
public class ACAQAlgorithmGraph {

    private DefaultDirectedGraph<ACAQDataSlot<?>, DefaultEdge> graph = new DefaultDirectedGraph<>(DefaultEdge.class);
    private Set<ACAQAlgorithm> algorithms = new HashSet<>();
    private EventBus eventBus = new EventBus();

    public ACAQAlgorithmGraph() {

    }

    public void insertNode(ACAQAlgorithm algorithm) {
        algorithms.add(algorithm);
        algorithm.getEventBus().register(this);
        repairGraph();
    }

    public void removeNode(ACAQAlgorithm algorithm) {
        algorithms.remove(algorithm);
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
        for(ACAQAlgorithm algorithm : algorithms) {
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
        for(ACAQAlgorithm algorithm : algorithms) {

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

    public Set<ACAQAlgorithm> getNodes() {
        return algorithms;
    }

    public boolean containsNode(ACAQAlgorithm algorithm) {
        return algorithms.contains(algorithm);
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
}
