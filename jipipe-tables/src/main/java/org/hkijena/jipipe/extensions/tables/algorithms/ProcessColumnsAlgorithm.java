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
import org.hkijena.jipipe.api.JIPipeIssueReport;
import org.hkijena.jipipe.api.exceptions.UserFriendlyRuntimeException;
import org.hkijena.jipipe.api.nodes.JIPipeDataBatch;
import org.hkijena.jipipe.api.nodes.JIPipeInputSlot;
import org.hkijena.jipipe.api.nodes.JIPipeNodeInfo;
import org.hkijena.jipipe.api.nodes.JIPipeOutputSlot;
import org.hkijena.jipipe.api.nodes.JIPipeSimpleIteratingAlgorithm;
import org.hkijena.jipipe.api.nodes.categories.TableNodeTypeCategory;
import org.hkijena.jipipe.api.parameters.JIPipeParameter;
import org.hkijena.jipipe.extensions.expressions.ExpressionParameterSettings;
import org.hkijena.jipipe.extensions.expressions.ExpressionParameters;
import org.hkijena.jipipe.extensions.expressions.TableColumnValuesExpressionParameterVariableSource;
import org.hkijena.jipipe.extensions.tables.datatypes.DoubleArrayTableColumn;
import org.hkijena.jipipe.extensions.tables.datatypes.RelabeledTableColumn;
import org.hkijena.jipipe.extensions.tables.datatypes.ResultsTableData;
import org.hkijena.jipipe.extensions.tables.datatypes.StringArrayTableColumn;
import org.hkijena.jipipe.extensions.tables.datatypes.TableColumn;
import org.hkijena.jipipe.extensions.tables.parameters.collections.ExpressionTableColumnProcessorParameterList;
import org.hkijena.jipipe.extensions.tables.parameters.processors.ExpressionTableColumnProcessorParameter;
import org.hkijena.jipipe.utils.StringUtils;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Algorithm that integrates columns
 */
@JIPipeDocumentation(name = "Apply expression to columns", description = "Applies an expression function to all column values. " +
        "The result of the operation is stored in the same or a new column.")
@JIPipeOrganization(nodeTypeCategory = TableNodeTypeCategory.class)
@JIPipeInputSlot(value = ResultsTableData.class, slotName = "Input", autoCreate = true)
@JIPipeOutputSlot(value = ResultsTableData.class, slotName = "Output", autoCreate = true)
public class ProcessColumnsAlgorithm extends JIPipeSimpleIteratingAlgorithm {

    private ExpressionTableColumnProcessorParameterList processorParameters = new ExpressionTableColumnProcessorParameterList();
    private boolean append = false;

    /**
     * Creates a new instance
     *
     * @param info algorithm info
     */
    public ProcessColumnsAlgorithm(JIPipeNodeInfo info) {
        super(info);
        processorParameters.addNewInstance();
    }

    /**
     * Creates a copy
     *
     * @param other the original
     */
    public ProcessColumnsAlgorithm(ProcessColumnsAlgorithm other) {
        super(other);
        this.processorParameters = new ExpressionTableColumnProcessorParameterList(other.processorParameters);
        this.append = other.append;
    }

    @Override
    protected void runIteration(JIPipeDataBatch dataBatch, JIPipeProgressInfo progressInfo) {
        ResultsTableData input = dataBatch.getInputData(getFirstInputSlot(), ResultsTableData.class, progressInfo);
        List<TableColumn> resultColumns = new ArrayList<>();
        ExpressionParameters expressionParameters = new ExpressionParameters();
        if (append) {
            for (String columnName : input.getColumnNames()) {
                resultColumns.add(input.getColumnReference(input.getColumnIndex(columnName)));
            }
        }
        for (ExpressionTableColumnProcessorParameter processor : processorParameters) {
            String sourceColumn = processor.getInput().queryFirst(input.getColumnNames(), new ExpressionParameters());
            if (sourceColumn == null) {
                throw new UserFriendlyRuntimeException(new NullPointerException(),
                        "Unable to find column matching " + processor.getInput(),
                        "Algorithm '" + getName() + "'",
                        "The column filter '" + processor.getInput() + "' tried to find a matching column in " + String.join(", ", input.getColumnNames()) + ". None of the columns matched.",
                        "Please check if the filter is correct.");
            }
            int columnIndex = input.getColumnIndex(sourceColumn);
            List<Object> values = new ArrayList<>();
            for (int i = 0; i < input.getRowCount(); i++) {
                values.add(input.getValueAt(i, columnIndex));
            }
            expressionParameters.set("column", columnIndex);
            expressionParameters.set("column_name", sourceColumn);
            expressionParameters.set("num_rows", input.getRowCount());
            expressionParameters.set("num_cols", input.getColumnCount());
            expressionParameters.set("values", values);
            Object result = processor.getParameter().evaluate(expressionParameters);
            TableColumn resultColumn;
            if (result instanceof Number) {
                resultColumn = new DoubleArrayTableColumn(new double[]{((Number) result).doubleValue()}, processor.getOutput());
            } else if (result instanceof Collection) {
                if (((Collection<?>) result).stream().allMatch(v -> v instanceof Number)) {
                    double[] data = new double[((Collection<?>) result).size()];
                    int i = 0;
                    for (Object o : (Collection<?>) result) {
                        data[i] = ((Number) o).doubleValue();
                        ++i;
                    }
                    resultColumn = new DoubleArrayTableColumn(data, processor.getOutput());
                } else {
                    String[] data = new String[((Collection<?>) result).size()];
                    int i = 0;
                    for (Object o : (Collection<?>) result) {
                        data[i] = StringUtils.nullToEmpty(o);
                        ++i;
                    }
                    resultColumn = new StringArrayTableColumn(data, processor.getOutput());
                }
            } else {
                resultColumn = new StringArrayTableColumn(new String[]{StringUtils.nullToEmpty(result)}, processor.getOutput());
            }
            resultColumns.add(new RelabeledTableColumn(resultColumn, processor.getOutput()));
        }

        // Combine into one table
        ResultsTableData output = new ResultsTableData(resultColumns);
        dataBatch.addOutputData(getFirstOutputSlot(), output, progressInfo);
    }

    @Override
    public void reportValidity(JIPipeIssueReport report) {
        report.resolve("Processors").report(processorParameters);
        Set<String> columnNames = new HashSet<>();
        for (ExpressionTableColumnProcessorParameter parameter : processorParameters) {
            if (columnNames.contains(parameter.getOutput())) {
                report.resolve("Processors").reportIsInvalid("Duplicate output column: " + parameter.getOutput(),
                        "There should not be multiple output columns with the same name.",
                        "Change the name to a unique non-empty string",
                        this);
                break;
            }
            if (StringUtils.isNullOrEmpty(parameter.getOutput())) {
                report.resolve("Processors").reportIsInvalid("An output column has no name!",
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
    @ExpressionParameterSettings(variableSource = TableColumnValuesExpressionParameterVariableSource.class)
    public ExpressionTableColumnProcessorParameterList getProcessorParameters() {
        return processorParameters;
    }

    @JIPipeParameter("processors")
    public void setProcessorParameters(ExpressionTableColumnProcessorParameterList processorParameters) {
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
