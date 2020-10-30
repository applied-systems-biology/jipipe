package org.hkijena.jipipe.extensions.tables.parameters;

import org.hkijena.jipipe.api.parameters.JIPipeParameterAccess;
import org.hkijena.jipipe.extensions.parameters.expressions.ExpressionParameterVariable;
import org.hkijena.jipipe.extensions.parameters.expressions.ExpressionParameterVariableSource;

import java.util.HashSet;
import java.util.Set;

public class TableCellExpressionParameterVariableSource implements ExpressionParameterVariableSource {
    private final static Set<ExpressionParameterVariable> VARIABLES;

    static {
        VARIABLES = new HashSet<>();
        VARIABLES.add(new ExpressionParameterVariable("Table row index", "The row index", "row"));
        VARIABLES.add(new ExpressionParameterVariable("Table column index", "The column index", "column"));
        VARIABLES.add(new ExpressionParameterVariable("Table column name", "The column name", "column_name"));
        VARIABLES.add(new ExpressionParameterVariable("Number of rows", "The number of rows within the table", "num_rows"));
        VARIABLES.add(new ExpressionParameterVariable("Number of columns", "The number of columns within the table", "num_cols"));
    }

    @Override
    public Set<ExpressionParameterVariable> getVariables(JIPipeParameterAccess parameterAccess) {
        return VARIABLES;
    }
}
