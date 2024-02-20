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

package org.hkijena.jipipe.api.nodes.algorithm;

import gnu.trove.set.TIntSet;
import gnu.trove.set.hash.TIntHashSet;
import org.hkijena.jipipe.api.JIPipeDataBatchGenerationResult;
import org.hkijena.jipipe.api.SetJIPipeDocumentation;
import org.hkijena.jipipe.api.JIPipeProgressInfo;
import org.hkijena.jipipe.api.annotation.JIPipeDataAnnotationMergeMode;
import org.hkijena.jipipe.api.annotation.JIPipeTextAnnotationMergeMode;
import org.hkijena.jipipe.api.data.JIPipeDataSlot;
import org.hkijena.jipipe.api.data.JIPipeInputDataSlot;
import org.hkijena.jipipe.api.data.JIPipeSlotConfiguration;
import org.hkijena.jipipe.api.nodes.*;
import org.hkijena.jipipe.api.nodes.iterationstep.*;
import org.hkijena.jipipe.api.parameters.*;
import org.hkijena.jipipe.api.validation.*;
import org.hkijena.jipipe.api.validation.contexts.GraphNodeValidationReportContext;
import org.hkijena.jipipe.extensions.expressions.JIPipeExpressionVariablesMap;
import org.hkijena.jipipe.extensions.parameters.library.primitives.optional.OptionalIntegerRange;
import org.hkijena.jipipe.extensions.parameters.library.primitives.ranges.IntegerRange;
import org.hkijena.jipipe.utils.ParameterUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;

/**
 * An {@link JIPipeAlgorithm} that iterates through each data row.
 * This is a simplified version of {@link JIPipeIteratingAlgorithm} that assumes that there is only one or zero input slots.
 * An error is thrown if there are more than one input slots.
 */
public abstract class JIPipeParameterlessSimpleIteratingAlgorithm extends JIPipeAlgorithm implements JIPipeParallelizedAlgorithm, JIPipeIterationStepAlgorithm {
    private IterationStepGenerationSettings iterationStepGenerationSettings = new IterationStepGenerationSettings();

    /**
     * Creates a new instance
     *
     * @param info              Algorithm info
     * @param slotConfiguration Slot configuration override
     */
    public JIPipeParameterlessSimpleIteratingAlgorithm(JIPipeNodeInfo info, JIPipeSlotConfiguration slotConfiguration) {
        super(info, slotConfiguration);
        registerSubParameter(iterationStepGenerationSettings);
    }

    /**
     * Creates a new instance
     *
     * @param info Algorithm info
     */
    public JIPipeParameterlessSimpleIteratingAlgorithm(JIPipeNodeInfo info) {
        super(info, null);
        registerSubParameter(iterationStepGenerationSettings);
    }

    /**
     * Copies the algorithm
     *
     * @param other The original
     */
    public JIPipeParameterlessSimpleIteratingAlgorithm(JIPipeParameterlessSimpleIteratingAlgorithm other) {
        super(other);
        this.iterationStepGenerationSettings = new IterationStepGenerationSettings(other.iterationStepGenerationSettings);
        registerSubParameter(iterationStepGenerationSettings);
    }

    @Override
    public void run(JIPipeGraphNodeRunContext runContext, JIPipeProgressInfo progressInfo) {
        if (getDataInputSlots().size() > 1)
            throw new JIPipeValidationRuntimeException(new JIPipeValidationReportEntry(JIPipeValidationReportEntryLevel.Error, new GraphNodeValidationReportContext(this),
                    "Too many input slots for JIPipeSimpleIteratingAlgorithm!",
                    "The developer of this algorithm chose the wrong node type. The one that was selected only supports at most one input.",
                    "Please contact the plugin developers and tell them to let algorithm '" + getInfo().getId() + "' inherit from 'JIPipeIteratingAlgorithm' instead."));
        if (isPassThrough() && canPassThrough()) {
            progressInfo.log("Data passed through to output");
            runPassThrough(runContext, progressInfo);
            return;
        }

        if (getInputSlots().isEmpty()) {
            final int row = 0;
            JIPipeProgressInfo slotProgress = progressInfo.resolveAndLog("Data row", row, 1);
            JIPipeSingleIterationStep iterationStep = new JIPipeSingleIterationStep(this);
            runIteration(iterationStep, new JIPipeMutableIterationContext(row, 1), slotProgress);
        } else {

            boolean withLimit = iterationStepGenerationSettings.getLimit().isEnabled();
            IntegerRange limit = iterationStepGenerationSettings.getLimit().getContent();
            TIntSet allowedIndices = withLimit ? new TIntHashSet(limit.getIntegers(0, getFirstInputSlot().getRowCount(), new JIPipeExpressionVariablesMap())) : null;

            if (!supportsParallelization() || !isParallelizationEnabled() || runContext.getThreadPool() == null || runContext.getThreadPool().getMaxThreads() <= 1) {
                for (int i = 0; i < getFirstInputSlot().getRowCount(); i++) {
                    if (withLimit && !allowedIndices.contains(i))
                        continue;
                    if (progressInfo.isCancelled())
                        return;
                    JIPipeProgressInfo slotProgress = progressInfo.resolveAndLog("Data row", i, getFirstInputSlot().getRowCount());
                    JIPipeSingleIterationStep iterationStep = new JIPipeSingleIterationStep(this);
                    iterationStep.setInputData(getFirstInputSlot(), i);
                    iterationStep.addMergedTextAnnotations(getFirstInputSlot().getTextAnnotations(i), JIPipeTextAnnotationMergeMode.Merge);
                    runIteration(iterationStep, new JIPipeMutableIterationContext(i, getFirstInputSlot().getRowCount()), slotProgress);
                }
            } else {
                List<Runnable> tasks = new ArrayList<>();
                for (int i = 0; i < getFirstInputSlot().getRowCount(); i++) {
                    if (withLimit && !allowedIndices.contains(i))
                        continue;
                    int rowIndex = i;
                    tasks.add(() -> {
                        if (progressInfo.isCancelled())
                            return;
                        JIPipeProgressInfo slotProgress = progressInfo.resolveAndLog("Data row", rowIndex, getFirstInputSlot().getRowCount());
                        JIPipeSingleIterationStep iterationStep = new JIPipeSingleIterationStep(this);
                        iterationStep.setInputData(getFirstInputSlot(), rowIndex);
                        iterationStep.addMergedTextAnnotations(getFirstInputSlot().getTextAnnotations(rowIndex), JIPipeTextAnnotationMergeMode.Merge);
                        runIteration(iterationStep, new JIPipeMutableIterationContext(rowIndex, getFirstInputSlot().getRowCount()), slotProgress);
                    });
                }
                progressInfo.log(String.format("Running %d batches (batch size %d) in parallel. Available threads = %d",
                        tasks.size(),
                        getParallelizationBatchSize(),
                        runContext.getThreadPool().getMaxThreads()));
                for (Future<Exception> batch : runContext.getThreadPool().scheduleBatches(tasks, getParallelizationBatchSize())) {
                    try {
                        Exception exception = batch.get();
                        if (exception != null)
                            throw new RuntimeException(exception);
                    } catch (InterruptedException | ExecutionException e) {
                        throw new RuntimeException(e);
                    }
                }
            }
        }
    }

    @Override
    public void reportValidity(JIPipeValidationReportContext reportContext, JIPipeValidationReport report) {
        if (getDataInputSlots().size() > 1) {
            report.add(new JIPipeValidationReportEntry(JIPipeValidationReportEntryLevel.Error, reportContext,
                    "Error in source code detected!",
                    "The developer of this algorithm chose the wrong node type. The one that was selected only supports at most one input.",
                    "Please contact the plugin developers and tell them to let algorithm '" + getInfo().getId() + "' inherit from 'JIPipeIteratingAlgorithm' instead."));
        }
    }

    /**
     * Runs code on one data row
     *
     * @param iterationStep    The data interface
     * @param iterationContext The iteration context
     * @param progressInfo     the progress info from the run
     */
    protected abstract void runIteration(JIPipeSingleIterationStep iterationStep, JIPipeIterationContext iterationContext, JIPipeProgressInfo progressInfo);

    @Override
    public boolean supportsParallelization() {
        return false;
    }

    @Override
    public int getParallelizationBatchSize() {
        return 1;
    }

    @SetJIPipeDocumentation(name = "Input management", description = "This algorithm has one input and will iterate through each row of its input and apply the workload. " +
            "Use following settings to control which data batches are generated.")
    @JIPipeParameter(value = "jipipe:data-batch-generation", hidden = true)
    public IterationStepGenerationSettings getDataBatchGenerationSettings() {
        return iterationStepGenerationSettings;
    }

    @Override
    public JIPipeIterationStepGenerationSettings getGenerationSettingsInterface() {
        return iterationStepGenerationSettings;
    }

    @Override
    public boolean isParameterUIVisible(JIPipeParameterTree tree, JIPipeParameterCollection subParameter) {
        if (ParameterUtils.isHiddenLocalParameterCollection(tree, subParameter, "jipipe:data-batch-generation", "jipipe:adaptive-parameters")) {
            return false;
        }
        return super.isParameterUIVisible(tree, subParameter);
    }

    @Override
    public boolean isParameterUIVisible(JIPipeParameterTree tree, JIPipeParameterAccess access) {
        if (ParameterUtils.isHiddenLocalParameter(tree, access, "jipipe:parallelization:enabled")) {
            return false;
        }
        return super.isParameterUIVisible(tree, access);
    }

    @Override
    public JIPipeDataBatchGenerationResult generateDataBatchesGenerationResult(List<JIPipeInputDataSlot> slots, JIPipeProgressInfo progressInfo) {
        List<JIPipeMultiIterationStep> batches = new ArrayList<>();
        JIPipeDataSlot slot = slots.get(0);
        boolean withLimit = iterationStepGenerationSettings.getLimit().isEnabled();
        IntegerRange limit = iterationStepGenerationSettings.getLimit().getContent();
        TIntSet allowedIndices = withLimit ? new TIntHashSet(limit.getIntegers(0, slot.getRowCount(), new JIPipeExpressionVariablesMap())) : null;
        for (int i = 0; i < slot.getRowCount(); i++) {
            if (withLimit && !allowedIndices.contains(i))
                continue;
            JIPipeMultiIterationStep iterationStep = new JIPipeMultiIterationStep(this);
            iterationStep.addInputData(slot, i);
            iterationStep.addMergedTextAnnotations(slot.getTextAnnotations(i), JIPipeTextAnnotationMergeMode.Merge);
            iterationStep.addMergedDataAnnotations(slot.getDataAnnotations(i), JIPipeDataAnnotationMergeMode.MergeTables);
            batches.add(iterationStep);
        }

        // Generate result object
        JIPipeDataBatchGenerationResult result = new JIPipeDataBatchGenerationResult();
        result.setDataBatches(batches);

        return result;
    }

    public static class IterationStepGenerationSettings extends AbstractJIPipeParameterCollection implements JIPipeIterationStepGenerationSettings {
        private OptionalIntegerRange limit = new OptionalIntegerRange(new IntegerRange("0-9"), false);

        public IterationStepGenerationSettings() {
        }

        public IterationStepGenerationSettings(IterationStepGenerationSettings other) {
            this.limit = new OptionalIntegerRange(other.limit);
        }

        @SetJIPipeDocumentation(name = "Limit", description = "Limits which data batches are generated. The first index is zero.")
        @JIPipeParameter("limit")
        public OptionalIntegerRange getLimit() {
            return limit;
        }

        @JIPipeParameter("limit")
        public void setLimit(OptionalIntegerRange limit) {
            this.limit = limit;
        }
    }
}
