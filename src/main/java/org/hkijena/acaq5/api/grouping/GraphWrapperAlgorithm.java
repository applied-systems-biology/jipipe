package org.hkijena.acaq5.api.grouping;

import com.google.common.eventbus.Subscribe;
import org.hkijena.acaq5.ACAQDependency;
import org.hkijena.acaq5.api.ACAQGraphRunner;
import org.hkijena.acaq5.api.ACAQRunnerStatus;
import org.hkijena.acaq5.api.ACAQRunnerSubStatus;
import org.hkijena.acaq5.api.ACAQValidityReport;
import org.hkijena.acaq5.api.algorithm.ACAQAlgorithm;
import org.hkijena.acaq5.api.algorithm.ACAQAlgorithmDeclaration;
import org.hkijena.acaq5.api.algorithm.ACAQAlgorithmGraph;
import org.hkijena.acaq5.api.algorithm.ACAQGraphNode;
import org.hkijena.acaq5.api.data.ACAQDataSlot;
import org.hkijena.acaq5.api.data.ACAQDefaultMutableSlotConfiguration;
import org.hkijena.acaq5.api.data.ACAQMutableSlotConfiguration;
import org.hkijena.acaq5.api.data.ACAQSlotDefinition;
import org.hkijena.acaq5.api.events.SlotsChangedEvent;

import java.util.Collections;
import java.util.Map;
import java.util.Set;
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
    private boolean preventUpdateSlots = false;
    private boolean slotConfigurationIsComplete;

    /**
     * @param declaration  the declaration
     * @param wrappedGraph the graph wrapper
     */
    public GraphWrapperAlgorithm(ACAQAlgorithmDeclaration declaration, ACAQAlgorithmGraph wrappedGraph) {
        super(declaration, new ACAQDefaultMutableSlotConfiguration());
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
    public void updateGroupSlots() {
        if (preventUpdateSlots)
            return;
        ACAQDefaultMutableSlotConfiguration slotConfiguration = (ACAQDefaultMutableSlotConfiguration) getSlotConfiguration();
        ACAQMutableSlotConfiguration inputSlotConfiguration = (ACAQMutableSlotConfiguration) getGroupInput().getSlotConfiguration();
        ACAQMutableSlotConfiguration outputSlotConfiguration = (ACAQMutableSlotConfiguration) getGroupOutput().getSlotConfiguration();

        slotConfiguration.setInputSealed(true);
        slotConfiguration.setOutputSealed(true);
        slotConfiguration.setAllowInheritedOutputSlots(false);
        slotConfiguration.clearInputSlots(false);
        slotConfiguration.clearOutputSlots(false);
        slotConfigurationIsComplete = true;
        for (Map.Entry<String, ACAQSlotDefinition> entry : inputSlotConfiguration.getInputSlots().entrySet()) {
            slotConfiguration.addSlot(entry.getKey(), entry.getValue(), false);
        }
        for (Map.Entry<String, ACAQSlotDefinition> entry : outputSlotConfiguration.getOutputSlots().entrySet()) {
            if (!slotConfiguration.getInputSlots().containsKey(entry.getKey()))
                slotConfiguration.addSlot(entry.getKey(), entry.getValue(), false);
            else
                slotConfigurationIsComplete = false;
        }
    }

    public boolean isPreventUpdateSlots() {
        return preventUpdateSlots;
    }

    public void setPreventUpdateSlots(boolean preventUpdateSlots) {
        this.preventUpdateSlots = preventUpdateSlots;
    }

    /**
     * Gets the graphs's input node
     *
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
        }
        if (algorithmInput == null) {
            // Create if it doesn't exist
            algorithmInput = ACAQAlgorithm.newInstance("graph-wrapper:input");
            wrappedGraph.insertNode(algorithmInput, ACAQAlgorithmGraph.COMPARTMENT_DEFAULT);
        }
        return algorithmInput;
    }

    /**
     * Gets the graphs's output node
     *
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
        }
        if (algorithmOutput == null) {
            // Create if it doesn't exist
            algorithmOutput = ACAQAlgorithm.newInstance("graph-wrapper:output");
            wrappedGraph.insertNode(algorithmOutput, ACAQAlgorithmGraph.COMPARTMENT_DEFAULT);
        }
        return algorithmOutput;
    }

    @Override
    public void run(ACAQRunnerSubStatus subProgress, Consumer<ACAQRunnerSubStatus> algorithmProgress, Supplier<Boolean> isCancelled) {
        // Iterate through own input slots and pass them to the equivalents in group input
        for (ACAQDataSlot inputSlot : getInputSlots()) {
            ACAQDataSlot groupInputSlot = getGroupInput().getInputSlot(inputSlot.getName());
            groupInputSlot.copyFrom(inputSlot);
        }

        // Run the graph
        Consumer<ACAQRunnerStatus> subGraphStatus = runnerStatus -> algorithmProgress.accept(subProgress.resolve(String.format("Sub-graph %d/%d: %s",
                runnerStatus.getProgress(),
                runnerStatus.getMaxProgress(),
                runnerStatus.getMessage())));
        try {
            for (ACAQGraphNode value : wrappedGraph.getAlgorithmNodes().values()) {
                if (value instanceof ACAQAlgorithm) {
                    ((ACAQAlgorithm) value).setThreadPool(getThreadPool());
                }
            }
            ACAQGraphRunner runner = new ACAQGraphRunner(wrappedGraph);
            runner.setAlgorithmsWithExternalInput(Collections.singleton(getGroupInput()));
            runner.run(subGraphStatus, isCancelled);
        } finally {
            for (ACAQGraphNode value : wrappedGraph.getAlgorithmNodes().values()) {
                if (value instanceof ACAQAlgorithm) {
                    ((ACAQAlgorithm) value).setThreadPool(null);
                }
            }
        }

        // Copy into output
        for (ACAQDataSlot outputSlot : getOutputSlots()) {
            ACAQDataSlot groupOutputSlot = getGroupOutput().getOutputSlot(outputSlot.getName());
            outputSlot.copyFrom(groupOutputSlot);
        }

        // Clear all data in the wrapped graph
        clearWrappedGraphData();
    }

    private void clearWrappedGraphData() {
        for (ACAQDataSlot slot : wrappedGraph.traverse()) {
            slot.clearData();
        }
    }

    @Override
    public void reportValidity(ACAQValidityReport report) {
        super.reportValidity(report);
        report.forCategory("Wrapped graph").report(wrappedGraph);
        if (!slotConfigurationIsComplete) {
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
        if (this.wrappedGraph != wrappedGraph) {
            for (ACAQGraphNode value : wrappedGraph.getAlgorithmNodes().values()) {
                value.setCompartment(ACAQAlgorithmGraph.COMPARTMENT_DEFAULT);
            }
            this.wrappedGraph = wrappedGraph;
            this.algorithmInput = null;
            this.algorithmOutput = null;
            this.ioSlotWatcher = new IOSlotWatcher();
            updateGroupSlots();
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
         * Should be triggered the slot configuration was changed
         *
         * @param event The event
         */
        @Subscribe
        public void onIOSlotsChanged(SlotsChangedEvent event) {
            updateGroupSlots();
        }
    }
}
