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

package org.hkijena.jipipe.api.nodes;

import com.google.common.eventbus.EventBus;
import gnu.trove.set.TIntSet;
import gnu.trove.set.hash.TIntHashSet;
import org.hkijena.jipipe.api.JIPipeDocumentation;
import org.hkijena.jipipe.api.JIPipeIssueReport;
import org.hkijena.jipipe.api.JIPipeProgressInfo;
import org.hkijena.jipipe.api.annotation.JIPipeDataAnnotationMergeMode;
import org.hkijena.jipipe.api.annotation.JIPipeTextAnnotationMergeMode;
import org.hkijena.jipipe.api.data.JIPipeDataSlot;
import org.hkijena.jipipe.api.data.JIPipeInputDataSlot;
import org.hkijena.jipipe.api.data.JIPipeSlotConfiguration;
import org.hkijena.jipipe.api.exceptions.UserFriendlyRuntimeException;
import org.hkijena.jipipe.api.parameters.*;
import org.hkijena.jipipe.extensions.expressions.ExpressionVariables;
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
public abstract class JIPipeParameterlessSimpleIteratingAlgorithm extends JIPipeAlgorithm implements JIPipeParallelizedAlgorithm, JIPipeDataBatchAlgorithm {

    private boolean parallelizationEnabled = true;
    private DataBatchGenerationSettings dataBatchGenerationSettings = new DataBatchGenerationSettings();

    /**
     * Creates a new instance
     *
     * @param info              Algorithm info
     * @param slotConfiguration Slot configuration override
     */
    public JIPipeParameterlessSimpleIteratingAlgorithm(JIPipeNodeInfo info, JIPipeSlotConfiguration slotConfiguration) {
        super(info, slotConfiguration);
        registerSubParameter(dataBatchGenerationSettings);
    }

    /**
     * Creates a new instance
     *
     * @param info Algorithm info
     */
    public JIPipeParameterlessSimpleIteratingAlgorithm(JIPipeNodeInfo info) {
        super(info, null);
        registerSubParameter(dataBatchGenerationSettings);
    }

    /**
     * Copies the algorithm
     *
     * @param other The original
     */
    public JIPipeParameterlessSimpleIteratingAlgorithm(JIPipeParameterlessSimpleIteratingAlgorithm other) {
        super(other);
        this.dataBatchGenerationSettings = new DataBatchGenerationSettings(other.dataBatchGenerationSettings);
        this.parallelizationEnabled = other.parallelizationEnabled;
        registerSubParameter(dataBatchGenerationSettings);
    }

    @Override
    public void run(JIPipeProgressInfo progressInfo) {
        if (getDataInputSlots().size() > 1)
            throw new UserFriendlyRuntimeException("Too many input slots for JIPipeSimpleIteratingAlgorithm!",
                    "Error in source code detected!",
                    "Algorithm '" + getName() + "'",
                    "The developer of this algorithm chose the wrong node type. The one that was selected only supports at most one input.",
                    "Please contact the plugin developers and tell them to let algorithm '" + getInfo().getId() + "' inherit from 'JIPipeIteratingAlgorithm' instead.");
        if (isPassThrough() && canPassThrough()) {
            progressInfo.log("Data passed through to output");
            runPassThrough(progressInfo);
            return;
        }

        if (getInputSlots().isEmpty()) {
            final int row = 0;
            JIPipeProgressInfo slotProgress = progressInfo.resolveAndLog("Data row", row, 1);
            JIPipeDataBatch dataBatch = new JIPipeDataBatch(this);
            runIteration(dataBatch, slotProgress);
        } else {

            boolean withLimit = dataBatchGenerationSettings.getLimit().isEnabled();
            IntegerRange limit = dataBatchGenerationSettings.getLimit().getContent();
            TIntSet allowedIndices = withLimit ? new TIntHashSet(limit.getIntegers(0, getFirstInputSlot().getRowCount(), new ExpressionVariables())) : null;

            if (!supportsParallelization() || !isParallelizationEnabled() || getThreadPool() == null || getThreadPool().getMaxThreads() <= 1) {
                for (int i = 0; i < getFirstInputSlot().getRowCount(); i++) {
                    if (withLimit && !allowedIndices.contains(i))
                        continue;
                    if (progressInfo.isCancelled())
                        return;
                    JIPipeProgressInfo slotProgress = progressInfo.resolveAndLog("Data row", i, getFirstInputSlot().getRowCount());
                    JIPipeDataBatch dataBatch = new JIPipeDataBatch(this);
                    dataBatch.setInputData(getFirstInputSlot(), i);
                    dataBatch.addMergedTextAnnotations(getFirstInputSlot().getTextAnnotations(i), JIPipeTextAnnotationMergeMode.Merge);
                    runIteration(dataBatch, slotProgress);
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
                        JIPipeDataBatch dataBatch = new JIPipeDataBatch(this);
                        dataBatch.setInputData(getFirstInputSlot(), rowIndex);
                        dataBatch.addMergedTextAnnotations(getFirstInputSlot().getTextAnnotations(rowIndex), JIPipeTextAnnotationMergeMode.Merge);
                        runIteration(dataBatch, slotProgress);
                    });
                }
                progressInfo.log(String.format("Running %d batches (batch size %d) in parallel. Available threads = %d", tasks.size(), getParallelizationBatchSize(), getThreadPool().getMaxThreads()));
                for (Future<Exception> batch : getThreadPool().scheduleBatches(tasks, getParallelizationBatchSize())) {
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
    public void reportValidity(JIPipeIssueReport report) {
        if (getDataInputSlots().size() > 1) {
            report.resolve("Internals").reportIsInvalid(
                    "Error in source code detected!",
                    "The developer of this algorithm chose the wrong node type. The one that was selected only supports at most one input.",
                    "Please contact the plugin developers and tell them to let algorithm '" + getInfo().getId() + "' inherit from 'JIPipeIteratingAlgorithm' instead.",
                    this);
        }
    }

    /**
     * Runs code on one data row
     *
     * @param dataBatch    The data interface
     * @param progressInfo the progress info from the run
     */
    protected abstract void runIteration(JIPipeDataBatch dataBatch, JIPipeProgressInfo progressInfo);

    @Override
    public boolean supportsParallelization() {
        return false;
    }

    @Override
    public int getParallelizationBatchSize() {
        return 1;
    }

    @JIPipeDocumentation(name = "Enable parallelization", description = "If enabled, the workload can be calculated across multiple threads to for speedup. " +
            "Please note that the actual usage of multiple threads depend on the runtime settings and the algorithm implementation. " +
            "We recommend to use the runtime parameters to control parallelization in most cases.")
    @JIPipeParameter(value = "jipipe:parallelization:enabled", pinned = true)
    @Override
    public boolean isParallelizationEnabled() {
        return parallelizationEnabled;
    }

    @Override
    @JIPipeParameter("jipipe:parallelization:enabled")
    public void setParallelizationEnabled(boolean parallelizationEnabled) {
        this.parallelizationEnabled = parallelizationEnabled;
    }

    @JIPipeDocumentation(name = "Data batch generation", description = "This algorithm has one input and will iterate through each row of its input and apply the workload. " +
            "Use following settings to control which data batches are generated.")
    @JIPipeParameter(value = "jipipe:data-batch-generation", hidden = true)
    public DataBatchGenerationSettings getDataBatchGenerationSettings() {
        return dataBatchGenerationSettings;
    }

    @Override
    public JIPipeDataBatchGenerationSettings getGenerationSettingsInterface() {
        return dataBatchGenerationSettings;
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
    public List<JIPipeMergingDataBatch> generateDataBatchesDryRun(List<JIPipeInputDataSlot> slots, JIPipeProgressInfo progressInfo) {
        List<JIPipeMergingDataBatch> batches = new ArrayList<>();
        JIPipeDataSlot slot = slots.get(0);
        boolean withLimit = dataBatchGenerationSettings.getLimit().isEnabled();
        IntegerRange limit = dataBatchGenerationSettings.getLimit().getContent();
        TIntSet allowedIndices = withLimit ? new TIntHashSet(limit.getIntegers(0, slot.getRowCount(), new ExpressionVariables())) : null;
        for (int i = 0; i < slot.getRowCount(); i++) {
            if (withLimit && !allowedIndices.contains(i))
                continue;
            JIPipeMergingDataBatch dataBatch = new JIPipeMergingDataBatch(this);
            dataBatch.addInputData(slot, i);
            dataBatch.addMergedTextAnnotations(slot.getTextAnnotations(i), JIPipeTextAnnotationMergeMode.Merge);
            dataBatch.addMergedDataAnnotations(slot.getDataAnnotations(i), JIPipeDataAnnotationMergeMode.MergeTables);
            batches.add(dataBatch);
        }
        return batches;
    }

    public static class DataBatchGenerationSettings extends AbstractJIPipeParameterCollection implements JIPipeDataBatchGenerationSettings {
        private OptionalIntegerRange limit = new OptionalIntegerRange(new IntegerRange("0-9"), false);

        public DataBatchGenerationSettings() {
        }

        public DataBatchGenerationSettings(DataBatchGenerationSettings other) {
            this.limit = new OptionalIntegerRange(other.limit);
        }

        @JIPipeDocumentation(name = "Limit", description = "Limits which data batches are generated. The first index is zero.")
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
