package org.hkijena.jipipe.api;

import com.google.common.collect.BiMap;
import com.google.common.collect.HashBiMap;
import org.hkijena.jipipe.api.data.JIPipeData;
import org.hkijena.jipipe.api.data.JIPipeDataSlot;
import org.hkijena.jipipe.api.data.JIPipeDataSlotInfo;
import org.hkijena.jipipe.api.data.JIPipeSlotType;
import org.hkijena.jipipe.api.events.AbstractJIPipeEvent;
import org.hkijena.jipipe.api.events.JIPipeEventEmitter;
import org.hkijena.jipipe.api.nodes.JIPipeGraph;
import org.hkijena.jipipe.api.nodes.JIPipeGraphEdge;
import org.hkijena.jipipe.api.nodes.JIPipeGraphNode;
import org.jgrapht.graph.DefaultDirectedGraph;
import org.jgrapht.graph.DefaultEdge;

import java.util.HashSet;
import java.util.Set;

/**
 * Used to track garbage collection for the execution of a {@link JIPipeGraph}
 */
public class JIPipeGraphGCHelper {
    private final JIPipeGraph graph;
    private final DefaultDirectedGraph<JIPipeDataSlot, DefaultEdge> gcGraph;
    private final Set<JIPipeDataSlot> completedSlots = new HashSet<>();
    private final BiMap<JIPipeGraphNode, JIPipeDataSlot> dummyNodes = HashBiMap.create();

    private final SlotCompletedEventEmitter slotCompletedEventEmitter = new SlotCompletedEventEmitter();

    public JIPipeGraphGCHelper(JIPipeGraph graph) {
        this.graph = graph;
        this.gcGraph = new DefaultDirectedGraph<>(DefaultEdge.class);
        initialize();
    }

    private void initialize() {
        for (JIPipeDataSlot node : graph.getSlotNodes()) {
            gcGraph.addVertex(node);
        }
        // Copy all edges. We only copy inter-node edges
        for (JIPipeGraphEdge edge : graph.getGraph().edgeSet()) {
            JIPipeDataSlot edgeSource = graph.getGraph().getEdgeSource(edge);
            JIPipeDataSlot edgeTarget = graph.getGraph().getEdgeTarget(edge);
            if (edgeSource.getNode() != edgeTarget.getNode()) {
                gcGraph.addEdge(edgeSource, edgeTarget);
            }
        }
        // We create a dummy node for each JIPipeGraphNode and apply connections there.
        // This makes it easier to deal with nodes that have no inputs or outputs
        for (JIPipeGraphNode graphNode : graph.getGraphNodes()) {
            JIPipeDataSlot dummy = new JIPipeDataSlotInfo(JIPipeData.class, JIPipeSlotType.Output, "Dummy", "").createInstance(graphNode);
            gcGraph.addVertex(dummy);
            dummyNodes.put(graphNode, dummy);
            for (JIPipeDataSlot inputSlot : graphNode.getInputSlots()) {
                gcGraph.addEdge(inputSlot, dummy);
            }
            for (JIPipeDataSlot outputSlot : graphNode.getOutputSlots()) {
                gcGraph.addEdge(dummy, outputSlot);
            }
        }
    }

    /**
     * Marks a slot node as done (all outgoing/ingoing data transfers have been handled) by setting the usage counter to zero
     *
     * @param slot the slot
     */
    public void markAsCompleted(JIPipeDataSlot slot) {
        if (!completedSlots.contains(slot)) {
            isolate(slot);
            completedSlots.add(slot);
            if (!dummyNodes.containsValue(slot)) {
                slotCompletedEventEmitter.emit(new SlotCompletedEvent(this, slot));
            }
        }
    }

    private void isolate(JIPipeDataSlot slot) {
        for (DefaultEdge edge : gcGraph.edgesOf(slot)) {
            gcGraph.removeEdge(edge);
        }
    }

    /**
     * Checks the slot for completion (degree is zero)
     *
     * @param slot the slot
     */
    private void checkForCompletion(JIPipeDataSlot slot) {
        if (gcGraph.degreeOf(slot) == 0 && !completedSlots.contains(slot)) {
            completedSlots.add(slot);
            if (!dummyNodes.containsValue(slot)) {
                slotCompletedEventEmitter.emit(new SlotCompletedEvent(this, slot));
            }
        }
    }

    /**
     * Marks an output slot as used
     *
     * @param source the output slot
     * @param target the input slot
     */
    public void markCopyOutputToInput(JIPipeDataSlot source, JIPipeDataSlot target) {
        if (!source.isOutput()) {
            throw new UnsupportedOperationException();
        }
        gcGraph.removeEdge(source, target);
        checkForCompletion(source);
    }

    /**
     * Marks a node as executed, marking inputs for GC
     *
     * @param node the node
     */
    public void markNodeExecuted(JIPipeGraphNode node) {
        JIPipeDataSlot dummy = dummyNodes.get(node);
        for (JIPipeDataSlot inputSlot : node.getInputSlots()) {
            gcGraph.removeEdge(inputSlot, dummy);
            checkForCompletion(inputSlot);
        }
        for (JIPipeDataSlot outputSlot : node.getOutputSlots()) {
            gcGraph.removeEdge(dummy, outputSlot);
            checkForCompletion(outputSlot);
        }
    }

    /**
     * Marks all remaining slots as completed
     */
    public void markAllAsCompleted() {
        for (JIPipeDataSlot slotNode : graph.getSlotNodes()) {
            markAsCompleted(slotNode);
        }
    }

    public SlotCompletedEventEmitter getSlotCompletedEventEmitter() {
        return slotCompletedEventEmitter;
    }

    /**
     * Gets all slots with a usage counter of zero
     *
     * @return the slots
     */
    public Set<JIPipeDataSlot> getCompletedSlots() {
        return completedSlots;
    }

    /**
     * Gets all slots with a usage counter of greater than zero
     *
     * @return the slots
     */
    public Set<JIPipeDataSlot> getIncompleteSlots() {
        Set<JIPipeDataSlot> result = new HashSet<>(graph.getSlotNodes());
        result.removeAll(completedSlots);
        return result;
    }

    public JIPipeGraph getGraph() {
        return graph;
    }

    @Override
    public String toString() {
        return "Graph GC [" + gcGraph.vertexSet().size() + " vertices, " + gcGraph.edgeSet().size() + " edges]";
    }

    public interface SlotCompletedEventListener {
        void onGCSlotCompletedEvent(SlotCompletedEvent event);
    }

    public static class SlotCompletedEvent extends AbstractJIPipeEvent {
        private final JIPipeDataSlot slot;

        public SlotCompletedEvent(JIPipeGraphGCHelper source, JIPipeDataSlot slot) {
            super(source);
            this.slot = slot;
        }

        public JIPipeDataSlot getSlot() {
            return slot;
        }
    }

    public static class SlotCompletedEventEmitter extends JIPipeEventEmitter<SlotCompletedEvent, SlotCompletedEventListener> {

        @Override
        protected void call(SlotCompletedEventListener slotCompletedEventListener, SlotCompletedEvent event) {
            slotCompletedEventListener.onGCSlotCompletedEvent(event);
        }
    }
}
