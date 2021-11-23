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

import com.fasterxml.jackson.core.JsonProcessingException;
import org.hkijena.jipipe.api.JIPipeDocumentation;
import org.hkijena.jipipe.api.JIPipeProgressInfo;
import org.hkijena.jipipe.api.data.JIPipeAnnotation;
import org.hkijena.jipipe.api.data.JIPipeAnnotationMergeStrategy;
import org.hkijena.jipipe.api.data.JIPipeData;
import org.hkijena.jipipe.api.data.JIPipeDataSlot;
import org.hkijena.jipipe.api.data.JIPipeSlotConfiguration;
import org.hkijena.jipipe.api.parameters.JIPipeParameter;
import org.hkijena.jipipe.api.parameters.JIPipeParameterAccess;
import org.hkijena.jipipe.api.parameters.JIPipeParameterCollection;
import org.hkijena.jipipe.api.parameters.JIPipeParameterTree;
import org.hkijena.jipipe.extensions.expressions.ExpressionVariables;
import org.hkijena.jipipe.extensions.expressions.StringQueryExpression;
import org.hkijena.jipipe.extensions.parameters.pairs.StringQueryExpressionAndStringPairParameter;
import org.hkijena.jipipe.utils.ParameterUtils;
import org.hkijena.jipipe.utils.ResourceUtils;
import org.hkijena.jipipe.utils.StringUtils;
import org.hkijena.jipipe.utils.json.JsonUtils;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

/**
 * An {@link JIPipeAlgorithm} that applies a single iteration with a data batch containing all data.
 */
public abstract class JIPipeSingleIterationAlgorithm extends JIPipeParameterSlotAlgorithm implements JIPipeParallelizedAlgorithm, JIPipeDataBatchAlgorithm {

    public static final String SINGLE_ITERATION_ALGORITHM_DESCRIPTION = "This algorithm merges all annotations and data annotations. " +
            "Use the data batch settings to determine how annotations and data annotations are merged";

    private boolean parallelizationEnabled = true;
    private JIPipeSingleIterationAlgorithmDataBatchGenerationSettings dataBatchGenerationSettings = new JIPipeSingleIterationAlgorithmDataBatchGenerationSettings();
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
        registerSubParameter(dataBatchGenerationSettings);
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
        registerSubParameter(dataBatchGenerationSettings);
        registerSubParameter(adaptiveParameterSettings);
    }

    /**
     * Copies the algorithm
     *
     * @param other The original
     */
    public JIPipeSingleIterationAlgorithm(JIPipeSingleIterationAlgorithm other) {
        super(other);
        this.dataBatchGenerationSettings = new JIPipeSingleIterationAlgorithmDataBatchGenerationSettings(other.dataBatchGenerationSettings);
        this.adaptiveParameterSettings = new JIPipeAdaptiveParameterSettings(other.adaptiveParameterSettings);
        this.parallelizationEnabled = other.parallelizationEnabled;
        adaptiveParameterSettings.setNode(this);
        registerSubParameter(dataBatchGenerationSettings);
        registerSubParameter(adaptiveParameterSettings);
    }

    @Override
    public JIPipeDataBatchGenerationSettings getGenerationSettingsInterface() {
        return dataBatchGenerationSettings;
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
    public List<JIPipeMergingDataBatch> generateDataBatchesDryRun(List<JIPipeDataSlot> slots, JIPipeProgressInfo progressInfo) {
        JIPipeMergingDataBatchBuilder builder = new JIPipeMergingDataBatchBuilder();
        builder.setNode(this);
        builder.setSlots(slots);
        builder.setApplyMerging(true);
        builder.setAnnotationMergeStrategy(dataBatchGenerationSettings.getAnnotationMergeStrategy());
        builder.setDataAnnotationMergeStrategy(dataBatchGenerationSettings.getDataAnnotationMergeStrategy());
        builder.setReferenceColumns(JIPipeColumMatching.MergeAll, new StringQueryExpression());
        return builder.build(progressInfo);
    }

    /**
     * A pass-through variant for merging algorithms.
     * Passes the data batch to the single output
     *
     * @param progressInfo progress info
     * @param dataBatch    the data batch
     */
    protected void runPassThrough(JIPipeProgressInfo progressInfo, JIPipeMergingDataBatch dataBatch) {
        progressInfo.log("Passing trough (via dynamic pass-through)");
        for (int row : dataBatch.getInputSlotRows().get(getFirstInputSlot())) {
            dataBatch.addOutputData(getFirstOutputSlot(), getFirstInputSlot().getData(row, JIPipeData.class, progressInfo), progressInfo);
        }
    }

    @Override
    public void runParameterSet(JIPipeProgressInfo progressInfo, List<JIPipeAnnotation> parameterAnnotations) {

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
        if (getEffectiveInputSlotCount() == 0) {
            if (progressInfo.isCancelled())
                return;
            final int row = 0;
            JIPipeProgressInfo slotProgress = progressInfo.resolveAndLog("Data row", row, 1);
            JIPipeMergingDataBatch dataBatch = new JIPipeMergingDataBatch(this);
            dataBatch.addGlobalAnnotations(parameterAnnotations, dataBatchGenerationSettings.getAnnotationMergeStrategy());
            uploadAdaptiveParameters(dataBatch, tree, parameterBackups, progressInfo);
            if (isPassThrough()) {
                runPassThrough(slotProgress, dataBatch);
            } else {
                runIteration(dataBatch, slotProgress);
            }
            return;
        }

        List<JIPipeMergingDataBatch> dataBatches = generateDataBatchesDryRun(getNonParameterInputSlots(), progressInfo);
        for (JIPipeMergingDataBatch dataBatch : dataBatches) {
            dataBatch.addGlobalAnnotations(parameterAnnotations, dataBatchGenerationSettings.getAnnotationMergeStrategy());
        }

        // There should be only one batch, but we iterate anyways
        for (int i = 0; i < dataBatches.size(); i++) {
            if (progressInfo.isCancelled())
                return;
            JIPipeProgressInfo slotProgress = progressInfo.resolveAndLog("Data row", i, dataBatches.size());
            uploadAdaptiveParameters(dataBatches.get(i), tree, parameterBackups, progressInfo);
            if (isPassThrough()) {
                runPassThrough(slotProgress, dataBatches.get(i));
            } else {
                runIteration(dataBatches.get(i), slotProgress);
            }
        }
    }

    private void uploadAdaptiveParameters(JIPipeMergingDataBatch dataBatch, JIPipeParameterTree tree, Map<String, Object> parameterBackups, JIPipeProgressInfo progressInfo) {
        ExpressionVariables expressionVariables = new ExpressionVariables();
        for (JIPipeAnnotation annotation : dataBatch.getGlobalAnnotations().values()) {
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
                    annotateWithParameter(dataBatch, key, target, newValue);
                }
            } else if (target.getFieldClass().isAssignableFrom(newValue.getClass())) {
                // Set new value
                progressInfo.log("Set adaptive parameter " + key + " to value " + JsonUtils.toJsonString(newValue));
                target.set(newValue);
                if (getAdaptiveParameterSettings().isAttachParameterAnnotations()) {
                    annotateWithParameter(dataBatch, key, target, newValue);
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
                    annotateWithParameter(dataBatch, key, target, newValue);
                }
            }
        }
    }

    private void annotateWithParameter(JIPipeMergingDataBatch dataBatch, String key, JIPipeParameterAccess target, Object newValue) {
        String name;
        if (getAdaptiveParameterSettings().isParameterAnnotationsUseInternalNames())
            name = key;
        else
            name = target.getName();
        name = getAdaptiveParameterSettings().getParameterAnnotationsPrefix() + name;
        String value = JsonUtils.toJsonString(newValue);
        dataBatch.addGlobalAnnotation(new JIPipeAnnotation(name, value), JIPipeAnnotationMergeStrategy.Merge);
    }

    /**
     * Runs code on one data row
     *
     * @param dataBatch    The data interface
     * @param progressInfo the progress from the run
     */
    protected abstract void runIteration(JIPipeMergingDataBatch dataBatch, JIPipeProgressInfo progressInfo);

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

    @JIPipeDocumentation(name = "Data batch generation", description = "This algorithm can have multiple inputs. " +
            "This node merges all data of all inputs into one batch. Here you can determine how annotations and data annotations are copied to the output.")
    @JIPipeParameter(value = "jipipe:data-batch-generation", collapsed = true,
            iconURL = ResourceUtils.RESOURCE_BASE_PATH + "/icons/actions/package.png",
            iconDarkURL = ResourceUtils.RESOURCE_BASE_PATH + "/dark/icons/actions/package.png")
    public JIPipeSingleIterationAlgorithmDataBatchGenerationSettings getDataBatchGenerationSettings() {
        return dataBatchGenerationSettings;
    }

    @JIPipeDocumentation(name = "Adaptive parameters", description = "You can use the following settings to generate parameter values for each data batch based on annotations.")
    @JIPipeParameter(value = "jipipe:adaptive-parameters", collapsed = true,
            iconURL = ResourceUtils.RESOURCE_BASE_PATH + "/icons/actions/insert-function.png",
            iconDarkURL = ResourceUtils.RESOURCE_BASE_PATH + "/dark/icons/actions/insert-function.png")
    public JIPipeAdaptiveParameterSettings getAdaptiveParameterSettings() {
        return adaptiveParameterSettings;
    }

}
