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

package org.hkijena.pipelinej.extensions.tables.algorithms;

import org.hkijena.pipelinej.api.ACAQDocumentation;
import org.hkijena.pipelinej.api.ACAQOrganization;
import org.hkijena.pipelinej.api.ACAQRunnerSubStatus;
import org.hkijena.pipelinej.api.ACAQValidityReport;
import org.hkijena.pipelinej.api.algorithm.*;
import org.hkijena.pipelinej.api.data.ACAQAnnotation;
import org.hkijena.pipelinej.api.parameters.ACAQParameter;
import org.hkijena.pipelinej.extensions.tables.ResultsTableData;
import org.hkijena.pipelinej.extensions.parameters.predicates.StringPredicate;
import org.hkijena.pipelinej.extensions.parameters.primitives.StringParameterSettings;
import org.hkijena.pipelinej.extensions.tables.TableColumn;
import org.hkijena.pipelinej.utils.ResourceUtils;
import org.hkijena.pipelinej.utils.StringUtils;

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
    @StringParameterSettings(monospace = true, icon = ResourceUtils.RESOURCE_BASE_PATH + "/icons/annotation.png")
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
