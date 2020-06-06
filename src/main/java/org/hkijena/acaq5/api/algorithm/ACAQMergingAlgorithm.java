package org.hkijena.acaq5.api.algorithm;

import com.google.common.collect.ImmutableList;
import gnu.trove.iterator.TIntIterator;
import gnu.trove.set.TIntSet;
import gnu.trove.set.hash.TIntHashSet;
import org.hkijena.acaq5.api.ACAQDocumentation;
import org.hkijena.acaq5.api.ACAQRunnerSubStatus;
import org.hkijena.acaq5.api.data.ACAQDataSlot;
import org.hkijena.acaq5.api.data.ACAQSlotConfiguration;
import org.hkijena.acaq5.api.data.traits.ACAQTraitConfiguration;
import org.hkijena.acaq5.api.events.ParameterChangedEvent;
import org.hkijena.acaq5.api.exceptions.UserFriendlyRuntimeException;
import org.hkijena.acaq5.api.parameters.ACAQParameter;
import org.hkijena.acaq5.api.parameters.ACAQParameterVisibility;
import org.hkijena.acaq5.api.traits.ACAQTrait;
import org.hkijena.acaq5.api.traits.ACAQTraitDeclaration;
import org.hkijena.acaq5.api.traits.ACAQTraitDeclarationRefList;
import org.hkijena.acaq5.extensions.parameters.references.ACAQTraitDeclarationRef;

import java.util.*;
import java.util.function.Consumer;
import java.util.function.Supplier;

/**
 * An {@link ACAQAlgorithm} that applies a similar algorithm to {@link ACAQIteratingAlgorithm}, but does create {@link ACAQMultiDataInterface} instead.
 * This algorithm instead just groups the data based on the annotations and passes those groups to
 * the runIteration() function. This is useful for merging algorithms.
 * Please note that the single-input case will still group the data into multiple groups, or just one group if no grouping could be acquired.
 */
public abstract class ACAQMergingAlgorithm extends ACAQAlgorithm {

    private ACAQIteratingAlgorithm.ColumnMatching dataSetMatching = ACAQIteratingAlgorithm.ColumnMatching.Intersection;
    private boolean skipIncompleteDataSets = false;
    private ACAQTraitDeclarationRefList customColumns = new ACAQTraitDeclarationRefList();

    /**
     * Creates a new instance
     *
     * @param declaration        Algorithm declaration
     * @param slotConfiguration  Slot configuration override
     * @param traitConfiguration Trait configuration override
     */
    public ACAQMergingAlgorithm(ACAQAlgorithmDeclaration declaration, ACAQSlotConfiguration slotConfiguration, ACAQTraitConfiguration traitConfiguration) {
        super(declaration, slotConfiguration, traitConfiguration);
    }

    /**
     * Creates a new instance
     *
     * @param declaration       Algorithm declaration
     * @param slotConfiguration Slot configuration override
     */
    public ACAQMergingAlgorithm(ACAQAlgorithmDeclaration declaration, ACAQSlotConfiguration slotConfiguration) {
        super(declaration, slotConfiguration, null);
    }

    /**
     * Creates a new instance
     *
     * @param declaration Algorithm declaration
     */
    public ACAQMergingAlgorithm(ACAQAlgorithmDeclaration declaration) {
        super(declaration, null, null);
    }

    /**
     * Copies the algorithm
     *
     * @param other The original
     */
    public ACAQMergingAlgorithm(ACAQMergingAlgorithm other) {
        super(other);
        this.dataSetMatching = other.dataSetMatching;
        this.skipIncompleteDataSets = other.skipIncompleteDataSets;
        this.customColumns = new ACAQTraitDeclarationRefList(other.customColumns);
    }

    /**
     * Returns annotation types that should be ignored by the internal logic.
     * Use this if you have some counting/sorting annotation that should not be included into the set of annotations used to match data.
     *
     * @return annotation types that should be ignored by the internal logic
     */
    protected Set<ACAQTraitDeclaration> getIgnoredTraitColumns() {
        return Collections.emptySet();
    }

    @Override
    public void run(ACAQRunnerSubStatus subProgress, Consumer<ACAQRunnerSubStatus> algorithmProgress, Supplier<Boolean> isCancelled) {
        // Special case: No input slots
        if (getInputSlots().isEmpty()) {
            if (isCancelled.get())
                return;
            final int row = 0;
            ACAQRunnerSubStatus slotProgress = subProgress.resolve("Data row " + (row + 1) + " / " + 1);
            algorithmProgress.accept(slotProgress);
            ACAQMultiDataInterface dataInterface = new ACAQMultiDataInterface(this);
            runIteration(dataInterface, slotProgress, algorithmProgress, isCancelled);
            return;
        }

        // First find all columns to sort by
        Set<ACAQTraitDeclaration> referenceTraitColumns;

        switch (dataSetMatching) {
            case Custom:
                referenceTraitColumns = new HashSet<>();
                for (ACAQTraitDeclarationRef customColumn : customColumns) {
                    referenceTraitColumns.add(customColumn.getDeclaration());
                }
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

        // Remove ignored columns
        referenceTraitColumns.removeAll(getIgnoredTraitColumns());

        // Organize the input data by Dataset -> Slot -> Data row
        Map<ACAQDataSetKey, Map<String, TIntSet>> dataSets = new HashMap<>();
        for (ACAQDataSlot inputSlot : getInputSlots()) {
            for (int row = 0; row < inputSlot.getRowCount(); row++) {
                ACAQDataSetKey key = new ACAQDataSetKey();
                for (ACAQTraitDeclaration referenceTraitColumn : referenceTraitColumns) {
                    key.getEntries().put(referenceTraitColumn, null);
                }
                for (ACAQTrait annotation : inputSlot.getAnnotations(row)) {
                    if (annotation != null && referenceTraitColumns.contains(annotation.getDeclaration())) {
                        key.getEntries().put(annotation.getDeclaration(), annotation);
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
                            "Please check the input of the algorithm by running the testbench on each input algorithm. " +
                                    "You can also choose to skip incomplete data sets, although you might lose data in those cases.");
                }
                dataSets.remove(dataSetEntry.getKey());
            }
        }

        // Generate data interfaces
        List<ACAQMultiDataInterface> dataInterfaces = new ArrayList<>();
        for (Map.Entry<ACAQDataSetKey, Map<String, TIntSet>> dataSetEntry : ImmutableList.copyOf(dataSets.entrySet())) {

            ACAQMultiDataInterface dataInterface = new ACAQMultiDataInterface(this);
            for (Map.Entry<String, TIntSet> dataSlotEntry : dataSetEntry.getValue().entrySet()) {
                ACAQDataSlot inputSlot = getInputSlot(dataSlotEntry.getKey());
                TIntSet rows = dataSetEntry.getValue().get(inputSlot.getName());
                for (TIntIterator it = rows.iterator(); it.hasNext(); ) {
                    int row = it.next();
                    dataInterface.addData(inputSlot, row);
                    dataInterface.addGlobalAnnotations(inputSlot.getAnnotations(row), true);
                }
            }

            dataInterfaces.add(dataInterface);
        }

        for (int i = 0; i < dataInterfaces.size(); i++) {
            if (isCancelled.get())
                return;
            ACAQRunnerSubStatus slotProgress = subProgress.resolve("Data row " + (i + 1) + " / " + dataInterfaces.size());
            algorithmProgress.accept(slotProgress);
            runIteration(dataInterfaces.get(i), slotProgress, algorithmProgress, isCancelled);
        }
    }

    private Set<ACAQTraitDeclaration> getInputTraitColumnIntersection() {
        Set<ACAQTraitDeclaration> result = null;
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

    private Set<ACAQTraitDeclaration> getInputTraitColumnUnion() {
        Set<ACAQTraitDeclaration> result = new HashSet<>();
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
    protected abstract void runIteration(ACAQMultiDataInterface dataInterface, ACAQRunnerSubStatus subProgress, Consumer<ACAQRunnerSubStatus> algorithmProgress, Supplier<Boolean> isCancelled);

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
        getEventBus().post(new ParameterChangedEvent(this, "acaq:iterating-algorithm:column-matching"));
    }

    @ACAQDocumentation(name = "Data set matching annotations", description = "Only used if 'Data set matching strategy' is set to 'Custom'. " +
            "Determines which annotation columns are referred to match data sets.")
    @ACAQParameter(value = "acaq:iterating-algorithm:custom-matched-columns", uiOrder = 999, visibility = ACAQParameterVisibility.Visible)
    public ACAQTraitDeclarationRefList getCustomColumns() {
        if (customColumns == null)
            customColumns = new ACAQTraitDeclarationRefList();
        return customColumns;
    }

    @ACAQParameter(value = "acaq:iterating-algorithm:custom-matched-columns", visibility = ACAQParameterVisibility.Visible)
    public void setCustomColumns(ACAQTraitDeclarationRefList customColumns) {
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
}
