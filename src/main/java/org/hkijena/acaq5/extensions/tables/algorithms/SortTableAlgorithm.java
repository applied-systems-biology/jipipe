package org.hkijena.acaq5.extensions.tables.algorithms;

import org.hkijena.acaq5.api.ACAQDocumentation;
import org.hkijena.acaq5.api.ACAQOrganization;
import org.hkijena.acaq5.api.ACAQRunnerSubStatus;
import org.hkijena.acaq5.api.ACAQValidityReport;
import org.hkijena.acaq5.api.algorithm.*;
import org.hkijena.acaq5.api.exceptions.UserFriendlyRuntimeException;
import org.hkijena.acaq5.api.parameters.ACAQParameter;
import org.hkijena.acaq5.extensions.imagejalgorithms.ij1.measure.SortOrder;
import org.hkijena.acaq5.extensions.imagejdatatypes.datatypes.ResultsTableData;
import org.hkijena.acaq5.extensions.parameters.pairs.StringFilterAndSortOrderPair;
import org.hkijena.acaq5.extensions.parameters.predicates.StringPredicate;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.function.Consumer;
import java.util.function.Supplier;

/**
 * Algorithm that integrates columns
 */
@ACAQDocumentation(name = "Sort table", description = "Sorts the table by columns")
@ACAQOrganization(algorithmCategory = ACAQAlgorithmCategory.Processor, menuPath = "Tables")
@AlgorithmInputSlot(value = ResultsTableData.class, slotName = "Input", autoCreate = true)
@AlgorithmOutputSlot(value = ResultsTableData.class, slotName = "Output", autoCreate = true)
public class SortTableAlgorithm extends ACAQSimpleIteratingAlgorithm {

    private StringFilterAndSortOrderPair.List sortOrderList = new StringFilterAndSortOrderPair.List();

    /**
     * Creates a new instance
     *
     * @param declaration algorithm declaration
     */
    public SortTableAlgorithm(ACAQAlgorithmDeclaration declaration) {
        super(declaration);
        sortOrderList.addNewInstance();
    }

    /**
     * Creates a copy
     *
     * @param other the original
     */
    public SortTableAlgorithm(SortTableAlgorithm other) {
        super(other);
        this.sortOrderList = new StringFilterAndSortOrderPair.List(other.sortOrderList);
    }

    @Override
    protected void runIteration(ACAQDataInterface dataInterface, ACAQRunnerSubStatus subProgress, Consumer<ACAQRunnerSubStatus> algorithmProgress, Supplier<Boolean> isCancelled) {
        ResultsTableData input = dataInterface.getInputData(getFirstInputSlot(), ResultsTableData.class);

        if (sortOrderList.isEmpty()) {
            dataInterface.addOutputData(getFirstOutputSlot(), input.duplicate());
            return;
        }
        Comparator<Integer> comparator = getRowComparator(sortOrderList.get(0), input);
        for (int i = 1; i < sortOrderList.size(); i++) {
            comparator = comparator.thenComparing(getRowComparator(sortOrderList.get(i), input));
        }

        java.util.List sortedRows = new ArrayList<>();
        for (int i = 0; i < input.getRowCount(); i++) {
            sortedRows.add(i);
        }
        sortedRows.sort(comparator);

        ResultsTableData output = input.getRows(sortedRows);
        dataInterface.addOutputData(getFirstOutputSlot(), output);
    }

    private Comparator<Integer> getRowComparator(StringFilterAndSortOrderPair pair, ResultsTableData input) {
        Comparator<Integer> result = null;
        StringPredicate filter = pair.getKey();
        for (String columnName : input.getColumnNames()) {
            if (filter.test(columnName)) {
                boolean isNumeric = input.isNumeric(input.getColumnIndex(columnName));
                Comparator<Integer> chain = (r1, r2) -> {
                    if (isNumeric) {
                        double v1 = input.getValueAsDouble(r1, columnName);
                        double v2 = input.getValueAsDouble(r2, columnName);
                        if (pair.getValue() == SortOrder.Ascending)
                            return Double.compare(v1, v2);
                        else
                            return -Double.compare(v1, v2);
                    } else {
                        String v1 = input.getValueAsString(r1, columnName);
                        String v2 = input.getValueAsString(r2, columnName);
                        if (pair.getValue() == SortOrder.Ascending)
                            return v1.compareTo(v2);
                        else
                            return -v1.compareTo(v2);
                    }
                };
                if (result == null)
                    result = chain;
                else
                    result = result.thenComparing(chain);
            }
        }
        if (result == null) {
            throw new UserFriendlyRuntimeException("Could not find column that matches '" + filter.toString() + "'!",
                    "Could not find column!",
                    "Algorithm '" + getName() + "'",
                    "A plot generator algorithm was instructed to extract a column matching the rule '" + filter.toString() + "' for plotting. The column could note be found. " +
                            "The table contains only following columns: " + String.join(", ", input.getColumnNames()),
                    "Please check if your input columns are set up with valid filters. Please check the input of the plot generator " +
                            "via the testbench to see if the input data is correct. You can also select a generator instead of picking a column.");
        }
        return result;
    }

    @Override
    public void reportValidity(ACAQValidityReport report) {
        report.forCategory("Filters").report(sortOrderList);
    }

    @ACAQDocumentation(name = "Filters", description = "Allows you determine by which columns the table is sorted. The order determines the " +
            "sorting priority. Columns can be matched multiple times.")
    @ACAQParameter("sort-order")
    public StringFilterAndSortOrderPair.List getSortOrderList() {
        return sortOrderList;
    }

    @ACAQParameter("sort-order")
    public void setSortOrderList(StringFilterAndSortOrderPair.List sortOrderList) {
        this.sortOrderList = sortOrderList;
    }
}
