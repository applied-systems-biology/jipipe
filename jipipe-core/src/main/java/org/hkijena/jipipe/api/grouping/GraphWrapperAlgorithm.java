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
import gnu.trove.set.TIntSet;
import gnu.trove.set.hash.TIntHashSet;
import org.hkijena.jipipe.JIPipe;
import org.hkijena.jipipe.JIPipeDependency;
import org.hkijena.jipipe.api.JIPipeDocumentation;
import org.hkijena.jipipe.api.JIPipeGraphRunner;
import org.hkijena.jipipe.api.JIPipeProgressInfo;
import org.hkijena.jipipe.api.JIPipeValidityReport;
import org.hkijena.jipipe.api.data.*;
import org.hkijena.jipipe.api.nodes.*;
import org.hkijena.jipipe.api.parameters.JIPipeParameter;
import org.hkijena.jipipe.api.parameters.JIPipeParameterCollection;
import org.hkijena.jipipe.api.parameters.JIPipeParameterVisibility;
import org.hkijena.jipipe.extensions.parameters.generators.IntegerRange;
import org.hkijena.jipipe.extensions.parameters.primitives.EnumItemInfo;
import org.hkijena.jipipe.extensions.parameters.primitives.EnumParameterSettings;
import org.hkijena.jipipe.utils.UIUtils;

import javax.swing.*;
import java.util.*;

/**
 * An algorithm that wraps another algorithm graph
 */
public class GraphWrapperAlgorithm extends JIPipeAlgorithm implements JIPipeDataBatchAlgorithm {

    private JIPipeGraph wrappedGraph;
    private GraphWrapperAlgorithmInput algorithmInput;
    private GraphWrapperAlgorithmOutput algorithmOutput;
    private IOSlotWatcher ioSlotWatcher;
    private boolean preventUpdateSlots = false;
    private boolean slotConfigurationIsComplete;
    private IterationMode iterationMode = IterationMode.PassThrough;
    private JIPipeMergingAlgorithm.DataBatchGenerationSettings batchGenerationSettings = new JIPipeMergingAlgorithm.DataBatchGenerationSettings();

    /**
     * @param info         the info
     * @param wrappedGraph the graph wrapper
     */
    public GraphWrapperAlgorithm(JIPipeNodeInfo info, JIPipeGraph wrappedGraph) {
        super(info, new JIPipeDefaultMutableSlotConfiguration());
        this.setWrappedGraph(wrappedGraph);
    }

    /**
     * Copies the algorithm
     *
     * @param other the original
     */
    public GraphWrapperAlgorithm(GraphWrapperAlgorithm other) {
        super(other);
        this.iterationMode = other.iterationMode;
        this.batchGenerationSettings = new JIPipeMergingAlgorithm.DataBatchGenerationSettings(other.batchGenerationSettings);
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
        for (Map.Entry<String, JIPipeDataSlotInfo> entry : inputSlotConfiguration.getInputSlots().entrySet()) {
            slotConfiguration.addSlot(entry.getKey(), entry.getValue(), false);
        }
        for (Map.Entry<String, JIPipeDataSlotInfo> entry : outputSlotConfiguration.getOutputSlots().entrySet()) {
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
            for (JIPipeGraphNode node : wrappedGraph.getNodes().values()) {
                if (node instanceof GraphWrapperAlgorithmInput) {
                    algorithmInput = (GraphWrapperAlgorithmInput) node;
                    break;
                }
            }
        }
        if (algorithmInput == null) {
            // Create if it doesn't exist
            algorithmInput = JIPipe.createNode("graph-wrapper:input", GraphWrapperAlgorithmInput.class);
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
            for (JIPipeGraphNode node : wrappedGraph.getNodes().values()) {
                if (node instanceof GraphWrapperAlgorithmOutput) {
                    algorithmOutput = (GraphWrapperAlgorithmOutput) node;
                    break;
                }
            }
        }
        if (algorithmOutput == null) {
            // Create if it doesn't exist
            algorithmOutput = JIPipe.createNode("graph-wrapper:output", GraphWrapperAlgorithmOutput.class);
            wrappedGraph.insertNode(algorithmOutput, JIPipeGraph.COMPARTMENT_DEFAULT);
        }
        return algorithmOutput;
    }

    @Override
    public void run(JIPipeProgressInfo progressInfo) {
        if(iterationMode == IterationMode.PassThrough) {
            runWithDataPassThrough(progressInfo);
        }
        else {
            runPerBatch(progressInfo);
        }
    }

    private void runPerBatch(JIPipeProgressInfo progressInfo) {
        if(getEffectiveInputSlots().isEmpty()) {
            runWithDataPassThrough(progressInfo);
            return;
        }
        List<JIPipeMergingDataBatch> dataBatches = generateDataBatchesDryRun(getEffectiveInputSlots());
        for (int i = 0; i < dataBatches.size(); i++) {
            JIPipeProgressInfo batchProgress = progressInfo.resolveAndLog("Data batch", i, dataBatches.size());
            JIPipeMergingDataBatch dataBatch = dataBatches.get(i);

            // Iterate through own input slots and pass them to the equivalents in group input
            for (JIPipeDataSlot inputSlot : getInputSlots()) {
                JIPipeDataSlot groupInputSlot = getGroupInput().getInputSlot(inputSlot.getName());
                for (Integer row : dataBatch.getInputRows(inputSlot)) {
                    groupInputSlot.addData(inputSlot.getVirtualData(row), inputSlot.getAnnotations(row), JIPipeAnnotationMergeStrategy.OverwriteExisting);
                }
            }

            // Run the graph
            try {
                for (JIPipeGraphNode value : wrappedGraph.getNodes().values()) {
                    if (value instanceof JIPipeAlgorithm) {
                        ((JIPipeAlgorithm) value).setThreadPool(getThreadPool());
                    }
                }
                JIPipeGraphRunner runner = new JIPipeGraphRunner(wrappedGraph);
                runner.setProgressInfo(batchProgress.resolve("Sub-graph"));
                runner.setAlgorithmsWithExternalInput(Collections.singleton(getGroupInput()));
                runner.run();
            } finally {
                for (JIPipeGraphNode value : wrappedGraph.getNodes().values()) {
                    if (value instanceof JIPipeAlgorithm) {
                        ((JIPipeAlgorithm) value).setThreadPool(null);
                    }
                }
            }

            // Copy into output
            for (JIPipeDataSlot outputSlot : getOutputSlots()) {
                JIPipeDataSlot groupOutputSlot = getGroupOutput().getOutputSlot(outputSlot.getName());
                outputSlot.addData(groupOutputSlot, progressInfo);
            }

            // Clear all data in the wrapped graph
            clearWrappedGraphData();
        }
    }

    private void runWithDataPassThrough(JIPipeProgressInfo progressInfo) {
        // Iterate through own input slots and pass them to the equivalents in group input
        for (JIPipeDataSlot inputSlot : getInputSlots()) {
            JIPipeDataSlot groupInputSlot = getGroupInput().getInputSlot(inputSlot.getName());
            groupInputSlot.addData(inputSlot, progressInfo);
        }

        // Run the graph
        try {
            for (JIPipeGraphNode value : wrappedGraph.getNodes().values()) {
                if (value instanceof JIPipeAlgorithm) {
                    ((JIPipeAlgorithm) value).setThreadPool(getThreadPool());
                }
            }
            JIPipeGraphRunner runner = new JIPipeGraphRunner(wrappedGraph);
            runner.setProgressInfo(progressInfo.resolve("Sub-graph"));
            runner.setAlgorithmsWithExternalInput(Collections.singleton(getGroupInput()));
            runner.run();
        } finally {
            for (JIPipeGraphNode value : wrappedGraph.getNodes().values()) {
                if (value instanceof JIPipeAlgorithm) {
                    ((JIPipeAlgorithm) value).setThreadPool(null);
                }
            }
        }

        // Copy into output
        for (JIPipeDataSlot outputSlot : getOutputSlots()) {
            JIPipeDataSlot groupOutputSlot = getGroupOutput().getOutputSlot(outputSlot.getName());
            outputSlot.addData(groupOutputSlot, progressInfo);
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
            for (JIPipeGraphNode value : wrappedGraph.getNodes().values()) {
                value.setCompartment(JIPipeGraph.COMPARTMENT_DEFAULT);
            }
            this.wrappedGraph = wrappedGraph;
            this.algorithmInput = null;
            this.algorithmOutput = null;
            this.ioSlotWatcher = new IOSlotWatcher();
            updateGroupSlots();
        }
    }

    @Override
    public JIPipeParameterCollection getGenerationSettingsInterface() {
        return batchGenerationSettings;
    }

    @Override
    public List<JIPipeMergingDataBatch> generateDataBatchesDryRun(List<JIPipeDataSlot> slots) {
        if(iterationMode == IterationMode.PassThrough) {
            JIPipeMergingDataBatch dataBatch = new JIPipeMergingDataBatch(this);
            for (JIPipeDataSlot inputSlot : getEffectiveInputSlots()) {
                for (int row = 0; row < inputSlot.getRowCount(); row++) {
                    dataBatch.addData(inputSlot, row);
                }
            }
            return Collections.singletonList(dataBatch);
        }
        else {
            JIPipeMergingDataBatchBuilder builder = new JIPipeMergingDataBatchBuilder();
            builder.setNode(this);
            builder.setApplyMerging(iterationMode == IterationMode.MergingDataBatch);
            builder.setSlots(slots);
            builder.setAnnotationMergeStrategy(batchGenerationSettings.getAnnotationMergeStrategy());
            builder.setReferenceColumns(batchGenerationSettings.getDataSetMatching(),
                    batchGenerationSettings.getCustomColumns());
            List<JIPipeMergingDataBatch> dataBatches = builder.build();
            dataBatches.sort(Comparator.naturalOrder());
            boolean withLimit = batchGenerationSettings.getLimit().isEnabled();
            IntegerRange limit = batchGenerationSettings.getLimit().getContent();
            TIntSet allowedIndices = withLimit ? new TIntHashSet(limit.getIntegers()) : null;
            if (withLimit) {
                List<JIPipeMergingDataBatch> limitedBatches = new ArrayList<>();
                for (int i = 0; i < dataBatches.size(); i++) {
                    if (allowedIndices.contains(i)) {
                        limitedBatches.add(dataBatches.get(i));
                    }
                }
                dataBatches = limitedBatches;
            }
            return dataBatches;
        }
    }

    public JIPipeMergingAlgorithm.DataBatchGenerationSettings getBatchGenerationSettings() {
        return batchGenerationSettings;
    }

    public IterationMode getIterationMode() {
        return iterationMode;
    }

    public void setIterationMode(IterationMode iterationMode) {
        this.iterationMode = iterationMode;
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
        public void onIOSlotsChanged(JIPipeSlotConfiguration.SlotsChangedEvent event) {
            updateGroupSlots();
        }
    }

    /**
     * Determines how the data is iterated
     */
    @EnumParameterSettings(itemInfo = IterationModeEnumInfo.class)
    public enum IterationMode {
        PassThrough,
        IteratingDataBatch,
        MergingDataBatch;

        @Override
        public String toString() {
            switch (this) {
                case PassThrough:
                    return "Pass data through";
                case MergingDataBatch:
                    return "Per merging data batch";
                case IteratingDataBatch:
                    return "Per iterating data batch";
                default:
                    throw new UnsupportedOperationException();
            }
        }
    }

    public static class IterationModeEnumInfo implements EnumItemInfo {

        @Override
        public Icon getIcon(Object value) {
            switch ((IterationMode)value) {
                case MergingDataBatch:
                    return UIUtils.getIconFromResources("actions/rabbitvcs-merge.png");
                case IteratingDataBatch:
                    return UIUtils.getIconFromResources("actions/media-playlist-normal.png");
                case PassThrough:
                    return UIUtils.getIconFromResources("actions/draw-arrow-forward.png");
                default:
                    throw new UnsupportedOperationException();
            }
        }

        @Override
        public String getLabel(Object value) {
            return value.toString();
        }

        @Override
        public String getTooltip(Object value) {
            switch ((IterationMode)value) {
                case PassThrough:
                    return "Passes data from the inputs through the I/O nodes of the wrapped graph. " +
                            "The wrapped graph is then executed once.";
                case IteratingDataBatch:
                    return "Iterates through all data batches of the group node. " +
                            "The wrapped graph is executed for each data batch. " +
                            "This uses an iterating data batch (only one data row per slot per batch)";
                case MergingDataBatch:
                    return "Iterates through all data batches of the group node. " +
                            "The wrapped graph is executed for each data batch. " +
                            "This uses an merging data batch (multiple data rows per slot per batch)";
                default:
                    throw new UnsupportedOperationException();
            }
        }
    }
}
