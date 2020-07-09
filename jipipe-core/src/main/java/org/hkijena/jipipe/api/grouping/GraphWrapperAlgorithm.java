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

package org.hkijena.jipipe.api.grouping;

import com.google.common.eventbus.Subscribe;
import org.hkijena.jipipe.JIPipeDependency;
import org.hkijena.jipipe.api.JIPipeGraphRunner;
import org.hkijena.jipipe.api.JIPipeRunnerStatus;
import org.hkijena.jipipe.api.JIPipeRunnerSubStatus;
import org.hkijena.jipipe.api.JIPipeValidityReport;
import org.hkijena.jipipe.api.algorithm.JIPipeAlgorithm;
import org.hkijena.jipipe.api.algorithm.JIPipeAlgorithmDeclaration;
import org.hkijena.jipipe.api.algorithm.JIPipeGraph;
import org.hkijena.jipipe.api.algorithm.JIPipeGraphNode;
import org.hkijena.jipipe.api.data.JIPipeDataSlot;
import org.hkijena.jipipe.api.data.JIPipeDefaultMutableSlotConfiguration;
import org.hkijena.jipipe.api.data.JIPipeMutableSlotConfiguration;
import org.hkijena.jipipe.api.data.JIPipeSlotDefinition;
import org.hkijena.jipipe.api.events.SlotsChangedEvent;

import java.util.Collections;
import java.util.Map;
import java.util.Set;
import java.util.function.Consumer;
import java.util.function.Supplier;

/**
 * An algorithm that wraps another algorithm graph
 */
public class GraphWrapperAlgorithm extends JIPipeAlgorithm {

    private JIPipeGraph wrappedGraph;
    private GraphWrapperAlgorithmInput algorithmInput;
    private GraphWrapperAlgorithmOutput algorithmOutput;
    private IOSlotWatcher ioSlotWatcher;
    private boolean preventUpdateSlots = false;
    private boolean slotConfigurationIsComplete;

    /**
     * @param declaration  the declaration
     * @param wrappedGraph the graph wrapper
     */
    public GraphWrapperAlgorithm(JIPipeAlgorithmDeclaration declaration, JIPipeGraph wrappedGraph) {
        super(declaration, new JIPipeDefaultMutableSlotConfiguration());
        this.setWrappedGraph(wrappedGraph);
    }

    /**
     * Copies the algorithm
     *
     * @param other the original
     */
    public GraphWrapperAlgorithm(GraphWrapperAlgorithm other) {
        super(other);
        setWrappedGraph(new JIPipeGraph(other.wrappedGraph));
    }

    /**
     * Updates the slots from the wrapped graph
     */
    public void updateGroupSlots() {
        if (preventUpdateSlots)
            return;
        JIPipeDefaultMutableSlotConfiguration slotConfiguration = (JIPipeDefaultMutableSlotConfiguration) getSlotConfiguration();
        JIPipeMutableSlotConfiguration inputSlotConfiguration = (JIPipeMutableSlotConfiguration) getGroupInput().getSlotConfiguration();
        JIPipeMutableSlotConfiguration outputSlotConfiguration = (JIPipeMutableSlotConfiguration) getGroupOutput().getSlotConfiguration();

        slotConfiguration.setInputSealed(true);
        slotConfiguration.setOutputSealed(true);
        slotConfiguration.setAllowInheritedOutputSlots(false);
        slotConfiguration.clearInputSlots(false);
        slotConfiguration.clearOutputSlots(false);
        slotConfigurationIsComplete = true;
        for (Map.Entry<String, JIPipeSlotDefinition> entry : inputSlotConfiguration.getInputSlots().entrySet()) {
            slotConfiguration.addSlot(entry.getKey(), entry.getValue(), false);
        }
        for (Map.Entry<String, JIPipeSlotDefinition> entry : outputSlotConfiguration.getOutputSlots().entrySet()) {
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
            for (JIPipeGraphNode node : wrappedGraph.getAlgorithmNodes().values()) {
                if (node instanceof GraphWrapperAlgorithmInput) {
                    algorithmInput = (GraphWrapperAlgorithmInput) node;
                    break;
                }
            }
        }
        if (algorithmInput == null) {
            // Create if it doesn't exist
            algorithmInput = JIPipeAlgorithm.newInstance("graph-wrapper:input");
            wrappedGraph.insertNode(algorithmInput, JIPipeGraph.COMPARTMENT_DEFAULT);
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
            for (JIPipeGraphNode node : wrappedGraph.getAlgorithmNodes().values()) {
                if (node instanceof GraphWrapperAlgorithmOutput) {
                    algorithmOutput = (GraphWrapperAlgorithmOutput) node;
                    break;
                }
            }
        }
        if (algorithmOutput == null) {
            // Create if it doesn't exist
            algorithmOutput = JIPipeAlgorithm.newInstance("graph-wrapper:output");
            wrappedGraph.insertNode(algorithmOutput, JIPipeGraph.COMPARTMENT_DEFAULT);
        }
        return algorithmOutput;
    }

    @Override
    public void run(JIPipeRunnerSubStatus subProgress, Consumer<JIPipeRunnerSubStatus> algorithmProgress, Supplier<Boolean> isCancelled) {
        // Iterate through own input slots and pass them to the equivalents in group input
        for (JIPipeDataSlot inputSlot : getInputSlots()) {
            JIPipeDataSlot groupInputSlot = getGroupInput().getInputSlot(inputSlot.getName());
            groupInputSlot.copyFrom(inputSlot);
        }

        // Run the graph
        Consumer<JIPipeRunnerStatus> subGraphStatus = runnerStatus -> algorithmProgress.accept(subProgress.resolve(String.format("Sub-graph %d/%d: %s",
                runnerStatus.getProgress(),
                runnerStatus.getMaxProgress(),
                runnerStatus.getMessage())));
        try {
            for (JIPipeGraphNode value : wrappedGraph.getAlgorithmNodes().values()) {
                if (value instanceof JIPipeAlgorithm) {
                    ((JIPipeAlgorithm) value).setThreadPool(getThreadPool());
                }
            }
            JIPipeGraphRunner runner = new JIPipeGraphRunner(wrappedGraph);
            runner.setAlgorithmsWithExternalInput(Collections.singleton(getGroupInput()));
            runner.run(subGraphStatus, isCancelled);
        } finally {
            for (JIPipeGraphNode value : wrappedGraph.getAlgorithmNodes().values()) {
                if (value instanceof JIPipeAlgorithm) {
                    ((JIPipeAlgorithm) value).setThreadPool(null);
                }
            }
        }

        // Copy into output
        for (JIPipeDataSlot outputSlot : getOutputSlots()) {
            JIPipeDataSlot groupOutputSlot = getGroupOutput().getOutputSlot(outputSlot.getName());
            outputSlot.copyFrom(groupOutputSlot);
        }

        // Clear all data in the wrapped graph
        clearWrappedGraphData();
    }

    private void clearWrappedGraphData() {
        for (JIPipeDataSlot slot : wrappedGraph.traverseSlots()) {
            slot.clearData();
        }
    }

    @Override
    public void reportValidity(JIPipeValidityReport report) {
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
    public Set<JIPipeDependency> getDependencies() {
        Set<JIPipeDependency> result = super.getDependencies();
        result.addAll(wrappedGraph.getDependencies());
        return result;
    }

    public JIPipeGraph getWrappedGraph() {
        return wrappedGraph;
    }

    public void setWrappedGraph(JIPipeGraph wrappedGraph) {
        if (this.wrappedGraph != wrappedGraph) {
            for (JIPipeGraphNode value : wrappedGraph.getAlgorithmNodes().values()) {
                value.setCompartment(JIPipeGraph.COMPARTMENT_DEFAULT);
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
