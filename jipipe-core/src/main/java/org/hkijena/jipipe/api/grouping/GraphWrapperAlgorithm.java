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

import com.google.common.collect.HashMultimap;
import com.google.common.collect.Multimap;
import com.google.common.primitives.Ints;
import gnu.trove.set.TIntSet;
import gnu.trove.set.hash.TIntHashSet;
import org.hkijena.jipipe.JIPipe;
import org.hkijena.jipipe.JIPipeDependency;
import org.hkijena.jipipe.api.JIPipeDataBatchGenerationResult;
import org.hkijena.jipipe.api.run.JIPipeLegacyGraphRunner;
import org.hkijena.jipipe.api.JIPipeProgressInfo;
import org.hkijena.jipipe.api.JIPipeProject;
import org.hkijena.jipipe.api.annotation.JIPipeDataAnnotationMergeMode;
import org.hkijena.jipipe.api.annotation.JIPipeTextAnnotationMergeMode;
import org.hkijena.jipipe.api.data.*;
import org.hkijena.jipipe.api.nodes.*;
import org.hkijena.jipipe.api.nodes.iterationstep.JIPipeIterationStepAlgorithm;
import org.hkijena.jipipe.api.nodes.iterationstep.JIPipeIterationStepGenerationSettings;
import org.hkijena.jipipe.api.nodes.iterationstep.JIPipeMultiIterationStep;
import org.hkijena.jipipe.api.nodes.iterationstep.JIPipeMultiIterationStepGenerator;
import org.hkijena.jipipe.api.nodes.algorithm.JIPipeMergingAlgorithmIterationStepGenerationSettings;
import org.hkijena.jipipe.api.validation.JIPipeValidationReport;
import org.hkijena.jipipe.api.validation.JIPipeValidationReportContext;
import org.hkijena.jipipe.api.validation.contexts.ParameterValidationReportContext;
import org.hkijena.jipipe.extensions.expressions.JIPipeExpressionVariablesMap;
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
public class GraphWrapperAlgorithm extends JIPipeAlgorithm implements JIPipeIterationStepAlgorithm {

    private JIPipeGraph wrappedGraph;
    private GraphWrapperAlgorithmInput algorithmInput;
    private GraphWrapperAlgorithmOutput algorithmOutput;
    private IOSlotWatcher ioSlotWatcher;
    private boolean preventUpdateSlots = false;
    private IterationMode iterationMode = IterationMode.PassThrough;
    private JIPipeMergingAlgorithmIterationStepGenerationSettings batchGenerationSettings = new JIPipeMergingAlgorithmIterationStepGenerationSettings();

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
        this.batchGenerationSettings = new JIPipeMergingAlgorithmIterationStepGenerationSettings(other.batchGenerationSettings);
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

        Multimap<String, JIPipeDataSlot> inputSourcesBackup = HashMultimap.create();
        Multimap<String, JIPipeDataSlot> outputTargetsBackup = HashMultimap.create();

        // Backup connections
        if(getParentGraph() != null) {
            for (JIPipeInputDataSlot inputSlot : getInputSlots()) {
                for (JIPipeDataSlot sourceSlot : getParentGraph().getInputIncomingSourceSlots(inputSlot)) {
                    inputSourcesBackup.put(inputSlot.getName(), sourceSlot);
                }
            }
            for (JIPipeOutputDataSlot outputSlot : getOutputSlots()) {
                for (JIPipeDataSlot targetSlot : getParentGraph().getOutputOutgoingTargetSlots(outputSlot)) {
                    outputTargetsBackup.put(outputSlot.getName(), targetSlot);
                }
            }
        }

        // Delete and re-add slots
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

        // Reconnect
        for (Map.Entry<String, JIPipeDataSlot> entry : inputSourcesBackup.entries()) {
            JIPipeInputDataSlot inputSlot = getInputSlot(entry.getKey());
            JIPipeDataSlot sourceSlot = entry.getValue();
            if(inputSlot != null && inputSlot.accepts(sourceSlot.getAcceptedDataType())) {
                try {
                    getParentGraph().connect(sourceSlot, inputSlot);
                }
                catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }
        for (Map.Entry<String, JIPipeDataSlot> entry : outputTargetsBackup.entries()) {
            JIPipeOutputDataSlot outputSlot = getOutputSlot(entry.getKey());
            JIPipeDataSlot targetSlot = entry.getValue();
            if(outputSlot != null && targetSlot.accepts(outputSlot.getAcceptedDataType())) {
                try {
                    getParentGraph().connect(outputSlot, targetSlot);
                }
                catch (Exception e) {
                    e.printStackTrace();
                }
            }
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
    public void run(JIPipeGraphNodeRunContext runContext, JIPipeProgressInfo progressInfo) {
        if (iterationMode == IterationMode.PassThrough) {
            runWithDataPassThrough(runContext, progressInfo);
        } else {
            runPerBatch(runContext, progressInfo);
        }
    }

    private void runPerBatch(JIPipeGraphNodeRunContext runContext, JIPipeProgressInfo progressInfo) {
        if (getDataInputSlots().isEmpty()) {
            runWithDataPassThrough(runContext, progressInfo);
            return;
        }
        List<JIPipeMultiIterationStep> iterationSteps = generateDataBatchesGenerationResult(getDataInputSlots(), progressInfo).getDataBatches();
        for (int i = 0; i < iterationSteps.size(); i++) {
            JIPipeProgressInfo batchProgress = progressInfo.resolveAndLog("Data batch", i, iterationSteps.size());
            JIPipeMultiIterationStep iterationStep = iterationSteps.get(i);

            // Iterate through own input slots and pass them to the equivalents in group input
            for (JIPipeDataSlot inputSlot : getInputSlots()) {
                JIPipeDataSlot groupInputSlot = getGroupInput().getInputSlot(inputSlot.getName());
                for (Integer row : iterationStep.getInputRows(inputSlot)) {
                    groupInputSlot.addData(inputSlot.getDataItemStore(row),
                            inputSlot.getTextAnnotations(row),
                            JIPipeTextAnnotationMergeMode.OverwriteExisting,
                            inputSlot.getDataAnnotations(row),
                            JIPipeDataAnnotationMergeMode.OverwriteExisting,
                            inputSlot.getDataContext(row),
                            batchProgress);
                }
            }

            // Run the graph
            JIPipeLegacyGraphRunner runner = new JIPipeLegacyGraphRunner(wrappedGraph);
            runner.setRunContext(runContext);
            runner.setProgressInfo(batchProgress.detachProgress().resolve("Group"));
            runner.setAlgorithmsWithExternalInput(Collections.singleton(getGroupInput()));
            runner.getPersistentDataNodes().add(getGroupOutput());
            runner.run();

            // Copy into output
            for (JIPipeDataSlot outputSlot : getOutputSlots()) {
                JIPipeDataSlot groupOutputSlot = getGroupOutput().getOutputSlot(outputSlot.getName());
                outputSlot.addDataFromSlot(groupOutputSlot, progressInfo);
            }

            // Clear all data in the wrapped graph
            clearWrappedGraphData();
        }
    }

    private void runWithDataPassThrough(JIPipeGraphNodeRunContext runContext, JIPipeProgressInfo progressInfo) {
        // Iterate through own input slots and pass them to the equivalents in group input
        for (JIPipeDataSlot inputSlot : getInputSlots()) {
            JIPipeDataSlot groupInputSlot = getGroupInput().getInputSlot(inputSlot.getName());
            groupInputSlot.addDataFromSlot(inputSlot, progressInfo);
        }

        // Run the graph
        JIPipeLegacyGraphRunner runner = new JIPipeLegacyGraphRunner(wrappedGraph);
        runner.setProgressInfo(progressInfo.detachProgress().resolve("Sub-graph"));
        runner.setAlgorithmsWithExternalInput(Collections.singleton(getGroupInput()));
        runner.getPersistentDataNodes().add(getGroupOutput());
        runner.setRunContext(runContext);
        runner.run();

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
    public void reportValidity(JIPipeValidationReportContext reportContext, JIPipeValidationReport report) {
        super.reportValidity(reportContext, report);

        report.report(new ParameterValidationReportContext(reportContext, this, "Wrapped graph", "wrapped-graph"), wrappedGraph);
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
    public JIPipeIterationStepGenerationSettings getGenerationSettingsInterface() {
        return batchGenerationSettings;
    }

    @Override
    public JIPipeDataBatchGenerationResult generateDataBatchesGenerationResult(List<JIPipeInputDataSlot> slots, JIPipeProgressInfo progressInfo) {
        if (iterationMode == IterationMode.PassThrough) {
            JIPipeMultiIterationStep iterationStep = new JIPipeMultiIterationStep(this);
            for (JIPipeDataSlot inputSlot : getDataInputSlots()) {
                for (int row = 0; row < inputSlot.getRowCount(); row++) {
                    iterationStep.addInputData(inputSlot, row);
                }
            }

            // Generate result object
            JIPipeDataBatchGenerationResult result = new JIPipeDataBatchGenerationResult();
            result.setDataBatches(iterationStep);

            return result;

        } else {
            JIPipeMultiIterationStepGenerator builder = new JIPipeMultiIterationStepGenerator();
            builder.setNode(this);
            builder.setApplyMerging(iterationMode == IterationMode.MergingDataBatch);
            builder.setSlots(slots);
            builder.setAnnotationMergeStrategy(batchGenerationSettings.getAnnotationMergeStrategy());
            builder.setReferenceColumns(batchGenerationSettings.getColumnMatching(),
                    batchGenerationSettings.getCustomColumns());
            builder.setCustomAnnotationMatching(batchGenerationSettings.getCustomAnnotationMatching());
            builder.setAnnotationMatchingMethod(batchGenerationSettings.getAnnotationMatchingMethod());
            List<JIPipeMultiIterationStep> iterationSteps = builder.build(progressInfo);
            iterationSteps.sort(Comparator.naturalOrder());
            boolean withLimit = batchGenerationSettings.getLimit().isEnabled();
            IntegerRange limit = batchGenerationSettings.getLimit().getContent();
            TIntSet allowedIndices = withLimit ? new TIntHashSet(limit.getIntegers(0, iterationSteps.size(), new JIPipeExpressionVariablesMap())) : null;
            if (withLimit) {
                progressInfo.log("[INFO] Applying limit to all data batches. Allowed indices are " + Ints.join(", ", allowedIndices.toArray()));
                List<JIPipeMultiIterationStep> limitedBatches = new ArrayList<>();
                for (int i = 0; i < iterationSteps.size(); i++) {
                    if (allowedIndices.contains(i)) {
                        limitedBatches.add(iterationSteps.get(i));
                    }
                }
                iterationSteps = limitedBatches;
            }
            List<JIPipeMultiIterationStep> incomplete = new ArrayList<>();
            for (JIPipeMultiIterationStep iterationStep : iterationSteps) {
                if (iterationStep.isIncomplete()) {
                    incomplete.add(iterationStep);
                    progressInfo.log("[WARN] INCOMPLETE DATA BATCH FOUND: " + iterationStep);
                }
            }
            if (!incomplete.isEmpty() && batchGenerationSettings.isSkipIncompleteDataSets()) {
                progressInfo.log("[WARN] SKIPPING INCOMPLETE DATA BATCHES AS REQUESTED");
                iterationSteps.removeAll(incomplete);
            }

            // Generate result object
            JIPipeDataBatchGenerationResult result = new JIPipeDataBatchGenerationResult();
            result.setDataBatches(iterationSteps);
            result.setReferenceTextAnnotationColumns(builder.getReferenceColumns());

            return result;
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

    public JIPipeMergingAlgorithmIterationStepGenerationSettings getBatchGenerationSettings() {
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
                    return "Loop (multiple data per slot)";
                case IteratingDataBatch:
                    return "Loop (single data per slot)";
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
                    return "Iterates through all iteration steps of the group node. " +
                            "The wrapped graph is executed for each data batch. " +
                            "This uses an iterating data batch (only one data row per slot per batch)";
                case MergingDataBatch:
                    return "Iterates through all iteration steps of the group node. " +
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
