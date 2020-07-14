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

import org.hkijena.jipipe.api.JIPipeDocumentation;
import org.hkijena.jipipe.api.JIPipeOrganization;
import org.hkijena.jipipe.api.JIPipeRunnerSubStatus;
import org.hkijena.jipipe.api.JIPipeValidityReport;
import org.hkijena.jipipe.api.algorithm.*;
import org.hkijena.jipipe.api.exceptions.UserFriendlyRuntimeException;
import org.hkijena.jipipe.api.parameters.JIPipeParameter;
import org.hkijena.jipipe.api.registries.JIPipeTableRegistry;
import org.hkijena.jipipe.extensions.tables.ColumnOperation;
import org.hkijena.jipipe.extensions.tables.datatypes.ResultsTableData;
import org.hkijena.jipipe.extensions.tables.datatypes.TableColumn;
import org.hkijena.jipipe.extensions.tables.parameters.collections.ConvertingTableColumnProcessorParameterList;
import org.hkijena.jipipe.extensions.tables.parameters.processors.ConvertingTableColumnProcessorParameter;
import org.hkijena.jipipe.utils.StringUtils;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.function.Consumer;
import java.util.function.Supplier;

/**
 * Algorithm that integrates columns
 */
@JIPipeDocumentation(name = "Apply function to each cell", description = "Applies a function to each individual cell")
@JIPipeOrganization(algorithmCategory = JIPipeNodeCategory.Processor, menuPath = "Tables")
@JIPipeInputSlot(value = ResultsTableData.class, slotName = "Input", autoCreate = true)
@JIPipeOutputSlot(value = ResultsTableData.class, slotName = "Output", autoCreate = true)
public class ConvertColumnsAlgorithm extends JIPipeSimpleIteratingAlgorithm {

    private ConvertingTableColumnProcessorParameterList processorParameters = new ConvertingTableColumnProcessorParameterList();
    private boolean append = true;

    /**
     * Creates a new instance
     *
     * @param info algorithm info
     */
    public ConvertColumnsAlgorithm(JIPipeNodeInfo info) {
        super(info);
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
    protected void runIteration(JIPipeDataBatch dataBatch, JIPipeRunnerSubStatus subProgress, Consumer<JIPipeRunnerSubStatus> algorithmProgress, Supplier<Boolean> isCancelled) {
        ResultsTableData input = dataBatch.getInputData(getFirstInputSlot(), ResultsTableData.class);
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
            ColumnOperation columnOperation = ((JIPipeTableRegistry.ColumnOperationEntry) processor.getParameter().getValue()).getOperation();
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
        dataBatch.addOutputData(getFirstOutputSlot(), output);
    }

    @Override
    public void reportValidity(JIPipeValidityReport report) {
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

    @JIPipeDocumentation(name = "Processors", description = "Defines which columns are processed")
    @JIPipeParameter("processors")
    public ConvertingTableColumnProcessorParameterList getProcessorParameters() {
        return processorParameters;
    }

    @JIPipeParameter("processors")
    public void setProcessorParameters(ConvertingTableColumnProcessorParameterList processorParameters) {
        this.processorParameters = processorParameters;
    }

    @JIPipeDocumentation(name = "Append to existing table", description = "If enabled, the converted columns are appended to the existing table. Existing columns are overwritten.")
    @JIPipeParameter("append")
    public boolean isAppend() {
        return append;
    }

    @JIPipeParameter("append")
    public void setAppend(boolean append) {
        this.append = append;
    }
}
