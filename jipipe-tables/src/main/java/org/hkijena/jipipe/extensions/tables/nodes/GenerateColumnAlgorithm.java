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

package org.hkijena.jipipe.extensions.tables.nodes;

import org.hkijena.jipipe.api.JIPipeDocumentation;
import org.hkijena.jipipe.api.JIPipeNode;
import org.hkijena.jipipe.api.JIPipeProgressInfo;
import org.hkijena.jipipe.api.nodes.*;
import org.hkijena.jipipe.api.nodes.categories.TableNodeTypeCategory;
import org.hkijena.jipipe.api.nodes.iterationstep.JIPipeIterationContext;
import org.hkijena.jipipe.api.nodes.iterationstep.JIPipeSingleIterationStep;
import org.hkijena.jipipe.api.nodes.algorithm.JIPipeSimpleIteratingAlgorithm;
import org.hkijena.jipipe.api.parameters.JIPipeParameter;
import org.hkijena.jipipe.api.parameters.JIPipeParameterPersistence;
import org.hkijena.jipipe.api.validation.JIPipeValidationReport;
import org.hkijena.jipipe.api.validation.JIPipeValidationReportContext;
import org.hkijena.jipipe.api.validation.contexts.ParameterValidationReportContext;
import org.hkijena.jipipe.extensions.expressions.*;
import org.hkijena.jipipe.extensions.expressions.variables.TextAnnotationsExpressionParameterVariableSource;
import org.hkijena.jipipe.extensions.parameters.api.pairs.PairParameterSettings;
import org.hkijena.jipipe.extensions.parameters.library.primitives.optional.OptionalIntegerParameter;
import org.hkijena.jipipe.extensions.tables.datatypes.ResultsTableData;
import org.hkijena.jipipe.extensions.tables.parameters.collections.ExpressionTableColumnGeneratorProcessorParameterList;
import org.hkijena.jipipe.extensions.tables.parameters.processors.ExpressionTableColumnGeneratorProcessor;
import org.hkijena.jipipe.utils.ResourceUtils;

/**
 * Algorithm that adds or replaces a column by a generated value
 */
@JIPipeDocumentation(name = "Add table column", description = "Adds a new column. By default no changes are applied if the column already exists. " +
        "Can be optionally configured to replace existing columns.")
@JIPipeNodeAlias(nodeTypeCategory = TableNodeTypeCategory.class, menuPath = "Append", aliasName = "Add missing columns")
@JIPipeNode(nodeTypeCategory = TableNodeTypeCategory.class)
@JIPipeInputSlot(value = ResultsTableData.class, slotName = "Input", autoCreate = true)
@JIPipeOutputSlot(value = ResultsTableData.class, slotName = "Output", autoCreate = true)
public class GenerateColumnAlgorithm extends JIPipeSimpleIteratingAlgorithm {

    private final CustomExpressionVariablesParameter customFilterVariables;
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
        this.customFilterVariables = new CustomExpressionVariablesParameter(this);
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
        this.customFilterVariables = new CustomExpressionVariablesParameter(other.customFilterVariables, this);
    }

    @Override
    protected void runIteration(JIPipeSingleIterationStep iterationStep, JIPipeIterationContext iterationContext, JIPipeProgressInfo progressInfo) {
        ResultsTableData table = (ResultsTableData) iterationStep.getInputData(getFirstInputSlot(), ResultsTableData.class, progressInfo).duplicate(progressInfo);
        if (ensureMinNumberOfRows.isEnabled()) {
            table.addRows(ensureMinNumberOfRows.getContent() - table.getRowCount());
        }
        ExpressionVariables variableSet = new ExpressionVariables();
        variableSet.putAnnotations(iterationStep.getMergedTextAnnotations());
        customFilterVariables.writeToVariables(variableSet, true, "custom.", true, "custom");

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
    public void reportValidity(JIPipeValidationReportContext context, JIPipeValidationReport report) {
        super.reportValidity(context, report);
        report.report(new ParameterValidationReportContext(context, this, "Columns", "columns"), columns);
    }

    @JIPipeDocumentation(name = "Custom expression variables", description = "Here you can add parameters that will be included into the expression as variables <code>custom.[key]</code>. Alternatively, you can access them via <code>GET_ITEM(\"custom\", \"[key]\")</code>.")
    @JIPipeParameter(value = "custom-filter-variables", iconURL = ResourceUtils.RESOURCE_BASE_PATH + "/icons/actions/insert-math-expression.png",
            iconDarkURL = ResourceUtils.RESOURCE_BASE_PATH + "/dark/icons/actions/insert-math-expression.png", persistence = JIPipeParameterPersistence.NestedCollection)
    public CustomExpressionVariablesParameter getCustomFilterVariables() {
        return customFilterVariables;
    }

    @JIPipeDocumentation(name = "Replace existing data", description = "If the target column exists, replace its content")
    @JIPipeParameter("replace-existing")
    public boolean isReplaceIfExists() {
        return replaceIfExists;
    }

    @JIPipeParameter("replace-existing")
    public void setReplaceIfExists(boolean replaceIfExists) {
        this.replaceIfExists = replaceIfExists;
    }

    @JIPipeDocumentation(name = "Columns", description = "Columns to be generated. The function is applied for each row. " +
            "You will have the standard set of table location variables available (e.g. the row index), but also access to the other column values within the same row. " +
            "Access them just as any variable. If the column name has special characters or spaces, use the $ operator. Example: " +
            "<pre>$\"%Area\" * 10</pre>")
    @JIPipeParameter("columns")
    @ExpressionParameterSettings(variableSource = TableCellExpressionParameterVariableSource.class)
    @ExpressionParameterSettingsVariable(fromClass = TextAnnotationsExpressionParameterVariableSource.class)
    @PairParameterSettings(singleRow = false, keyLabel = "Function", valueLabel = "Output column")
    @ExpressionParameterSettingsVariable(key = "custom", name = "Custom variables", description = "A map containing custom expression variables (keys are the parameter keys)")
    @ExpressionParameterSettingsVariable(name = "custom.<Custom variable key>", description = "Custom variable parameters are added with a prefix 'custom.'")
    public ExpressionTableColumnGeneratorProcessorParameterList getColumns() {
        return columns;
    }

    @JIPipeParameter("columns")
    public void setColumns(ExpressionTableColumnGeneratorProcessorParameterList columns) {
        this.columns = columns;
    }

    @JIPipeDocumentation(name = "Ensure minimum number of rows", description = "Ensures that the table has at least the specified number of rows prior to adding columns.")
    @JIPipeParameter("ensure-min-number-of-rows")
    public OptionalIntegerParameter getEnsureMinNumberOfRows() {
        return ensureMinNumberOfRows;
    }

    @JIPipeParameter("ensure-min-number-of-rows")
    public void setEnsureMinNumberOfRows(OptionalIntegerParameter ensureMinNumberOfRows) {
        this.ensureMinNumberOfRows = ensureMinNumberOfRows;
    }
}
