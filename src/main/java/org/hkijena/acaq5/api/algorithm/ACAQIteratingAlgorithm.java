package org.hkijena.acaq5.api.algorithm;

import com.google.common.collect.ImmutableList;
import gnu.trove.iterator.TIntIterator;
import gnu.trove.set.TIntSet;
import gnu.trove.set.hash.TIntHashSet;
import org.hkijena.acaq5.api.ACAQDocumentation;
import org.hkijena.acaq5.api.ACAQRunnerSubStatus;
import org.hkijena.acaq5.api.data.ACAQAnnotation;
import org.hkijena.acaq5.api.data.ACAQDataSlot;
import org.hkijena.acaq5.api.data.ACAQSlotConfiguration;
import org.hkijena.acaq5.api.events.ParameterChangedEvent;
import org.hkijena.acaq5.api.exceptions.UserFriendlyRuntimeException;
import org.hkijena.acaq5.api.parameters.ACAQParameter;
import org.hkijena.acaq5.api.parameters.ACAQParameterVisibility;
import org.hkijena.acaq5.extensions.parameters.primitives.StringList;
import org.hkijena.acaq5.extensions.parameters.primitives.StringParameterSettings;
import org.hkijena.acaq5.utils.ResourceUtils;

import java.util.*;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.function.Consumer;
import java.util.function.Supplier;

/**
 * An {@link ACAQAlgorithm} that iterates through each data row.
 * This algorithm utilizes the {@link ACAQDataInterface} class to iterate through input data sets.
 * It offers various parameters that control how data sets are matched.
 * If your algorithm only has one input and will never have more than one input slot, we recommend using {@link ACAQSimpleIteratingAlgorithm}
 * instead that comes without the additional data set matching strategies
 */
public abstract class ACAQIteratingAlgorithm extends ACAQAlgorithm implements ACAQParallelizedAlgorithm {

    public static final String ITERATING_ALGORITHM_DESCRIPTION = "This algorithm groups the incoming data based on the annotations. " +
            "Those groups can consist of one data item per slot.";

    private ColumnMatching dataSetMatching = ColumnMatching.Intersection;
    private boolean allowDuplicateDataSets = true;
    private boolean skipIncompleteDataSets = false;
    private StringList customColumns = new StringList();
    private boolean parallelizationEnabled = true;

    /**
     * Creates a new instance
     *
     * @param declaration       Algorithm declaration
     * @param slotConfiguration Slot configuration override
     */
    public ACAQIteratingAlgorithm(ACAQAlgorithmDeclaration declaration, ACAQSlotConfiguration slotConfiguration) {
        super(declaration, slotConfiguration);
    }

    /**
     * Creates a new instance
     *
     * @param declaration Algorithm declaration
     */
    public ACAQIteratingAlgorithm(ACAQAlgorithmDeclaration declaration) {
        super(declaration, null);
    }

    /**
     * Copies the algorithm
     *
     * @param other The original
     */
    public ACAQIteratingAlgorithm(ACAQIteratingAlgorithm other) {
        super(other);
        this.dataSetMatching = other.dataSetMatching;
        this.allowDuplicateDataSets = other.allowDuplicateDataSets;
        this.skipIncompleteDataSets = other.skipIncompleteDataSets;
        this.customColumns = new StringList(other.customColumns);
        this.parallelizationEnabled = other.parallelizationEnabled;
    }

    @Override
    public void run(ACAQRunnerSubStatus subProgress, Consumer<ACAQRunnerSubStatus> algorithmProgress, Supplier<Boolean> isCancelled) {

        if (isPassThrough() && canPassThrough()) {
            algorithmProgress.accept(subProgress.resolve("Data passed through to output"));
            runPassThrough();
            return;
        }

        // Special case: No input slots
        if (getInputSlots().isEmpty()) {
            if (isCancelled.get())
                return;
            final int row = 0;
            ACAQRunnerSubStatus slotProgress = subProgress.resolve("Data row " + (row + 1) + " / " + 1);
            algorithmProgress.accept(slotProgress);
            ACAQDataInterface dataInterface = new ACAQDataInterface(this);
            runIteration(dataInterface, slotProgress, algorithmProgress, isCancelled);
            return;
        }

        // First find all columns to sort by
        Set<String> referenceTraitColumns;

        switch (dataSetMatching) {
            case Custom:
                referenceTraitColumns = new HashSet<>(customColumns);
                break;
            case Union:
                referenceTraitColumns = getInputTraitColumnUnion();
                break;
            case Intersection:
                referenceTraitColumns = getInputTraitColumnIntersection();
                break;
            default:
                throw new UnsupportedOperationException("Unknown column matching strategy: " + dataSetMatching);
        }

        // Organize the input data by Dataset -> Slot -> Data row
        Map<ACAQDataSetKey, Map<String, TIntSet>> dataSets = new HashMap<>();
        for (ACAQDataSlot inputSlot : getInputSlots()) {
            for (int row = 0; row < inputSlot.getRowCount(); row++) {
                ACAQDataSetKey key = new ACAQDataSetKey();
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

        // Check for duplicates
        if (!allowDuplicateDataSets) {
            for (Map.Entry<ACAQDataSetKey, Map<String, TIntSet>> dataSetEntry : dataSets.entrySet()) {
                for (Map.Entry<String, TIntSet> slotEntry : dataSetEntry.getValue().entrySet()) {
                    if (slotEntry.getValue().size() > 1) {
                        throw new UserFriendlyRuntimeException("Duplicate data set found!",
                                "A duplicate data set was found!",
                                "Algorithm '" + getName() + "'",
                                "The algorithm needs to assign input to a unique data set via the data annotations. " +
                                        "The duplicate data set is: " + dataSetEntry.getKey(),
                                "Please check the input of the algorithm by running the quick run on each input algorithm. " +
                                        "You can then either modify the pipeline to make the data sets unique or modify the settings of this algorithm " +
                                        "to enable duplicate entries.");
                    }
                }
            }
        }

        // Check for missing data sets
        for (Map.Entry<ACAQDataSetKey, Map<String, TIntSet>> dataSetEntry : ImmutableList.copyOf(dataSets.entrySet())) {
            boolean incomplete = false;
            for (ACAQDataSlot inputSlot : getInputSlots()) {
                TIntSet slotEntry = dataSetEntry.getValue().getOrDefault(inputSlot.getName(), null);
                if (slotEntry == null) {
                    incomplete = true;
                    break;
                }
            }
            if (incomplete) {
                if (!skipIncompleteDataSets) {
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
        List<ACAQDataInterface> dataInterfaces = new ArrayList<>();
        for (Map.Entry<ACAQDataSetKey, Map<String, TIntSet>> dataSetEntry : ImmutableList.copyOf(dataSets.entrySet())) {
            List<ACAQDataInterface> dataInterfacesForDataSet = new ArrayList<>();
            // Create the first batch
            {
                ACAQDataSlot inputSlot = getFirstInputSlot();
                TIntSet rows = dataSetEntry.getValue().get(inputSlot.getName());
                for (TIntIterator it = rows.iterator(); it.hasNext(); ) {
                    int row = it.next();
                    ACAQDataInterface dataInterface = new ACAQDataInterface(this);
                    dataInterface.setData(inputSlot, row);
                    dataInterface.addGlobalAnnotations(inputSlot.getAnnotations(row), true);
                    dataInterfacesForDataSet.add(dataInterface);
                }
            }
            // Create subsequent batches
            for (int slotIndex = 1; slotIndex < getInputSlots().size(); slotIndex++) {
                ACAQDataSlot inputSlot = getInputSlots().get(slotIndex);
                TIntSet rows = dataSetEntry.getValue().get(inputSlot.getName());

                List<ACAQDataInterface> backup = ImmutableList.copyOf(dataInterfacesForDataSet);

                int rowIndex = 0;
                for (TIntIterator it = rows.iterator(); it.hasNext(); ) {
                    int row = it.next();

                    if (rowIndex == 0) {
                        // For the first row just add the row to the existing batches
                        for (ACAQDataInterface dataInterface : dataInterfacesForDataSet) {
                            dataInterface.setData(inputSlot, row);
                            dataInterface.addGlobalAnnotations(inputSlot.getAnnotations(row), true);
                        }
                    } else {
                        // We have to copy each input entry and adapt it to the row
                        for (ACAQDataInterface dataInterface : backup) {
                            ACAQDataInterface copy = new ACAQDataInterface(dataInterface);
                            copy.setData(inputSlot, row);
                            copy.addGlobalAnnotations(inputSlot.getAnnotations(row), true);
                            dataInterfacesForDataSet.add(copy);
                        }
                    }

                    ++rowIndex;
                }

            }

            dataInterfaces.addAll(dataInterfacesForDataSet);
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
                    ACAQRunnerSubStatus slotProgress = subProgress.resolve("Data row " + (rowIndex + 1) + " / " + dataInterfaces.size());
                    algorithmProgress.accept(slotProgress);
                    runIteration(dataInterfaces.get(rowIndex), slotProgress, algorithmProgress, isCancelled);
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
    protected abstract void runIteration(ACAQDataInterface dataInterface, ACAQRunnerSubStatus subProgress, Consumer<ACAQRunnerSubStatus> algorithmProgress, Supplier<Boolean> isCancelled);

    @ACAQDocumentation(name = "Data set matching strategy", description = "Algorithms with multiple inputs require to match the incoming data " +
            "to data sets. This allows you to determine how interesting data annotation columns are extracted from the incoming data. " +
            "Union matches using the union of annotation columns. Intersection intersects the sets of available columns. You can also" +
            " customize which columns should be available.")
    @ACAQParameter(value = "acaq:iterating-algorithm:column-matching", uiOrder = 999, visibility = ACAQParameterVisibility.Visible)
    public ColumnMatching getDataSetMatching() {
        return dataSetMatching;
    }

    @ACAQParameter("acaq:iterating-algorithm:column-matching")
    public void setDataSetMatching(ColumnMatching dataSetMatching) {
        this.dataSetMatching = dataSetMatching;
        getEventBus().post(new ParameterChangedEvent(this, "acaq:iterating-algorithm:column-matching"));
    }

    @ACAQDocumentation(name = "Allow duplicate data sets", description = "If disabled, there will be an error if duplicate data sets are detected. " +
            "Data sets are detected by grouping incoming data via their data annotations.")
    @ACAQParameter(value = "acaq:iterating-algorithm:allow-duplicates", uiOrder = 999, visibility = ACAQParameterVisibility.Visible)
    public boolean isAllowDuplicateDataSets() {
        return allowDuplicateDataSets;
    }

    @ACAQParameter("acaq:iterating-algorithm:allow-duplicates")
    public void setAllowDuplicateDataSets(boolean allowDuplicateDataSets) {
        this.allowDuplicateDataSets = allowDuplicateDataSets;
        getEventBus().post(new ParameterChangedEvent(this, "acaq:iterating-algorithm:allow-duplicates"));
    }

    @ACAQDocumentation(name = "Data set matching annotations", description = "Only used if 'Data set matching strategy' is set to 'Custom'. " +
            "Determines which annotation columns are referred to match data sets.")
    @ACAQParameter(value = "acaq:iterating-algorithm:custom-matched-columns", uiOrder = 999, visibility = ACAQParameterVisibility.Visible)
    @StringParameterSettings(monospace = true, icon = ResourceUtils.RESOURCE_BASE_PATH + "/icons/data-types/imgplus.png")
    public StringList getCustomColumns() {
        if (customColumns == null)
            customColumns = new StringList();
        return customColumns;
    }

    @ACAQParameter(value = "acaq:iterating-algorithm:custom-matched-columns", visibility = ACAQParameterVisibility.Visible)
    public void setCustomColumns(StringList customColumns) {
        this.customColumns = customColumns;
        getEventBus().post(new ParameterChangedEvent(this, "acaq:iterating-algorithm:custom-matched-columns"));
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
        getEventBus().post(new ParameterChangedEvent(this, "acaq:iterating-algorithm:skip-incomplete"));
    }

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

    /**
     * Strategies that determine how to detect the columns that should be used for matching
     */
    public enum ColumnMatching {
        Union,
        Intersection,
        Custom
    }
}
