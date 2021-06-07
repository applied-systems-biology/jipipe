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
import com.google.common.eventbus.EventBus;
import gnu.trove.set.TIntSet;
import gnu.trove.set.hash.TIntHashSet;
import org.hkijena.jipipe.api.JIPipeDocumentation;
import org.hkijena.jipipe.api.JIPipeProgressInfo;
import org.hkijena.jipipe.api.data.JIPipeAnnotation;
import org.hkijena.jipipe.api.data.JIPipeAnnotationMergeStrategy;
import org.hkijena.jipipe.api.data.JIPipeDataSlot;
import org.hkijena.jipipe.api.data.JIPipeSlotConfiguration;
import org.hkijena.jipipe.api.exceptions.UserFriendlyRuntimeException;
import org.hkijena.jipipe.api.parameters.*;
import org.hkijena.jipipe.extensions.expressions.ExpressionParameters;
import org.hkijena.jipipe.extensions.expressions.StringQueryExpression;
import org.hkijena.jipipe.extensions.parameters.generators.IntegerRange;
import org.hkijena.jipipe.extensions.parameters.generators.OptionalIntegerRange;
import org.hkijena.jipipe.extensions.parameters.pairs.StringQueryExpressionAndStringPairParameter;
import org.hkijena.jipipe.extensions.parameters.primitives.StringParameterSettings;
import org.hkijena.jipipe.utils.JsonUtils;
import org.hkijena.jipipe.utils.ResourceUtils;
import org.hkijena.jipipe.utils.StringUtils;

import java.util.*;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;

/**
 * An {@link JIPipeAlgorithm} that iterates through each data row.
 * This algorithm utilizes the {@link JIPipeDataBatch} class to iterate through input data sets.
 * It offers various parameters that control how data sets are matched.
 * If your algorithm only has one input and will never have more than one input slot, we recommend using {@link JIPipeSimpleIteratingAlgorithm}
 * instead that comes without the additional data set matching strategies
 */
public abstract class JIPipeIteratingAlgorithm extends JIPipeParameterSlotAlgorithm implements JIPipeParallelizedAlgorithm, JIPipeDataBatchAlgorithm {

    public static final String ITERATING_ALGORITHM_DESCRIPTION = "This algorithm groups the incoming data based on the annotations. " +
            "Those groups can consist of one data item per slot.";

    private DataBatchGenerationSettings dataBatchGenerationSettings = new DataBatchGenerationSettings();
    private JIPipeAdaptiveParameterSettings adaptiveParameterSettings = new JIPipeAdaptiveParameterSettings();
    private boolean parallelizationEnabled = true;

    /**
     * Creates a new instance
     *
     * @param info              Algorithm info
     * @param slotConfiguration Slot configuration override
     */
    public JIPipeIteratingAlgorithm(JIPipeNodeInfo info, JIPipeSlotConfiguration slotConfiguration) {
        super(info, slotConfiguration);
    }

    /**
     * Creates a new instance
     *
     * @param info Algorithm info
     */
    public JIPipeIteratingAlgorithm(JIPipeNodeInfo info) {
        super(info, null);
        registerSubParameter(dataBatchGenerationSettings);
        registerSubParameter(adaptiveParameterSettings);
    }

    /**
     * Copies the algorithm
     *
     * @param other The original
     */
    public JIPipeIteratingAlgorithm(JIPipeIteratingAlgorithm other) {
        super(other);
        this.dataBatchGenerationSettings = new DataBatchGenerationSettings(other.dataBatchGenerationSettings);
        this.adaptiveParameterSettings = new JIPipeAdaptiveParameterSettings(other.adaptiveParameterSettings);
        this.parallelizationEnabled = other.parallelizationEnabled;
        registerSubParameter(dataBatchGenerationSettings);
        registerSubParameter(adaptiveParameterSettings);
    }

    @Override
    public JIPipeParameterCollection getGenerationSettingsInterface() {
        return dataBatchGenerationSettings;
    }

    @Override
    public List<JIPipeMergingDataBatch> generateDataBatchesDryRun(List<JIPipeDataSlot> slots) {
        JIPipeMergingDataBatchBuilder builder = new JIPipeMergingDataBatchBuilder();
        builder.setNode(this);
        builder.setApplyMerging(false);
        builder.setSlots(slots);
        builder.setAnnotationMergeStrategy(dataBatchGenerationSettings.annotationMergeStrategy);
        builder.setReferenceColumns(dataBatchGenerationSettings.dataSetMatching,
                dataBatchGenerationSettings.customColumns);
        List<JIPipeMergingDataBatch> dataBatches = builder.build();
        dataBatches.sort(Comparator.naturalOrder());
        boolean withLimit = dataBatchGenerationSettings.getLimit().isEnabled();
        IntegerRange limit = dataBatchGenerationSettings.getLimit().getContent();
        TIntSet allowedIndices = withLimit ? new TIntHashSet(limit.getIntegers()) : null;
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

        // Adaptive parameter backups
        JIPipeParameterTree tree = null;
        Map<String, Object> parameterBackups = new HashMap<>();
        if (getAdaptiveParameterSettings().isEnabled() && !getAdaptiveParameterSettings().getOverriddenParameters().isEmpty()) {
            tree = new JIPipeParameterTree(this);
            for (Map.Entry<String, JIPipeParameterAccess> entry : tree.getParameters().entrySet()) {
                parameterBackups.put(entry.getKey(), entry.getValue().get(Object.class));
            }
        }

        List<JIPipeDataBatch> dataBatches;

        // Special case: No input slots
        if (getEffectiveInputSlotCount() == 0) {
            if (progressInfo.isCancelled().get())
                return;
            final int row = 0;
            JIPipeProgressInfo slotProgress = progressInfo.resolveAndLog("Data row", row, 1);
            JIPipeDataBatch dataBatch = new JIPipeDataBatch(this);
            dataBatch.addGlobalAnnotations(parameterAnnotations, dataBatchGenerationSettings.annotationMergeStrategy);
            uploadAdaptiveParameters(dataBatch, tree, parameterBackups, progressInfo);
            runIteration(dataBatch, slotProgress);
            return;
        } else if (getEffectiveInputSlotCount() == 1) {
            dataBatches = new ArrayList<>();
            for (int row = 0; row < getFirstInputSlot().getRowCount(); row++) {
                if (progressInfo.isCancelled().get())
                    break;
                JIPipeDataBatch dataBatch = new JIPipeDataBatch(this);
                dataBatch.setData(getFirstInputSlot(), row);
                dataBatch.addGlobalAnnotations(parameterAnnotations, dataBatchGenerationSettings.annotationMergeStrategy);
                dataBatch.addGlobalAnnotations(getFirstInputSlot().getAnnotations(row), dataBatchGenerationSettings.annotationMergeStrategy);
                uploadAdaptiveParameters(dataBatch, tree, parameterBackups, progressInfo);
                dataBatches.add(dataBatch);
            }
        } else {
            // First generate merging data batches
            List<JIPipeMergingDataBatch> mergingDataBatches = generateDataBatchesDryRun(getNonParameterInputSlots());

            // Check for incomplete batches
            if (dataBatchGenerationSettings.skipIncompleteDataSets) {
                mergingDataBatches.removeIf(JIPipeMergingDataBatch::isIncomplete);
            } else {
                for (JIPipeMergingDataBatch batch : mergingDataBatches) {
                    if (progressInfo.isCancelled().get())
                        break;
                    if (batch.isIncomplete()) {
                        throw new UserFriendlyRuntimeException("Incomplete data set found!",
                                "An incomplete data set was found!",
                                "Algorithm '" + getName() + "'",
                                "The algorithm needs to assign input a unique data set via annotations, but there is " +
                                        "not a data set for each input slot.",
                                "Please check the input of the algorithm by running the quick run on each input algorithm. " +
                                        "You can also choose to skip incomplete data sets, although you might lose data in those cases.");
                    }
                }
            }
            // Convert to single batch and attach parameters
            dataBatches = JIPipeMergingDataBatchBuilder.convertMergingToSingleDataBatches(mergingDataBatches);
            for (JIPipeDataBatch dataBatch : dataBatches) {
                dataBatch.addGlobalAnnotations(parameterAnnotations, dataBatchGenerationSettings.annotationMergeStrategy);
            }
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
                if (progressInfo.isCancelled().get())
                    return;
                JIPipeProgressInfo slotProgress = progressInfo.resolveAndLog("Data row", i, dataBatches.size());
                uploadAdaptiveParameters(dataBatches.get(i), tree, parameterBackups, progressInfo);
                runIteration(dataBatches.get(i), slotProgress);
            }
        } else {
            List<Runnable> tasks = new ArrayList<>();
            for (int i = 0; i < getFirstInputSlot().getRowCount(); i++) {
                int rowIndex = i;
                JIPipeParameterTree finalTree = tree;
                tasks.add(() -> {
                    if (progressInfo.isCancelled().get())
                        return;
                    JIPipeProgressInfo slotProgress = progressInfo.resolveAndLog("Data row", rowIndex, dataBatches.size());
                    uploadAdaptiveParameters(dataBatches.get(rowIndex), finalTree, parameterBackups, progressInfo);
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

    private void uploadAdaptiveParameters(JIPipeDataBatch dataBatch, JIPipeParameterTree tree, Map<String, Object> parameterBackups, JIPipeProgressInfo progressInfo) {
        ExpressionParameters expressionParameters = new ExpressionParameters();
        for (JIPipeAnnotation annotation : dataBatch.getAnnotations().values()) {
            expressionParameters.put(annotation.getName(), annotation.getValue());
        }
        for (StringQueryExpressionAndStringPairParameter overriddenParameter : getAdaptiveParameterSettings().getOverriddenParameters()) {
            String key = overriddenParameter.getValue();
            JIPipeParameterAccess target = tree.getParameters().getOrDefault(key, null);
            if (target == null) {
                progressInfo.log("Unable to find parameter '" + key + "' in " + getName() + "! Ignoring.");
                continue;
            }
            Object oldValue = parameterBackups.get(key);
            expressionParameters.put("default", oldValue);
            Object newValue = overriddenParameter.getKey().evaluate(expressionParameters);
            if (Objects.equals(newValue, oldValue)) {
                // No changes
                if (getAdaptiveParameterSettings().isAttachParameterAnnotations() && !getAdaptiveParameterSettings().isAttachOnlyNonDefaultParameterAnnotations()) {
                    annotateWithParameter(dataBatch, key, target, newValue);
                }
            } else if (target.getFieldClass().isAssignableFrom(newValue.getClass())) {
                // Set new value
                target.set(newValue);
                if (getAdaptiveParameterSettings().isAttachParameterAnnotations()) {
                    annotateWithParameter(dataBatch, key, target, newValue);
                }
            } else {
                // Is JSON. Parse
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

    private void annotateWithParameter(JIPipeDataBatch dataBatch, String key, JIPipeParameterAccess target, Object newValue) {
        String name;
        if (getAdaptiveParameterSettings().isParameterAnnotationsUseInternalNames())
            name = key;
        else
            name = target.getName();
        name = getAdaptiveParameterSettings().getParameterAnnotationsPrefix() + name;
        String value = JsonUtils.toJsonString(newValue);
        dataBatch.addGlobalAnnotation(new JIPipeAnnotation(name, value), JIPipeAnnotationMergeStrategy.Merge);
    }

    @JIPipeDocumentation(name = "Adaptive parameters", description = "You can use the following settings to generate parameter values for each data batch based on annotations.")
    @JIPipeParameter(value = "jipipe:adaptive-parameters", visibility = JIPipeParameterVisibility.Visible, collapsed = true,
            iconURL = ResourceUtils.RESOURCE_BASE_PATH + "/icons/actions/insert-function.png",
            iconDarkURL = ResourceUtils.RESOURCE_BASE_PATH + "/dark/icons/actions/insert-function.png")
    public JIPipeAdaptiveParameterSettings getAdaptiveParameterSettings() {
        return adaptiveParameterSettings;
    }

    @JIPipeDocumentation(name = "Data batch generation", description = "This algorithm can have multiple inputs. This means that JIPipe has to match incoming data into batches via metadata annotations. " +
            "The following settings allow you to control which columns are used as reference to organize data.")
    @JIPipeParameter(value = "jipipe:data-batch-generation", visibility = JIPipeParameterVisibility.Visible, collapsed = true,
            iconURL = ResourceUtils.RESOURCE_BASE_PATH + "/icons/actions/package.png",
            iconDarkURL = ResourceUtils.RESOURCE_BASE_PATH + "/dark/icons/actions/package.png")
    public DataBatchGenerationSettings getDataBatchGenerationSettings() {
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
    @JIPipeParameter(value = "jipipe:parallelization:enabled", visibility = JIPipeParameterVisibility.Visible)
    @Override
    public boolean isParallelizationEnabled() {
        return parallelizationEnabled;
    }

    @Override
    @JIPipeParameter("jipipe:parallelization:enabled")
    public void setParallelizationEnabled(boolean parallelizationEnabled) {
        this.parallelizationEnabled = parallelizationEnabled;
    }

    /**
     * Runs code on one data row
     *
     * @param dataBatch    The data interface
     * @param progressInfo the progress info from the run
     */
    protected abstract void runIteration(JIPipeDataBatch dataBatch, JIPipeProgressInfo progressInfo);

    /**
     * Groups data batch generation settings
     */
    public static class DataBatchGenerationSettings implements JIPipeParameterCollection {
        private final EventBus eventBus = new EventBus();
        private JIPipeColumnGrouping dataSetMatching = JIPipeColumnGrouping.PrefixHashUnion;
        private boolean skipIncompleteDataSets = false;
        private StringQueryExpression customColumns = new StringQueryExpression();
        private JIPipeAnnotationMergeStrategy annotationMergeStrategy = JIPipeAnnotationMergeStrategy.Merge;
        private OptionalIntegerRange limit = new OptionalIntegerRange(new IntegerRange("0-9"), false);

        public DataBatchGenerationSettings() {
        }

        public DataBatchGenerationSettings(DataBatchGenerationSettings other) {
            this.dataSetMatching = other.dataSetMatching;
            this.skipIncompleteDataSets = other.skipIncompleteDataSets;
            this.customColumns = new StringQueryExpression(other.customColumns);
            this.annotationMergeStrategy = other.annotationMergeStrategy;
            this.limit = new OptionalIntegerRange(other.limit);
        }

        @Override
        public EventBus getEventBus() {
            return eventBus;
        }

        @JIPipeDocumentation(name = "Grouping method", description = "Algorithms with multiple inputs require to match the incoming data " +
                "to data sets. This allows you to determine how interesting data annotation columns are extracted from the incoming data. " +
                "Union matches using the union of annotation columns. Intersection intersects the sets of available columns. You can also" +
                " customize which columns should be included or excluded.")
        @JIPipeParameter(value = "column-matching", uiOrder = 999, visibility = JIPipeParameterVisibility.Visible)
        public JIPipeColumnGrouping getDataSetMatching() {
            return dataSetMatching;
        }

        @JIPipeParameter("column-matching")
        public void setDataSetMatching(JIPipeColumnGrouping dataSetMatching) {
            this.dataSetMatching = dataSetMatching;

        }

        @JIPipeDocumentation(name = "Custom grouping columns", description = "Only used if 'Grouping method' is set to 'Custom'. " +
                "Determines which annotation columns are referred to group data sets. " + StringQueryExpression.DOCUMENTATION_DESCRIPTION)
        @JIPipeParameter(value = "custom-matched-columns-expression", uiOrder = 999, visibility = JIPipeParameterVisibility.Visible)
        @StringParameterSettings(monospace = true, icon = ResourceUtils.RESOURCE_BASE_PATH + "/icons/data-types/annotation.png")
        public StringQueryExpression getCustomColumns() {
            if (customColumns == null)
                customColumns = new StringQueryExpression();
            return customColumns;
        }

        @JIPipeParameter(value = "custom-matched-columns-expression", visibility = JIPipeParameterVisibility.Visible)
        public void setCustomColumns(StringQueryExpression customColumns) {
            this.customColumns = customColumns;
        }

        @JIPipeDocumentation(name = "Skip incomplete data sets", description = "If enabled, incomplete data sets are silently skipped. " +
                "Otherwise an error is displayed if such a configuration is detected.")
        @JIPipeParameter(value = "skip-incomplete", visibility = JIPipeParameterVisibility.Visible)
        public boolean isSkipIncompleteDataSets() {
            return skipIncompleteDataSets;
        }

        @JIPipeParameter("skip-incomplete")
        public void setSkipIncompleteDataSets(boolean skipIncompleteDataSets) {
            this.skipIncompleteDataSets = skipIncompleteDataSets;

        }

        @JIPipeDocumentation(name = "Merge same annotation values", description = "Determines which strategy is applied if data sets that " +
                "define different values for the same annotation columns are encountered.")
        @JIPipeParameter("annotation-merge-strategy")
        public JIPipeAnnotationMergeStrategy getAnnotationMergeStrategy() {
            return annotationMergeStrategy;
        }

        @JIPipeParameter("annotation-merge-strategy")
        public void setAnnotationMergeStrategy(JIPipeAnnotationMergeStrategy annotationMergeStrategy) {
            this.annotationMergeStrategy = annotationMergeStrategy;
        }

        @JIPipeDocumentation(name = "Limit", description = "Limits which data batches are generated. The first index is zero.\n" + IntegerRange.DOCUMENTATION_DESCRIPTION)
        @JIPipeParameter(value = "limit")
        public OptionalIntegerRange getLimit() {
            return limit;
        }

        @JIPipeParameter("limit")
        public void setLimit(OptionalIntegerRange limit) {
            this.limit = limit;
        }
    }
}
