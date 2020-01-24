package org.hkijena.acaq5.api;

import com.google.common.eventbus.EventBus;
import com.google.common.eventbus.Subscribe;
import org.hkijena.acaq5.api.events.AlgorithmSlotsChangedEvent;
import org.hkijena.acaq5.api.events.AlgorithmGraphChangedEvent;
import org.jgrapht.*;
import org.jgrapht.alg.cycle.CycleDetector;
import org.jgrapht.graph.*;

import java.util.HashSet;
import java.util.Set;

/**
 * Manages multiple {@link ACAQAlgorithm} instances as graph
 */
public class ACAQAlgorithmGraph {

    private DefaultDirectedGraph<ACAQDataSlot, DefaultEdge> graph = new DefaultDirectedGraph<>(DefaultEdge.class);
    private Set<ACAQAlgorithm> algorithms = new HashSet<>();
    private EventBus eventBus = new EventBus();

    public ACAQAlgorithmGraph() {

    }

    public void insertNode(ACAQAlgorithm algorithm) {
        algorithms.add(algorithm);
        algorithm.getEventBus().register(this);

        // Add input and output slots as vertices
        algorithm.getInputSlots().values().stream().forEach(graph::addVertex);
        algorithm.getOutputSlots().values().stream().forEach(graph::addVertex);

        // Connect input -> output in the graph
        for(ACAQDataSlot inputSlot : algorithm.getInputSlots().values()) {
            for(ACAQDataSlot outputSlot : algorithm.getOutputSlots().values()) {
                graph.addEdge(inputSlot, outputSlot);
            }
        }

        getEventBus().post(new AlgorithmGraphChangedEvent(this));
    }

    public void removeNode(ACAQAlgorithm algorithm) {
        algorithms.remove(algorithm);
        algorithm.getEventBus().unregister(this);
        for(ACAQDataSlot slot : algorithm.getInputSlots().values()) {
            graph.removeVertex(slot);
        }
        for(ACAQDataSlot slot : algorithm.getOutputSlots().values()) {
            graph.removeVertex(slot);
        }
        getEventBus().post(new AlgorithmGraphChangedEvent(this));
    }

    public boolean canConnect(ACAQOutputDataSlot<?> source, ACAQInputDataSlot<?> target) {
        Graph<ACAQDataSlot, DefaultEdge> copy = (Graph<ACAQDataSlot, DefaultEdge>)graph.clone();
        copy.addEdge(source, target);
        CycleDetector<ACAQDataSlot, DefaultEdge> cycleDetector = new CycleDetector<>(copy);
        return !cycleDetector.detectCycles();
    }

    public void connect(ACAQOutputDataSlot<?> source, ACAQInputDataSlot<?> target) {
        if(!canConnect(source, target))
            throw new RuntimeException("Cannot connect data slots! This would cause a cycle!");
        graph.addEdge(source, target);
        getEventBus().post(new AlgorithmGraphChangedEvent(this));
    }

    public void repairGraph() {
        // Remove deleted slots from the graph
        Set<ACAQDataSlot> toRemove = new HashSet<>();
        for(ACAQAlgorithm algorithm : algorithms) {
            for(ACAQDataSlot slot : graph.vertexSet()) {
                if(slot.getAlgorithm() == algorithm && !algorithm.getInputSlots().containsValue(slot) &&
                        !algorithm.getInputSlots().containsValue(slot)) {
                    toRemove.add(slot);
                }
            }
        }
        toRemove.stream().forEach(graph::removeVertex);
        if(!toRemove.isEmpty())
            getEventBus().post(new AlgorithmGraphChangedEvent(this));
    }

    @Subscribe
    public void onAlgorithmSlotsChanged(AlgorithmSlotsChangedEvent event) {
        repairGraph();
    }

    public EventBus getEventBus() {
        return eventBus;
    }
}
