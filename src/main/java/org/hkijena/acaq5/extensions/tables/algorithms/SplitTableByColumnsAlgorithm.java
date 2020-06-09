package org.hkijena.acaq5.extensions.tables.algorithms;

import org.hkijena.acaq5.api.ACAQDocumentation;
import org.hkijena.acaq5.api.ACAQOrganization;
import org.hkijena.acaq5.api.ACAQRunnerSubStatus;
import org.hkijena.acaq5.api.ACAQValidityReport;
import org.hkijena.acaq5.api.algorithm.*;
import org.hkijena.acaq5.api.parameters.ACAQParameter;
import org.hkijena.acaq5.api.registries.ACAQTraitRegistry;
import org.hkijena.acaq5.api.traits.ACAQTrait;
import org.hkijena.acaq5.extensions.imagejdatatypes.datatypes.ResultsTableData;
import org.hkijena.acaq5.extensions.parameters.collections.StringFilterListParameter;
import org.hkijena.acaq5.extensions.parameters.references.ACAQTraitDeclarationRef;
import org.hkijena.acaq5.extensions.tables.datatypes.TableColumn;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
import java.util.function.Supplier;
import java.util.stream.Collectors;

/**
 * Algorithm that integrates columns
 */
@ACAQDocumentation(name = "Split table by columns", description = "Splits a table into multiple tables according to list of selected columns. " +
        "Sub-tables that have the same values in the selected columns are put into the same output table.")
@ACAQOrganization(algorithmCategory = ACAQAlgorithmCategory.Processor, menuPath = "Tables")
@AlgorithmInputSlot(value = ResultsTableData.class, slotName = "Input", autoCreate = true)
@AlgorithmOutputSlot(value = TableColumn.class, slotName = "Output", autoCreate = true)
public class SplitTableByColumnsAlgorithm extends ACAQSimpleIteratingAlgorithm {

    private ACAQTraitDeclarationRef generatedAnnotation = new ACAQTraitDeclarationRef(ACAQTraitRegistry.getInstance().getDeclarationById("row-filter"));
    private StringFilterListParameter columns = new StringFilterListParameter();

    /**
     * Creates a new instance
     *
     * @param declaration algorithm declaration
     */
    public SplitTableByColumnsAlgorithm(ACAQAlgorithmDeclaration declaration) {
        super(declaration);
    }

    @Override
    protected void runIteration(ACAQDataInterface dataInterface, ACAQRunnerSubStatus subProgress, Consumer<ACAQRunnerSubStatus> algorithmProgress, Supplier<Boolean> isCancelled) {
       ResultsTableData input = dataInterface.getInputData(getFirstInputSlot(), ResultsTableData.class);
        List<String> interestingColumns = input.getColumnNames().stream().filter(columns).distinct().collect(Collectors.toList());
        if(interestingColumns.isEmpty()) {
            dataInterface.addOutputData(getFirstOutputSlot(), input.duplicate());
        }
        else {
            List<String> rowConditions = new ArrayList<>();
            for (int row = 0; row < input.getRowCount(); row++) {
                int finalRow = row;
                rowConditions.add(interestingColumns.stream().map(col -> col + "=" + input.getValueAsString(finalRow, col)).collect(Collectors.joining(";")));
            }
            List<Integer> rows = new ArrayList<>(input.getRowCount());
            for (int i = 0; i < input.getRowCount(); i++) {
                rows.add(i);
            }
            Map<String, List<Integer>> groupedByCondition = rows.stream().collect(Collectors.groupingBy(rowConditions::get));
            for (Map.Entry<String, List<Integer>> entry : groupedByCondition.entrySet()) {
                   ResultsTableData output = input.getRows(entry.getValue());
                   List<ACAQTrait> traits = new ArrayList<>();
                   if(generatedAnnotation.getDeclaration() != null) {
                       traits.add(generatedAnnotation.getDeclaration().newInstance(entry.getKey()));
                   }
                   dataInterface.addOutputData(getFirstOutputSlot(), output, traits);
            }
        }
    }

    /**
     * Creates a copy
     *
     * @param other the original
     */
    public SplitTableByColumnsAlgorithm(SplitTableByColumnsAlgorithm other) {
        super(other);
        this.generatedAnnotation = new ACAQTraitDeclarationRef(other.generatedAnnotation);
        this.columns = new StringFilterListParameter(other.columns);
    }

    @Override
    public void reportValidity(ACAQValidityReport report) {
        report.forCategory("Filters").report(columns);
    }

    @ACAQDocumentation(name = "Generated annotation", description = "Optional. The annotation that is created for each table column. The column header will be stored inside it.")
    @ACAQParameter("generated-annotation")
    public ACAQTraitDeclarationRef getGeneratedAnnotation() {
        return generatedAnnotation;
    }

    @ACAQParameter("generated-annotation")
    public void setGeneratedAnnotation(ACAQTraitDeclarationRef generatedAnnotation) {
        this.generatedAnnotation = generatedAnnotation;
    }

    @ACAQDocumentation(name = "Selected columns", description = "The list of columns to select")
    @ACAQParameter("columns")
    public StringFilterListParameter getColumns() {
        return columns;
    }

    @ACAQParameter("columns")
    public void setColumns(StringFilterListParameter columns) {
        this.columns = columns;
    }
}
