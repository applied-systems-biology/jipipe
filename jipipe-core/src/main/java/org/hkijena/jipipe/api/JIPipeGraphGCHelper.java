package org.hkijena.jipipe.api;

import com.google.common.eventbus.EventBus;
import gnu.trove.map.TObjectIntMap;
import gnu.trove.map.hash.TObjectIntHashMap;
import org.hkijena.jipipe.api.data.JIPipeDataSlot;
import org.hkijena.jipipe.api.nodes.JIPipeAlgorithm;
import org.hkijena.jipipe.api.nodes.JIPipeGraph;
import org.hkijena.jipipe.api.nodes.JIPipeGraphNode;

import java.util.HashSet;
import java.util.Set;

/**
 * Used to track garbage collection for the execution of a {@link JIPipeGraph}
 */
public class JIPipeGraphGCHelper {
    private final EventBus eventBus = new EventBus();
    private final JIPipeGraph graph;
    private final TObjectIntMap<JIPipeDataSlot> slotScore = new TObjectIntHashMap<>();
    private final Set<JIPipeDataSlot> completedSlots = new HashSet<>();
    private final Set<JIPipeDataSlot> flushedSlots = new HashSet<>();

    public JIPipeGraphGCHelper(JIPipeGraph graph) {
        this.graph = graph;
        initialize();
    }

    private void initialize() {
        for (JIPipeDataSlot node : graph.getSlotNodes()) {
            if (node.isInput()) {
                slotScore.put(node, graph.getGraph().inDegreeOf(node));
            } else if (node.isOutput()) {
                slotScore.put(node, graph.getGraph().outDegreeOf(node));
            } else {
                throw new UnsupportedOperationException("Unknown slot type!");
            }
        }
    }

    /**
     * Marks a slot node as done (all outgoing/ingoing data transfers have been handled) by setting the usage counter to zero
     * @param slot the slot
     */
    public void markAsCompleted(JIPipeDataSlot slot) {
        if(!completedSlots.contains(slot)) {
            slotScore.put(slot, 0);
            completedSlots.add(slot);
            eventBus.post(new SlotCompletedEvent(this, slot));
        }
    }

    /**
     * Marks a slot node as flushed (all outgoing/ingoing data transfers have been handled and data was flushed) by setting the usage counter to a negative value
     * @param slot the slot
     */
    public void markAsFlushed(JIPipeDataSlot slot) {
        if(!flushedSlots.contains(slot)) {
            slotScore.put(slot, -1);
            if(!completedSlots.contains(slot)) {
                completedSlots.add(slot);
                eventBus.post(new SlotCompletedEvent(this, slot));
            }
            flushedSlots.add(slot);
            eventBus.post(new SlotFlushedEvent(this, slot));
        }
    }

    /**
     * Decrements the usage counter of the slot. Has no effect if the counter is zero or less.
     * @param slot the slot
     */
    public void decrementUsageCounter(JIPipeDataSlot slot) {
        int i = slotScore.get(slot);
        if(i > 1) {
            slotScore.put(slot, i - 1);
        }
        else if(i == 1) {
            slotScore.put(slot, 0);
            completedSlots.add(slot);
            eventBus.post(new SlotCompletedEvent(this, slot));
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

    /**
     * Decrements the usage counter of the slot. Has no effect if the counter is zero or less.
     * @param slot the slot
     */
    public void incrementUsageCounter(JIPipeDataSlot slot) {
        slotScore.increment(slot);
    }

    public EventBus getEventBus() {
        return eventBus;
    }

    /**
     * Gets all slots with a usage counter of zero
     * @return the slots
     */
    public Set<JIPipeDataSlot> getCompletedSlots() {
        return completedSlots;
    }

    /**
     * Gets all slots with a usage counter of greater than zero
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

    /**
     * Helper method for deactivation of a node
     * @param node the node
     */
    public void deactivateNode(JIPipeGraphNode node) {
        // Mark sources
        for (JIPipeDataSlot inputSlot : node.getInputSlots()) {
            for (JIPipeDataSlot sourceSlot : graph.getSourceSlots(inputSlot)) {
                decrementUsageCounter(sourceSlot);
            }
        }
        // Mark inputs as completed
        for (JIPipeDataSlot inputSlot : node.getInputSlots()) {
            markAsCompleted(inputSlot);
        }
    }

    public static class SlotCompletedEvent {
        private final JIPipeGraphGCHelper source;
        private final JIPipeDataSlot slot;

        public SlotCompletedEvent(JIPipeGraphGCHelper source, JIPipeDataSlot slot) {
            this.source = source;
            this.slot = slot;
        }

        public JIPipeGraphGCHelper getSource() {
            return source;
        }

        public JIPipeDataSlot getSlot() {
            return slot;
        }
    }

    public static class SlotFlushedEvent {
        private final JIPipeGraphGCHelper source;
        private final JIPipeDataSlot slot;

        public SlotFlushedEvent(JIPipeGraphGCHelper source, JIPipeDataSlot slot) {
            this.source = source;
            this.slot = slot;
        }

        public JIPipeGraphGCHelper getSource() {
            return source;
        }

        public JIPipeDataSlot getSlot() {
            return slot;
        }
    }
}
