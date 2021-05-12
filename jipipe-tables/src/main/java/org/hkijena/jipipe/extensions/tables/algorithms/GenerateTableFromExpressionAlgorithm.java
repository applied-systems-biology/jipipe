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
import org.hkijena.jipipe.api.nodes.JIPipeDataBatch;
import org.hkijena.jipipe.api.nodes.JIPipeNodeInfo;
import org.hkijena.jipipe.api.nodes.JIPipeOutputSlot;
import org.hkijena.jipipe.api.nodes.JIPipeSimpleIteratingAlgorithm;
import org.hkijena.jipipe.api.nodes.categories.DataSourceNodeTypeCategory;
import org.hkijena.jipipe.api.parameters.JIPipeContextAction;
import org.hkijena.jipipe.api.parameters.JIPipeParameter;
import org.hkijena.jipipe.extensions.expressions.DefaultExpressionParameter;
import org.hkijena.jipipe.extensions.expressions.ExpressionParameterSettings;
import org.hkijena.jipipe.extensions.expressions.ExpressionParameters;
import org.hkijena.jipipe.extensions.expressions.TableCellExpressionParameterVariableSource;
import org.hkijena.jipipe.extensions.parameters.pairs.PairParameterSettings;
import org.hkijena.jipipe.extensions.tables.datatypes.ResultsTableData;
import org.hkijena.jipipe.extensions.tables.parameters.collections.ExpressionTableColumnGeneratorProcessorParameterList;
import org.hkijena.jipipe.extensions.tables.parameters.processors.ExpressionTableColumnGeneratorProcessor;
import org.hkijena.jipipe.ui.JIPipeWorkbench;
import org.hkijena.jipipe.utils.ResourceUtils;
import org.hkijena.jipipe.utils.UIUtils;

/**
 * Algorithm that adds or replaces a column by a generated value
 */
@JIPipeDocumentation(name = "Table from expressions", description = "Generates a table from expressions")
@JIPipeOrganization(nodeTypeCategory = DataSourceNodeTypeCategory.class)
@JIPipeOutputSlot(value = ResultsTableData.class, slotName = "Output", autoCreate = true)
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
    protected void runIteration(JIPipeDataBatch dataBatch, JIPipeProgressInfo progressInfo) {
        ResultsTableData table = new ResultsTableData();
        table.addRows(generatedRows);
        ExpressionParameters variableSet = new ExpressionParameters();
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
        dataBatch.addOutputData(getFirstOutputSlot(), table, progressInfo);
    }

    @Override
    public void reportValidity(JIPipeValidityReport report) {
        report.forCategory("Columns").report(columns);
    }


    @JIPipeDocumentation(name = "Columns", description = "Columns to be generated")
    @JIPipeParameter("columns")
    @ExpressionParameterSettings(variableSource = TableCellExpressionParameterVariableSource.class)
    @PairParameterSettings(singleRow = false, keyLabel = "Function", valueLabel = "Output column")
    public ExpressionTableColumnGeneratorProcessorParameterList getColumns() {
        return columns;
    }

    @JIPipeParameter("columns")
    public void setColumns(ExpressionTableColumnGeneratorProcessorParameterList columns) {
        this.columns = columns;
    }

    @JIPipeDocumentation(name = "Generated rows", description = "Determines how many rows to generate")
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

    @JIPipeDocumentation(name = "Load example", description = "Loads example parameters that showcase how to use this algorithm.")
    @JIPipeContextAction(iconURL = ResourceUtils.RESOURCE_BASE_PATH + "/icons/actions/graduation-cap.png")
    public void setToExample(JIPipeWorkbench parent) {
        if (UIUtils.confirmResetParameters(parent, "Load example")) {
            columns.clear();
            ExpressionTableColumnGeneratorProcessor columnX = columns.addNewInstance();
            columnX.setKey(new DefaultExpressionParameter("row / 100"));
            columnX.setValue("x");
            ExpressionTableColumnGeneratorProcessor columnY = columns.addNewInstance();
            columnY.setKey(new DefaultExpressionParameter("SIN(row / 100)"));
            columnY.setValue("y");
            getEventBus().post(new ParameterChangedEvent(this, "columns"));
        }
    }
}
