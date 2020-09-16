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

import com.google.common.collect.HashMultimap;
import com.google.common.collect.Multimap;
import org.hkijena.jipipe.api.JIPipeDocumentation;
import org.hkijena.jipipe.api.JIPipeOrganization;
import org.hkijena.jipipe.api.JIPipeRunnerSubStatus;
import org.hkijena.jipipe.api.JIPipeValidityReport;
import org.hkijena.jipipe.api.nodes.JIPipeDataBatch;
import org.hkijena.jipipe.api.nodes.JIPipeInputSlot;
import org.hkijena.jipipe.api.nodes.JIPipeNodeInfo;
import org.hkijena.jipipe.api.nodes.JIPipeOutputSlot;
import org.hkijena.jipipe.api.nodes.JIPipeSimpleIteratingAlgorithm;
import org.hkijena.jipipe.api.nodes.categories.TableNodeTypeCategory;
import org.hkijena.jipipe.api.parameters.JIPipeParameter;
import org.hkijena.jipipe.extensions.parameters.pairs.StringFilterAndStringOrDoubleFilterPair;
import org.hkijena.jipipe.extensions.parameters.predicates.StringOrDoublePredicate;
import org.hkijena.jipipe.extensions.parameters.util.LogicalOperation;
import org.hkijena.jipipe.extensions.tables.datatypes.ResultsTableData;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.Supplier;
import java.util.stream.Collectors;

/**
 * Algorithm that integrates columns
 */
@JIPipeDocumentation(name = "Filter table", description = "Filters the table by values")
@JIPipeOrganization(nodeTypeCategory = TableNodeTypeCategory.class)
@JIPipeInputSlot(value = ResultsTableData.class, slotName = "Input", autoCreate = true)
@JIPipeOutputSlot(value = ResultsTableData.class, slotName = "Output", autoCreate = true)
public class FilterTableAlgorithm extends JIPipeSimpleIteratingAlgorithm {

    private StringFilterAndStringOrDoubleFilterPair.List filters = new StringFilterAndStringOrDoubleFilterPair.List();
    private LogicalOperation betweenColumnOperation = LogicalOperation.LogicalAnd;
    private LogicalOperation sameColumnOperation = LogicalOperation.LogicalAnd;
    private boolean invert = false;

    /**
     * Creates a new instance
     *
     * @param info algorithm info
     */
    public FilterTableAlgorithm(JIPipeNodeInfo info) {
        super(info);
    }

    /**
     * Creates a copy
     *
     * @param other the original
     */
    public FilterTableAlgorithm(FilterTableAlgorithm other) {
        super(other);
        this.filters = new StringFilterAndStringOrDoubleFilterPair.List(other.filters);
        this.betweenColumnOperation = other.betweenColumnOperation;
        this.sameColumnOperation = other.sameColumnOperation;
        this.invert = other.invert;
    }

    @Override
    protected void runIteration(JIPipeDataBatch dataBatch, JIPipeRunnerSubStatus subProgress, Consumer<JIPipeRunnerSubStatus> algorithmProgress, Supplier<Boolean> isCancelled) {
        ResultsTableData input = dataBatch.getInputData(getFirstInputSlot(), ResultsTableData.class);
        Multimap<String, StringOrDoublePredicate> filterPerColumn = HashMultimap.create();
        for (StringFilterAndStringOrDoubleFilterPair pair : filters) {
            List<String> targetedColumns = input.getColumnNames().stream().filter(pair.getKey()).collect(Collectors.toList());
            for (String targetedColumn : targetedColumns) {
                filterPerColumn.put(targetedColumn, pair.getValue());
            }
        }
        List<Integer> selectedRows = new ArrayList<>();
        for (int row = 0; row < input.getRowCount(); row++) {
            List<Boolean> betweenColumns = new ArrayList<>();
            for (String columnName : filterPerColumn.keySet()) {
                int columnIndex = input.getColumnIndex(columnName);
                boolean isNumeric = input.isNumeric(columnIndex);

                List<Boolean> withinColumn = new ArrayList<>();
                for (StringOrDoublePredicate filter : filterPerColumn.get(columnName)) {
                    boolean result;
                    if (isNumeric) {
                        result = filter.test(input.getValueAsDouble(row, columnIndex));
                    } else {
                        result = filter.test(input.getValueAsString(row, columnIndex));
                    }
                    withinColumn.add(result);
                }

                boolean withinResult = sameColumnOperation.apply(withinColumn);
                betweenColumns.add(withinResult);
            }
            boolean betweenColumnResult = betweenColumnOperation.apply(betweenColumns);
            if (betweenColumnResult == !invert) {
                selectedRows.add(row);
            }
        }

        ResultsTableData output = input.getRows(selectedRows);
        dataBatch.addOutputData(getFirstOutputSlot(), output);
    }

    @Override
    public void reportValidity(JIPipeValidityReport report) {
        report.forCategory("Filters").report(filters);
    }

    @JIPipeDocumentation(name = "Filters", description = "Allows you to select how to filter the values.")
    @JIPipeParameter("filters")
    public StringFilterAndStringOrDoubleFilterPair.List getFilters() {
        return filters;
    }

    @JIPipeParameter("filters")
    public void setFilters(StringFilterAndStringOrDoubleFilterPair.List filters) {
        this.filters = filters;
    }

    @JIPipeParameter("same-column-operation")
    @JIPipeDocumentation(name = "Connect same columns by", description = "The logical operation to apply between filters that filter the same column")
    public LogicalOperation getSameColumnOperation() {
        return sameColumnOperation;
    }

    @JIPipeParameter("same-column-operation")
    public void setSameColumnOperation(LogicalOperation sameColumnOperation) {
        this.sameColumnOperation = sameColumnOperation;
    }

    @JIPipeParameter("between-column-operation")
    @JIPipeDocumentation(name = "Connect different columns by", description = "The logical operation to apply between different columns")
    public LogicalOperation getBetweenColumnOperation() {
        return betweenColumnOperation;
    }

    @JIPipeParameter("between-column-operation")
    public void setBetweenColumnOperation(LogicalOperation betweenColumnOperation) {
        this.betweenColumnOperation = betweenColumnOperation;
    }

    @JIPipeParameter("invert")
    @JIPipeDocumentation(name = "Invert filter", description = "If true, the filter is inverted")
    public boolean isInvert() {
        return invert;
    }

    @JIPipeParameter("invert")
    public void setInvert(boolean invert) {
        this.invert = invert;

    }
}
