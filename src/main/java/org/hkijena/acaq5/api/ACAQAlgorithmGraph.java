package org.hkijena.acaq5.api;

import com.google.common.eventbus.EventBus;
import com.google.common.eventbus.Subscribe;
import org.hkijena.acaq5.api.events.AlgorithmGraphChangedEvent;
import org.hkijena.acaq5.api.events.AlgorithmSlotsChangedEvent;
import org.jgrapht.Graph;
import org.jgrapht.alg.cycle.CycleDetector;
import org.jgrapht.graph.DefaultDirectedGraph;
import org.jgrapht.graph.DefaultEdge;

import java.util.Collections;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

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

        // Add input and output slots as vertices
        algorithm.getInputSlots().stream().forEach(graph::addVertex);
        algorithm.getOutputSlots().stream().forEach(graph::addVertex);

        // Connect input -> output in the graph
        for(ACAQDataSlot inputSlot : algorithm.getInputSlots()) {
            for(ACAQDataSlot outputSlot : algorithm.getOutputSlots()) {
                graph.addEdge(inputSlot, outputSlot);
            }
        }

        getEventBus().post(new AlgorithmGraphChangedEvent(this));
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

    public boolean canConnect(ACAQDataSlot<?> source, ACAQDataSlot<?> target) {
        if(!source.isOutput() || !target.isInput())
            return false;
        Graph<ACAQDataSlot<?>, DefaultEdge> copy = (Graph<ACAQDataSlot<?>, DefaultEdge>)graph.clone();
        copy.addEdge(source, target);
        CycleDetector<ACAQDataSlot<?>, DefaultEdge> cycleDetector = new CycleDetector<>(copy);
        return !cycleDetector.detectCycles();
    }

    public void connect(ACAQDataSlot<?> source, ACAQDataSlot<?> target) {
        if(!canConnect(source, target))
            throw new RuntimeException("Cannot connect data slots!");
        graph.addEdge(source, target);
        getEventBus().post(new AlgorithmGraphChangedEvent(this));
    }

    public void repairGraph() {
        // Remove deleted slots from the graph
        Set<ACAQDataSlot<?>> toRemove = new HashSet<>();
        for(ACAQAlgorithm algorithm : algorithms) {
            for(ACAQDataSlot<?> slot : graph.vertexSet()) {
                if(slot.getAlgorithm() == algorithm && !algorithm.getInputSlots().contains(slot) &&
                        !algorithm.getInputSlots().contains(slot)) {
                    toRemove.add(slot);
                }
            }
        }
        toRemove.forEach(graph::removeVertex);
        if(!toRemove.isEmpty())
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
            result.add(source);
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
}
