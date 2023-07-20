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

import com.google.common.primitives.Ints;
import gnu.trove.set.TIntSet;
import gnu.trove.set.hash.TIntHashSet;
import org.hkijena.jipipe.JIPipe;
import org.hkijena.jipipe.JIPipeDependency;
import org.hkijena.jipipe.api.JIPipeGraphRunner;
import org.hkijena.jipipe.api.JIPipeProject;
import org.hkijena.jipipe.api.validation.JIPipeValidationReport;
import org.hkijena.jipipe.api.JIPipeProgressInfo;
import org.hkijena.jipipe.api.annotation.JIPipeDataAnnotationMergeMode;
import org.hkijena.jipipe.api.annotation.JIPipeTextAnnotationMergeMode;
import org.hkijena.jipipe.api.data.*;
import org.hkijena.jipipe.api.nodes.*;
import org.hkijena.jipipe.api.validation.JIPipeValidationReportContext;
import org.hkijena.jipipe.api.validation.contexts.ParameterValidationReportContext;
import org.hkijena.jipipe.extensions.expressions.ExpressionVariables;
import org.hkijena.jipipe.extensions.parameters.api.enums.EnumItemInfo;
import org.hkijena.jipipe.extensions.parameters.api.enums.EnumParameterSettings;
import org.hkijena.jipipe.extensions.parameters.library.primitives.ranges.IntegerRange;
import org.hkijena.jipipe.utils.UIUtils;

import javax.swing.*;
import java.nio.file.Path;
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
    private IterationMode iterationMode = IterationMode.PassThrough;
    private JIPipeMergingAlgorithmDataBatchGenerationSettings batchGenerationSettings = new JIPipeMergingAlgorithmDataBatchGenerationSettings();

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
        this.batchGenerationSettings = new JIPipeMergingAlgorithmDataBatchGenerationSettings(other.batchGenerationSettings);
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
        slotConfiguration.clearInputSlots(false);
        slotConfiguration.clearOutputSlots(false);
        for (Map.Entry<String, JIPipeDataSlotInfo> entry : inputSlotConfiguration.getInputSlots().entrySet()) {
            slotConfiguration.addSlot(entry.getKey(), entry.getValue(), false);
        }
        for (Map.Entry<String, JIPipeDataSlotInfo> entry : outputSlotConfiguration.getOutputSlots().entrySet()) {
            slotConfiguration.addSlot(entry.getKey(), entry.getValue(), false);
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
            for (JIPipeGraphNode node : wrappedGraph.getGraphNodes()) {
                if (node instanceof GraphWrapperAlgorithmInput) {
                    algorithmInput = (GraphWrapperAlgorithmInput) node;
                    break;
                }
            }
        }
        if (algorithmInput == null) {
            // Create if it doesn't exist
            algorithmInput = JIPipe.createNode("graph-wrapper:input");
            wrappedGraph.insertNode(algorithmInput);
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
            for (JIPipeGraphNode node : wrappedGraph.getGraphNodes()) {
                if (node instanceof GraphWrapperAlgorithmOutput) {
                    algorithmOutput = (GraphWrapperAlgorithmOutput) node;
                    break;
                }
            }
        }
        if (algorithmOutput == null) {
            // Create if it doesn't exist
            algorithmOutput = JIPipe.createNode("graph-wrapper:output");
            wrappedGraph.insertNode(algorithmOutput);
        }
        return algorithmOutput;
    }

    @Override
    public void run(JIPipeProgressInfo progressInfo) {
        if (iterationMode == IterationMode.PassThrough) {
            runWithDataPassThrough(progressInfo);
        } else {
            runPerBatch(progressInfo);
        }
    }

    private void runPerBatch(JIPipeProgressInfo progressInfo) {
        if (getDataInputSlots().isEmpty()) {
            runWithDataPassThrough(progressInfo);
            return;
        }
        List<JIPipeMergingDataBatch> dataBatches = generateDataBatchesDryRun(getDataInputSlots(), progressInfo);
        for (int i = 0; i < dataBatches.size(); i++) {
            JIPipeProgressInfo batchProgress = progressInfo.resolveAndLog("Data batch", i, dataBatches.size());
            JIPipeMergingDataBatch dataBatch = dataBatches.get(i);

            // Iterate through own input slots and pass them to the equivalents in group input
            for (JIPipeDataSlot inputSlot : getInputSlots()) {
                JIPipeDataSlot groupInputSlot = getGroupInput().getInputSlot(inputSlot.getName());
                for (Integer row : dataBatch.getInputRows(inputSlot)) {
                    groupInputSlot.addData(inputSlot.getDataItemStore(row),
                            inputSlot.getTextAnnotations(row),
                            JIPipeTextAnnotationMergeMode.OverwriteExisting,
                            inputSlot.getDataAnnotations(row),
                            JIPipeDataAnnotationMergeMode.OverwriteExisting);
                }
            }

            // Run the graph
            try {
                for (JIPipeGraphNode value : wrappedGraph.getGraphNodes()) {
                    if (value instanceof JIPipeAlgorithm) {
                        ((JIPipeAlgorithm) value).setThreadPool(getThreadPool());
                    }
                }
                JIPipeGraphRunner runner = new JIPipeGraphRunner(wrappedGraph);
                runner.setThreadPool(getThreadPool());
                runner.setProgressInfo(batchProgress.detachProgress().resolve("Sub-graph"));
                runner.setAlgorithmsWithExternalInput(Collections.singleton(getGroupInput()));
                runner.getPersistentDataNodes().add(getGroupOutput());
                runner.run();
            } finally {
                for (JIPipeGraphNode value : wrappedGraph.getGraphNodes()) {
                    if (value instanceof JIPipeAlgorithm) {
                        ((JIPipeAlgorithm) value).setThreadPool(null);
                    }
                }
            }

            // Copy into output
            for (JIPipeDataSlot outputSlot : getOutputSlots()) {
                JIPipeDataSlot groupOutputSlot = getGroupOutput().getOutputSlot(outputSlot.getName());
                outputSlot.addDataFromSlot(groupOutputSlot, progressInfo);
            }

            // Clear all data in the wrapped graph
            clearWrappedGraphData();
        }
    }

    private void runWithDataPassThrough(JIPipeProgressInfo progressInfo) {
        // Iterate through own input slots and pass them to the equivalents in group input
        for (JIPipeDataSlot inputSlot : getInputSlots()) {
            JIPipeDataSlot groupInputSlot = getGroupInput().getInputSlot(inputSlot.getName());
            groupInputSlot.addDataFromSlot(inputSlot, progressInfo);
        }

        // Run the graph
        try {
            for (JIPipeGraphNode value : wrappedGraph.getGraphNodes()) {
                if (value instanceof JIPipeAlgorithm) {
                    ((JIPipeAlgorithm) value).setThreadPool(getThreadPool());
                }
            }
            JIPipeGraphRunner runner = new JIPipeGraphRunner(wrappedGraph);
            runner.setThreadPool(getThreadPool());
            runner.setProgressInfo(progressInfo.detachProgress().resolve("Sub-graph"));
            runner.setAlgorithmsWithExternalInput(Collections.singleton(getGroupInput()));
            runner.getPersistentDataNodes().add(getGroupOutput());
            runner.run();
        } finally {
            for (JIPipeGraphNode value : wrappedGraph.getGraphNodes()) {
                if (value instanceof JIPipeAlgorithm) {
                    ((JIPipeAlgorithm) value).setThreadPool(null);
                }
            }
        }

        // Copy into output
        for (JIPipeDataSlot outputSlot : getOutputSlots()) {
            JIPipeDataSlot groupOutputSlot = getGroupOutput().getOutputSlot(outputSlot.getName());
            outputSlot.addDataFromSlot(groupOutputSlot, progressInfo);
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
    public void reportValidity(JIPipeValidationReportContext context, JIPipeValidationReport report) {
        super.reportValidity(context, report);

        report.report(new ParameterValidationReportContext(context, this, "Wrapped graph", "wrapped-graph"), wrappedGraph);
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
            for (JIPipeGraphNode value : wrappedGraph.getGraphNodes()) {
                wrappedGraph.setCompartment(value.getUUIDInParentGraph(), null);
            }
            this.wrappedGraph = wrappedGraph;
            this.algorithmInput = null;
            this.algorithmOutput = null;
            this.ioSlotWatcher = new IOSlotWatcher();
            updateGroupSlots();
        }
    }

    @Override
    public JIPipeDataBatchGenerationSettings getGenerationSettingsInterface() {
        return batchGenerationSettings;
    }

    @Override
    public List<JIPipeMergingDataBatch> generateDataBatchesDryRun(List<JIPipeInputDataSlot> slots, JIPipeProgressInfo progressInfo) {
        if (iterationMode == IterationMode.PassThrough) {
            JIPipeMergingDataBatch dataBatch = new JIPipeMergingDataBatch(this);
            for (JIPipeDataSlot inputSlot : getDataInputSlots()) {
                for (int row = 0; row < inputSlot.getRowCount(); row++) {
                    dataBatch.addInputData(inputSlot, row);
                }
            }
            return Collections.singletonList(dataBatch);
        } else {
            JIPipeMergingDataBatchBuilder builder = new JIPipeMergingDataBatchBuilder();
            builder.setNode(this);
            builder.setApplyMerging(iterationMode == IterationMode.MergingDataBatch);
            builder.setSlots(slots);
            builder.setAnnotationMergeStrategy(batchGenerationSettings.getAnnotationMergeStrategy());
            builder.setReferenceColumns(batchGenerationSettings.getColumnMatching(),
                    batchGenerationSettings.getCustomColumns());
            builder.setCustomAnnotationMatching(batchGenerationSettings.getCustomAnnotationMatching());
            builder.setAnnotationMatchingMethod(batchGenerationSettings.getAnnotationMatchingMethod());
            List<JIPipeMergingDataBatch> dataBatches = builder.build(progressInfo);
            dataBatches.sort(Comparator.naturalOrder());
            boolean withLimit = batchGenerationSettings.getLimit().isEnabled();
            IntegerRange limit = batchGenerationSettings.getLimit().getContent();
            TIntSet allowedIndices = withLimit ? new TIntHashSet(limit.getIntegers(0, dataBatches.size(), new ExpressionVariables())) : null;
            if (withLimit) {
                progressInfo.log("[INFO] Applying limit to all data batches. Allowed indices are " + Ints.join(", ", allowedIndices.toArray()));
                List<JIPipeMergingDataBatch> limitedBatches = new ArrayList<>();
                for (int i = 0; i < dataBatches.size(); i++) {
                    if (allowedIndices.contains(i)) {
                        limitedBatches.add(dataBatches.get(i));
                    }
                }
                dataBatches = limitedBatches;
            }
            List<JIPipeMergingDataBatch> incomplete = new ArrayList<>();
            for (JIPipeMergingDataBatch dataBatch : dataBatches) {
                if(dataBatch.isIncomplete()) {
                    incomplete.add(dataBatch);
                    progressInfo.log("[WARN] INCOMPLETE DATA BATCH FOUND: " + dataBatch);
                }
            }
            if (!incomplete.isEmpty() && batchGenerationSettings.isSkipIncompleteDataSets()) {
                progressInfo.log("[WARN] SKIPPING INCOMPLETE DATA BATCHES AS REQUESTED");
                dataBatches.removeAll(incomplete);
            }
            return dataBatches;
        }
    }

    @Override
    public void setScratchBaseDirectory(Path scratchBaseDirectory) {
        super.setScratchBaseDirectory(scratchBaseDirectory);
        if (wrappedGraph != null) {
            for (JIPipeGraphNode node : wrappedGraph.getGraphNodes()) {
                node.setScratchBaseDirectory(scratchBaseDirectory);
            }
        }
    }

    @Override
    public void setBaseDirectory(Path baseDirectory) {
        super.setBaseDirectory(baseDirectory);
        if (wrappedGraph != null) {
            for (JIPipeGraphNode node : wrappedGraph.getGraphNodes()) {
                node.setBaseDirectory(baseDirectory);
            }
        }
    }

    @Override
    public void setProjectDirectory(Path projectDirectory) {
        super.setProjectDirectory(projectDirectory);
        if (wrappedGraph != null) {
            for (JIPipeGraphNode node : wrappedGraph.getGraphNodes()) {
                node.setProjectDirectory(projectDirectory);
            }
        }
    }

    @Override
    public void setRuntimeProject(JIPipeProject runtimeProject) {
        super.setRuntimeProject(runtimeProject);
        if (wrappedGraph != null) {
            for (JIPipeGraphNode node : wrappedGraph.getGraphNodes()) {
                node.setRuntimeProject(runtimeProject);
            }
        }
    }

    @Override
    public void setInternalStoragePath(Path internalStoragePath) {
        super.setInternalStoragePath(internalStoragePath);

        // Also update the storage paths of the internal nodes
        Path scratch = getNewScratch();
        for (JIPipeGraphNode node : getWrappedGraph().getGraphNodes()) {
            node.setInternalStoragePath(scratch.resolve(node.getAliasIdInParentGraph()));
        }
        for (JIPipeDataSlot slot : getWrappedGraph().getSlotNodes()) {
            if (slot.isOutput()) {
                slot.setSlotStoragePath(slot.getNode().getInternalStoragePath().resolve(slot.getName()));
            }
        }
    }

    public JIPipeMergingAlgorithmDataBatchGenerationSettings getBatchGenerationSettings() {
        return batchGenerationSettings;
    }

    public IterationMode getIterationMode() {
        return iterationMode;
    }

    public void setIterationMode(IterationMode iterationMode) {
        this.iterationMode = iterationMode;
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
            switch ((IterationMode) value) {
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
            switch ((IterationMode) value) {
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

    /**
     * Keeps track of changes in the graph wrapper's input and output slots
     */
    private class IOSlotWatcher implements JIPipeSlotConfiguration.SlotConfigurationChangedEventListener {
        public IOSlotWatcher() {
            getGroupInput().getSlotConfiguration().getSlotConfigurationChangedEventEmitter().subscribe(this);
            getGroupOutput().getSlotConfiguration().getSlotConfigurationChangedEventEmitter().subscribe(this);
        }

        @Override
        public void onSlotConfigurationChanged(JIPipeSlotConfiguration.SlotConfigurationChangedEvent event) {
            updateGroupSlots();
        }
    }
}
