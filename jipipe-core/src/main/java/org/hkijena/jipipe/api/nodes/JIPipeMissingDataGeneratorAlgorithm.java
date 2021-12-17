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

import gnu.trove.set.TIntSet;
import gnu.trove.set.hash.TIntHashSet;
import org.hkijena.jipipe.api.JIPipeDocumentation;
import org.hkijena.jipipe.api.JIPipeProgressInfo;
import org.hkijena.jipipe.api.data.JIPipeAnnotation;
import org.hkijena.jipipe.api.data.JIPipeAnnotationMergeStrategy;
import org.hkijena.jipipe.api.data.JIPipeDataAnnotationMergeStrategy;
import org.hkijena.jipipe.api.data.JIPipeDataSlot;
import org.hkijena.jipipe.api.data.JIPipeSlotConfiguration;
import org.hkijena.jipipe.api.data.JIPipeVirtualData;
import org.hkijena.jipipe.api.exceptions.UserFriendlyRuntimeException;
import org.hkijena.jipipe.api.parameters.JIPipeParameter;
import org.hkijena.jipipe.api.parameters.JIPipeParameterAccess;
import org.hkijena.jipipe.api.parameters.JIPipeParameterTree;
import org.hkijena.jipipe.extensions.parameters.generators.IntegerRange;
import org.hkijena.jipipe.utils.ParameterUtils;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;

/**
 * An {@link JIPipeAlgorithm} that passes incoming data to their corresponding output slots if the data batch is complete
 * and otherwise runs a generator function that generates missing data items.
 * Original annotations are preserved.
 */
public abstract class JIPipeMissingDataGeneratorAlgorithm extends JIPipeParameterSlotAlgorithm implements JIPipeParallelizedAlgorithm, JIPipeDataBatchAlgorithm {

    public static final String GENERATOR_ALGORITHM_DESCRIPTION = "This algorithm groups the incoming data based on the annotations. " +
            "Those groups can consist of one or multiple data item per slot. " +
            "If items are missing, they will be generated according to this node's generator function. " +
            "Otherwise data will be passed through.";

    private JIPipeMissingDataGeneratorDataBatchGenerationSettings dataBatchGenerationSettings = new JIPipeMissingDataGeneratorDataBatchGenerationSettings();
    private boolean parallelizationEnabled = true;
    private boolean keepOriginalAnnotations = true;

    /**
     * Creates a new instance
     *
     * @param info              Algorithm info
     * @param slotConfiguration Slot configuration override
     */
    public JIPipeMissingDataGeneratorAlgorithm(JIPipeNodeInfo info, JIPipeSlotConfiguration slotConfiguration) {
        super(info, slotConfiguration);
    }

    /**
     * Creates a new instance
     *
     * @param info Algorithm info
     */
    public JIPipeMissingDataGeneratorAlgorithm(JIPipeNodeInfo info) {
        super(info, null);
        registerSubParameter(dataBatchGenerationSettings);
    }

    /**
     * Copies the algorithm
     *
     * @param other The original
     */
    public JIPipeMissingDataGeneratorAlgorithm(JIPipeMissingDataGeneratorAlgorithm other) {
        super(other);
        this.dataBatchGenerationSettings = new JIPipeMissingDataGeneratorDataBatchGenerationSettings(other.dataBatchGenerationSettings);
        this.parallelizationEnabled = other.parallelizationEnabled;
        this.keepOriginalAnnotations = other.keepOriginalAnnotations;
        registerSubParameter(dataBatchGenerationSettings);
    }

    @Override
    public JIPipeDataBatchGenerationSettings getGenerationSettingsInterface() {
        return dataBatchGenerationSettings;
    }

    @Override
    public List<JIPipeMergingDataBatch> generateDataBatchesDryRun(List<JIPipeDataSlot> slots, JIPipeProgressInfo progressInfo) {
        JIPipeMergingDataBatchBuilder builder = new JIPipeMergingDataBatchBuilder();
        builder.setNode(this);
        builder.setApplyMerging(dataBatchGenerationSettings.isAllowMerging());
        builder.setSlots(slots);
        builder.setAnnotationMergeStrategy(dataBatchGenerationSettings.getAnnotationMergeStrategy());
        builder.setReferenceColumns(dataBatchGenerationSettings.getDataSetMatching(),
                dataBatchGenerationSettings.getCustomColumns());
        List<JIPipeMergingDataBatch> dataBatches = builder.build(progressInfo);
        dataBatches.sort(Comparator.naturalOrder());
        boolean withLimit = dataBatchGenerationSettings.getLimit().isEnabled();
        IntegerRange limit = dataBatchGenerationSettings.getLimit().getContent();
        TIntSet allowedIndices = withLimit ? new TIntHashSet(limit.getIntegers(0, dataBatches.size())) : null;
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

    @Override
    public void runParameterSet(JIPipeProgressInfo progressInfo, List<JIPipeAnnotation> parameterAnnotations) {

        if (isPassThrough() && canPassThrough()) {
            progressInfo.log("Data passed through to output");
            runPassThrough(progressInfo);
            return;
        }

        List<JIPipeMergingDataBatch> dataBatches;

        // No input slots -> Nothing to do
        if (getEffectiveInputSlotCount() == 0) {
            return;
        } else if (getEffectiveInputSlotCount() == 1) {
            dataBatches = new ArrayList<>();
            for (int row = 0; row < getFirstInputSlot().getRowCount(); row++) {
                if (progressInfo.isCancelled())
                    break;
                JIPipeMergingDataBatch dataBatch = new JIPipeMergingDataBatch(this);
                dataBatch.setInputData(getFirstInputSlot(), row);
                dataBatch.addMergedAnnotations(parameterAnnotations, dataBatchGenerationSettings.getAnnotationMergeStrategy());
                dataBatch.addMergedAnnotations(getFirstInputSlot().getAnnotations(row), dataBatchGenerationSettings.getAnnotationMergeStrategy());
                dataBatches.add(dataBatch);
            }
        } else {
            dataBatches = generateDataBatchesDryRun(getNonParameterInputSlots(), progressInfo);
        }

        if (dataBatches == null) {
            throw new UserFriendlyRuntimeException("Unable to split data into batches!",
                    "Unable to split data into batches!",
                    "Algorithm '" + getName() + "'",
                    "The algorithm needs to assign input a unique data set via annotations, but there are either missing elements or multiple data per slot.",
                    "Please check the input of the algorithm by running the quick run on each input algorithm. " +
                            "Try to switch to the 'Data batches' tab to preview how data is split into batches.");
        }

        if (!supportsParallelization() || !isParallelizationEnabled() || getThreadPool() == null || getThreadPool().getMaxThreads() <= 1) {
            for (int i = 0; i < dataBatches.size(); i++) {
                if (progressInfo.isCancelled())
                    return;
                JIPipeProgressInfo slotProgress = progressInfo.resolveAndLog("Data row", i, dataBatches.size());
                runIteration(dataBatches.get(i), slotProgress);
            }
        } else {
            List<Runnable> tasks = new ArrayList<>();
            for (int i = 0; i < getFirstInputSlot().getRowCount(); i++) {
                int rowIndex = i;
                tasks.add(() -> {
                    if (progressInfo.isCancelled())
                        return;
                    JIPipeProgressInfo slotProgress = progressInfo.resolveAndLog("Data row", rowIndex, dataBatches.size());
                    runIteration(dataBatches.get(rowIndex), slotProgress);
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

    @Override
    public boolean isParameterUIVisible(JIPipeParameterTree tree, JIPipeParameterAccess access) {
        if (ParameterUtils.isHiddenLocalParameter(tree, access, "jipipe:parallelization:enabled")) {
            return false;
        }
        return super.isParameterUIVisible(tree, access);
    }

    @JIPipeDocumentation(name = "Data batch generation", description = "This algorithm can have multiple inputs. This means that JIPipe has to match incoming data into batches via metadata annotations. " +
            "The following settings allow you to control which columns are used as reference to organize data.")
    @JIPipeParameter(value = "jipipe:data-batch-generation", collapsed = true)
    public JIPipeMissingDataGeneratorDataBatchGenerationSettings getDataBatchGenerationSettings() {
        return dataBatchGenerationSettings;
    }

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

    @JIPipeDocumentation(name = "Keep original annotations", description = "If enabled, outputs that were not generated " +
            "keep their original annotations. Otherwise the merged annotations from the data batch are used.")
    @JIPipeParameter("keep-original-annotations")
    public boolean isKeepOriginalAnnotations() {
        return keepOriginalAnnotations;
    }

    @JIPipeParameter("keep-original-annotations")
    public void setKeepOriginalAnnotations(boolean keepOriginalAnnotations) {
        this.keepOriginalAnnotations = keepOriginalAnnotations;
    }

    /**
     * Runs code on one data row
     *
     * @param dataBatch    The data interface
     * @param progressInfo the progress info from the run
     */
    protected void runIteration(JIPipeMergingDataBatch dataBatch, JIPipeProgressInfo progressInfo) {
        for (JIPipeDataSlot inputSlot : getEffectiveInputSlots()) {
            JIPipeDataSlot outputSlot = getCorrespondingOutputSlot(inputSlot);
            if (outputSlot == null)
                continue;
            JIPipeProgressInfo slotProgress = progressInfo.resolveAndLog("Input slot '" + inputSlot.getName() + "'");
            Set<Integer> rows = dataBatch.getInputRows(inputSlot);
            if (rows.isEmpty()) {
                slotProgress.log("No rows. Generating data.");
                runGenerator(dataBatch, inputSlot, outputSlot, slotProgress);
            } else {
                if (keepOriginalAnnotations) {
                    for (int row : rows) {
                        JIPipeVirtualData virtualData = inputSlot.getVirtualData(row);
                        List<JIPipeAnnotation> annotations = inputSlot.getAnnotations(row);
                        outputSlot.addData(virtualData,
                                annotations,
                                JIPipeAnnotationMergeStrategy.OverwriteExisting,
                                inputSlot.getDataAnnotations(row),
                                JIPipeDataAnnotationMergeStrategy.OverwriteExisting);
                    }
                } else {
                    for (int row : rows) {
                        JIPipeVirtualData virtualData = inputSlot.getVirtualData(row);
                        dataBatch.addOutputData(outputSlot, virtualData);
                    }
                }
            }
        }
    }

    /**
     * Gets the output slot that correspond to the input slot.
     * Can return null, meaning that no data is transferred.
     * By default, this method returns the output with the same name as the input
     *
     * @param inputSlot the input slot
     * @return the output slot or null
     */
    protected JIPipeDataSlot getCorrespondingOutputSlot(JIPipeDataSlot inputSlot) {
        return getOutputSlotMap().getOrDefault(inputSlot.getName(), null);
    }

    /**
     * Generates data and puts the output into the specified output slot
     *
     * @param dataBatch    the data batch
     * @param inputSlot    the input slot that should be generated. Please note that it does not contain any data for this batch.
     * @param outputSlot   the output slot where data should be put.
     * @param progressInfo the progress info
     */
    protected abstract void runGenerator(JIPipeMergingDataBatch dataBatch, JIPipeDataSlot inputSlot, JIPipeDataSlot outputSlot, JIPipeProgressInfo progressInfo);

}
