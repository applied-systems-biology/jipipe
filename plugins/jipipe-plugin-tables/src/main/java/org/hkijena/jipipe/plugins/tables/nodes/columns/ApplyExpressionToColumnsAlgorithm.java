/*
 * Copyright by Zoltán Cseresnyés, Ruman Gerst
 *
 * Research Group Applied Systems Biology - Head: Prof. Dr. Marc Thilo Figge
 * https://www.leibniz-hki.de/en/applied-systems-biology.html
 * HKI-Center for Systems Biology of Infection
 * Leibniz Institute for Natural Product Research and Infection Biology - Hans Knöll Institute (HKI)
 * Adolf-Reichwein-Straße 23, 07745 Jena, Germany
 *
 * The project code is licensed under MIT.
 * See the LICENSE file provided with the code for the full license.
 */

package org.hkijena.jipipe.plugins.tables.nodes.columns;

import com.google.common.primitives.Doubles;
import org.hkijena.jipipe.api.ConfigureJIPipeNode;
import org.hkijena.jipipe.api.JIPipeProgressInfo;
import org.hkijena.jipipe.api.SetJIPipeDocumentation;
import org.hkijena.jipipe.api.annotation.JIPipeTextAnnotation;
import org.hkijena.jipipe.api.annotation.JIPipeTextAnnotationMergeMode;
import org.hkijena.jipipe.api.nodes.AddJIPipeInputSlot;
import org.hkijena.jipipe.api.nodes.AddJIPipeOutputSlot;
import org.hkijena.jipipe.api.nodes.JIPipeGraphNodeRunContext;
import org.hkijena.jipipe.api.nodes.JIPipeNodeInfo;
import org.hkijena.jipipe.api.nodes.algorithm.JIPipeSimpleIteratingAlgorithm;
import org.hkijena.jipipe.api.nodes.categories.TableNodeTypeCategory;
import org.hkijena.jipipe.api.nodes.iterationstep.JIPipeIterationContext;
import org.hkijena.jipipe.api.nodes.iterationstep.JIPipeSingleIterationStep;
import org.hkijena.jipipe.api.parameters.JIPipeParameter;
import org.hkijena.jipipe.api.validation.JIPipeValidationReport;
import org.hkijena.jipipe.api.validation.JIPipeValidationReportContext;
import org.hkijena.jipipe.api.validation.JIPipeValidationRuntimeException;
import org.hkijena.jipipe.api.validation.contexts.ParameterValidationReportContext;
import org.hkijena.jipipe.plugins.expressions.JIPipeExpressionParameterSettings;
import org.hkijena.jipipe.plugins.expressions.JIPipeExpressionParameterVariable;
import org.hkijena.jipipe.plugins.expressions.JIPipeExpressionVariablesMap;
import org.hkijena.jipipe.plugins.expressions.TableColumnValuesExpressionParameterVariablesInfo;
import org.hkijena.jipipe.plugins.expressions.custom.JIPipeCustomExpressionVariablesParameterVariablesInfo;
import org.hkijena.jipipe.plugins.tables.datatypes.*;
import org.hkijena.jipipe.plugins.tables.parameters.collections.ExpressionTableColumnProcessorParameterList;
import org.hkijena.jipipe.plugins.tables.parameters.processors.ExpressionTableColumnProcessorParameter;
import org.hkijena.jipipe.utils.StringUtils;

import java.util.*;

/**
 * Algorithm that integrates columns
 */
@SetJIPipeDocumentation(name = "Apply expression to columns", description = "Applies an expression function to all column values. " +
        "The result of the operation is stored in the same or a new column.")
@ConfigureJIPipeNode(nodeTypeCategory = TableNodeTypeCategory.class)
@AddJIPipeInputSlot(value = ResultsTableData.class, name = "Input", create = true)
@AddJIPipeOutputSlot(value = ResultsTableData.class, slotName = "Output", create = true)
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
    protected void runIteration(JIPipeSingleIterationStep iterationStep, JIPipeIterationContext iterationContext, JIPipeGraphNodeRunContext runContext, JIPipeProgressInfo progressInfo) {
        ResultsTableData input = iterationStep.getInputData(getFirstInputSlot(), ResultsTableData.class, progressInfo);
        List<TableColumn> resultColumns = new ArrayList<>();
        JIPipeExpressionVariablesMap expressionVariables = new JIPipeExpressionVariablesMap();
        Map<String, String> annotationsMap = JIPipeTextAnnotation.annotationListToMap(iterationStep.getMergedTextAnnotations().values(), JIPipeTextAnnotationMergeMode.OverwriteExisting);
        expressionVariables.set("annotations", annotationsMap);
        getDefaultCustomExpressionVariables().writeToVariables(expressionVariables);
        if (append) {
            for (String columnName : input.getColumnNames()) {
                resultColumns.add(input.getColumnReference(input.getColumnIndex(columnName)));
            }
        }
        for (int col = 0; col < input.getColumnCount(); col++) {
            TableColumn column = input.getColumnReference(col);
            if (column.isNumeric()) {
                expressionVariables.set(column.getLabel(), Doubles.asList(column.getDataAsDouble(column.getRows())));
            } else {
                expressionVariables.set(column.getLabel(), Arrays.asList(column.getDataAsString(column.getRows())));
            }
        }
        for (ExpressionTableColumnProcessorParameter processor : processorParameters) {
            String sourceColumn = processor.getInput().queryFirst(input.getColumnNames(), new JIPipeExpressionVariablesMap());
            if (sourceColumn == null) {
                throw new JIPipeValidationRuntimeException(new NullPointerException("Could not find column matching '" + processor.getInput() + "'"),
                        "Could not find column matching '" + processor.getInput() + "'",
                        "You tried to rename a column '" + processor.getInput() + "', but it was not found.",
                        "Please check if the table '" + input + "' contains the column.");
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
        iterationStep.addOutputData(getFirstOutputSlot(), output, progressInfo);
    }

    @Override
    public void reportValidity(JIPipeValidationReportContext reportContext, JIPipeValidationReport report) {
        super.reportValidity(reportContext, report);
        report.report(new ParameterValidationReportContext(reportContext, this, "Processors", "processors"), processorParameters);
    }

    @SetJIPipeDocumentation(name = "Processors", description = "Defines which columns are processed")
    @JIPipeParameter("processors")
    @JIPipeExpressionParameterSettings(variableSource = TableColumnValuesExpressionParameterVariablesInfo.class)
    @JIPipeExpressionParameterVariable(fromClass = JIPipeCustomExpressionVariablesParameterVariablesInfo.class)
    public ExpressionTableColumnProcessorParameterList getProcessorParameters() {
        return processorParameters;
    }

    @JIPipeParameter("processors")
    public void setProcessorParameters(ExpressionTableColumnProcessorParameterList processorParameters) {
        this.processorParameters = processorParameters;
    }

    @Override
    public boolean isEnableDefaultCustomExpressionVariables() {
        return true;
    }

    @SetJIPipeDocumentation(name = "Append to existing table", description = "If enabled, the converted columns are appended to the existing table. Existing columns are overwritten.")
    @JIPipeParameter("append")
    public boolean isAppend() {
        return append;
    }

    @JIPipeParameter("append")
    public void setAppend(boolean append) {
        this.append = append;
    }
}
