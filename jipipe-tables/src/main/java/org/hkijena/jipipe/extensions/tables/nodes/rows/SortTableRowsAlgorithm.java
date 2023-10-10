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
 *
 */

package org.hkijena.jipipe.extensions.tables.nodes.rows;

import org.hkijena.jipipe.api.JIPipeDocumentation;
import org.hkijena.jipipe.api.JIPipeNode;
import org.hkijena.jipipe.api.JIPipeProgressInfo;
import org.hkijena.jipipe.api.nodes.*;
import org.hkijena.jipipe.api.nodes.categories.TableNodeTypeCategory;
import org.hkijena.jipipe.api.nodes.databatch.JIPipeDataBatch;
import org.hkijena.jipipe.api.nodes.utils.JIPipeSimpleIteratingAlgorithm;
import org.hkijena.jipipe.api.parameters.JIPipeParameter;
import org.hkijena.jipipe.api.validation.*;
import org.hkijena.jipipe.api.validation.contexts.GraphNodeValidationReportContext;
import org.hkijena.jipipe.api.validation.contexts.ParameterValidationReportContext;
import org.hkijena.jipipe.extensions.expressions.StringQueryExpression;
import org.hkijena.jipipe.extensions.parameters.api.pairs.PairParameterSettings;
import org.hkijena.jipipe.extensions.parameters.library.pairs.StringQueryExpressionAndSortOrderPairParameter;
import org.hkijena.jipipe.extensions.parameters.library.util.SortOrder;
import org.hkijena.jipipe.extensions.tables.datatypes.ResultsTableData;
import org.hkijena.jipipe.utils.NaturalOrderComparator;

import java.util.ArrayList;
import java.util.Comparator;

/**
 * Algorithm that integrates columns
 */
@JIPipeDocumentation(name = "Sort table rows", description = "Sorts the table rows by columns")
@JIPipeNode(nodeTypeCategory = TableNodeTypeCategory.class)
@JIPipeInputSlot(value = ResultsTableData.class, slotName = "Input", autoCreate = true)
@JIPipeOutputSlot(value = ResultsTableData.class, slotName = "Output", autoCreate = true)
public class SortTableRowsAlgorithm extends JIPipeSimpleIteratingAlgorithm {

    private StringQueryExpressionAndSortOrderPairParameter.List sortOrderList = new StringQueryExpressionAndSortOrderPairParameter.List();
    private boolean useNaturalSortOrder = true;
    private boolean reverseSortOrder = false;

    /**
     * Creates a new instance
     *
     * @param info algorithm info
     */
    public SortTableRowsAlgorithm(JIPipeNodeInfo info) {
        super(info);
        sortOrderList.addNewInstance();
    }

    /**
     * Creates a copy
     *
     * @param other the original
     */
    public SortTableRowsAlgorithm(SortTableRowsAlgorithm other) {
        super(other);
        this.useNaturalSortOrder = other.useNaturalSortOrder;
        this.reverseSortOrder = other.reverseSortOrder;
        this.sortOrderList = new StringQueryExpressionAndSortOrderPairParameter.List(other.sortOrderList);
    }

    @Override
    protected void runIteration(JIPipeDataBatch dataBatch, JIPipeProgressInfo progressInfo) {
        ResultsTableData input = dataBatch.getInputData(getFirstInputSlot(), ResultsTableData.class, progressInfo);

        if (sortOrderList.isEmpty()) {
            dataBatch.addOutputData(getFirstOutputSlot(), input.duplicate(progressInfo), progressInfo);
            return;
        }
        Comparator<Integer> comparator = getRowComparator(sortOrderList.get(0), input);
        for (int i = 1; i < sortOrderList.size(); i++) {
            comparator = comparator.thenComparing(getRowComparator(sortOrderList.get(i), input));
        }
        if (reverseSortOrder) {
            comparator = comparator.reversed();
        }

        java.util.List sortedRows = new ArrayList<>();
        for (int i = 0; i < input.getRowCount(); i++) {
            sortedRows.add(i);
        }
        sortedRows.sort(comparator);

        ResultsTableData output = input.getRows(sortedRows);
        dataBatch.addOutputData(getFirstOutputSlot(), output, progressInfo);
    }

    private Comparator<Integer> getRowComparator(StringQueryExpressionAndSortOrderPairParameter pair, ResultsTableData input) {
        Comparator<Integer> result = null;
        StringQueryExpression expression = pair.getKey();
        for (String columnName : input.getColumnNames()) {
            if (expression.test(columnName)) {
                boolean isNumeric = input.isNumericColumn(input.getColumnIndex(columnName));
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
//                        if (pair.getValue() == SortOrder.Ascending)
//                            return v1.compareTo(v2);
//                        else
//                            return -v1.compareTo(v2);
                        if (pair.getValue() == SortOrder.Ascending)
                            return NaturalOrderComparator.INSTANCE.compare(v1, v2);
                        else
                            return -NaturalOrderComparator.INSTANCE.compare(v1, v2);
                    }
                };
                if (result == null)
                    result = chain;
                else
                    result = result.thenComparing(chain);
            }
        }
        if (result == null) {
            throw new JIPipeValidationRuntimeException(new JIPipeValidationReportEntry(JIPipeValidationReportEntryLevel.Error,
                    new GraphNodeValidationReportContext(this),
                    "Could not find column that matches '" + expression.toString() + "'!",
                    "A plot generator algorithm was instructed to extract a column matching the rule '" + expression + "' for plotting. The column could note be found. " +
                            "The table contains only following columns: " + String.join(", ", input.getColumnNames()),
                    "Please check if your input columns are set up with valid filters. Please check the input of the plot generator " +
                            "via the quick run to see if the input data is correct. You can also select a generator instead of picking a column."));
        }
        return result;
    }

    @JIPipeDocumentation(name = "Reverse sort order", description = "If enabled, the sort order is reversed.")
    @JIPipeParameter("reverse-sort-order")
    public boolean isReverseSortOrder() {
        return reverseSortOrder;
    }

    @JIPipeParameter("reverse-sort-order")
    public void setReverseSortOrder(boolean reverseSortOrder) {
        this.reverseSortOrder = reverseSortOrder;
    }

    @Override
    public void reportValidity(JIPipeValidationReportContext context, JIPipeValidationReport report) {
        super.reportValidity(context, report);
        report.report(new ParameterValidationReportContext(this, "Filters", "sort-order"), sortOrderList);
    }

    @JIPipeDocumentation(name = "Filters", description = "Allows you determine by which columns the table is sorted. The order determines the " +
            "sorting priority. Columns can be matched multiple times. ")
    @JIPipeParameter("sort-order")
    @PairParameterSettings(singleRow = false)
    public StringQueryExpressionAndSortOrderPairParameter.List getSortOrderList() {
        return sortOrderList;
    }

    @JIPipeParameter("sort-order")
    public void setSortOrderList(StringQueryExpressionAndSortOrderPairParameter.List sortOrderList) {
        this.sortOrderList = sortOrderList;
    }

    @JIPipeDocumentation(name = "Sort strings by natural order", description = "If enabled, strings are sorted by natural order (e.g. 1, 2, 15 100, ...). If disabled, " +
            "strings are sorted lexicographically (e.g. 1, 15, 100, 2)")
    @JIPipeParameter("use-natural-sort-order")
    public boolean isUseNaturalSortOrder() {
        return useNaturalSortOrder;
    }

    @JIPipeParameter("use-natural-sort-order")
    public void setUseNaturalSortOrder(boolean useNaturalSortOrder) {
        this.useNaturalSortOrder = useNaturalSortOrder;
    }
}
