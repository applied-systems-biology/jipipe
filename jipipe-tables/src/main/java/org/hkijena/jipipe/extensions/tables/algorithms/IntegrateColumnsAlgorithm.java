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
import org.hkijena.jipipe.api.JIPipeProgressInfo;
import org.hkijena.jipipe.api.JIPipeValidityReport;
import org.hkijena.jipipe.api.exceptions.UserFriendlyRuntimeException;
import org.hkijena.jipipe.api.nodes.*;
import org.hkijena.jipipe.api.nodes.categories.TableNodeTypeCategory;
import org.hkijena.jipipe.api.parameters.JIPipeParameter;
import org.hkijena.jipipe.api.registries.JIPipeExpressionRegistry;
import org.hkijena.jipipe.extensions.tables.ColumnOperation;
import org.hkijena.jipipe.extensions.tables.datatypes.ResultsTableData;
import org.hkijena.jipipe.extensions.tables.datatypes.TableColumn;
import org.hkijena.jipipe.extensions.tables.parameters.collections.IntegratingTableColumnProcessorParameterList;
import org.hkijena.jipipe.extensions.tables.parameters.processors.IntegratingTableColumnProcessorParameter;
import org.hkijena.jipipe.utils.StringUtils;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * Algorithm that integrates columns
 */
@JIPipeDocumentation(name = "Integrate table columns", description = "Integrates table columns by applying operations like average, standard deviation, or median")
@JIPipeOrganization(nodeTypeCategory = TableNodeTypeCategory.class)
@JIPipeInputSlot(value = ResultsTableData.class, slotName = "Input", autoCreate = true)
@JIPipeOutputSlot(value = ResultsTableData.class, slotName = "Output", autoCreate = true)
public class IntegrateColumnsAlgorithm extends JIPipeSimpleIteratingAlgorithm {

    private IntegratingTableColumnProcessorParameterList processorParameters = new IntegratingTableColumnProcessorParameterList();

    /**
     * Creates a new instance
     *
     * @param info algorithm info
     */
    public IntegrateColumnsAlgorithm(JIPipeNodeInfo info) {
        super(info);
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
    protected void runIteration(JIPipeDataBatch dataBatch, JIPipeProgressInfo progressInfo) {
        ResultsTableData input = dataBatch.getInputData(getFirstInputSlot(), ResultsTableData.class, progressInfo);
        Map<String, TableColumn> resultColumns = new HashMap<>();
        for (IntegratingTableColumnProcessorParameter processor : processorParameters) {
            String sourceColumn = processor.getInput().queryFirst(input.getColumnNames());
            if (sourceColumn == null) {
                throw new UserFriendlyRuntimeException(new NullPointerException(),
                        "Unable to find column matching " + processor.getInput(),
                        "Algorithm '" + getName() + "'",
                        "The column filter '" + processor.getInput() + "' tried to find a matching column in " + String.join(", ", input.getColumnNames()) + ". None of the columns matched.",
                        "Please check if the filter is correct.");
            }
            TableColumn sourceColumnData = input.getColumnReference(input.getColumnIndex(sourceColumn));
            ColumnOperation columnOperation = ((JIPipeExpressionRegistry.ColumnOperationEntry) processor.getParameter().getValue()).getOperation();
            TableColumn resultColumn = columnOperation.apply(sourceColumnData);
            resultColumns.put(processor.getOutput(), resultColumn);
        }

        // Combine into one table
        ResultsTableData output = new ResultsTableData(resultColumns);
        dataBatch.addOutputData(getFirstOutputSlot(), output, progressInfo);
    }

    @Override
    public void reportValidity(JIPipeValidityReport report) {
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

    @JIPipeDocumentation(name = "Processors", description = "Defines which columns are processed")
    @JIPipeParameter("processors")
    public IntegratingTableColumnProcessorParameterList getProcessorParameters() {
        return processorParameters;
    }

    @JIPipeParameter("processors")
    public void setProcessorParameters(IntegratingTableColumnProcessorParameterList processorParameters) {
        this.processorParameters = processorParameters;
    }
}
