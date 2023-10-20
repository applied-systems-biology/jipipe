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

import com.fasterxml.jackson.core.JsonProcessingException;
import gnu.trove.set.TIntSet;
import gnu.trove.set.hash.TIntHashSet;
import org.hkijena.jipipe.api.JIPipeDataBatchGenerationResult;
import org.hkijena.jipipe.api.JIPipeDocumentation;
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
import org.hkijena.jipipe.api.validation.*;
import org.hkijena.jipipe.api.validation.contexts.GraphNodeValidationReportContext;
import org.hkijena.jipipe.extensions.expressions.ExpressionVariables;
import org.hkijena.jipipe.extensions.parameters.library.pairs.StringQueryExpressionAndStringPairParameter;
import org.hkijena.jipipe.extensions.parameters.library.primitives.ranges.IntegerRange;
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

    private boolean parallelizationEnabled = true;
    private JIPipeSimpleIteratingAlgorithmIterationStepGenerationSettings iterationStepGenerationSettings = new JIPipeSimpleIteratingAlgorithmIterationStepGenerationSettings();
    private JIPipeAdaptiveParameterSettings adaptiveParameterSettings = new JIPipeAdaptiveParameterSettings();

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
        this.parallelizationEnabled = other.parallelizationEnabled;
        adaptiveParameterSettings.setNode(this);
        registerSubParameter(iterationStepGenerationSettings);
        registerSubParameter(adaptiveParameterSettings);
    }

    /**
     * A pass-through variant for iterating algorithms.
     * Passes the data batch to the single output
     *
     * @param progressInfo progress info
     * @param iterationStep    the data batch
     */
    protected void runPassThrough(JIPipeProgressInfo progressInfo, JIPipeSingleIterationStep iterationStep) {
        progressInfo.log("Passing trough (via dynamic pass-through)");
        iterationStep.addOutputData(getFirstOutputSlot(), iterationStep.getInputData(getFirstInputSlot(), JIPipeData.class, progressInfo), progressInfo);
    }

    @Override
    public void runParameterSet(JIPipeProgressInfo progressInfo, List<JIPipeTextAnnotation> parameterAnnotations) {
        if (getDataInputSlotCount() > 1)
            throw new JIPipeValidationRuntimeException(new JIPipeValidationReportEntry(JIPipeValidationReportEntryLevel.Error, new GraphNodeValidationReportContext(this),
                    "Too many input slots for JIPipeSimpleIteratingAlgorithm!",
                    "The developer of this algorithm chose the wrong node type. The one that was selected only supports at most one input.",
                    "Please contact the plugin developers and tell them to let algorithm '" + getInfo().getId() + "' inherit from 'JIPipeIteratingAlgorithm' instead."));
        if (isPassThrough() && canPassThrough()) {
            progressInfo.log("Data passed through to output");
            runPassThrough(progressInfo);
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

        // Generate data batches
        if (getDataInputSlotCount() == 0) {
            JIPipeSingleIterationStep iterationStep = new JIPipeSingleIterationStep(this);
            iterationStep.addMergedTextAnnotations(parameterAnnotations, JIPipeTextAnnotationMergeMode.Merge);
            uploadAdaptiveParameters(iterationStep, tree, parameterBackups, progressInfo);

            iterationSteps = new ArrayList<>();
            iterationSteps.add(iterationStep);
        } else {

            boolean withLimit = iterationStepGenerationSettings.getLimit().isEnabled();
            IntegerRange limit = iterationStepGenerationSettings.getLimit().getContent();
            TIntSet allowedIndices = withLimit ? new TIntHashSet(limit.getIntegers(0, getFirstInputSlot().getRowCount(), new ExpressionVariables())) : null;


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
            progressInfo.log("Generating dummy data batch because of the [all inputs empty optional] condition");
            // Generate a dummy batch
            JIPipeSingleIterationStep iterationStep = new JIPipeSingleIterationStep(this);
            iterationStep.addMergedTextAnnotations(parameterAnnotations, JIPipeTextAnnotationMergeMode.Merge);
            uploadAdaptiveParameters(iterationStep, tree, parameterBackups, progressInfo);
            iterationSteps.add(iterationStep);
        }

        // Execute the workload
        boolean hasAdaptiveParameters = getAdaptiveParameterSettings().isEnabled() && !getAdaptiveParameterSettings().getOverriddenParameters().isEmpty();
        final int numIterationSteps = iterationSteps.size();
        if (!supportsParallelization() || !isParallelizationEnabled() || getThreadPool() == null || getThreadPool().getMaxThreads() <= 1 || iterationSteps.size() <= 1 || hasAdaptiveParameters) {
            for (int i = 0; i < iterationSteps.size(); i++) {
                if (progressInfo.isCancelled())
                    return;
                JIPipeProgressInfo slotProgress = progressInfo.resolveAndLog("Data row", i, iterationSteps.size());
                uploadAdaptiveParameters(iterationSteps.get(i), tree, parameterBackups, progressInfo);
                if (isPassThrough()) {
                    runPassThrough(slotProgress, iterationSteps.get(i));
                } else {
                    runIteration(iterationSteps.get(i), new JIPipeMutableIterationContext(i, numIterationSteps), slotProgress);
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
                        runIteration(iterationSteps.get(iterationStepIndex), new JIPipeMutableIterationContext(iterationStepIndex, numIterationSteps), slotProgress);
                    }
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

    private void uploadAdaptiveParameters(JIPipeSingleIterationStep iterationStep, JIPipeParameterTree tree, Map<String, Object> parameterBackups, JIPipeProgressInfo progressInfo) {
        ExpressionVariables expressionVariables = new ExpressionVariables();
        for (JIPipeTextAnnotation annotation : iterationStep.getMergedTextAnnotations().values()) {
            expressionVariables.put(annotation.getName(), annotation.getValue());
        }
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
    public void reportValidity(JIPipeValidationReportContext context, JIPipeValidationReport report) {
        if (getDataInputSlots().size() > 1) {
            report.add(new JIPipeValidationReportEntry(JIPipeValidationReportEntryLevel.Error, context,
                    "Error in source code detected!",
                    "The developer of this algorithm chose the wrong node type. The one that was selected only supports at most one input.",
                    "Please contact the plugin developers and tell them to let algorithm '" + getInfo().getId() + "' inherit from 'JIPipeIteratingAlgorithm' instead."));
        }
    }

    /**
     * Runs code on one data row
     *
     * @param iterationStep    The data interface
     * @param iterationContext
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

    @JIPipeDocumentation(name = "Input management", description = "This algorithm has one input and will iterate through each row of its input and apply the workload. " +
            "Use following settings to control which data batches are generated.")
    @JIPipeParameter(value = "jipipe:data-batch-generation",
            hidden = true,
            iconURL = ResourceUtils.RESOURCE_BASE_PATH + "/icons/actions/package.png",
            iconDarkURL = ResourceUtils.RESOURCE_BASE_PATH + "/dark/icons/actions/package.png")
    public JIPipeSimpleIteratingAlgorithmIterationStepGenerationSettings getDataBatchGenerationSettings() {
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
    public JIPipeIterationStepGenerationSettings getGenerationSettingsInterface() {
        return iterationStepGenerationSettings;
    }

    @JIPipeDocumentation(name = "Adaptive parameters", description = "You can use the following settings to generate parameter values for each data batch based on annotations.")
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
        TIntSet allowedIndices = withLimit ? new TIntHashSet(limit.getIntegers(0, slot.getRowCount(), new ExpressionVariables())) : null;
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
