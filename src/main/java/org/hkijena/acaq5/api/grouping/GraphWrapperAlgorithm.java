package org.hkijena.acaq5.api.grouping;

import com.google.common.eventbus.Subscribe;
import org.hkijena.acaq5.ACAQDependency;
import org.hkijena.acaq5.api.ACAQRunnerSubStatus;
import org.hkijena.acaq5.api.ACAQValidityReport;
import org.hkijena.acaq5.api.algorithm.*;
import org.hkijena.acaq5.api.data.ACAQDataSlot;
import org.hkijena.acaq5.api.data.ACAQMutableSlotConfiguration;
import org.hkijena.acaq5.api.data.ACAQSlotDefinition;
import org.hkijena.acaq5.api.events.SlotAddedEvent;
import org.hkijena.acaq5.api.events.SlotRemovedEvent;
import org.hkijena.acaq5.api.events.SlotRenamedEvent;

import java.util.*;
import java.util.function.Consumer;
import java.util.function.Supplier;

/**
 * An algorithm that wraps another algorithm graph
 */
public class GraphWrapperAlgorithm extends ACAQAlgorithm {

    private ACAQAlgorithmGraph wrappedGraph;
    private GraphWrapperAlgorithmInput algorithmInput;
    private GraphWrapperAlgorithmOutput algorithmOutput;
    private IOSlotWatcher ioSlotWatcher;
    private boolean slotConfigurationIsComplete;

    /**
     * @param declaration the declaration
     */
    public GraphWrapperAlgorithm(ACAQAlgorithmDeclaration declaration, ACAQAlgorithmGraph wrappedGraph) {
        super(declaration, new ACAQMutableSlotConfiguration());
        this.setWrappedGraph(wrappedGraph);
    }

    /**
     * Copies the algorithm
     *
     * @param other the original
     */
    public GraphWrapperAlgorithm(GraphWrapperAlgorithm other) {
        super(other);
        setWrappedGraph(new ACAQAlgorithmGraph(other.wrappedGraph));
    }

    /**
     * Updates the slots from the wrapped graph
     */
    private void updateSlots() {
        ACAQMutableSlotConfiguration slotConfiguration = (ACAQMutableSlotConfiguration) getSlotConfiguration();
        ACAQMutableSlotConfiguration inputSlotConfiguration = (ACAQMutableSlotConfiguration) getGroupInput().getSlotConfiguration();
        ACAQMutableSlotConfiguration outputSlotConfiguration = (ACAQMutableSlotConfiguration) getGroupOutput().getSlotConfiguration();

        slotConfiguration.setInputSealed(true);
        slotConfiguration.setOutputSealed(true);
        slotConfiguration.setAllowInheritedOutputSlots(false);
        slotConfiguration.clearInputSlots(false);
        slotConfiguration.clearOutputSlots(false);
        slotConfigurationIsComplete = true;
        for (Map.Entry<String, ACAQSlotDefinition> entry : inputSlotConfiguration.getSlots().entrySet()) {
            if(entry.getValue().getSlotType() == ACAQDataSlot.SlotType.Input)
                slotConfiguration.addSlot(entry.getKey(), entry.getValue(), false);
        }
        for (Map.Entry<String, ACAQSlotDefinition> entry : outputSlotConfiguration.getSlots().entrySet()) {
            if(entry.getValue().getSlotType() == ACAQDataSlot.SlotType.Output) {
                String name = entry.getKey().substring("Output ".length());
                if (!slotConfiguration.getSlots().containsKey(name))
                    slotConfiguration.addSlot(name, entry.getValue(), false);
                else
                    slotConfigurationIsComplete = false;
            }
        }
    }



    /**
     * Gets the graphs's input node
     * @return the graph's input node
     */
    public GraphWrapperAlgorithmInput getGroupInput() {
        if (algorithmInput == null) {
            for (ACAQGraphNode node : wrappedGraph.getAlgorithmNodes().values()) {
                if (node instanceof GraphWrapperAlgorithmInput) {
                    algorithmInput = (GraphWrapperAlgorithmInput) node;
                    break;
                }
            }

            // Create if it doesn't exist
            algorithmInput = ACAQAlgorithm.newInstance("graph-wrapper:input");
            wrappedGraph.insertNode(algorithmInput, ACAQAlgorithmGraph.COMPARTMENT_DEFAULT);
        }
        return algorithmInput;
    }

    /**
     * Gets the graphs's output node
     * @return the graph's output node
     */
    public GraphWrapperAlgorithmOutput getGroupOutput() {
        if (algorithmOutput == null) {
            for (ACAQGraphNode node : wrappedGraph.getAlgorithmNodes().values()) {
                if (node instanceof GraphWrapperAlgorithmOutput) {
                    algorithmOutput = (GraphWrapperAlgorithmOutput) node;
                    break;
                }
            }

            // Create if it doesn't exist
            algorithmOutput = ACAQAlgorithm.newInstance("graph-wrapper:output");
            wrappedGraph.insertNode(algorithmOutput, ACAQAlgorithmGraph.COMPARTMENT_DEFAULT);
        }
        return algorithmOutput;
    }

    @Override
    public void run(ACAQRunnerSubStatus subProgress, Consumer<ACAQRunnerSubStatus> algorithmProgress, Supplier<Boolean> isCancelled) {
       clearWrappedGraphData();
    }

    private void clearWrappedGraphData() {
        for (ACAQDataSlot slot : wrappedGraph.traverse()) {
            slot.clearData();
        }
    }

    @Override
    public void reportValidity(ACAQValidityReport report) {
        report.forCategory("Wrapped graph").report(wrappedGraph);
        if(!slotConfigurationIsComplete) {
            report.forCategory("Slots").reportIsInvalid("Could not create some output slots!",
                    "Some output slots are missing, as they have names that are already present in the set of input slots.",
                    "Check all outside-facing slots have unique names.",
                    this);
        }
    }

    @Override
    public Set<ACAQDependency> getDependencies() {
        Set<ACAQDependency> result = super.getDependencies();
        result.addAll(wrappedGraph.getDependencies());
        return result;
    }

    public ACAQAlgorithmGraph getWrappedGraph() {
        return wrappedGraph;
    }

    public void setWrappedGraph(ACAQAlgorithmGraph wrappedGraph) {
        if(this.wrappedGraph != wrappedGraph) {
            this.wrappedGraph = wrappedGraph;
            this.ioSlotWatcher = new IOSlotWatcher();
            updateSlots();
        }
    }

    /**
     * Keeps track of changes in the graph wrapper's input and output slots
     */
    private class IOSlotWatcher {
        public IOSlotWatcher() {
            getGroupInput().getSlotConfiguration().getEventBus().register(this);
            getGroupOutput().getSlotConfiguration().getEventBus().register(this);
        }

        /**
         * Should be triggered when a slot is added to the slot configuration
         *
         * @param event The event
         */
        @Subscribe
        public void onSlotAdded(SlotAddedEvent event) {
            updateSlots();
        }

        /**
         * Should be triggered when a slot is removed from the slot configuration
         *
         * @param event The event
         */
        @Subscribe
        public void onSlotRemoved(SlotRemovedEvent event) {
            updateSlots();
        }


        /**
         * Should be triggered when a slot is renamed in the slot configuration
         *
         * @param event The event
         */
        @Subscribe
        public void onSlotRenamed(SlotRenamedEvent event) {
            updateSlots();
        }
    }
}
