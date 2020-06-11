package org.hkijena.acaq5.extensions.tables.algorithms;

import com.google.common.collect.HashMultimap;
import com.google.common.collect.Multimap;
import org.hkijena.acaq5.api.ACAQDocumentation;
import org.hkijena.acaq5.api.ACAQOrganization;
import org.hkijena.acaq5.api.ACAQRunnerSubStatus;
import org.hkijena.acaq5.api.ACAQValidityReport;
import org.hkijena.acaq5.api.algorithm.*;
import org.hkijena.acaq5.api.events.ParameterChangedEvent;
import org.hkijena.acaq5.api.parameters.ACAQParameter;
import org.hkijena.acaq5.extensions.imagejalgorithms.ij1.LogicalOperation;
import org.hkijena.acaq5.extensions.imagejdatatypes.datatypes.ResultsTableData;
import org.hkijena.acaq5.extensions.parameters.pairs.StringFilterAndStringOrDoubleFilterPair;
import org.hkijena.acaq5.extensions.parameters.predicates.StringOrDoublePredicate;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.Supplier;
import java.util.stream.Collectors;

/**
 * Algorithm that integrates columns
 */
@ACAQDocumentation(name = "Filter table", description = "Filters the table by values")
@ACAQOrganization(algorithmCategory = ACAQAlgorithmCategory.Processor, menuPath = "Tables")
@AlgorithmInputSlot(value = ResultsTableData.class, slotName = "Input", autoCreate = true)
@AlgorithmOutputSlot(value = ResultsTableData.class, slotName = "Output", autoCreate = true)
public class FilterTableAlgorithm extends ACAQSimpleIteratingAlgorithm {

    private StringFilterAndStringOrDoubleFilterPair.List filters = new StringFilterAndStringOrDoubleFilterPair.List();
    private LogicalOperation betweenColumnOperation = LogicalOperation.LogicalAnd;
    private LogicalOperation sameColumnOperation = LogicalOperation.LogicalAnd;
    private boolean invert = false;

    /**
     * Creates a new instance
     *
     * @param declaration algorithm declaration
     */
    public FilterTableAlgorithm(ACAQAlgorithmDeclaration declaration) {
        super(declaration);
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
    protected void runIteration(ACAQDataInterface dataInterface, ACAQRunnerSubStatus subProgress, Consumer<ACAQRunnerSubStatus> algorithmProgress, Supplier<Boolean> isCancelled) {
        ResultsTableData input = dataInterface.getInputData(getFirstInputSlot(), ResultsTableData.class);
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
        dataInterface.addOutputData(getFirstOutputSlot(), output);
    }

    @Override
    public void reportValidity(ACAQValidityReport report) {
        report.forCategory("Filters").report(filters);
    }

    @ACAQDocumentation(name = "Filters", description = "Allows you to select how to filter the values.")
    @ACAQParameter("filters")
    public StringFilterAndStringOrDoubleFilterPair.List getFilters() {
        return filters;
    }

    @ACAQParameter("filters")
    public void setFilters(StringFilterAndStringOrDoubleFilterPair.List filters) {
        this.filters = filters;
    }

    @ACAQParameter("same-column-operation")
    @ACAQDocumentation(name = "Connect same columns by", description = "The logical operation to apply between filters that filter the same column")
    public LogicalOperation getSameColumnOperation() {
        return sameColumnOperation;
    }

    @ACAQParameter("same-column-operation")
    public void setSameColumnOperation(LogicalOperation sameColumnOperation) {
        this.sameColumnOperation = sameColumnOperation;
    }

    @ACAQParameter("between-column-operation")
    @ACAQDocumentation(name = "Connect different columns by", description = "The logical operation to apply between different columns")
    public LogicalOperation getBetweenColumnOperation() {
        return betweenColumnOperation;
    }

    @ACAQParameter("between-column-operation")
    public void setBetweenColumnOperation(LogicalOperation betweenColumnOperation) {
        this.betweenColumnOperation = betweenColumnOperation;
    }

    @ACAQParameter("invert")
    @ACAQDocumentation(name = "Invert filter", description = "If true, the filter is inverted")
    public boolean isInvert() {
        return invert;
    }

    @ACAQParameter("invert")
    public void setInvert(boolean invert) {
        this.invert = invert;
        getEventBus().post(new ParameterChangedEvent(this, "invert"));
    }
}
