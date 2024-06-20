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

package org.hkijena.jipipe.plugins.tables.nodes;

import org.hkijena.jipipe.api.ConfigureJIPipeNode;
import org.hkijena.jipipe.api.JIPipeProgressInfo;
import org.hkijena.jipipe.api.SetJIPipeDocumentation;
import org.hkijena.jipipe.api.nodes.*;
import org.hkijena.jipipe.api.nodes.algorithm.JIPipeSimpleIteratingAlgorithm;
import org.hkijena.jipipe.api.nodes.categories.TableNodeTypeCategory;
import org.hkijena.jipipe.api.nodes.iterationstep.JIPipeIterationContext;
import org.hkijena.jipipe.api.nodes.iterationstep.JIPipeSingleIterationStep;
import org.hkijena.jipipe.api.parameters.JIPipeParameter;
import org.hkijena.jipipe.api.validation.JIPipeValidationReport;
import org.hkijena.jipipe.api.validation.JIPipeValidationReportContext;
import org.hkijena.jipipe.api.validation.contexts.ParameterValidationReportContext;
import org.hkijena.jipipe.plugins.expressions.JIPipeExpressionParameterSettings;
import org.hkijena.jipipe.plugins.expressions.JIPipeExpressionParameterVariable;
import org.hkijena.jipipe.plugins.expressions.JIPipeExpressionVariablesMap;
import org.hkijena.jipipe.plugins.expressions.TableCellExpressionParameterVariablesInfo;
import org.hkijena.jipipe.plugins.expressions.custom.JIPipeCustomExpressionVariablesParameterVariablesInfo;
import org.hkijena.jipipe.plugins.expressions.variables.JIPipeTextAnnotationsExpressionParameterVariablesInfo;
import org.hkijena.jipipe.plugins.parameters.api.pairs.PairParameterSettings;
import org.hkijena.jipipe.plugins.parameters.library.primitives.optional.OptionalIntegerParameter;
import org.hkijena.jipipe.plugins.tables.datatypes.ResultsTableData;
import org.hkijena.jipipe.plugins.tables.parameters.collections.ExpressionTableColumnGeneratorProcessorParameterList;
import org.hkijena.jipipe.plugins.tables.parameters.processors.ExpressionTableColumnGeneratorProcessor;

/**
 * Algorithm that adds or replaces a column by a generated value
 */
@SetJIPipeDocumentation(name = "Add table column", description = "Adds a new column. By default no changes are applied if the column already exists. " +
        "Can be optionally configured to replace existing columns.")
@AddJIPipeNodeAlias(nodeTypeCategory = TableNodeTypeCategory.class, menuPath = "Append", aliasName = "Add missing columns")
@ConfigureJIPipeNode(nodeTypeCategory = TableNodeTypeCategory.class)
@AddJIPipeInputSlot(value = ResultsTableData.class, name = "Input", create = true)
@AddJIPipeOutputSlot(value = ResultsTableData.class, name = "Output", create = true)
public class GenerateColumnAlgorithm extends JIPipeSimpleIteratingAlgorithm {

    private ExpressionTableColumnGeneratorProcessorParameterList columns = new ExpressionTableColumnGeneratorProcessorParameterList();
    private boolean replaceIfExists = false;
    private OptionalIntegerParameter ensureMinNumberOfRows = new OptionalIntegerParameter(false, 1);

    /**
     * Creates a new instance
     *
     * @param info algorithm info
     */
    public GenerateColumnAlgorithm(JIPipeNodeInfo info) {
        super(info);
        columns.addNewInstance();
    }

    /**
     * Creates a copy
     *
     * @param other the original
     */
    public GenerateColumnAlgorithm(GenerateColumnAlgorithm other) {
        super(other);
        this.replaceIfExists = other.replaceIfExists;
        this.columns = new ExpressionTableColumnGeneratorProcessorParameterList(other.columns);
        this.ensureMinNumberOfRows = new OptionalIntegerParameter(other.ensureMinNumberOfRows);
    }

    @Override
    protected void runIteration(JIPipeSingleIterationStep iterationStep, JIPipeIterationContext iterationContext, JIPipeGraphNodeRunContext runContext, JIPipeProgressInfo progressInfo) {
        ResultsTableData table = (ResultsTableData) iterationStep.getInputData(getFirstInputSlot(), ResultsTableData.class, progressInfo).duplicate(progressInfo);
        if (ensureMinNumberOfRows.isEnabled()) {
            table.addRows(ensureMinNumberOfRows.getContent() - table.getRowCount());
        }
        JIPipeExpressionVariablesMap variableSet = new JIPipeExpressionVariablesMap();
        variableSet.putAnnotations(iterationStep.getMergedTextAnnotations());
        getDefaultCustomExpressionVariables().writeToVariables(variableSet);

        variableSet.set("num_rows", table.getRowCount());
        for (ExpressionTableColumnGeneratorProcessor entry : columns) {
            String columnName = entry.getValue();

            if (table.getColumnIndex(columnName) != -1 && !replaceIfExists)
                continue;

            int columnId = table.getOrCreateColumnIndex(columnName, false);
            variableSet.set("column", columnId);
            variableSet.set("column_name", columnName);
            variableSet.set("num_cols", table.getColumnCount());
            for (int row = 0; row < table.getRowCount(); row++) {
                for (int col = 0; col < table.getColumnCount(); col++) {
                    if (col != columnId) {
                        variableSet.set(table.getColumnName(col), table.getValueAt(row, col));
                    }
                }
                variableSet.set("row", row);
                Object value = entry.getKey().evaluate(variableSet);
                if (!(value instanceof Number) && !(value instanceof String))
                    value = "" + value;
                table.setValueAt(value, row, columnId);
            }
        }
        iterationStep.addOutputData(getFirstOutputSlot(), table, progressInfo);
    }

    @Override
    public void reportValidity(JIPipeValidationReportContext reportContext, JIPipeValidationReport report) {
        super.reportValidity(reportContext, report);
        report.report(new ParameterValidationReportContext(reportContext, this, "Columns", "columns"), columns);
    }

    @Override
    public boolean isEnableDefaultCustomExpressionVariables() {
        return true;
    }

    @SetJIPipeDocumentation(name = "Replace existing data", description = "If the target column exists, replace its content")
    @JIPipeParameter("replace-existing")
    public boolean isReplaceIfExists() {
        return replaceIfExists;
    }

    @JIPipeParameter("replace-existing")
    public void setReplaceIfExists(boolean replaceIfExists) {
        this.replaceIfExists = replaceIfExists;
    }

    @SetJIPipeDocumentation(name = "Columns", description = "Columns to be generated. The function is applied for each row. " +
            "You will have the standard set of table location variables available (e.g. the row index), but also access to the other column values within the same row. " +
            "Access them just as any variable. If the column name has special characters or spaces, use the $ operator. Example: " +
            "<pre>$\"%Area\" * 10</pre>")
    @JIPipeParameter("columns")
    @JIPipeExpressionParameterSettings(variableSource = TableCellExpressionParameterVariablesInfo.class)
    @JIPipeExpressionParameterVariable(fromClass = JIPipeTextAnnotationsExpressionParameterVariablesInfo.class)
    @PairParameterSettings(singleRow = false, keyLabel = "Function", valueLabel = "Output column")
    @JIPipeExpressionParameterVariable(fromClass = JIPipeCustomExpressionVariablesParameterVariablesInfo.class)
    public ExpressionTableColumnGeneratorProcessorParameterList getColumns() {
        return columns;
    }

    @JIPipeParameter("columns")
    public void setColumns(ExpressionTableColumnGeneratorProcessorParameterList columns) {
        this.columns = columns;
    }

    @SetJIPipeDocumentation(name = "Ensure minimum number of rows", description = "Ensures that the table has at least the specified number of rows prior to adding columns.")
    @JIPipeParameter("ensure-min-number-of-rows")
    public OptionalIntegerParameter getEnsureMinNumberOfRows() {
        return ensureMinNumberOfRows;
    }

    @JIPipeParameter("ensure-min-number-of-rows")
    public void setEnsureMinNumberOfRows(OptionalIntegerParameter ensureMinNumberOfRows) {
        this.ensureMinNumberOfRows = ensureMinNumberOfRows;
    }
}
