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

import com.google.common.primitives.Ints;
import gnu.trove.set.TIntSet;
import gnu.trove.set.hash.TIntHashSet;
import org.hkijena.jipipe.api.JIPipeDataBatchGenerationResult;
import org.hkijena.jipipe.api.SetJIPipeDocumentation;
import org.hkijena.jipipe.api.AddJIPipeDocumentationDescription;
import org.hkijena.jipipe.api.JIPipeProgressInfo;
import org.hkijena.jipipe.api.annotation.JIPipeDataAnnotationMergeMode;
import org.hkijena.jipipe.api.annotation.JIPipeTextAnnotation;
import org.hkijena.jipipe.api.annotation.JIPipeTextAnnotationMergeMode;
import org.hkijena.jipipe.api.data.*;
import org.hkijena.jipipe.api.nodes.*;
import org.hkijena.jipipe.api.nodes.iterationstep.*;
import org.hkijena.jipipe.api.parameters.JIPipeParameter;
import org.hkijena.jipipe.api.parameters.JIPipeParameterAccess;
import org.hkijena.jipipe.api.parameters.JIPipeParameterCollection;
import org.hkijena.jipipe.api.parameters.JIPipeParameterTree;
import org.hkijena.jipipe.api.validation.JIPipeValidationReportEntry;
import org.hkijena.jipipe.api.validation.JIPipeValidationReportEntryLevel;
import org.hkijena.jipipe.api.validation.JIPipeValidationRuntimeException;
import org.hkijena.jipipe.api.validation.contexts.GraphNodeValidationReportContext;
import org.hkijena.jipipe.extensions.expressions.JIPipeExpressionVariablesMap;
import org.hkijena.jipipe.extensions.parameters.library.primitives.ranges.IntegerRange;
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
@AddJIPipeDocumentationDescription(description = "This algorithm groups the incoming data based on the annotations. " +
        "Those groups can consist of one data item per slot. " +
        "If items are missing, they will be generated according to this node's generator function. " +
        "Otherwise data will be passed through.")
public abstract class JIPipeIteratingMissingDataGeneratorAlgorithm extends JIPipeParameterSlotAlgorithm implements JIPipeParallelizedAlgorithm, JIPipeIterationStepAlgorithm {

    private JIPipeIteratingMissingDataGeneratorIterationStepGenerationSettings iterationStepGenerationSettings = new JIPipeIteratingMissingDataGeneratorIterationStepGenerationSettings();
    private boolean keepOriginalAnnotations = true;

    /**
     * Creates a new instance
     *
     * @param info              Algorithm info
     * @param slotConfiguration Slot configuration override
     */
    public JIPipeIteratingMissingDataGeneratorAlgorithm(JIPipeNodeInfo info, JIPipeSlotConfiguration slotConfiguration) {
        super(info, slotConfiguration);
    }

    /**
     * Creates a new instance
     *
     * @param info Algorithm info
     */
    public JIPipeIteratingMissingDataGeneratorAlgorithm(JIPipeNodeInfo info) {
        super(info, null);
        registerSubParameter(iterationStepGenerationSettings);
    }

    /**
     * Copies the algorithm
     *
     * @param other The original
     */
    public JIPipeIteratingMissingDataGeneratorAlgorithm(JIPipeIteratingMissingDataGeneratorAlgorithm other) {
        super(other);
        this.iterationStepGenerationSettings = new JIPipeIteratingMissingDataGeneratorIterationStepGenerationSettings(other.iterationStepGenerationSettings);
        this.keepOriginalAnnotations = other.keepOriginalAnnotations;
        registerSubParameter(iterationStepGenerationSettings);
    }

    @Override
    public JIPipeIterationStepGenerationSettings getGenerationSettingsInterface() {
        return iterationStepGenerationSettings;
    }

    @Override
    public JIPipeDataBatchGenerationResult generateDataBatchesGenerationResult(List<JIPipeInputDataSlot> slots, JIPipeProgressInfo progressInfo) {
        JIPipeMultiIterationStepGenerator builder = new JIPipeMultiIterationStepGenerator();
        builder.setNode(this);
        builder.setApplyMerging(false);
        builder.setSlots(slots);
        builder.setAnnotationMergeStrategy(iterationStepGenerationSettings.getAnnotationMergeStrategy());
        builder.setReferenceColumns(iterationStepGenerationSettings.getColumnMatching(),
                iterationStepGenerationSettings.getCustomColumns());
        builder.setAnnotationMatchingMethod(iterationStepGenerationSettings.getAnnotationMatchingMethod());
        builder.setCustomAnnotationMatching(iterationStepGenerationSettings.getCustomAnnotationMatching());
        builder.setForceFlowGraphSolver(iterationStepGenerationSettings.isForceFlowGraphSolver());
        List<JIPipeMultiIterationStep> iterationSteps = builder.build(progressInfo);
        iterationSteps.sort(Comparator.naturalOrder());
        boolean withLimit = iterationStepGenerationSettings.getLimit().isEnabled();
        IntegerRange limit = iterationStepGenerationSettings.getLimit().getContent();
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

        // Generate result object
        JIPipeDataBatchGenerationResult result = new JIPipeDataBatchGenerationResult();
        result.setDataBatches(iterationSteps);
        result.setReferenceTextAnnotationColumns(builder.getReferenceColumns());

        return result;
    }

    @Override
    public void runParameterSet(JIPipeGraphNodeRunContext runContext, JIPipeProgressInfo progressInfo, List<JIPipeTextAnnotation> parameterAnnotations) {

        if (isPassThrough() && canPassThrough()) {
            progressInfo.log("Data passed through to output");
            runPassThrough(runContext, progressInfo);
            return;
        }

        List<JIPipeMultiIterationStep> iterationSteps;

        // No input slots -> Nothing to do
        if (getDataInputSlotCount() == 0) {
            return;
        } else if (getDataInputSlotCount() == 1) {
            iterationSteps = new ArrayList<>();
            for (int row = 0; row < getFirstInputSlot().getRowCount(); row++) {
                if (progressInfo.isCancelled())
                    break;
                JIPipeMultiIterationStep iterationStep = new JIPipeMultiIterationStep(this);
                iterationStep.setInputData(getFirstInputSlot(), row);
                iterationStep.addMergedTextAnnotations(parameterAnnotations, iterationStepGenerationSettings.getAnnotationMergeStrategy());
                iterationStep.addMergedTextAnnotations(getFirstInputSlot().getTextAnnotations(row), iterationStepGenerationSettings.getAnnotationMergeStrategy());
                iterationSteps.add(iterationStep);
            }
        } else {
            iterationSteps = generateDataBatchesGenerationResult(getNonParameterInputSlots(), progressInfo).getDataBatches();
        }

        if (iterationSteps == null) {
            throw new JIPipeValidationRuntimeException(new JIPipeValidationReportEntry(JIPipeValidationReportEntryLevel.Error, new GraphNodeValidationReportContext(this), "Unable to split data into batches!",
                    "The algorithm needs to assign input a unique data set via annotations, but there are either missing elements or multiple data per slot.",
                    "Please check the input of the algorithm by running the quick run on each input algorithm. " +
                            "Try to switch to the 'Data batches' tab to preview how data is split into batches."));
        }

        final int numIterationSteps = iterationSteps.size();
        if (!supportsParallelization() || !isParallelizationEnabled() || runContext.getThreadPool() == null || runContext.getThreadPool().getMaxThreads() <= 1) {
            for (int i = 0; i < iterationSteps.size(); i++) {
                if (progressInfo.isCancelled())
                    return;
                JIPipeProgressInfo slotProgress = progressInfo.resolveAndLog("Data row", i, iterationSteps.size());
                runIteration(iterationSteps.get(i), new JIPipeMutableIterationContext(i, numIterationSteps), runContext, slotProgress);
            }
        } else {
            List<Runnable> tasks = new ArrayList<>();
            for (int i = 0; i < getFirstInputSlot().getRowCount(); i++) {
                int rowIndex = i;
                tasks.add(() -> {
                    if (progressInfo.isCancelled())
                        return;
                    JIPipeProgressInfo slotProgress = progressInfo.resolveAndLog("Data row", rowIndex, iterationSteps.size());
                    runIteration(iterationSteps.get(rowIndex), new JIPipeMutableIterationContext(rowIndex, numIterationSteps), runContext, slotProgress);
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

    @SetJIPipeDocumentation(name = "Input management", description = "This algorithm can have multiple inputs. This means that JIPipe has to match incoming data into batches via metadata annotations. " +
            "The following settings allow you to control which columns are used as reference to organize data.")
    @JIPipeParameter(value = "jipipe:data-batch-generation", collapsed = true)
    public JIPipeIteratingMissingDataGeneratorIterationStepGenerationSettings getDataBatchGenerationSettings() {
        return iterationStepGenerationSettings;
    }

    @Override
    public boolean supportsParallelization() {
        return false;
    }

    @Override
    public int getParallelizationBatchSize() {
        return 1;
    }

    @SetJIPipeDocumentation(name = "Keep original annotations", description = "If enabled, outputs that were not generated " +
            "keep their original annotations. Otherwise the merged annotations from the data batch are used.")
    @JIPipeParameter(value = "keep-original-annotations", pinned = true)
    public boolean isKeepOriginalAnnotations() {
        return keepOriginalAnnotations;
    }

    @JIPipeParameter("keep-original-annotations")
    public void setKeepOriginalAnnotations(boolean keepOriginalAnnotations) {
        this.keepOriginalAnnotations = keepOriginalAnnotations;
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

    /**
     * Runs code on one data row
     *
     * @param iterationStep    The data interface
     * @param iterationContext The iteration context
     * @param runContext
     * @param progressInfo     the progress info from the run
     */
    protected void runIteration(JIPipeMultiIterationStep iterationStep, JIPipeIterationContext iterationContext, JIPipeGraphNodeRunContext runContext, JIPipeProgressInfo progressInfo) {
        for (JIPipeInputDataSlot inputSlot : getDataInputSlots()) {
            JIPipeOutputDataSlot outputSlot = getCorrespondingOutputSlot(inputSlot);
            if (outputSlot == null)
                continue;
            JIPipeProgressInfo slotProgress = progressInfo.resolveAndLog("Input slot '" + inputSlot.getName() + "'");
            Set<Integer> rows = iterationStep.getInputRows(inputSlot);
            if (rows.isEmpty()) {
                slotProgress.log("No rows. Generating data.");
                runGenerator(iterationStep, inputSlot, outputSlot, slotProgress);
            } else {
                if (keepOriginalAnnotations) {
                    for (int row : rows) {
                        JIPipeDataItemStore virtualData = inputSlot.getDataItemStore(row);
                        List<JIPipeTextAnnotation> annotations = inputSlot.getTextAnnotations(row);
                        outputSlot.addData(virtualData,
                                annotations,
                                JIPipeTextAnnotationMergeMode.OverwriteExisting,
                                inputSlot.getDataAnnotations(row),
                                JIPipeDataAnnotationMergeMode.OverwriteExisting,
                                inputSlot.getDataContext(row).branch(this),
                                slotProgress);
                    }
                } else {
                    for (int row : rows) {
                        JIPipeDataItemStore virtualData = inputSlot.getDataItemStore(row);
                        iterationStep.addOutputData(outputSlot, virtualData, progressInfo);
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
    protected JIPipeOutputDataSlot getCorrespondingOutputSlot(JIPipeDataSlot inputSlot) {
        return getOutputSlotMap().getOrDefault(inputSlot.getName(), null);
    }

    /**
     * Generates data and puts the output into the specified output slot
     *
     * @param iterationStep    the data batch
     * @param inputSlot    the input slot that should be generated. Please note that it does not contain any data for this batch.
     * @param outputSlot   the output slot where data should be put.
     * @param progressInfo the progress info
     */
    protected abstract void runGenerator(JIPipeMultiIterationStep iterationStep, JIPipeInputDataSlot inputSlot, JIPipeOutputDataSlot outputSlot, JIPipeProgressInfo progressInfo);

}
