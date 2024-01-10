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

package org.hkijena.jipipe.extensions.expressions;

import org.hkijena.jipipe.api.JIPipeDocumentationDescription;
import org.hkijena.jipipe.extensions.tables.datatypes.ResultsTableData;

/**
 * Expression for querying strings
 */
@ExpressionParameterSettings(variableSource = TableCellValueExpressionParameterVariableSource.class)
@JIPipeDocumentationDescription(description = "This parameter accesses all table cells. Please take a look at the expression builder for more information about the available variables.")
public class TableCellValueQueryExpression extends JIPipeExpressionParameter {

    public TableCellValueQueryExpression() {
    }

    public TableCellValueQueryExpression(String expression) {
        super(expression);
    }

    public TableCellValueQueryExpression(AbstractExpressionParameter other) {
        super(other);
    }


    /**
     * Returns true if the table cell is accepted by the expression
     *
     * @param tableData the table
     * @param row       the row
     * @param col       the column
     * @return if the expression accepts this cell
     */
    public boolean test(ResultsTableData tableData, int row, int col) {
        if ("true".equals(getExpression()) || getExpression().trim().isEmpty())
            return true;
        ExpressionVariables variableSet = new ExpressionVariables();
        variableSet.set("row", row);
        variableSet.set("column", col);
        variableSet.set("column_name", tableData.getColumnName(col));
        variableSet.set("num_rows", tableData.getRowCount());
        variableSet.set("num_cols", tableData.getColumnCount());
        variableSet.set("value", tableData.getValueAt(row, col));
        return test(variableSet);
    }
}
