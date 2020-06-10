package org.hkijena.acaq5.extensions.tables.algorithms;

import org.hkijena.acaq5.api.ACAQDocumentation;
import org.hkijena.acaq5.api.ACAQOrganization;
import org.hkijena.acaq5.api.ACAQRunnerSubStatus;
import org.hkijena.acaq5.api.ACAQValidityReport;
import org.hkijena.acaq5.api.algorithm.*;
import org.hkijena.acaq5.api.exceptions.UserFriendlyRuntimeException;
import org.hkijena.acaq5.api.parameters.ACAQParameter;
import org.hkijena.acaq5.api.registries.ACAQTableRegistry;
import org.hkijena.acaq5.extensions.imagejdatatypes.datatypes.ResultsTableData;
import org.hkijena.acaq5.extensions.tables.datatypes.TableColumn;
import org.hkijena.acaq5.extensions.tables.operations.ColumnOperation;
import org.hkijena.acaq5.extensions.tables.parameters.collections.IntegratingTableColumnProcessorParameterList;
import org.hkijena.acaq5.extensions.tables.parameters.processors.IntegratingTableColumnProcessorParameter;
import org.hkijena.acaq5.utils.StringUtils;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.function.Consumer;
import java.util.function.Supplier;

/**
 * Algorithm that integrates columns
 */
@ACAQDocumentation(name = "Integrate table columns", description = "Integrates table columns by applying operations like average, standard deviation, or median")
@ACAQOrganization(algorithmCategory = ACAQAlgorithmCategory.Processor, menuPath = "Tables")
@AlgorithmInputSlot(value = ResultsTableData.class, slotName = "Input", autoCreate = true)
@AlgorithmOutputSlot(value = ResultsTableData.class, slotName = "Output", autoCreate = true)
public class IntegrateColumnsAlgorithm extends ACAQSimpleIteratingAlgorithm {

    private IntegratingTableColumnProcessorParameterList processorParameters = new IntegratingTableColumnProcessorParameterList();

    /**
     * Creates a new instance
     *
     * @param declaration algorithm declaration
     */
    public IntegrateColumnsAlgorithm(ACAQAlgorithmDeclaration declaration) {
        super(declaration);
        processorParameters.addNewInstance();
    }

    /**
     * Creates a copy
     *
     * @param other the original
     */
    public IntegrateColumnsAlgorithm(IntegrateColumnsAlgorithm other) {
        super(other);
        this.processorParameters = new IntegratingTableColumnProcessorParameterList(other.processorParameters);
    }

    @Override
    protected void runIteration(ACAQDataInterface dataInterface, ACAQRunnerSubStatus subProgress, Consumer<ACAQRunnerSubStatus> algorithmProgress, Supplier<Boolean> isCancelled) {
        ResultsTableData input = dataInterface.getInputData(getFirstInputSlot(), ResultsTableData.class);
        Map<String, TableColumn> resultColumns = new HashMap<>();
        for (IntegratingTableColumnProcessorParameter processor : processorParameters) {
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
            TableColumn resultColumn = columnOperation.run(sourceColumnData);
            resultColumns.put(processor.getOutput(), resultColumn);
        }

        // Combine into one table
        ResultsTableData output = new ResultsTableData(resultColumns);
        dataInterface.addOutputData(getFirstOutputSlot(), output);
    }

    @Override
    public void reportValidity(ACAQValidityReport report) {
        report.forCategory("Processors").report(processorParameters);
        Set<String> columnNames = new HashSet<>();
        for (IntegratingTableColumnProcessorParameter parameter : processorParameters) {
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
    public IntegratingTableColumnProcessorParameterList getProcessorParameters() {
        return processorParameters;
    }

    @ACAQParameter("processors")
    public void setProcessorParameters(IntegratingTableColumnProcessorParameterList processorParameters) {
        this.processorParameters = processorParameters;
    }
}
