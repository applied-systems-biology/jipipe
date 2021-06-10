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
import org.hkijena.jipipe.api.JIPipeProgressInfo;
import org.hkijena.jipipe.api.JIPipeValidityReport;
import org.hkijena.jipipe.api.data.JIPipeAnnotationMergeStrategy;
import org.hkijena.jipipe.api.data.JIPipeDataSlot;
import org.hkijena.jipipe.api.data.JIPipeSlotConfiguration;
import org.hkijena.jipipe.api.exceptions.UserFriendlyRuntimeException;
import org.hkijena.jipipe.api.parameters.JIPipeParameter;
import org.hkijena.jipipe.api.parameters.JIPipeParameterAccess;
import org.hkijena.jipipe.api.parameters.JIPipeParameterCollection;
import org.hkijena.jipipe.api.parameters.JIPipeParameterTree;
import org.hkijena.jipipe.extensions.parameters.generators.IntegerRange;
import org.hkijena.jipipe.extensions.parameters.generators.OptionalIntegerRange;
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
        if (getEffectiveInputSlots().size() > 1)
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
            TIntSet allowedIndices = withLimit ? new TIntHashSet(limit.getIntegers()) : null;

            if (!supportsParallelization() || !isParallelizationEnabled() || getThreadPool() == null || getThreadPool().getMaxThreads() <= 1) {
                for (int i = 0; i < getFirstInputSlot().getRowCount(); i++) {
                    if (withLimit && !allowedIndices.contains(i))
                        continue;
                    if (progressInfo.isCancelled().get())
                        return;
                    JIPipeProgressInfo slotProgress = progressInfo.resolveAndLog("Data row", i, getFirstInputSlot().getRowCount());
                    JIPipeDataBatch dataBatch = new JIPipeDataBatch(this);
                    dataBatch.setData(getFirstInputSlot(), i);
                    dataBatch.addGlobalAnnotations(getFirstInputSlot().getAnnotations(i), JIPipeAnnotationMergeStrategy.Merge);
                    runIteration(dataBatch, slotProgress);
                }
            } else {
                List<Runnable> tasks = new ArrayList<>();
                for (int i = 0; i < getFirstInputSlot().getRowCount(); i++) {
                    if (withLimit && !allowedIndices.contains(i))
                        continue;
                    int rowIndex = i;
                    tasks.add(() -> {
                        if (progressInfo.isCancelled().get())
                            return;
                        JIPipeProgressInfo slotProgress = progressInfo.resolveAndLog("Data row", rowIndex, getFirstInputSlot().getRowCount());
                        JIPipeDataBatch dataBatch = new JIPipeDataBatch(this);
                        dataBatch.setData(getFirstInputSlot(), rowIndex);
                        dataBatch.addGlobalAnnotations(getFirstInputSlot().getAnnotations(rowIndex), JIPipeAnnotationMergeStrategy.Merge);
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
    public void reportValidity(JIPipeValidityReport report) {
        if (getEffectiveInputSlots().size() > 1) {
            report.forCategory("Internals").reportIsInvalid(
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
    @JIPipeParameter(value = "jipipe:parallelization:enabled")
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
    @JIPipeParameter(value = "jipipe:data-batch-generation", collapsed = true)
    public DataBatchGenerationSettings getDataBatchGenerationSettings() {
        return dataBatchGenerationSettings;
    }

    @Override
    public JIPipeParameterCollection getGenerationSettingsInterface() {
        return dataBatchGenerationSettings;
    }

    @Override
    public boolean isParameterUIVisible(JIPipeParameterTree tree, JIPipeParameterCollection subParameter) {
        if(ParameterUtils.isHiddenLocalParameterCollection(tree, subParameter, "jipipe:data-batch-generation", "jipipe:adaptive-parameters")) {
            return false;
        }
        return super.isParameterUIVisible(tree, subParameter);
    }

    @Override
    public boolean isParameterUIVisible(JIPipeParameterTree tree, JIPipeParameterAccess access) {
        if(ParameterUtils.isHiddenLocalParameter(tree, access, "jipipe:parallelization:enabled")) {
            return false;
        }
        return super.isParameterUIVisible(tree, access);
    }

    @Override
    public List<JIPipeMergingDataBatch> generateDataBatchesDryRun(List<JIPipeDataSlot> slots) {
        List<JIPipeMergingDataBatch> batches = new ArrayList<>();
        JIPipeDataSlot slot = slots.get(0);
        boolean withLimit = dataBatchGenerationSettings.getLimit().isEnabled();
        IntegerRange limit = dataBatchGenerationSettings.getLimit().getContent();
        TIntSet allowedIndices = withLimit ? new TIntHashSet(limit.getIntegers()) : null;
        for (int i = 0; i < slot.getRowCount(); i++) {
            if (withLimit && !allowedIndices.contains(i))
                continue;
            JIPipeMergingDataBatch dataBatch = new JIPipeMergingDataBatch(this);
            dataBatch.addData(slot, i);
            dataBatch.addGlobalAnnotations(slot.getAnnotations(i), JIPipeAnnotationMergeStrategy.Merge);
            batches.add(dataBatch);
        }
        return batches;
    }

    public static class DataBatchGenerationSettings implements JIPipeParameterCollection {
        private final EventBus eventBus = new EventBus();
        private OptionalIntegerRange limit = new OptionalIntegerRange(new IntegerRange("0-9"), false);

        public DataBatchGenerationSettings() {
        }

        public DataBatchGenerationSettings(DataBatchGenerationSettings other) {
            this.limit = new OptionalIntegerRange(other.limit);
        }

        @Override
        public EventBus getEventBus() {
            return eventBus;
        }

        @JIPipeDocumentation(name = "Limit", description = "Limits which data batches are generated. The first index is zero.\n" + IntegerRange.DOCUMENTATION_DESCRIPTION)
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
