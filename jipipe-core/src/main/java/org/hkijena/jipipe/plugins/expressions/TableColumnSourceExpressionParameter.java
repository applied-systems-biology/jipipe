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

package org.hkijena.jipipe.plugins.expressions;

import com.google.common.primitives.Doubles;
import org.hkijena.jipipe.api.AddJIPipeDocumentationDescription;
import org.hkijena.jipipe.api.parameters.JIPipeParameterAccess;
import org.hkijena.jipipe.api.parameters.JIPipeParameterTree;
import org.hkijena.jipipe.api.validation.JIPipeValidatable;
import org.hkijena.jipipe.api.validation.JIPipeValidationReportEntry;
import org.hkijena.jipipe.api.validation.JIPipeValidationReportEntryLevel;
import org.hkijena.jipipe.api.validation.JIPipeValidationRuntimeException;
import org.hkijena.jipipe.api.validation.contexts.UnspecifiedValidationReportContext;
import org.hkijena.jipipe.plugins.parameters.api.pairs.PairParameter;
import org.hkijena.jipipe.plugins.parameters.api.pairs.PairParameterSettings;
import org.hkijena.jipipe.plugins.tables.datatypes.DoubleArrayTableColumn;
import org.hkijena.jipipe.plugins.tables.datatypes.ResultsTableData;
import org.hkijena.jipipe.plugins.tables.datatypes.StringArrayTableColumn;
import org.hkijena.jipipe.plugins.tables.datatypes.TableColumn;
import org.hkijena.jipipe.utils.StringUtils;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

/**
 * Parameter that acts as source (via matching a column) or a generator
 */
@JIPipeExpressionParameterSettings(variableSource = TableColumnSourceExpressionParameter.VariablesInfo.class, hint = "table column/data")
@PairParameterSettings(keyLabel = "Column source", valueLabel = "Column name/value")
@AddJIPipeDocumentationDescription(description = "This parameter can be used to either select an existing column from a table or to generate a new column by providing a value for each row." +
        "<ul>" +
        "<li>Selecting columns by exact name: Type in the name of the column into the value field. Quotation marks are optional.</li>" +
        "<li>Selecting columns by filtering: The value expression is called for each existing column and provided as variable 'value'. Return TRUE at any point to select the value. Example: <code>value == \"Mean\"</code></li>" +
        "<li>Generating columns: The value expression is called for each row. Return a string or number. You have access to the other column values inside the row (as variables). Example: <code>Mean + 0.5 * X</code>. " +
        "If you do not provide a valid expression, the expression itself is put in as column value (string)</li>" +
        "</ul>")
public class TableColumnSourceExpressionParameter extends PairParameter<TableColumnSourceExpressionParameter.TableSourceType, JIPipeExpressionParameter> implements JIPipeValidatable {

    public TableColumnSourceExpressionParameter() {
        super(TableColumnSourceExpressionParameter.TableSourceType.class, JIPipeExpressionParameter.class);
        setKey(TableSourceType.ExistingColumn);
        setValue(new JIPipeExpressionParameter());
    }

    public TableColumnSourceExpressionParameter(TableColumnSourceExpressionParameter.TableSourceType type, String expression) {
        super(TableColumnSourceExpressionParameter.TableSourceType.class, JIPipeExpressionParameter.class);
        setKey(type);
        setValue(new JIPipeExpressionParameter(expression));
    }

    public TableColumnSourceExpressionParameter(TableColumnSourceExpressionParameter other) {
        super(other);
        this.setKey(other.getKey());
        this.setValue(new JIPipeExpressionParameter(other.getValue()));
    }

    /**
     * Picks or generates a table column based on selecting by name, matching one column by boolean expressions, or using a mathematical expression.
     * Operations are applied in order until a column is generated
     *
     * @param table the table
     * @return the column
     */
    public TableColumn pickOrGenerateColumn(ResultsTableData table) {
        JIPipeExpressionVariablesMap variables = new JIPipeExpressionVariablesMap();
        variables.set("num_rows", table.getRowCount());
        variables.set("num_cols", table.getColumnCount());

        if (getKey() == TableSourceType.ExistingColumn) {
            // Try to parse the expression
            try {
                Object evaluationResult = getValue().evaluate(variables);
                int columnIndex = table.getColumnIndex(StringUtils.nullToEmpty(evaluationResult));
                if (columnIndex != -1) {
                    return table.getColumnReference(columnIndex);
                }
            } catch (Exception e) {
            }

            // Must be a column name
            int columnIndex = table.getColumnIndex(getValue().getExpression());
            if (columnIndex != -1) {
                return table.getColumnReference(columnIndex);
            }
        } else {
            // Try to generate it
            Object[] rawData = new Object[table.getRowCount()];
            boolean isNumeric = true;
            boolean success = true;

            // Write statistics into variables
            for (int col = 0; col < table.getColumnCount(); col++) {
                TableColumn column = table.getColumnReference(col);
                if (column.isNumeric()) {
                    variables.set("all." + column.getLabel(), new ArrayList<>(Doubles.asList(column.getDataAsDouble(column.getRows()))));
                } else {
                    variables.set("all." + column.getLabel(), new ArrayList<>(Arrays.asList(column.getDataAsString(column.getRows()))));
                }
            }
            try {
                variables.set("num_rows", table.getRowCount());
                for (int row = 0; row < table.getRowCount(); row++) {
                    variables.set("row", row);
                    for (int col = 0; col < table.getColumnCount(); col++) {
                        variables.set(table.getColumnName(col), table.getValueAt(row, col));
                    }
                    rawData[row] = getValue().evaluate(variables);
                    if (!(rawData[row] instanceof Number))
                        isNumeric = false;
                }
            } catch (Exception e) {
                isNumeric = false;
                success = false;
            }

            if (!success) {
                // Is a string value
                Arrays.fill(rawData, getValue().getExpression());
            }
            if (isNumeric) {
                double[] data = new double[table.getRowCount()];
                for (int row = 0; row < table.getRowCount(); row++) {
                    data[row] = ((Number) rawData[row]).doubleValue();
                }
                return new DoubleArrayTableColumn(data, "Generated");
            } else {
                String[] data = new String[table.getRowCount()];
                for (int row = 0; row < table.getRowCount(); row++) {
                    data[row] = "" + rawData[row];
                }
                return new StringArrayTableColumn(data, "Generated");
            }
        }
        throw new JIPipeValidationRuntimeException(new JIPipeValidationReportEntry(JIPipeValidationReportEntryLevel.Error, new UnspecifiedValidationReportContext(), "Could not find column!",
                "Could not find or generate column!",
                "A node requested from you to specify a table column. You entered the expression '" + getValue().getExpression() + "', but it did not yield in a column.",
                "Check if the expression is correct. If you want an existing column, it should return a string. If you want to search for one, it should return a boolean value. " +
                        "If you want to generate one, it can return a number or string."));
    }

    @Override
    public JIPipeExpressionParameter getValue() {
        // Add the UI variables
        super.getValue().getAdditionalUIVariables().addAll(VariablesInfo.VARIABLES);
        return super.getValue();
    }

    public enum TableSourceType {
        Generate,
        ExistingColumn;


        @Override
        public String toString() {
            switch (this) {
                case Generate:
                    return "Generate (apply expression per row)";
                case ExistingColumn:
                    return "Select existing column";
                default:
                    throw new UnsupportedOperationException();
            }
        }
    }

    public static class VariablesInfo implements ExpressionParameterVariablesInfo {
        public final static Set<JIPipeExpressionParameterVariableInfo> VARIABLES;

        static {
            VARIABLES = new HashSet<>();
            VARIABLES.add(new JIPipeExpressionParameterVariableInfo("value", "value", "For selecting columns: Contains the currently selected column. Return TRUE if the column should be selected."));
            VARIABLES.add(new JIPipeExpressionParameterVariableInfo("row", "row", "For generating columns: The current row."));
            VARIABLES.add(new JIPipeExpressionParameterVariableInfo("", "<Column>", "For generating columns: The value of the column in the current row"));
            VARIABLES.add(new JIPipeExpressionParameterVariableInfo("", "all.<Column>", "For generating columns: All values of a column"));
            VARIABLES.add(new JIPipeExpressionParameterVariableInfo("num_rows", "Number of rows", "The number of rows within the table"));
            VARIABLES.add(new JIPipeExpressionParameterVariableInfo("num_cols", "Number of columns", "The number of columns within the table"));
        }

        @Override
        public Set<JIPipeExpressionParameterVariableInfo> getVariables(JIPipeParameterTree parameterTree, JIPipeParameterAccess parameterAccess) {
            return VARIABLES;
        }
    }
}
