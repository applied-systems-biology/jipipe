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

package org.hkijena.pipelinej.api.algorithm;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.google.common.collect.HashMultimap;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Multimap;
import com.google.common.eventbus.EventBus;
import gnu.trove.iterator.TIntIterator;
import gnu.trove.set.TIntSet;
import gnu.trove.set.hash.TIntHashSet;
import org.hkijena.pipelinej.api.ACAQDocumentation;
import org.hkijena.pipelinej.api.ACAQRunnerSubStatus;
import org.hkijena.pipelinej.api.data.ACAQAnnotation;
import org.hkijena.pipelinej.api.data.ACAQDataSlot;
import org.hkijena.pipelinej.api.data.ACAQSlotConfiguration;
import org.hkijena.pipelinej.api.exceptions.UserFriendlyRuntimeException;
import org.hkijena.pipelinej.api.parameters.ACAQParameter;
import org.hkijena.pipelinej.api.parameters.ACAQParameterCollection;
import org.hkijena.pipelinej.api.parameters.ACAQParameterVisibility;
import org.hkijena.pipelinej.extensions.parameters.primitives.StringList;
import org.hkijena.pipelinej.extensions.parameters.primitives.StringParameterSettings;
import org.hkijena.pipelinej.utils.JsonUtils;
import org.hkijena.pipelinej.utils.ResourceUtils;

import java.util.*;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.function.Consumer;
import java.util.function.Supplier;
import java.util.stream.Collectors;

/**
 * An {@link ACAQAlgorithm} that applies a similar algorithm to {@link ACAQIteratingAlgorithm}, but does create {@link ACAQMergingDataBatch} instead.
 * This algorithm instead just groups the data based on the annotations and passes those groups to
 * the runIteration() function. This is useful for merging algorithms.
 * Please note that the single-input case will still group the data into multiple groups, or just one group if no grouping could be acquired.
 */
public abstract class ACAQMergingAlgorithm extends ACAQParameterSlotAlgorithm implements ACAQParallelizedAlgorithm {

    public static final String MERGING_ALGORITHM_DESCRIPTION = "This algorithm groups the incoming data based on the annotations. " +
            "Those groups can consist of multiple data items. If you want to group all data into one output, set the matching strategy to 'Custom' and " +
            "leave 'Data set matching annotations' empty.";

    private boolean parallelizationEnabled = true;
    private DataBatchGenerationSettings dataBatchGenerationSettings = new DataBatchGenerationSettings();


    /**
     * Creates a new instance
     *
     * @param declaration       Algorithm declaration
     * @param slotConfiguration Slot configuration override
     */
    public ACAQMergingAlgorithm(ACAQAlgorithmDeclaration declaration, ACAQSlotConfiguration slotConfiguration) {
        super(declaration, slotConfiguration);
        registerSubParameter(dataBatchGenerationSettings);
    }

    /**
     * Creates a new instance
     *
     * @param declaration Algorithm declaration
     */
    public ACAQMergingAlgorithm(ACAQAlgorithmDeclaration declaration) {
        super(declaration, null);
    }

    /**
     * Copies the algorithm
     *
     * @param other The original
     */
    public ACAQMergingAlgorithm(ACAQMergingAlgorithm other) {
        super(other);
        this.dataBatchGenerationSettings = new DataBatchGenerationSettings(other.dataBatchGenerationSettings);
        this.parallelizationEnabled = other.parallelizationEnabled;
        registerSubParameter(dataBatchGenerationSettings);
    }

    /**
     * Returns annotation types that should be ignored by the internal logic.
     * Use this if you have some counting/sorting annotation that should not be included into the set of annotations used to match data.
     *
     * @return annotation types that should be ignored by the internal logic
     */
    protected Set<String> getIgnoredTraitColumns() {
        return Collections.emptySet();
    }

    @Override
    public void runParameterSet(ACAQRunnerSubStatus subProgress, Consumer<ACAQRunnerSubStatus> algorithmProgress, Supplier<Boolean> isCancelled, List<ACAQAnnotation> parameterAnnotations) {
        // Special case: No input slots
        if (getEffectiveInputSlotCount() == 0) {
            if (isCancelled.get())
                return;
            final int row = 0;
            ACAQRunnerSubStatus slotProgress = subProgress.resolve("Data row " + (row + 1) + " / " + 1);
            algorithmProgress.accept(slotProgress);
            ACAQMergingDataBatch dataInterface = new ACAQMergingDataBatch(this);
            dataInterface.addGlobalAnnotations(parameterAnnotations, true);
            runIteration(dataInterface, slotProgress, algorithmProgress, isCancelled);
            return;
        }

        // First find all columns to sort by
        Set<String> referenceTraitColumns;

        switch (dataBatchGenerationSettings.dataSetMatching) {
            case Custom:
                referenceTraitColumns = new HashSet<>(dataBatchGenerationSettings.customColumns);
                break;
            case Union:
                referenceTraitColumns = getInputTraitColumnUnion();
                break;
            case Intersection:
                referenceTraitColumns = getInputTraitColumnIntersection();
                break;
            default:
                throw new UnsupportedOperationException("Unknown column matching strategy: " + dataBatchGenerationSettings.dataSetMatching);
        }

        // Remove ignored columns
        referenceTraitColumns.removeAll(getIgnoredTraitColumns());

        // Organize the input data by Dataset -> Slot -> Data row
        Map<ACAQUniqueDataBatch, Map<String, TIntSet>> dataSets = new HashMap<>();
        for (ACAQDataSlot inputSlot : getInputSlots()) {
            if (getParameterSlot() == inputSlot)
                continue;
            for (int row = 0; row < inputSlot.getRowCount(); row++) {
                ACAQUniqueDataBatch key = new ACAQUniqueDataBatch();
                for (String referenceTraitColumn : referenceTraitColumns) {
                    key.getEntries().put(referenceTraitColumn, null);
                }
                for (ACAQAnnotation annotation : inputSlot.getAnnotations(row)) {
                    if (annotation != null && referenceTraitColumns.contains(annotation.getName())) {
                        key.getEntries().put(annotation.getName(), annotation);
                    }
                }
                Map<String, TIntSet> dataSet = dataSets.getOrDefault(key, null);
                if (dataSet == null) {
                    dataSet = new HashMap<>();
                    dataSets.put(key, dataSet);
                }
                TIntSet rows = dataSet.getOrDefault(inputSlot.getName(), null);
                if (rows == null) {
                    rows = new TIntHashSet();
                    dataSet.put(inputSlot.getName(), rows);
                }
                rows.add(row);
            }
        }

        // Check for missing data sets
        for (Map.Entry<ACAQUniqueDataBatch, Map<String, TIntSet>> dataSetEntry : ImmutableList.copyOf(dataSets.entrySet())) {
            boolean incomplete = false;
            for (ACAQDataSlot inputSlot : getInputSlots()) {
                if (getParameterSlot() == inputSlot)
                    continue;
                TIntSet slotEntry = dataSetEntry.getValue().getOrDefault(inputSlot.getName(), null);
                if (slotEntry == null) {
                    incomplete = true;
                    break;
                }
            }
            if (incomplete) {
                if (!dataBatchGenerationSettings.skipIncompleteDataSets) {
                    throw new UserFriendlyRuntimeException("Incomplete data set found!",
                            "An incomplete data set was found!",
                            "Algorithm '" + getName() + "'",
                            "The algorithm needs to assign input a unique data set via annotations, but there is " +
                                    "not a data set for each input slot.",
                            "Please check the input of the algorithm by running the quick run on each input algorithm. " +
                                    "You can also choose to skip incomplete data sets, although you might lose data in those cases.");
                }
                dataSets.remove(dataSetEntry.getKey());
            }
        }

        // Generate data interfaces
        List<ACAQMergingDataBatch> dataInterfaces = new ArrayList<>();
        for (Map.Entry<ACAQUniqueDataBatch, Map<String, TIntSet>> dataSetEntry : ImmutableList.copyOf(dataSets.entrySet())) {

            ACAQMergingDataBatch dataInterface = new ACAQMergingDataBatch(this);
            Multimap<String, String> compoundTraits = HashMultimap.create();
            for (Map.Entry<String, TIntSet> dataSlotEntry : dataSetEntry.getValue().entrySet()) {
                ACAQDataSlot inputSlot = getInputSlot(dataSlotEntry.getKey());
                if (getParameterSlot() == inputSlot)
                    continue;
                TIntSet rows = dataSetEntry.getValue().get(inputSlot.getName());
                for (TIntIterator it = rows.iterator(); it.hasNext(); ) {
                    int row = it.next();
                    dataInterface.addData(inputSlot, row);

                    // Store all annotations
                    for (ACAQAnnotation annotation : inputSlot.getAnnotations(row)) {
                        if (annotation != null) {
                            compoundTraits.put(annotation.getName(), "" + annotation.getValue());
                        }
                    }
                }
            }

            // Create new merged annotations
            for (String declaration : compoundTraits.keySet()) {
                List<String> valueList = compoundTraits.get(declaration).stream().distinct().sorted().collect(Collectors.toList());
                String value;
                try {
                    value = JsonUtils.getObjectMapper().writeValueAsString(valueList);
                } catch (JsonProcessingException e) {
                    throw new RuntimeException(e);
                }
                dataInterface.addGlobalAnnotation(new ACAQAnnotation(declaration, value));
            }

            // Add parameter annotations
            dataInterface.addGlobalAnnotations(parameterAnnotations, true);

            dataInterfaces.add(dataInterface);
        }

        if (!supportsParallelization() || !isParallelizationEnabled() || getThreadPool() == null || getThreadPool().getMaxThreads() <= 1) {
            for (int i = 0; i < dataInterfaces.size(); i++) {
                if (isCancelled.get())
                    return;
                ACAQRunnerSubStatus slotProgress = subProgress.resolve("Data row " + (i + 1) + " / " + dataInterfaces.size());
                algorithmProgress.accept(slotProgress);
                runIteration(dataInterfaces.get(i), slotProgress, algorithmProgress, isCancelled);
            }
        } else {
            List<Runnable> tasks = new ArrayList<>();
            for (int i = 0; i < getFirstInputSlot().getRowCount(); i++) {
                int rowIndex = i;
                tasks.add(() -> {
                    if (isCancelled.get())
                        return;
                    ACAQRunnerSubStatus slotProgress = subProgress.resolve("Data row " + (rowIndex + 1) + " / " + getFirstInputSlot().getRowCount());
                    algorithmProgress.accept(slotProgress);
                    ACAQMergingDataBatch dataInterface = dataInterfaces.get(rowIndex);
                    runIteration(dataInterface, slotProgress, algorithmProgress, isCancelled);
                });
            }
            algorithmProgress.accept(subProgress.resolve(String.format("Running %d batches (batch size %d) in parallel. Available threads = %d", tasks.size(), getParallelizationBatchSize(), getThreadPool().getMaxThreads())));
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

    private Set<String> getInputTraitColumnIntersection() {
        Set<String> result = null;
        for (ACAQDataSlot inputSlot : getInputSlots()) {
            if (getParameterSlot() == inputSlot)
                continue;
            if (result == null) {
                result = new HashSet<>(inputSlot.getAnnotationColumns());
            } else {
                result.retainAll(inputSlot.getAnnotationColumns());
            }
        }
        if (result == null)
            result = new HashSet<>();
        return result;
    }

    private Set<String> getInputTraitColumnUnion() {
        Set<String> result = new HashSet<>();
        for (ACAQDataSlot inputSlot : getInputSlots()) {
            if (getParameterSlot() == inputSlot)
                continue;
            result.addAll(inputSlot.getAnnotationColumns());
        }
        return result;
    }

    /**
     * Runs code on one data row
     *
     * @param dataInterface     The data interface
     * @param subProgress       The current sub-progress this algorithm is scheduled in
     * @param algorithmProgress Consumer to publish a new sub-progress
     * @param isCancelled       Supplier that informs if the current task was canceled
     */
    protected abstract void runIteration(ACAQMergingDataBatch dataInterface, ACAQRunnerSubStatus subProgress, Consumer<ACAQRunnerSubStatus> algorithmProgress, Supplier<Boolean> isCancelled);

    @Override
    public boolean supportsParallelization() {
        return false;
    }

    @Override
    public int getParallelizationBatchSize() {
        return 1;
    }

    @ACAQDocumentation(name = "Enable parallelization", description = "If enabled, the workload can be calculated across multiple threads to for speedup. " +
            "Please note that the actual usage of multiple threads depend on the runtime settings and the algorithm implementation. " +
            "We recommend to use the runtime parameters to control parallelization in most cases.")
    @ACAQParameter(value = "acaq:parallelization:enabled", visibility = ACAQParameterVisibility.Visible)
    @Override
    public boolean isParallelizationEnabled() {
        return parallelizationEnabled;
    }

    @Override
    @ACAQParameter("acaq:parallelization:enabled")
    public void setParallelizationEnabled(boolean parallelizationEnabled) {
        this.parallelizationEnabled = parallelizationEnabled;
    }

    @ACAQDocumentation(name = "Merging data batch generation", description = "This algorithm can have multiple inputs. This means that ACAQ5 has to match incoming data into batches via metadata annotations. " +
            "The following settings allow you to control which columns are used as reference to organize data.")
    @ACAQParameter(value = "acaq:data-batch-generation", visibility = ACAQParameterVisibility.Visible)
    public DataBatchGenerationSettings getDataBatchGenerationSettings() {
        return dataBatchGenerationSettings;
    }

    public static class DataBatchGenerationSettings implements ACAQParameterCollection {
        private final EventBus eventBus = new EventBus();
        private ACAQIteratingAlgorithm.ColumnMatching dataSetMatching = ACAQIteratingAlgorithm.ColumnMatching.Intersection;
        private boolean skipIncompleteDataSets = false;
        private StringList customColumns = new StringList();

        public DataBatchGenerationSettings() {
        }

        public DataBatchGenerationSettings(DataBatchGenerationSettings other) {
            this.dataSetMatching = other.dataSetMatching;
            this.skipIncompleteDataSets = other.skipIncompleteDataSets;
            this.customColumns = new StringList(other.customColumns);
        }

        @Override
        public EventBus getEventBus() {
            return eventBus;
        }

        @ACAQDocumentation(name = "Data set matching strategy", description = "Algorithms with multiple inputs require to match the incoming data " +
                "to data sets. This allows you to determine how interesting data annotation columns are extracted from the incoming data. " +
                "Union matches using the union of annotation columns. Intersection intersects the sets of available columns. You can also" +
                " customize which columns should be available.")
        @ACAQParameter(value = "acaq:iterating-algorithm:column-matching", uiOrder = 999, visibility = ACAQParameterVisibility.Visible)
        public ACAQIteratingAlgorithm.ColumnMatching getDataSetMatching() {
            return dataSetMatching;
        }

        @ACAQParameter("acaq:iterating-algorithm:column-matching")
        public void setDataSetMatching(ACAQIteratingAlgorithm.ColumnMatching dataSetMatching) {
            this.dataSetMatching = dataSetMatching;

        }

        @ACAQDocumentation(name = "Data set matching annotations", description = "Only used if 'Data set matching strategy' is set to 'Custom'. " +
                "Determines which annotation columns are referred to match data sets.")
        @ACAQParameter(value = "acaq:iterating-algorithm:custom-matched-columns", uiOrder = 999, visibility = ACAQParameterVisibility.Visible)
        @StringParameterSettings(monospace = true, icon = ResourceUtils.RESOURCE_BASE_PATH + "/icons/annotation.png")
        public StringList getCustomColumns() {
            if (customColumns == null)
                customColumns = new StringList();
            return customColumns;
        }

        @ACAQParameter(value = "acaq:iterating-algorithm:custom-matched-columns", visibility = ACAQParameterVisibility.Visible)
        public void setCustomColumns(StringList customColumns) {
            this.customColumns = customColumns;

        }

        @ACAQDocumentation(name = "Skip incomplete data sets", description = "If enabled, incomplete data sets are silently skipped. " +
                "Otherwise an error is displayed if such a configuration is detected.")
        @ACAQParameter(value = "acaq:iterating-algorithm:skip-incomplete", visibility = ACAQParameterVisibility.Visible)
        public boolean isSkipIncompleteDataSets() {
            return skipIncompleteDataSets;
        }

        @ACAQParameter("acaq:iterating-algorithm:skip-incomplete")
        public void setSkipIncompleteDataSets(boolean skipIncompleteDataSets) {
            this.skipIncompleteDataSets = skipIncompleteDataSets;

        }
    }
}
