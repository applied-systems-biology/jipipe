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

package org.hkijena.jipipe.extensions.tables.algorithms;

import org.hkijena.jipipe.api.JIPipeDocumentation;
import org.hkijena.jipipe.api.JIPipeOrganization;
import org.hkijena.jipipe.api.JIPipeRunnerSubStatus;
import org.hkijena.jipipe.api.JIPipeValidityReport;
import org.hkijena.jipipe.api.algorithm.*;
import org.hkijena.jipipe.api.exceptions.UserFriendlyRuntimeException;
import org.hkijena.jipipe.api.parameters.JIPipeParameter;
import org.hkijena.jipipe.extensions.parameters.pairs.StringFilterAndSortOrderPair;
import org.hkijena.jipipe.extensions.parameters.predicates.StringPredicate;
import org.hkijena.jipipe.extensions.parameters.util.SortOrder;
import org.hkijena.jipipe.extensions.tables.ResultsTableData;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.function.Consumer;
import java.util.function.Supplier;

/**
 * Algorithm that integrates columns
 */
@JIPipeDocumentation(name = "Sort table", description = "Sorts the table by columns")
@JIPipeOrganization(algorithmCategory = JIPipeAlgorithmCategory.Processor, menuPath = "Tables")
@AlgorithmInputSlot(value = ResultsTableData.class, slotName = "Input", autoCreate = true)
@AlgorithmOutputSlot(value = ResultsTableData.class, slotName = "Output", autoCreate = true)
public class SortTableAlgorithm extends JIPipeSimpleIteratingAlgorithm {

    private StringFilterAndSortOrderPair.List sortOrderList = new StringFilterAndSortOrderPair.List();

    /**
     * Creates a new instance
     *
     * @param declaration algorithm declaration
     */
    public SortTableAlgorithm(JIPipeAlgorithmDeclaration declaration) {
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
    protected void runIteration(JIPipeDataInterface dataInterface, JIPipeRunnerSubStatus subProgress, Consumer<JIPipeRunnerSubStatus> algorithmProgress, Supplier<Boolean> isCancelled) {
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
                            "via the quick run to see if the input data is correct. You can also select a generator instead of picking a column.");
        }
        return result;
    }

    @Override
    public void reportValidity(JIPipeValidityReport report) {
        report.forCategory("Filters").report(sortOrderList);
    }

    @JIPipeDocumentation(name = "Filters", description = "Allows you determine by which columns the table is sorted. The order determines the " +
            "sorting priority. Columns can be matched multiple times.")
    @JIPipeParameter("sort-order")
    public StringFilterAndSortOrderPair.List getSortOrderList() {
        return sortOrderList;
    }

    @JIPipeParameter("sort-order")
    public void setSortOrderList(StringFilterAndSortOrderPair.List sortOrderList) {
        this.sortOrderList = sortOrderList;
    }
}
