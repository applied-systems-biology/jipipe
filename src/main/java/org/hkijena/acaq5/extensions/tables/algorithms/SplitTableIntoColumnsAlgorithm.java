package org.hkijena.acaq5.extensions.tables.algorithms;

import org.hkijena.acaq5.api.ACAQDocumentation;
import org.hkijena.acaq5.api.ACAQOrganization;
import org.hkijena.acaq5.api.ACAQRunnerSubStatus;
import org.hkijena.acaq5.api.ACAQValidityReport;
import org.hkijena.acaq5.api.algorithm.*;
import org.hkijena.acaq5.api.data.ACAQAnnotation;
import org.hkijena.acaq5.api.parameters.ACAQParameter;
import org.hkijena.acaq5.extensions.imagejdatatypes.datatypes.ResultsTableData;
import org.hkijena.acaq5.extensions.parameters.predicates.StringPredicate;
import org.hkijena.acaq5.extensions.tables.datatypes.TableColumn;
import org.hkijena.acaq5.utils.StringUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.Supplier;

/**
 * Algorithm that integrates columns
 */
@ACAQDocumentation(name = "Split table into columns", description = "Splits a table into individual columns")
@ACAQOrganization(algorithmCategory = ACAQAlgorithmCategory.Processor, menuPath = "Tables")
@AlgorithmInputSlot(value = ResultsTableData.class, slotName = "Input", autoCreate = true)
@AlgorithmOutputSlot(value = TableColumn.class, slotName = "Output", autoCreate = true)
public class SplitTableIntoColumnsAlgorithm extends ACAQSimpleIteratingAlgorithm {

    private String generatedAnnotation = "Column header";
    private StringPredicate.List filters = new StringPredicate.List();

    /**
     * Creates a new instance
     *
     * @param declaration algorithm declaration
     */
    public SplitTableIntoColumnsAlgorithm(ACAQAlgorithmDeclaration declaration) {
        super(declaration);
    }

    /**
     * Creates a copy
     *
     * @param other the original
     */
    public SplitTableIntoColumnsAlgorithm(SplitTableIntoColumnsAlgorithm other) {
        super(other);
        this.generatedAnnotation = other.generatedAnnotation;
        this.filters = new StringPredicate.List(other.filters);
    }

    @Override
    protected void runIteration(ACAQDataInterface dataInterface, ACAQRunnerSubStatus subProgress, Consumer<ACAQRunnerSubStatus> algorithmProgress, Supplier<Boolean> isCancelled) {
        ResultsTableData input = dataInterface.getInputData(getFirstInputSlot(), ResultsTableData.class);
        for (String columnName : input.getColumnNames()) {
            if (filters.isEmpty() || filters.test(columnName)) {
                TableColumn column = input.getColumnCopy(input.getColumnIndex(columnName));
                List<ACAQAnnotation> traitList = new ArrayList<>();
                if (!StringUtils.isNullOrEmpty(generatedAnnotation)) {
                    traitList.add(new ACAQAnnotation(generatedAnnotation, columnName));
                }
                dataInterface.addOutputData(getFirstOutputSlot(), column, traitList);
            }
        }
    }

    @Override
    public void reportValidity(ACAQValidityReport report) {
        report.forCategory("Filters").report(filters);
    }

    @ACAQDocumentation(name = "Generated annotation", description = "Optional. The annotation that is created for each table column. The column header will be stored inside it.")
    @ACAQParameter("generated-annotation")
    public String getGeneratedAnnotation() {
        return generatedAnnotation;
    }

    @ACAQParameter("generated-annotation")
    public void setGeneratedAnnotation(String generatedAnnotation) {
        this.generatedAnnotation = generatedAnnotation;
    }

    @ACAQDocumentation(name = "Filters", description = "Allows you to filter only specific columns that will be extracted. The filters are connected via OR.")
    @ACAQParameter("filters")
    public StringPredicate.List getFilters() {
        return filters;
    }

    @ACAQParameter("filters")
    public void setFilters(StringPredicate.List filters) {
        this.filters = filters;
    }
}
