package org.hkijena.acaq5.extensions.tables.algorithms;

import org.hkijena.acaq5.api.ACAQDocumentation;
import org.hkijena.acaq5.api.ACAQOrganization;
import org.hkijena.acaq5.api.ACAQRunnerSubStatus;
import org.hkijena.acaq5.api.ACAQValidityReport;
import org.hkijena.acaq5.api.algorithm.*;
import org.hkijena.acaq5.api.parameters.ACAQParameter;
import org.hkijena.acaq5.extensions.imagejdatatypes.datatypes.ResultsTableData;
import org.hkijena.acaq5.extensions.parameters.predicates.StringPredicate;

import java.util.function.Consumer;
import java.util.function.Supplier;

/**
 * Algorithm that removes columns
 */
@ACAQDocumentation(name = "Remove table column", description = "Removes one or multiple columns by name")
@ACAQOrganization(algorithmCategory = ACAQAlgorithmCategory.Processor, menuPath = "Tables")
@AlgorithmInputSlot(value = ResultsTableData.class, slotName = "Input", autoCreate = true)
@AlgorithmOutputSlot(value = ResultsTableData.class, slotName = "Output", autoCreate = true)
public class RemoveColumnAlgorithm extends ACAQSimpleIteratingAlgorithm {

    private StringPredicate.List filters = new StringPredicate.List();

    /**
     * Creates a new instance
     *
     * @param declaration algorithm declaration
     */
    public RemoveColumnAlgorithm(ACAQAlgorithmDeclaration declaration) {
        super(declaration);
    }

    /**
     * Creates a copy
     *
     * @param other the original
     */
    public RemoveColumnAlgorithm(RemoveColumnAlgorithm other) {
        super(other);
        this.filters = new StringPredicate.List(filters);
    }

    @Override
    protected void runIteration(ACAQDataInterface dataInterface, ACAQRunnerSubStatus subProgress, Consumer<ACAQRunnerSubStatus> algorithmProgress, Supplier<Boolean> isCancelled) {
        ResultsTableData table = (ResultsTableData) dataInterface.getInputData(getFirstInputSlot(), ResultsTableData.class).duplicate();
        for (int col = 0; col < table.getColumnCount(); col++) {
            if (filters.test(table.getColumnName(col))) {
                table.removeColumnAt(col);
                --col;
            }
        }
        dataInterface.addOutputData(getFirstOutputSlot(), table);
    }

    @Override
    public void reportValidity(ACAQValidityReport report) {
        report.forCategory("Filters").report(filters);
    }

    @ACAQDocumentation(name = "Filters", description = "Please create one or more filters to select the removed columns. " +
            "Filters are linked via a logical OR operation.")
    @ACAQParameter("filters")
    public StringPredicate.List getFilters() {
        return filters;
    }

    @ACAQParameter("filters")
    public void setFilters(StringPredicate.List filters) {
        this.filters = filters;
    }
}
