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
 *
 */

package org.hkijena.jipipe.extensions.tables.nodes.columns;

import com.google.common.primitives.Doubles;
import org.hkijena.jipipe.api.JIPipeDocumentation;
import org.hkijena.jipipe.api.JIPipeIssueReport;
import org.hkijena.jipipe.api.JIPipeNode;
import org.hkijena.jipipe.api.JIPipeProgressInfo;
import org.hkijena.jipipe.api.annotation.JIPipeTextAnnotation;
import org.hkijena.jipipe.api.annotation.JIPipeTextAnnotationMergeMode;
import org.hkijena.jipipe.api.exceptions.UserFriendlyRuntimeException;
import org.hkijena.jipipe.api.nodes.*;
import org.hkijena.jipipe.api.nodes.categories.TableNodeTypeCategory;
import org.hkijena.jipipe.api.parameters.JIPipeParameter;
import org.hkijena.jipipe.extensions.expressions.ExpressionParameterSettings;
import org.hkijena.jipipe.extensions.expressions.ExpressionVariables;
import org.hkijena.jipipe.extensions.expressions.TableColumnValuesExpressionParameterVariableSource;
import org.hkijena.jipipe.extensions.tables.datatypes.*;
import org.hkijena.jipipe.extensions.tables.parameters.collections.ExpressionTableColumnProcessorParameterList;
import org.hkijena.jipipe.extensions.tables.parameters.processors.ExpressionTableColumnProcessorParameter;
import org.hkijena.jipipe.utils.StringUtils;

import java.util.*;

/**
 * Algorithm that integrates columns
 */
@JIPipeDocumentation(name = "Apply expression to columns", description = "Applies an expression function to all column values. " +
        "The result of the operation is stored in the same or a new column.")
@JIPipeNode(nodeTypeCategory = TableNodeTypeCategory.class)
@JIPipeInputSlot(value = ResultsTableData.class, slotName = "Input", autoCreate = true)
@JIPipeOutputSlot(value = ResultsTableData.class, slotName = "Output", autoCreate = true)
public class ApplyExpressionToColumnsAlgorithm extends JIPipeSimpleIteratingAlgorithm {

    private ExpressionTableColumnProcessorParameterList processorParameters = new ExpressionTableColumnProcessorParameterList();
    private boolean append = false;

    /**
     * Creates a new instance
     *
     * @param info algorithm info
     */
    public ApplyExpressionToColumnsAlgorithm(JIPipeNodeInfo info) {
        super(info);
        processorParameters.addNewInstance();
    }

    /**
     * Creates a copy
     *
     * @param other the original
     */
    public ApplyExpressionToColumnsAlgorithm(ApplyExpressionToColumnsAlgorithm other) {
        super(other);
        this.processorParameters = new ExpressionTableColumnProcessorParameterList(other.processorParameters);
        this.append = other.append;
    }

    @Override
    protected void runIteration(JIPipeDataBatch dataBatch, JIPipeProgressInfo progressInfo) {
        ResultsTableData input = dataBatch.getInputData(getFirstInputSlot(), ResultsTableData.class, progressInfo);
        List<TableColumn> resultColumns = new ArrayList<>();
        ExpressionVariables expressionVariables = new ExpressionVariables();
        Map<String, String> annotationsMap = JIPipeTextAnnotation.annotationListToMap(dataBatch.getMergedTextAnnotations().values(), JIPipeTextAnnotationMergeMode.OverwriteExisting);
        expressionVariables.set("annotations", annotationsMap);
        if (append) {
            for (String columnName : input.getColumnNames()) {
                resultColumns.add(input.getColumnReference(input.getColumnIndex(columnName)));
            }
        }
        for (int col = 0; col < input.getColumnCount(); col++) {
            TableColumn column = input.getColumnReference(col);
            if(column.isNumeric()) {
                expressionVariables.set(column.getLabel(), Doubles.asList(column.getDataAsDouble(column.getRows())));
            }
            else {
                expressionVariables.set(column.getLabel(), Arrays.asList(column.getDataAsString(column.getRows())));
            }
        }
        for (ExpressionTableColumnProcessorParameter processor : processorParameters) {
            String sourceColumn = processor.getInput().queryFirst(input.getColumnNames(), new ExpressionVariables());
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
            expressionVariables.set("column", columnIndex);
            expressionVariables.set("column_name", sourceColumn);
            expressionVariables.set("num_rows", input.getRowCount());
            expressionVariables.set("num_cols", input.getColumnCount());
            expressionVariables.set("values", values);
            Object result = processor.getParameter().evaluate(expressionVariables);
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
