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

package org.hkijena.acaq5.extensions.tables.algorithms;

import org.hkijena.acaq5.api.ACAQDocumentation;
import org.hkijena.acaq5.api.ACAQOrganization;
import org.hkijena.acaq5.api.ACAQRunnerSubStatus;
import org.hkijena.acaq5.api.ACAQValidityReport;
import org.hkijena.acaq5.api.algorithm.ACAQAlgorithmCategory;
import org.hkijena.acaq5.api.algorithm.ACAQAlgorithmDeclaration;
import org.hkijena.acaq5.api.algorithm.ACAQDataInterface;
import org.hkijena.acaq5.api.algorithm.ACAQSimpleIteratingAlgorithm;
import org.hkijena.acaq5.api.algorithm.AlgorithmInputSlot;
import org.hkijena.acaq5.api.algorithm.AlgorithmOutputSlot;
import org.hkijena.acaq5.api.data.ACAQAnnotation;
import org.hkijena.acaq5.api.parameters.ACAQParameter;
import org.hkijena.acaq5.extensions.imagejdatatypes.datatypes.ResultsTableData;
import org.hkijena.acaq5.extensions.parameters.predicates.StringPredicate;
import org.hkijena.acaq5.extensions.parameters.primitives.StringParameterSettings;
import org.hkijena.acaq5.extensions.tables.datatypes.TableColumn;
import org.hkijena.acaq5.utils.ResourceUtils;
import org.hkijena.acaq5.utils.StringUtils;

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

    private String generatedAnnotation = "Row filter";
    private StringPredicate.List columns = new StringPredicate.List();

    /**
     * Creates a new instance
     *
     * @param declaration algorithm declaration
     */
    public SplitTableByColumnsAlgorithm(ACAQAlgorithmDeclaration declaration) {
        super(declaration);
    }

    /**
     * Creates a copy
     *
     * @param other the original
     */
    public SplitTableByColumnsAlgorithm(SplitTableByColumnsAlgorithm other) {
        super(other);
        this.generatedAnnotation = other.generatedAnnotation;
        this.columns = new StringPredicate.List(other.columns);
    }

    @Override
    protected void runIteration(ACAQDataInterface dataInterface, ACAQRunnerSubStatus subProgress, Consumer<ACAQRunnerSubStatus> algorithmProgress, Supplier<Boolean> isCancelled) {
        ResultsTableData input = dataInterface.getInputData(getFirstInputSlot(), ResultsTableData.class);
        List<String> interestingColumns = input.getColumnNames().stream().filter(columns).distinct().collect(Collectors.toList());
        if (interestingColumns.isEmpty()) {
            dataInterface.addOutputData(getFirstOutputSlot(), input.duplicate());
        } else {
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
                List<ACAQAnnotation> traits = new ArrayList<>();
                if (!StringUtils.isNullOrEmpty(generatedAnnotation)) {
                    traits.add(new ACAQAnnotation(generatedAnnotation, entry.getKey()));
                }
                dataInterface.addOutputData(getFirstOutputSlot(), output, traits);
            }
        }
    }

    @Override
    public void reportValidity(ACAQValidityReport report) {
        report.forCategory("Filters").report(columns);
    }

    @ACAQDocumentation(name = "Generated annotation", description = "Optional. The annotation that is created for each table column. The column header will be stored inside it.")
    @ACAQParameter("generated-annotation")
    @StringParameterSettings(monospace = true, icon = ResourceUtils.RESOURCE_BASE_PATH + "/icons/annotation.png")
    public String getGeneratedAnnotation() {
        return generatedAnnotation;
    }

    @ACAQParameter("generated-annotation")
    public void setGeneratedAnnotation(String generatedAnnotation) {
        this.generatedAnnotation = generatedAnnotation;
    }

    @ACAQDocumentation(name = "Selected columns", description = "The list of columns to select")
    @ACAQParameter("columns")
    public StringPredicate.List getColumns() {
        return columns;
    }

    @ACAQParameter("columns")
    public void setColumns(StringPredicate.List columns) {
        this.columns = columns;
    }
}
