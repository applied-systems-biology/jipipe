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

package org.hkijena.jipipe.extensions.parameters.expressions;

import com.fathzer.soft.javaluator.StaticVariableSet;
import org.hkijena.jipipe.api.JIPipeValidatable;
import org.hkijena.jipipe.api.JIPipeValidityReport;
import org.hkijena.jipipe.api.exceptions.UserFriendlyRuntimeException;
import org.hkijena.jipipe.extensions.tables.datatypes.DoubleArrayTableColumn;
import org.hkijena.jipipe.extensions.tables.datatypes.ResultsTableData;
import org.hkijena.jipipe.extensions.tables.datatypes.StringArrayTableColumn;
import org.hkijena.jipipe.extensions.tables.datatypes.TableColumn;

/**
 * Parameter that acts as source (via matching a column) or a generator
 */
@ExpressionParameterSettings(variableSource = TableCellExpressionParameterVariableSource.class)
public class TableColumnSourceExpressionParameter extends DefaultExpressionParameter implements JIPipeValidatable {

    public static final String DOCUMENTATION_DESCRIPTION = "This parameter is an expression that has three modes: " +
            "(1) Selecting an existing column by name, (2) Matching an existing column by boolean operators, and (3) Generating a new column based on a mathematical formula.<br/>" +
            "<ol><li>Type in the name of the existing column. Put the name in double quotes. Example: <pre>\"Area\"</pre></li>" +
            "<li>There will be two variables 'column_name' and 'column' available. The expression is repeated for all available columns. Example: <pre>\"Data\" IN column_name</pre></li>" +
            "<li>There will be : 'row', 'num_rows', and 'num_cols'. Use them to generate values. Example: <pre>row^2</pre></li></ol>";

    public TableColumnSourceExpressionParameter() {
    }

    public TableColumnSourceExpressionParameter(String expression) {
        super(expression);
    }

    public TableColumnSourceExpressionParameter(ExpressionParameter other) {
        super(other);
    }

    /**
     * Picks or generates a table column based on selecting by name, matching one column by boolean expressions, or using a mathematical expression.
     * Operations are applied in order until a column is generated
     * @param table the table
     * @return the column
     */
    public TableColumn pickColumn(ResultsTableData table) {
        StaticVariableSet<Object> variableSet = new StaticVariableSet<>();
        variableSet.set("num_rows", table.getRowCount());
        variableSet.set("num_cols", table.getColumnCount());

        // Option 1: Column name string
        try {
            String expression = getExpression();

            // Apply user-friendliness fix
//            if(!expression.startsWith("\"") && !expression.endsWith("\""))
//                expression = "\"" + expression + "\"";

            Object evaluationResult = getEvaluator().evaluate(expression, variableSet);
            if(evaluationResult instanceof String) {
                int columnIndex = table.getColumnIndex((String) evaluationResult);
                if(columnIndex != -1) {
                    return table.getColumnReference(columnIndex);
                }
            }
        }
        catch (Exception e) {
        }

        // Option 2: Column matching
        for (int col = 0; col < table.getColumnCount(); col++) {
            variableSet.set("column_name", table.getColumnName(col));
            variableSet.set("column", col);
            try {
                Object evaluationResult = getEvaluator().evaluate(getExpression(), variableSet);
                if(evaluationResult instanceof Boolean) {
                    if((boolean)evaluationResult) {
                        return table.getColumnReference(col);
                    }
                }
            }
            catch (Exception e) {
            }
        }

        // Option 3: Column generation
        try {
            Object[] rawData = new Object[table.getRowCount()];
            boolean isNumeric = true;
            for (int row = 0; row < table.getRowCount(); row++) {
                variableSet.set("row", row);
                rawData[row] = getEvaluator().evaluate(getExpression(), variableSet);
                if(!(rawData[row] instanceof Number))
                    isNumeric = false;
            }

            if(isNumeric) {
                double[] data = new double[table.getRowCount()];
                for (int row = 0; row < table.getRowCount(); row++) {
                    data[row] = ((Number)rawData[row]).doubleValue();
                }
                return new DoubleArrayTableColumn(data, "Generated");
            }
            else {
                String[] data = new String[table.getRowCount()];
                for (int row = 0; row < table.getRowCount(); row++) {
                    data[row] = "" + rawData[row];
                }
                return new StringArrayTableColumn(data, "Generated");
            }

        }
        catch (Exception e) {
            throw new UserFriendlyRuntimeException(e,
                    "Could not find or generate column!",
                    "Table column source parameter",
                    "A node requested from you to specify a table column. You entered the expression '" + getExpression() + "', but it did not yield in a column.",
                    "Check if the expression is correct. If you want an existing column, it should return a string. If you want to search for one, it should return a boolean value. " +
                            "If you want to generate one, it can return a number or string.");
        }
    }

    @Override
    public void reportValidity(JIPipeValidityReport report) {
        report.checkNonEmpty(getExpression(), this);
    }
}
