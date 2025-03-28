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
import org.hkijena.jipipe.api.AddJIPipeDocumentationDescription;
import org.hkijena.jipipe.api.JIPipeDataBatchGenerationResult;
import org.hkijena.jipipe.api.JIPipeProgressInfo;
import org.hkijena.jipipe.api.SetJIPipeDocumentation;
import org.hkijena.jipipe.api.annotation.JIPipeTextAnnotation;
import org.hkijena.jipipe.api.annotation.JIPipeTextAnnotationMergeMode;
import org.hkijena.jipipe.api.data.JIPipeData;
import org.hkijena.jipipe.api.data.JIPipeInputDataSlot;
import org.hkijena.jipipe.api.data.JIPipeSlotConfiguration;
import org.hkijena.jipipe.api.nodes.*;
import org.hkijena.jipipe.api.nodes.iterationstep.*;
import org.hkijena.jipipe.api.parameters.JIPipeParameter;
import org.hkijena.jipipe.api.parameters.JIPipeParameterAccess;
import org.hkijena.jipipe.api.parameters.JIPipeParameterCollection;
import org.hkijena.jipipe.api.parameters.JIPipeParameterTree;
import org.hkijena.jipipe.plugins.expressions.JIPipeExpressionVariablesMap;
import org.hkijena.jipipe.plugins.expressions.StringQueryExpression;
import org.hkijena.jipipe.plugins.parameters.library.pairs.StringQueryExpressionAndStringPairParameter;
import org.hkijena.jipipe.utils.ParameterUtils;
import org.hkijena.jipipe.utils.ResourceUtils;
import org.hkijena.jipipe.utils.StringUtils;
import org.hkijena.jipipe.utils.json.JsonUtils;

import java.util.*;

/**
 * An {@link JIPipeAlgorithm} that applies a single iteration with a iteration step containing all data.
 */
@AddJIPipeDocumentationDescription(description = "This algorithm merges all annotations and data annotations. " +
        "Use the iteration step settings to determine how annotations and data annotations are merged")
public abstract class JIPipeSingleIterationAlgorithm extends JIPipeParameterSlotAlgorithm implements JIPipeParallelizedAlgorithm, JIPipeIterationStepAlgorithm, JIPipeAdaptiveParametersAlgorithm {

    private boolean parallelizationEnabled = true;
    private JIPipeSingleIterationAlgorithmIterationStepGenerationSettings iterationStepGenerationSettings = new JIPipeSingleIterationAlgorithmIterationStepGenerationSettings();
    private JIPipeAdaptiveParameterSettings adaptiveParameterSettings = new JIPipeAdaptiveParameterSettings();


    /**
     * Creates a new instance
     *
     * @param info              Algorithm info
     * @param slotConfiguration Slot configuration override
     */
    public JIPipeSingleIterationAlgorithm(JIPipeNodeInfo info, JIPipeSlotConfiguration slotConfiguration) {
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
    public JIPipeSingleIterationAlgorithm(JIPipeNodeInfo info) {
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
    public JIPipeSingleIterationAlgorithm(JIPipeSingleIterationAlgorithm other) {
        super(other);
        this.iterationStepGenerationSettings = new JIPipeSingleIterationAlgorithmIterationStepGenerationSettings(other.iterationStepGenerationSettings);
        this.adaptiveParameterSettings = new JIPipeAdaptiveParameterSettings(other.adaptiveParameterSettings);
        this.parallelizationEnabled = other.parallelizationEnabled;
        adaptiveParameterSettings.setNode(this);
        registerSubParameter(iterationStepGenerationSettings);
        registerSubParameter(adaptiveParameterSettings);
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
    public JIPipeIterationStepGenerationSettings getGenerationSettingsInterface() {
        return iterationStepGenerationSettings;
    }

    /**
     * Returns annotation types that should be ignored by the internal logic.
     * Use this if you have some counting/sorting annotation that should not be included into the set of annotations used to match data.
     *
     * @return annotation types that should be ignored by the internal logic
     */
    public Set<String> getIgnoredAnnotationColumns() {
        return Collections.emptySet();
    }

    @Override
    public JIPipeDataBatchGenerationResult generateDataBatchesGenerationResult(List<JIPipeInputDataSlot> slots, JIPipeProgressInfo progressInfo) {
        JIPipeMultiIterationStepGenerator builder = new JIPipeMultiIterationStepGenerator();
        builder.setNode(this);
        builder.setSlots(slots);
        builder.setApplyMerging(true);
        builder.setAnnotationMergeStrategy(iterationStepGenerationSettings.getAnnotationMergeStrategy());
        builder.setDataAnnotationMergeStrategy(iterationStepGenerationSettings.getDataAnnotationMergeStrategy());
        builder.setReferenceColumns(JIPipeColumMatching.MergeAll, new StringQueryExpression());

        List<JIPipeMultiIterationStep> iterationSteps = builder.build(progressInfo);

        // Generate result object
        JIPipeDataBatchGenerationResult result = new JIPipeDataBatchGenerationResult();
        result.setDataBatches(iterationSteps);
        result.setReferenceTextAnnotationColumns(builder.getReferenceColumns());

        return result;
    }

    /**
     * A pass-through variant for merging algorithms.
     * Passes the iteration step to the single output
     *
     * @param progressInfo  progress info
     * @param iterationStep the iteration step
     */
    protected void runPassThrough(JIPipeProgressInfo progressInfo, JIPipeMultiIterationStep iterationStep) {
        progressInfo.log("Passing trough (via dynamic pass-through)");
        for (int row : iterationStep.getInputSlotRows().get(getFirstInputSlot())) {
            iterationStep.addOutputData(getFirstOutputSlot(), getFirstInputSlot().getData(row, JIPipeData.class, progressInfo), progressInfo);
        }
    }

    @Override
    public void runParameterSet(JIPipeGraphNodeRunContext runContext, JIPipeProgressInfo progressInfo, List<JIPipeTextAnnotation> parameterAnnotations) {

        // Adaptive parameter backups
        Map<String, Object> parameterBackups = new HashMap<>();
        JIPipeParameterTree tree = null;
        if (getAdaptiveParameterSettings().isEnabled() && !getAdaptiveParameterSettings().getOverriddenParameters().isEmpty()) {
            tree = new JIPipeParameterTree(this);
            for (Map.Entry<String, JIPipeParameterAccess> entry : tree.getParameters().entrySet()) {
                parameterBackups.put(entry.getKey(), entry.getValue().get(Object.class));
            }
        }

        // Special case: No input slots
        boolean doEmptyIterationStep = false;
        if(getDataInputSlotCount() == 0) {
            doEmptyIterationStep = true;
        }
        else if(isAllowEmptyIterationStep() && getDataInputSlots().stream().allMatch(slot -> slot.getInfo().isOptional() && slot.isEmpty())) {
            doEmptyIterationStep = true;
        }

        if (doEmptyIterationStep) {
            if (progressInfo.isCancelled())
                return;
            final int row = 0;
            JIPipeProgressInfo slotProgress = progressInfo.resolveAndLog("Data row", row, 1);
            JIPipeMultiIterationStep iterationStep = new JIPipeMultiIterationStep(this);
            iterationStep.addMergedTextAnnotations(parameterAnnotations, iterationStepGenerationSettings.getAnnotationMergeStrategy());
            uploadAdaptiveParameters(iterationStep, tree, parameterBackups, progressInfo);
            if (isPassThrough()) {
                runPassThrough(slotProgress, iterationStep);
            } else {
                runIteration(iterationStep, new JIPipeMutableIterationContext(row, 1), runContext, slotProgress);
            }
            return;
        }

        List<JIPipeMultiIterationStep> iterationSteps = generateDataBatchesGenerationResult(getNonParameterInputSlots(), progressInfo).getDataBatches();
        for (JIPipeMultiIterationStep iterationStep : iterationSteps) {
            iterationStep.addMergedTextAnnotations(parameterAnnotations, iterationStepGenerationSettings.getAnnotationMergeStrategy());
        }

        // There should be only one batch, but we iterate anyway
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
    }

    private void uploadAdaptiveParameters(JIPipeMultiIterationStep iterationStep, JIPipeParameterTree tree, Map<String, Object> parameterBackups, JIPipeProgressInfo progressInfo) {
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

    private void annotateWithParameter(JIPipeMultiIterationStep iterationStep, String key, JIPipeParameterAccess target, Object newValue) {
        String name;
        if (getAdaptiveParameterSettings().isParameterAnnotationsUseInternalNames())
            name = key;
        else
            name = target.getName();
        name = getAdaptiveParameterSettings().getParameterAnnotationsPrefix() + name;
        String value = JsonUtils.toJsonString(newValue);
        iterationStep.addMergedTextAnnotation(new JIPipeTextAnnotation(name, value), JIPipeTextAnnotationMergeMode.Merge);
    }

    /**
     * Runs code on one data row
     *
     * @param iterationStep    The data interface
     * @param iterationContext The iteration context
     * @param runContext       the run context
     * @param progressInfo     the progress from the run
     */
    protected abstract void runIteration(JIPipeMultiIterationStep iterationStep, JIPipeIterationContext iterationContext, JIPipeGraphNodeRunContext runContext, JIPipeProgressInfo progressInfo);

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
        return super.isParameterUIVisible(tree, access);
    }

    @SetJIPipeDocumentation(name = "Input management", description = "This algorithm can have multiple inputs. " +
            "This node merges all data of all inputs into one batch. Here you can determine how annotations and data annotations are copied to the output.")
    @JIPipeParameter(value = "jipipe:data-batch-generation", collapsed = true,
            iconURL = ResourceUtils.RESOURCE_BASE_PATH + "/icons/actions/package.png",
            iconDarkURL = ResourceUtils.RESOURCE_BASE_PATH + "/dark/icons/actions/package.png")
    public JIPipeSingleIterationAlgorithmIterationStepGenerationSettings getDataBatchGenerationSettings() {
        return iterationStepGenerationSettings;
    }

    @SetJIPipeDocumentation(name = "Adaptive parameters", description = "You can use the following settings to generate parameter values for each iteration step based on annotations.")
    @JIPipeParameter(value = "jipipe:adaptive-parameters", hidden = true,
            iconURL = ResourceUtils.RESOURCE_BASE_PATH + "/icons/actions/insert-function.png",
            iconDarkURL = ResourceUtils.RESOURCE_BASE_PATH + "/dark/icons/actions/insert-function.png")
    public JIPipeAdaptiveParameterSettings getAdaptiveParameterSettings() {
        return adaptiveParameterSettings;
    }

}
