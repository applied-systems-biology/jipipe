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
import gnu.trove.set.TIntSet;
import gnu.trove.set.hash.TIntHashSet;
import org.hkijena.jipipe.api.JIPipeDataBatchGenerationResult;
import org.hkijena.jipipe.api.JIPipeFixedThreadPool;
import org.hkijena.jipipe.api.JIPipeProgressInfo;
import org.hkijena.jipipe.api.SetJIPipeDocumentation;
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
import org.hkijena.jipipe.api.runtimepartitioning.JIPipeRuntimePartition;
import org.hkijena.jipipe.api.validation.*;
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
 * This is a simplified version of {@link JIPipeIteratingAlgorithm} that assumes that there is only one or zero input slots.
 * An error is thrown if there are more than one input slots.
 */
public abstract class JIPipeSimpleIteratingAlgorithm extends JIPipeParameterSlotAlgorithm implements JIPipeParallelizedAlgorithm, JIPipeIterationStepAlgorithm, JIPipeAdaptiveParametersAlgorithm {
    private JIPipeSimpleIteratingAlgorithmIterationStepGenerationSettings iterationStepGenerationSettings = new JIPipeSimpleIteratingAlgorithmIterationStepGenerationSettings();
    private JIPipeAdaptiveParameterSettings adaptiveParameterSettings = new JIPipeAdaptiveParameterSettings();
    private OptionalIntegerParameter localParallelizationNumThreads = new OptionalIntegerParameter(false, 1);

    /**
     * Creates a new instance
     *
     * @param info              Algorithm info
     * @param slotConfiguration Slot configuration override
     */
    public JIPipeSimpleIteratingAlgorithm(JIPipeNodeInfo info, JIPipeSlotConfiguration slotConfiguration) {
        super(info, slotConfiguration);
        adaptiveParameterSettings.setNode(this);
        registerSubParameter(iterationStepGenerationSettings);
        registerSubParameter(adaptiveParameterSettings);
    }

    /**
     * Creates a new instance
     *
     * @param info Algorithm info
     */
    public JIPipeSimpleIteratingAlgorithm(JIPipeNodeInfo info) {
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
    public JIPipeSimpleIteratingAlgorithm(JIPipeSimpleIteratingAlgorithm other) {
        super(other);
        this.iterationStepGenerationSettings = new JIPipeSimpleIteratingAlgorithmIterationStepGenerationSettings(other.iterationStepGenerationSettings);
        this.adaptiveParameterSettings = new JIPipeAdaptiveParameterSettings(other.adaptiveParameterSettings);
        this.localParallelizationNumThreads = new OptionalIntegerParameter(other.localParallelizationNumThreads);
        adaptiveParameterSettings.setNode(this);
        registerSubParameter(iterationStepGenerationSettings);
        registerSubParameter(adaptiveParameterSettings);
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
        if (getDataInputSlotCount() > 1)
            throw new JIPipeValidationRuntimeException(new JIPipeValidationReportEntry(JIPipeValidationReportEntryLevel.Error, new GraphNodeValidationReportContext(this),
                    "Too many input slots for JIPipeSimpleIteratingAlgorithm!",
                    "The developer of this algorithm chose the wrong node type. The one that was selected only supports at most one input.",
                    "Please contact the plugin developers and tell them to let algorithm '" + getInfo().getId() + "' inherit from 'JIPipeIteratingAlgorithm' instead."));
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

        // Generate iteration steps
        if (getDataInputSlotCount() == 0) {
            JIPipeSingleIterationStep iterationStep = new JIPipeSingleIterationStep(this);
            iterationStep.addMergedTextAnnotations(parameterAnnotations, JIPipeTextAnnotationMergeMode.Merge);
            uploadAdaptiveParameters(iterationStep, tree, parameterBackups, progressInfo);

            iterationSteps = new ArrayList<>();
            iterationSteps.add(iterationStep);
        } else {

            boolean withLimit = iterationStepGenerationSettings.getLimit().isEnabled();
            IntegerRange limit = iterationStepGenerationSettings.getLimit().getContent();
            TIntSet allowedIndices = withLimit ? new TIntHashSet(limit.getIntegers(0, getFirstInputSlot().getRowCount(), new JIPipeExpressionVariablesMap())) : null;


            iterationSteps = new ArrayList<>();

            for (int i = 0; i < getFirstInputSlot().getRowCount(); i++) {
                if (withLimit && !allowedIndices.contains(i))
                    continue;
                if (progressInfo.isCancelled())
                    return;
                JIPipeSingleIterationStep iterationStep = new JIPipeSingleIterationStep(this);
                iterationStep.setInputData(getFirstInputSlot(), i);
                iterationStep.addMergedTextAnnotations(getFirstInputSlot().getTextAnnotations(i), JIPipeTextAnnotationMergeMode.Merge);
                iterationStep.addMergedDataAnnotations(getFirstInputSlot().getDataAnnotations(i), JIPipeDataAnnotationMergeMode.MergeTables);
                iterationStep.addMergedTextAnnotations(parameterAnnotations, JIPipeTextAnnotationMergeMode.Merge);
                uploadAdaptiveParameters(iterationStep, tree, parameterBackups, progressInfo);

                iterationSteps.add(iterationStep);
            }
        }

        // Handle case: All optional input, no data
        if (iterationSteps.isEmpty() && getDataInputSlots().stream().allMatch(slot -> slot.getInfo().isOptional() && slot.isEmpty())) {
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
                iterationSteps.add(iterationStep);
            } else {
                progressInfo.log("Nothing to do (all slots empty)");
            }
        }

        // Execute the workload
        boolean hasAdaptiveParameters = getAdaptiveParameterSettings().isEnabled() && !getAdaptiveParameterSettings().getOverriddenParameters().isEmpty();
        final int numIterationSteps = iterationSteps.size();
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
                    runIteration(iterationSteps.get(i), new JIPipeMutableIterationContext(i, numIterationSteps), runContext, slotProgress);
                }
            }
        } else {
            List<Runnable> tasks = new ArrayList<>();
            for (int i = 0; i < iterationSteps.size(); i++) {
                int iterationStepIndex = i;
                JIPipeParameterTree finalTree = tree;
                tasks.add(() -> {
                    if (progressInfo.isCancelled())
                        return;
                    JIPipeProgressInfo slotProgress = progressInfo.resolveAndLog("Data row", iterationStepIndex, iterationSteps.size());
                    uploadAdaptiveParameters(iterationSteps.get(iterationStepIndex), finalTree, parameterBackups, progressInfo);
                    if (isPassThrough()) {
                        runPassThrough(slotProgress, iterationSteps.get(iterationStepIndex));
                    } else {
                        runIteration(iterationSteps.get(iterationStepIndex), new JIPipeMutableIterationContext(iterationStepIndex, numIterationSteps), runContext, slotProgress);
                    }
                });
            }

            JIPipeFixedThreadPool threadPool = runContext.getThreadPool();
            boolean isCustomThreadPool = false;
            if(threadPool == null) {
                int numThreads = Math.max(1, getLocalParallelizationNumThreads().getContent());
                progressInfo.log("Creating new thread pool with " + numThreads + " threads");
                threadPool = new JIPipeFixedThreadPool(numThreads);
                isCustomThreadPool = true;
            }

            progressInfo.log(String.format("Running %d batches (batch size %d) in parallel. Available threads = %d",
                    tasks.size(),
                    getParallelizationBatchSize(),
                    threadPool.getMaxThreads()));

            try {
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
                if(isCustomThreadPool) {
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
     * @param iterationContext the iteration context
     * @param runContext       the run context
     * @param progressInfo     the progress info from the run
     */
    protected abstract void runIteration(JIPipeSingleIterationStep iterationStep, JIPipeIterationContext iterationContext, JIPipeGraphNodeRunContext runContext, JIPipeProgressInfo progressInfo);

    @Override
    public boolean supportsParallelization() {
        return false;
    }

    @Override
    public int getParallelizationBatchSize() {
        return 1;
    }

    @SetJIPipeDocumentation(name = "Input management", description = "This algorithm has one input and will iterate through each row of its input and apply the workload. " +
            "Use following settings to control which iteration steps are generated.")
    @JIPipeParameter(value = "jipipe:data-batch-generation",
            iconURL = ResourceUtils.RESOURCE_BASE_PATH + "/icons/actions/package.png",
            iconDarkURL = ResourceUtils.RESOURCE_BASE_PATH + "/dark/icons/actions/package.png")
    public JIPipeSimpleIteratingAlgorithmIterationStepGenerationSettings getDataBatchGenerationSettings() {
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
        if("jipipe:local-parallelization-num-threads".equals(access.getKey()) && !supportsParallelization()) {
            return false;
        }
        return super.isParameterUIVisible(tree, access);
    }

    @Override
    public JIPipeIterationStepGenerationSettings getGenerationSettingsInterface() {
        return iterationStepGenerationSettings;
    }

    @SetJIPipeDocumentation(name = "Adaptive parameters", description = "You can use the following settings to generate parameter values for each iteration step based on annotations.")
    @JIPipeParameter(value = "jipipe:adaptive-parameters", hidden = true,
            iconURL = ResourceUtils.RESOURCE_BASE_PATH + "/icons/actions/insert-function.png",
            iconDarkURL = ResourceUtils.RESOURCE_BASE_PATH + "/dark/icons/actions/insert-function.png")
    public JIPipeAdaptiveParameterSettings getAdaptiveParameterSettings() {
        return adaptiveParameterSettings;
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
            batches.add(iterationStep);
        }

        // Generate result object
        JIPipeDataBatchGenerationResult result = new JIPipeDataBatchGenerationResult();
        result.setDataBatches(batches);

        return result;
    }

}
