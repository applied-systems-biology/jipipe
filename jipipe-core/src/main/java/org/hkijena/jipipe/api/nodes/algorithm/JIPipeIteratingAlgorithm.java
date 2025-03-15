/*
 * Copyright by Zoltán Cseresnyés, Ruman Gerst
 *
 * Research Group Applied Systems Biology - Head: Prof. Dr. Marc Thilo Figge
 * https://www.leibniz-hki.de/en/applied-systems-biology.html
 * HKI-Center for Systems Biology of Infection
 * Leibniz Institute for Natural Product Research and Infection Biology - Hans Knöll Institute (HKI)
 * Adolf-Reichwein-Straße 23, 07745 Jena, Germany
 *
 * The project code is licensed under MIT.
 * See the LICENSE file provided with the code for the full license.
 */

package org.hkijena.jipipe.api.nodes.algorithm;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.google.common.primitives.Ints;
import gnu.trove.set.TIntSet;
import gnu.trove.set.hash.TIntHashSet;
import org.hkijena.jipipe.api.*;
import org.hkijena.jipipe.api.annotation.JIPipeTextAnnotation;
import org.hkijena.jipipe.api.annotation.JIPipeTextAnnotationMergeMode;
import org.hkijena.jipipe.api.data.JIPipeData;
import org.hkijena.jipipe.api.data.JIPipeDataSlotRole;
import org.hkijena.jipipe.api.data.JIPipeInputDataSlot;
import org.hkijena.jipipe.api.data.JIPipeSlotConfiguration;
import org.hkijena.jipipe.api.nodes.*;
import org.hkijena.jipipe.api.nodes.iterationstep.*;
import org.hkijena.jipipe.api.parameters.JIPipeParameter;
import org.hkijena.jipipe.api.parameters.JIPipeParameterAccess;
import org.hkijena.jipipe.api.parameters.JIPipeParameterCollection;
import org.hkijena.jipipe.api.parameters.JIPipeParameterTree;
import org.hkijena.jipipe.api.runtimepartitioning.JIPipeRuntimePartition;
import org.hkijena.jipipe.api.validation.JIPipeValidationReportEntry;
import org.hkijena.jipipe.api.validation.JIPipeValidationReportEntryLevel;
import org.hkijena.jipipe.api.validation.JIPipeValidationRuntimeException;
import org.hkijena.jipipe.api.validation.contexts.GraphNodeValidationReportContext;
import org.hkijena.jipipe.plugins.expressions.JIPipeExpressionVariablesMap;
import org.hkijena.jipipe.plugins.parameters.library.pairs.StringQueryExpressionAndStringPairParameter;
import org.hkijena.jipipe.plugins.parameters.library.primitives.optional.OptionalIntegerParameter;
import org.hkijena.jipipe.plugins.parameters.library.primitives.ranges.IntegerRange;
import org.hkijena.jipipe.utils.ParameterUtils;
import org.hkijena.jipipe.utils.ResourceUtils;
import org.hkijena.jipipe.utils.StringUtils;
import org.hkijena.jipipe.utils.UIUtils;
import org.hkijena.jipipe.utils.json.JsonUtils;
import org.hkijena.jipipe.utils.ui.ViewOnlyMenuItem;

import java.util.*;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;

/**
 * An {@link JIPipeAlgorithm} that iterates through each data row.
 * This algorithm utilizes the {@link JIPipeSingleIterationStep} class to iterate through input data sets.
 * It offers various parameters that control how data sets are matched.
 * If your algorithm only has one input and will never have more than one input slot, we recommend using {@link JIPipeSimpleIteratingAlgorithm}
 * instead that comes without the additional data set matching strategies
 */
@AddJIPipeDocumentationDescription(description = "This algorithm groups the incoming data based on the annotations. " +
        "Those groups can consist of one data item per slot.")
public abstract class JIPipeIteratingAlgorithm extends JIPipeParameterSlotAlgorithm implements JIPipeParallelizedAlgorithm, JIPipeIterationStepAlgorithm, JIPipeAdaptiveParametersAlgorithm {

    private JIPipeIteratingAlgorithmIterationStepGenerationSettings iterationStepGenerationSettings = new JIPipeIteratingAlgorithmIterationStepGenerationSettings();
    private JIPipeAdaptiveParameterSettings adaptiveParameterSettings = new JIPipeAdaptiveParameterSettings();
    private OptionalIntegerParameter localParallelizationNumThreads = new OptionalIntegerParameter(false, 1);

    /**
     * Creates a new instance
     *
     * @param info              Algorithm info
     * @param slotConfiguration Slot configuration override
     */
    public JIPipeIteratingAlgorithm(JIPipeNodeInfo info, JIPipeSlotConfiguration slotConfiguration) {
        super(info, slotConfiguration);
        adaptiveParameterSettings.setNode(this);
        registerSubParameter(adaptiveParameterSettings);
        registerSubParameter(iterationStepGenerationSettings);
    }

    /**
     * Creates a new instance
     *
     * @param info Algorithm info
     */
    public JIPipeIteratingAlgorithm(JIPipeNodeInfo info) {
        super(info, null);
        adaptiveParameterSettings.setNode(this);
        registerSubParameter(iterationStepGenerationSettings);
        registerSubParameter(adaptiveParameterSettings);
    }

    /**
     * Copies the algorithm
     *
     * @param other The original
     */
    public JIPipeIteratingAlgorithm(JIPipeIteratingAlgorithm other) {
        super(other);
        this.iterationStepGenerationSettings = new JIPipeIteratingAlgorithmIterationStepGenerationSettings(other.iterationStepGenerationSettings);
        this.adaptiveParameterSettings = new JIPipeAdaptiveParameterSettings(other.adaptiveParameterSettings);
        this.localParallelizationNumThreads = new OptionalIntegerParameter(other.localParallelizationNumThreads);
        adaptiveParameterSettings.setNode(this);
        registerSubParameter(iterationStepGenerationSettings);
        registerSubParameter(adaptiveParameterSettings);
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
        builder.setDataAnnotationMergeStrategy(iterationStepGenerationSettings.getDataAnnotationMergeStrategy());
        builder.setReferenceColumns(iterationStepGenerationSettings.getColumnMatching(),
                iterationStepGenerationSettings.getCustomColumns());
        builder.setCustomAnnotationMatching(iterationStepGenerationSettings.getCustomAnnotationMatching());
        builder.setAnnotationMatchingMethod(iterationStepGenerationSettings.getAnnotationMatchingMethod());
        builder.setForceFlowGraphSolver(iterationStepGenerationSettings.isForceFlowGraphSolver());
        List<JIPipeMultiIterationStep> iterationSteps = builder.build(progressInfo);
        iterationSteps.removeIf(JIPipeMultiIterationStep::isEmpty);
        iterationSteps.sort(Comparator.naturalOrder());
        boolean withLimit = iterationStepGenerationSettings.getLimit().isEnabled();
        IntegerRange limit = iterationStepGenerationSettings.getLimit().getContent();
        TIntSet allowedIndices = withLimit ? new TIntHashSet(limit.getIntegers(0, iterationSteps.size(), new JIPipeExpressionVariablesMap())) : null;
        if (withLimit) {
            progressInfo.log("[INFO] Applying limit to all iteration steps. Allowed indices are " + Ints.join(", ", allowedIndices.toArray()));
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
        if (!incomplete.isEmpty() && iterationStepGenerationSettings.isSkipIncompleteDataSets()) {
            progressInfo.log("[WARN] SKIPPING INCOMPLETE DATA BATCHES AS REQUESTED");
            iterationSteps.removeAll(incomplete);
        }

        // Generate result object
        JIPipeDataBatchGenerationResult result = new JIPipeDataBatchGenerationResult();
        result.setDataBatches(iterationSteps);
        result.setReferenceTextAnnotationColumns(builder.getReferenceColumns());

        return result;
    }

//    @Override
//    public Dimension getUIInputSlotIconBaseDimensions(String slotName) {
//        return new Dimension(16,16);
//    }
//
//    @Override
//    public ImageIcon getUIInputSlotIcon(String slotName) {
//        JIPipeInputDataSlot inputSlot = getInputSlot(slotName);
//        if(inputSlot != null && inputSlot.getInfo().getRole() == JIPipeDataSlotRole.Data) {
//            return UIUtils.getIconInvertedFromResources("actions/1-to-n.png");
//        }
//        return super.getUIInputSlotIcon(slotName);
//    }

    @Override
    public void createUIInputSlotIconDescriptionMenuItems(String slotName, List<ViewOnlyMenuItem> target) {
        super.createUIInputSlotIconDescriptionMenuItems(slotName, target);
        JIPipeInputDataSlot inputSlot = getInputSlot(slotName);
        if (inputSlot != null && inputSlot.getInfo().getRole() == JIPipeDataSlotRole.Data) {
            target.add(new ViewOnlyMenuItem("<html>One-to-Many processing<br/><small>The data within this slot is processed one-by-one (1 input can produce N outputs)</small>",
                    UIUtils.getIconFromResources("actions/1-to-n.png")));
        }
    }

    /**
     * A pass-through variant for iterating algorithms.
     * Passes the iteration step to the single output
     *
     * @param progressInfo  progress info
     * @param iterationStep the iteration step
     */
    protected void runPassThrough(JIPipeProgressInfo progressInfo, JIPipeSingleIterationStep iterationStep) {
        progressInfo.log("Passing trough (via dynamic pass-through)");
        iterationStep.addOutputData(getFirstOutputSlot(), iterationStep.getInputData(getFirstInputSlot(), JIPipeData.class, progressInfo), progressInfo);
    }

    /**
     * If true, allow the execution of an empty iteration step if all inputs are optional and are empty
     *
     * @return whether empty iteration steps are allowed
     */
    protected boolean isAllowEmptyIterationStep() {
        return false;
    }

    @Override
    public void runParameterSet(JIPipeGraphNodeRunContext runContext, JIPipeProgressInfo progressInfo, List<JIPipeTextAnnotation> parameterAnnotations) {

        if (isPassThrough() && canPassThrough()) {
            progressInfo.log("Data passed through to output");
            runPassThrough(runContext, progressInfo);
            return;
        }

        // Adaptive parameter backups
        JIPipeParameterTree tree = null;
        Map<String, Object> parameterBackups = new HashMap<>();
        if (getAdaptiveParameterSettings().isEnabled() && !getAdaptiveParameterSettings().getOverriddenParameters().isEmpty()) {
            tree = new JIPipeParameterTree(this);
            for (Map.Entry<String, JIPipeParameterAccess> entry : tree.getParameters().entrySet()) {
                parameterBackups.put(entry.getKey(), entry.getValue().get(Object.class));
            }
        }

        List<JIPipeSingleIterationStep> iterationSteps;

        // Special case: No input slots
        if (getDataInputSlotCount() == 0) {
            if (progressInfo.isCancelled())
                return;
            JIPipeSingleIterationStep iterationStep = new JIPipeSingleIterationStep(this);
            iterationStep.addMergedTextAnnotations(parameterAnnotations, iterationStepGenerationSettings.getAnnotationMergeStrategy());
            uploadAdaptiveParameters(iterationStep, tree, parameterBackups, progressInfo);

            iterationSteps = new ArrayList<>();
            iterationSteps.add(iterationStep);
        } else if (getDataInputSlotCount() == 1) {

            boolean withLimit = iterationStepGenerationSettings.getLimit().isEnabled();
            IntegerRange limit = iterationStepGenerationSettings.getLimit().getContent();
            TIntSet allowedIndices = withLimit ? new TIntHashSet(limit.getIntegers(0, getFirstInputSlot().getRowCount(), new JIPipeExpressionVariablesMap())) : null;

            iterationSteps = new ArrayList<>();
            for (int row = 0; row < getFirstInputSlot().getRowCount(); row++) {
                if (progressInfo.isCancelled())
                    break;
                if (withLimit && !allowedIndices.contains(row))
                    continue;
                JIPipeSingleIterationStep iterationStep = new JIPipeSingleIterationStep(this);
                iterationStep.setInputData(getFirstInputSlot(), row);
                iterationStep.addMergedTextAnnotations(parameterAnnotations, iterationStepGenerationSettings.getAnnotationMergeStrategy());
                iterationStep.addMergedTextAnnotations(getFirstInputSlot().getTextAnnotations(row), iterationStepGenerationSettings.getAnnotationMergeStrategy());
                iterationStep.addMergedDataAnnotations(getFirstInputSlot().getDataAnnotations(row), iterationStepGenerationSettings.getDataAnnotationMergeStrategy());
                uploadAdaptiveParameters(iterationStep, tree, parameterBackups, progressInfo);
                iterationSteps.add(iterationStep);
            }
        } else {
            // First generate merging iteration steps
            List<JIPipeMultiIterationStep> mergingDataBatches = generateDataBatchesGenerationResult(getNonParameterInputSlots(), progressInfo).getDataBatches();

            // Check for incomplete batches
            List<JIPipeMultiIterationStep> incomplete = new ArrayList<>();
            for (JIPipeMultiIterationStep iterationStep : mergingDataBatches) {
                if (iterationStep.isIncomplete()) {
                    incomplete.add(iterationStep);
                    progressInfo.log("[WARN] INCOMPLETE DATA BATCH FOUND: " + iterationStep);
                }
            }
            if (iterationStepGenerationSettings.isSkipIncompleteDataSets()) {
                if (!incomplete.isEmpty() && iterationStepGenerationSettings.isSkipIncompleteDataSets()) {
                    progressInfo.log("[WARN] SKIPPING INCOMPLETE DATA BATCHES AS REQUESTED");
                    mergingDataBatches.removeAll(incomplete);
                }
            } else {
                for (JIPipeMultiIterationStep batch : mergingDataBatches) {
                    if (progressInfo.isCancelled())
                        break;
                    if (batch.isIncomplete()) {
                        throw new JIPipeValidationRuntimeException(new JIPipeValidationReportEntry(JIPipeValidationReportEntryLevel.Error, new GraphNodeValidationReportContext(this),
                                "Incomplete data set found!",
                                "The algorithm needs to assign input a unique data set via annotations, but there is " +
                                        "not a data set for each input slot.",
                                "Please check the input of the algorithm by running the quick run on each input algorithm. " +
                                        "You can also choose to skip incomplete data sets, although you might lose data in those cases."));
                    }
                }
            }
            // Convert to single batch and attach parameters
            iterationSteps = JIPipeMultiIterationStepGenerator.convertMergingToSingleDataBatches(mergingDataBatches);
            if (iterationSteps != null) {
                for (JIPipeSingleIterationStep iterationStep : iterationSteps) {
                    iterationStep.addMergedTextAnnotations(parameterAnnotations, iterationStepGenerationSettings.getAnnotationMergeStrategy());
                }
            }
        }

        // Handle case: All optional input, no data
        if ((iterationSteps == null || iterationSteps.isEmpty()) && getDataInputSlots().stream().allMatch(slot -> slot.getInfo().isOptional() && slot.isEmpty())) {

            // Check if we even have data
            boolean hasData = false;
            for (JIPipeInputDataSlot slot : getDataInputSlots()) {
                if (!slot.isEmpty()) {
                    hasData = true;
                }
            }

            if (!hasData && isAllowEmptyIterationStep()) {
                progressInfo.log("Fully empty, but node allowed empty iteration steps -> unlocking");
                hasData = true;
            }

            if (hasData) {
                progressInfo.log("Generating dummy iteration step because of the [all inputs empty optional] condition");
                // Generate a dummy batch
                JIPipeSingleIterationStep iterationStep = new JIPipeSingleIterationStep(this);
                iterationStep.addMergedTextAnnotations(parameterAnnotations, JIPipeTextAnnotationMergeMode.Merge);
                uploadAdaptiveParameters(iterationStep, tree, parameterBackups, progressInfo);
                iterationSteps = new ArrayList<>();
                iterationSteps.add(iterationStep);
            } else {
                progressInfo.log("Nothing to do (all slots empty)");
                iterationSteps = new ArrayList<>();
            }
        }

        if (iterationSteps == null) {
            throw new JIPipeValidationRuntimeException(new JIPipeValidationReportEntry(JIPipeValidationReportEntryLevel.Error, new GraphNodeValidationReportContext(this),
                    "Unable to split data into batches!",
                    "The algorithm needs to assign input a unique data set via annotations, but there are either missing elements or multiple data per slot.",
                    "Please check the input of the algorithm by running the quick run on each input algorithm. " +
                            "Try to switch to the 'Iteration stepes' tab to preview how data is split into batches."));
        }

        // Execute the workload
        boolean hasAdaptiveParameters = getAdaptiveParameterSettings().isEnabled() && !getAdaptiveParameterSettings().getOverriddenParameters().isEmpty();
        JIPipeRuntimePartition partition = runContext.getGraphRun().getRuntimePartition(getRuntimePartition());

        // Determine if we should enable parallelization
        boolean doParallelization = shouldDoParallelization(runContext, progressInfo, iterationSteps, hasAdaptiveParameters, partition);

        if (!doParallelization) {
            for (int i = 0; i < iterationSteps.size(); i++) {
                if (progressInfo.isCancelled())
                    return;
                JIPipeProgressInfo slotProgress = progressInfo.resolveAndLog("Data row", i, iterationSteps.size());
                uploadAdaptiveParameters(iterationSteps.get(i), tree, parameterBackups, progressInfo);
                if (isPassThrough()) {
                    runPassThrough(slotProgress, iterationSteps.get(i));
                } else {
                    runIteration(iterationSteps.get(i), new JIPipeMutableIterationContext(i, iterationSteps.size()), runContext, slotProgress);
                }
            }
        } else {
            List<Runnable> tasks = new ArrayList<>();
            final int numIterationSteps = iterationSteps.size();
            for (int i = 0; i < iterationSteps.size(); i++) {
                int iterationStepIndex = i;
                JIPipeParameterTree finalTree = tree;
                List<JIPipeSingleIterationStep> finalDataBatches = iterationSteps;
                tasks.add(() -> {
                    if (progressInfo.isCancelled())
                        return;
                    JIPipeProgressInfo slotProgress = progressInfo.resolveAndLog("Data row", iterationStepIndex, finalDataBatches.size());
                    uploadAdaptiveParameters(finalDataBatches.get(iterationStepIndex), finalTree, parameterBackups, progressInfo);
                    if (isPassThrough()) {
                        runPassThrough(slotProgress, finalDataBatches.get(iterationStepIndex));
                    } else {
                        runIteration(finalDataBatches.get(iterationStepIndex), new JIPipeMutableIterationContext(iterationStepIndex, numIterationSteps), runContext, slotProgress);
                    }
                });
            }

            JIPipeFixedThreadPool threadPool = runContext.getThreadPool();
            if(threadPool == null || threadPool.getMaxThreads() < getLocalParallelizationNumThreads().getContent()) {
                progressInfo.log("Creating new thread pool with " + getLocalParallelizationNumThreads().getContent() + " threads");
                threadPool = new JIPipeFixedThreadPool(getLocalParallelizationNumThreads().getContent());
            }

            try {
                progressInfo.log(String.format("Running %d batches (batch size %d) in parallel. Available threads = %d",
                        tasks.size(),
                        getParallelizationBatchSize(),
                        threadPool.getMaxThreads()));
                for (Future<Exception> batch : threadPool.scheduleBatches(tasks, getParallelizationBatchSize())) {
                    try {
                        Exception exception = batch.get();
                        if (exception != null)
                            throw new RuntimeException(exception);
                    } catch (InterruptedException | ExecutionException e) {
                        throw new RuntimeException(e);
                    }
                }
            }
            finally {
                if(threadPool != runContext.getThreadPool()) {
                    threadPool.shutdown();
                }
            }
        }
    }

    private boolean shouldDoParallelization(JIPipeGraphNodeRunContext runContext, JIPipeProgressInfo progressInfo, List<JIPipeSingleIterationStep> iterationSteps, boolean hasAdaptiveParameters, JIPipeRuntimePartition partition) {
        boolean doParallelization = iterationSteps.size() > 1 && supportsParallelization();
        if(doParallelization && hasAdaptiveParameters) {
            doParallelization = false;
            progressInfo.log("[INFO] Parallelization was DISABLED due to usage of adaptive parameters");
        }
        if(doParallelization) {
            if(localParallelizationNumThreads.isEnabled()) {
                if(localParallelizationNumThreads.getContent() > 1) {
                    progressInfo.log("[INFO] Local node-wide parallelization enabled via parameter");
                }
                else {
                    doParallelization = false;
                    progressInfo.log("[INFO] Local node-wide parallelization DISABLED, as number of threads is " + localParallelizationNumThreads.getContent());
                }
            }
            else if(!partition.isEnableParallelization() || runContext.getThreadPool() == null
                    || runContext.getThreadPool().getMaxThreads() <= 1) {
                doParallelization = false;
                progressInfo.log("[INFO] Parallelization was DISABLED due to run or partition settings");
            }
        }
        return doParallelization;
    }

    private void uploadAdaptiveParameters(JIPipeSingleIterationStep iterationStep, JIPipeParameterTree tree, Map<String, Object> parameterBackups, JIPipeProgressInfo progressInfo) {
        JIPipeExpressionVariablesMap expressionVariables = new JIPipeExpressionVariablesMap();

        // Upload common variables
        expressionVariables.putCommonVariables(iterationStep);

        for (StringQueryExpressionAndStringPairParameter overriddenParameter : getAdaptiveParameterSettings().getOverriddenParameters()) {
            String key = overriddenParameter.getValue();
            JIPipeParameterAccess target = tree.getParameters().getOrDefault(key, null);
            if (target == null) {
                progressInfo.log("Unable to find parameter '" + key + "' in " + getName() + "! Ignoring.");
                continue;
            }
            Object oldValue = parameterBackups.get(key);
            expressionVariables.put("default", oldValue);
            Object newValue = overriddenParameter.getKey().evaluate(expressionVariables);
            if (Objects.equals(newValue, oldValue)) {
                // No changes
                if (getAdaptiveParameterSettings().isAttachParameterAnnotations() && !getAdaptiveParameterSettings().isAttachOnlyNonDefaultParameterAnnotations()) {
                    annotateWithParameter(iterationStep, key, target, newValue);
                }
            } else if (target.getFieldClass().isAssignableFrom(newValue.getClass())) {
                // Set new value
                progressInfo.log("Set adaptive parameter " + key + " to value " + JsonUtils.toJsonString(newValue));
                target.set(newValue);
                if (getAdaptiveParameterSettings().isAttachParameterAnnotations()) {
                    annotateWithParameter(iterationStep, key, target, newValue);
                }
            } else {
                // Is JSON. Parse
                progressInfo.log("Set adaptive parameter " + key + " to value " + newValue);
                try {
                    newValue = JsonUtils.getObjectMapper().readerFor(target.getFieldClass()).readValue(StringUtils.nullToEmpty(newValue));
                } catch (JsonProcessingException e) {
                    throw new RuntimeException(e);
                }
                target.set(newValue);
                if (getAdaptiveParameterSettings().isAttachParameterAnnotations()) {
                    annotateWithParameter(iterationStep, key, target, newValue);
                }
            }
        }
    }

    private void annotateWithParameter(JIPipeSingleIterationStep iterationStep, String key, JIPipeParameterAccess target, Object newValue) {
        String name;
        if (getAdaptiveParameterSettings().isParameterAnnotationsUseInternalNames())
            name = key;
        else
            name = target.getName();
        name = getAdaptiveParameterSettings().getParameterAnnotationsPrefix() + name;
        String value = JsonUtils.toJsonString(newValue);
        iterationStep.addMergedTextAnnotation(new JIPipeTextAnnotation(name, value), JIPipeTextAnnotationMergeMode.Merge);
    }

    @SetJIPipeDocumentation(name = "Adaptive parameters", description = "You can use the following settings to generate parameter values for each iteration step based on annotations.")
    @JIPipeParameter(value = "jipipe:adaptive-parameters", hidden = true,
            iconURL = ResourceUtils.RESOURCE_BASE_PATH + "/icons/actions/insert-function.png",
            iconDarkURL = ResourceUtils.RESOURCE_BASE_PATH + "/dark/icons/actions/insert-function.png")
    public JIPipeAdaptiveParameterSettings getAdaptiveParameterSettings() {
        return adaptiveParameterSettings;
    }

    @SetJIPipeDocumentation(name = "Input management", description = "This algorithm can have multiple inputs. This means that JIPipe has to match incoming data into batches via metadata annotations. " +
            "The following settings allow you to control which columns are used as reference to organize data.")
    @JIPipeParameter(value = "jipipe:data-batch-generation", collapsed = true,
            iconURL = ResourceUtils.RESOURCE_BASE_PATH + "/icons/actions/package.png",
            iconDarkURL = ResourceUtils.RESOURCE_BASE_PATH + "/dark/icons/actions/package.png")
    public JIPipeIteratingAlgorithmIterationStepGenerationSettings getDataBatchGenerationSettings() {
        return iterationStepGenerationSettings;
    }

    @SetJIPipeDocumentation(name = "Local parallelization", description = "If enabled, override the partition- and run-wide parallelization settings and apply parallelization only on this node.")
    @JIPipeParameter(value = "jipipe:local-parallelization-num-threads", pinned = true)
    public OptionalIntegerParameter getLocalParallelizationNumThreads() {
        return localParallelizationNumThreads;
    }

    @JIPipeParameter("jipipe:local-parallelization-num-threads")
    public void setLocalParallelizationNumThreads(OptionalIntegerParameter localParallelizationNumThreads) {
        this.localParallelizationNumThreads = localParallelizationNumThreads;
    }

    @Override
    public boolean supportsParallelization() {
        return false;
    }

    @Override
    public int getParallelizationBatchSize() {
        return 1;
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
        if (ParameterUtils.isHiddenLocalParameter(tree, access, "jipipe:local-parallelization-num-threads")) {
            return false;
        }
        if("jipipe:local-parallelization-num-threads".equals(access.getKey()) && !supportsParallelization()) {
            return false;
        }
        return super.isParameterUIVisible(tree, access);
    }

    /**
     * Runs code on one data row
     *
     * @param iterationStep    The data interface
     * @param iterationContext The iteration context
     * @param runContext       the run context
     * @param progressInfo     the progress info from the run
     */
    protected abstract void runIteration(JIPipeSingleIterationStep iterationStep, JIPipeIterationContext iterationContext, JIPipeGraphNodeRunContext runContext, JIPipeProgressInfo progressInfo);

}
