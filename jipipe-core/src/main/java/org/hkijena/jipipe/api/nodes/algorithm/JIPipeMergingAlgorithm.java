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
import org.hkijena.jipipe.api.AddJIPipeDocumentationDescription;
import org.hkijena.jipipe.api.JIPipeDataBatchGenerationResult;
import org.hkijena.jipipe.api.JIPipeProgressInfo;
import org.hkijena.jipipe.api.SetJIPipeDocumentation;
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
import org.hkijena.jipipe.plugins.parameters.library.primitives.ranges.IntegerRange;
import org.hkijena.jipipe.utils.ParameterUtils;
import org.hkijena.jipipe.utils.ResourceUtils;
import org.hkijena.jipipe.utils.StringUtils;
import org.hkijena.jipipe.utils.UIUtils;
import org.hkijena.jipipe.utils.json.JsonUtils;
import org.hkijena.jipipe.utils.ui.ViewOnlyMenuItem;

import javax.swing.*;
import java.awt.*;
import java.util.*;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;

/**
 * An {@link JIPipeAlgorithm} that applies a similar algorithm to {@link JIPipeIteratingAlgorithm}, but does create {@link JIPipeMultiIterationStep} instead.
 * This algorithm instead just groups the data based on the annotations and passes those groups to
 * the runIteration() function. This is useful for merging algorithms.
 * Please note that the single-input case will still group the data into multiple groups, or just one group if no grouping could be acquired.
 */
@AddJIPipeDocumentationDescription(description = "This algorithm groups the incoming data based on the annotations. " +
        "Those groups can consist of multiple data items. If you want to group all data into one output, set the matching strategy to 'Custom' and " +
        "leave 'Data set matching annotations' empty.")
public abstract class JIPipeMergingAlgorithm extends JIPipeParameterSlotAlgorithm implements JIPipeParallelizedAlgorithm, JIPipeIterationStepAlgorithm, JIPipeAdaptiveParametersAlgorithm {
    private JIPipeMergingAlgorithmIterationStepGenerationSettings iterationStepGenerationSettings = new JIPipeMergingAlgorithmIterationStepGenerationSettings();
    private JIPipeAdaptiveParameterSettings adaptiveParameterSettings = new JIPipeAdaptiveParameterSettings();


    /**
     * Creates a new instance
     *
     * @param info              Algorithm info
     * @param slotConfiguration Slot configuration override
     */
    public JIPipeMergingAlgorithm(JIPipeNodeInfo info, JIPipeSlotConfiguration slotConfiguration) {
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
    public JIPipeMergingAlgorithm(JIPipeNodeInfo info) {
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
    public JIPipeMergingAlgorithm(JIPipeMergingAlgorithm other) {
        super(other);
        this.iterationStepGenerationSettings = new JIPipeMergingAlgorithmIterationStepGenerationSettings(other.iterationStepGenerationSettings);
        this.adaptiveParameterSettings = new JIPipeAdaptiveParameterSettings(other.adaptiveParameterSettings);
        adaptiveParameterSettings.setNode(this);
        registerSubParameter(iterationStepGenerationSettings);
        registerSubParameter(adaptiveParameterSettings);
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
        builder.setReferenceColumns(iterationStepGenerationSettings.getColumnMatching(),
                iterationStepGenerationSettings.getCustomColumns());
        builder.setCustomAnnotationMatching(iterationStepGenerationSettings.getCustomAnnotationMatching());
        builder.setAnnotationMatchingMethod(iterationStepGenerationSettings.getAnnotationMatchingMethod());
        builder.setForceFlowGraphSolver(iterationStepGenerationSettings.isForceFlowGraphSolver());
        builder.setForceNAIsAny(iterationStepGenerationSettings.isForceNAIsAny());
        List<JIPipeMultiIterationStep> iterationSteps = builder.build(progressInfo);
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
        if (getDataInputSlotCount() == 0) {
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
                runIteration(iterationStep, new JIPipeMutableIterationContext(0, 1), runContext, slotProgress);
            }
            return;
        }

        List<JIPipeMultiIterationStep> iterationSteps = generateDataBatchesGenerationResult(getNonParameterInputSlots(), progressInfo).getDataBatches();
        for (JIPipeMultiIterationStep iterationStep : iterationSteps) {
            iterationStep.addMergedTextAnnotations(parameterAnnotations, iterationStepGenerationSettings.getAnnotationMergeStrategy());
        }

        // Check for incomplete batches
        List<JIPipeMultiIterationStep> incomplete = new ArrayList<>();
        for (JIPipeMultiIterationStep iterationStep : iterationSteps) {
            if (iterationStep.isIncomplete()) {
                incomplete.add(iterationStep);
                progressInfo.log("[WARN] INCOMPLETE DATA BATCH FOUND: " + iterationStep);
            }
        }
        if (iterationStepGenerationSettings.isSkipIncompleteDataSets()) {
            if (!incomplete.isEmpty() && iterationStepGenerationSettings.isSkipIncompleteDataSets()) {
                progressInfo.log("[WARN] SKIPPING INCOMPLETE DATA BATCHES AS REQUESTED");
                iterationSteps.removeAll(incomplete);
            }
        } else {
            for (JIPipeMultiIterationStep batch : iterationSteps) {
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

        boolean hasAdaptiveParameters = getAdaptiveParameterSettings().isEnabled() && !getAdaptiveParameterSettings().getOverriddenParameters().isEmpty();
        final int numIterationSteps = iterationSteps.size();
        JIPipeRuntimePartition partition = runContext.getGraphRun().getRuntimePartition(getRuntimePartition());

        if (!supportsParallelization() || !partition.isEnableParallelization() ||
                runContext.getThreadPool() == null || runContext.getThreadPool().getMaxThreads() <= 1 ||
                iterationSteps.size() <= 1 || hasAdaptiveParameters) {
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
                    JIPipeMultiIterationStep iterationStep = iterationSteps.get(iterationStepIndex);
                    uploadAdaptiveParameters(iterationStep, finalTree, parameterBackups, progressInfo);
                    if (isPassThrough()) {
                        runPassThrough(slotProgress, iterationStep);
                    } else {
                        runIteration(iterationStep, new JIPipeMutableIterationContext(iterationStepIndex, numIterationSteps), runContext, slotProgress);
                    }
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

    @Override
    public Dimension getUIInputSlotIconBaseDimensions(String slotName) {
        JIPipeInputDataSlot inputSlot = getInputSlot(slotName);
        if (inputSlot != null && inputSlot.getInfo().getRole() == JIPipeDataSlotRole.Data) {
            return new Dimension(16, 16);
        }
        return super.getUIInputSlotIconBaseDimensions(slotName);
    }

    @Override
    public ImageIcon getUIInputSlotIcon(String slotName) {
        JIPipeInputDataSlot inputSlot = getInputSlot(slotName);
        if (inputSlot != null && inputSlot.getInfo().getRole() == JIPipeDataSlotRole.Data) {
            return UIUtils.getIconInvertedFromResources("actions/merge-down.png");
        }
        return super.getUIInputSlotIcon(slotName);
    }

    @Override
    public void createUIInputSlotIconDescriptionMenuItems(String slotName, List<ViewOnlyMenuItem> target) {
        super.createUIInputSlotIconDescriptionMenuItems(slotName, target);
        JIPipeInputDataSlot inputSlot = getInputSlot(slotName);
        if (inputSlot != null && inputSlot.getInfo().getRole() == JIPipeDataSlotRole.Data) {
            target.add(new ViewOnlyMenuItem("<html>Many-to-Many processing<br/><small>The data within this slot is merged based on annotations (N inputs produce M outputs)</small>",
                    UIUtils.getIconFromResources("actions/merge-down.png")));
        }
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

    @SetJIPipeDocumentation(name = "Merging iteration step generation", description = "This algorithm can have multiple inputs. " +
            "This means that JIPipe has to match incoming data into batches via metadata annotations. " +
            "The following settings allow you to control which columns are used as reference to organize data.")
    @JIPipeParameter(value = "jipipe:data-batch-generation", collapsed = true,
            iconURL = ResourceUtils.RESOURCE_BASE_PATH + "/icons/actions/package.png",
            iconDarkURL = ResourceUtils.RESOURCE_BASE_PATH + "/dark/icons/actions/package.png")
    public JIPipeMergingAlgorithmIterationStepGenerationSettings getDataBatchGenerationSettings() {
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
