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
import org.hkijena.pipelinej.api.exceptions.UserFriendlyRuntimeException;
import org.hkijena.pipelinej.api.parameters.ACAQParameter;
import org.hkijena.pipelinej.api.registries.ACAQTableRegistry;
import org.hkijena.pipelinej.extensions.tables.ResultsTableData;
import org.hkijena.pipelinej.extensions.tables.TableColumn;
import org.hkijena.pipelinej.extensions.tables.ColumnOperation;
import org.hkijena.pipelinej.extensions.tables.parameters.collections.ConvertingTableColumnProcessorParameterList;
import org.hkijena.pipelinej.extensions.tables.parameters.processors.ConvertingTableColumnProcessorParameter;
import org.hkijena.pipelinej.utils.StringUtils;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.function.Consumer;
import java.util.function.Supplier;

/**
 * Algorithm that integrates columns
 */
@ACAQDocumentation(name = "Apply function to each cell", description = "Applies a function to each individual cell")
@ACAQOrganization(algorithmCategory = ACAQAlgorithmCategory.Processor, menuPath = "Tables")
@AlgorithmInputSlot(value = ResultsTableData.class, slotName = "Input", autoCreate = true)
@AlgorithmOutputSlot(value = ResultsTableData.class, slotName = "Output", autoCreate = true)
public class ConvertColumnsAlgorithm extends ACAQSimpleIteratingAlgorithm {

    private ConvertingTableColumnProcessorParameterList processorParameters = new ConvertingTableColumnProcessorParameterList();
    private boolean append = true;

    /**
     * Creates a new instance
     *
     * @param declaration algorithm declaration
     */
    public ConvertColumnsAlgorithm(ACAQAlgorithmDeclaration declaration) {
        super(declaration);
        processorParameters.addNewInstance();
    }

    /**
     * Creates a copy
     *
     * @param other the original
     */
    public ConvertColumnsAlgorithm(ConvertColumnsAlgorithm other) {
        super(other);
        this.processorParameters = new ConvertingTableColumnProcessorParameterList(other.processorParameters);
        this.append = other.append;
    }

    @Override
    protected void runIteration(ACAQDataInterface dataInterface, ACAQRunnerSubStatus subProgress, Consumer<ACAQRunnerSubStatus> algorithmProgress, Supplier<Boolean> isCancelled) {
        ResultsTableData input = dataInterface.getInputData(getFirstInputSlot(), ResultsTableData.class);
        Map<String, TableColumn> resultColumns = new HashMap<>();
        for (ConvertingTableColumnProcessorParameter processor : processorParameters) {
            String sourceColumn = input.getColumnNames().stream().filter(processor.getInput()).findFirst().orElse(null);
            if (sourceColumn == null) {
                throw new UserFriendlyRuntimeException(new NullPointerException(),
                        "Unable to find column matching " + processor.getInput(),
                        "Algorithm '" + getName() + "'",
                        "The column filter '" + processor.getInput() + "' tried to find a matching column in " + String.join(", ", input.getColumnNames()) + ". None of the columns matched.",
                        "Please check if the filter is correct.");
            }
            TableColumn sourceColumnData = input.getColumnReference(input.getColumnIndex(sourceColumn));
            ColumnOperation columnOperation = ((ACAQTableRegistry.ColumnOperationEntry) processor.getParameter().getValue()).getOperation();
            TableColumn resultColumn = columnOperation.apply(sourceColumnData);
            resultColumns.put(processor.getOutput(), resultColumn);
        }
        if (append) {
            for (String columnName : input.getColumnNames()) {
                if (!resultColumns.containsKey(columnName)) {
                    resultColumns.put(columnName, input.getColumnReference(input.getColumnIndex(columnName)));
                }
            }
        }

        // Combine into one table
        ResultsTableData output = new ResultsTableData(resultColumns);
        dataInterface.addOutputData(getFirstOutputSlot(), output);
    }

    @Override
    public void reportValidity(ACAQValidityReport report) {
        report.forCategory("Processors").report(processorParameters);
        Set<String> columnNames = new HashSet<>();
        for (ConvertingTableColumnProcessorParameter parameter : processorParameters) {
            if (columnNames.contains(parameter.getOutput())) {
                report.forCategory("Processors").reportIsInvalid("Duplicate output column: " + parameter.getOutput(),
                        "There should not be multiple output columns with the same name.",
                        "Change the name to a unique non-empty string",
                        this);
                break;
            }
            if (StringUtils.isNullOrEmpty(parameter.getOutput())) {
                report.forCategory("Processors").reportIsInvalid("An output column has no name!",
                        "All output columns must have a non-empty name.",
                        "Change the name to a non-empty string",
                        this);
                break;
            }
            columnNames.add(parameter.getOutput());
        }
    }

    @ACAQDocumentation(name = "Processors", description = "Defines which columns are processed")
    @ACAQParameter("processors")
    public ConvertingTableColumnProcessorParameterList getProcessorParameters() {
        return processorParameters;
    }

    @ACAQParameter("processors")
    public void setProcessorParameters(ConvertingTableColumnProcessorParameterList processorParameters) {
        this.processorParameters = processorParameters;
    }

    @ACAQDocumentation(name = "Append to existing table", description = "If enabled, the converted columns are appended to the existing table. Existing columns are overwritten.")
    @ACAQParameter("append")
    public boolean isAppend() {
        return append;
    }

    @ACAQParameter("append")
    public void setAppend(boolean append) {
        this.append = append;
    }
}
