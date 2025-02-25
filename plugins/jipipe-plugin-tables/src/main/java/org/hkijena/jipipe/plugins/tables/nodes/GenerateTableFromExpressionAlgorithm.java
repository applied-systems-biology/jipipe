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
import org.hkijena.jipipe.api.data.JIPipeData;
import org.hkijena.jipipe.api.nodes.AddJIPipeInputSlot;
import org.hkijena.jipipe.api.nodes.AddJIPipeOutputSlot;
import org.hkijena.jipipe.api.nodes.JIPipeGraphNodeRunContext;
import org.hkijena.jipipe.api.nodes.JIPipeNodeInfo;
import org.hkijena.jipipe.api.nodes.algorithm.JIPipeSimpleIteratingAlgorithm;
import org.hkijena.jipipe.api.nodes.categories.DataSourceNodeTypeCategory;
import org.hkijena.jipipe.api.nodes.iterationstep.JIPipeIterationContext;
import org.hkijena.jipipe.api.nodes.iterationstep.JIPipeSingleIterationStep;
import org.hkijena.jipipe.api.parameters.JIPipeParameter;
import org.hkijena.jipipe.api.validation.JIPipeValidationReport;
import org.hkijena.jipipe.api.validation.JIPipeValidationReportContext;
import org.hkijena.jipipe.api.validation.contexts.ParameterValidationReportContext;
import org.hkijena.jipipe.plugins.expressions.AddJIPipeExpressionParameterVariable;
import org.hkijena.jipipe.plugins.expressions.JIPipeExpressionVariablesMap;
import org.hkijena.jipipe.plugins.expressions.TableCellExpressionParameterVariablesInfo;
import org.hkijena.jipipe.plugins.expressions.variables.JIPipeTextAnnotationsExpressionParameterVariablesInfo;
import org.hkijena.jipipe.plugins.parameters.api.pairs.PairParameterSettings;
import org.hkijena.jipipe.plugins.tables.datatypes.ResultsTableData;
import org.hkijena.jipipe.plugins.tables.parameters.collections.ExpressionTableColumnGeneratorProcessorParameterList;
import org.hkijena.jipipe.plugins.tables.parameters.processors.ExpressionTableColumnGeneratorProcessor;

/**
 * Algorithm that adds or replaces a column by a generated value
 */
@SetJIPipeDocumentation(name = "Generate table rows from expressions", description = "Generates a table from expressions that are applied for each table row, given by a parameter. " +
        "Use 'Generate table columns from expression' if you want to do not know the number of rows.")
@ConfigureJIPipeNode(nodeTypeCategory = DataSourceNodeTypeCategory.class)
@AddJIPipeInputSlot(value = JIPipeData.class, name = "Annotations", create = true, optional = true, description = "Optional source for annotations")
@AddJIPipeOutputSlot(value = ResultsTableData.class, name = "Output", create = true)
public class GenerateTableFromExpressionAlgorithm extends JIPipeSimpleIteratingAlgorithm {

    private ExpressionTableColumnGeneratorProcessorParameterList columns = new ExpressionTableColumnGeneratorProcessorParameterList();
    private int generatedRows = 10;

    /**
     * Creates a new instance
     *
     * @param info algorithm info
     */
    public GenerateTableFromExpressionAlgorithm(JIPipeNodeInfo info) {
        super(info);
        columns.addNewInstance();
    }

    /**
     * Creates a copy
     *
     * @param other the original
     */
    public GenerateTableFromExpressionAlgorithm(GenerateTableFromExpressionAlgorithm other) {
        super(other);
        this.generatedRows = other.generatedRows;
        this.columns = new ExpressionTableColumnGeneratorProcessorParameterList(other.columns);
    }

    @Override
    protected void runIteration(JIPipeSingleIterationStep iterationStep, JIPipeIterationContext iterationContext, JIPipeGraphNodeRunContext runContext, JIPipeProgressInfo progressInfo) {
        ResultsTableData table = new ResultsTableData();
        table.addRows(generatedRows);
        JIPipeExpressionVariablesMap variableSet = new JIPipeExpressionVariablesMap(iterationStep);

        variableSet.set("num_rows", generatedRows);
        variableSet.set("num_cols", columns.size());
        for (ExpressionTableColumnGeneratorProcessor entry : columns) {
            String columnName = entry.getValue();
            int columnId = table.getColumnIndex(columnName);
            if (columnId == -1)
                columnId = table.getOrCreateColumnIndex(columnName, false);
            variableSet.set("column", columnId);
            variableSet.set("column_name", columnName);
            for (int row = 0; row < table.getRowCount(); row++) {
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

    @SetJIPipeDocumentation(name = "Columns", description = "Columns to be generated")
    @JIPipeParameter("columns")
    @AddJIPipeExpressionParameterVariable(fromClass = TableCellExpressionParameterVariablesInfo.class)
    @AddJIPipeExpressionParameterVariable(fromClass = JIPipeTextAnnotationsExpressionParameterVariablesInfo.class)
    @PairParameterSettings(keyLabel = "Function", valueLabel = "Output column")
    public ExpressionTableColumnGeneratorProcessorParameterList getColumns() {
        return columns;
    }

    @JIPipeParameter("columns")
    public void setColumns(ExpressionTableColumnGeneratorProcessorParameterList columns) {
        this.columns = columns;
    }

    @SetJIPipeDocumentation(name = "Generated rows", description = "Determines how many rows to generate")
    @JIPipeParameter("generated-rows")
    public int getGeneratedRows() {
        return generatedRows;
    }

    @JIPipeParameter("generated-rows")
    public boolean setGeneratedRows(int generatedRows) {
        if (generatedRows < 0)
            return false;
        this.generatedRows = generatedRows;
        return true;
    }
}
