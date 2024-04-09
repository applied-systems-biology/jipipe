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

import org.hkijena.jipipe.api.parameters.JIPipeParameterAccess;
import org.hkijena.jipipe.api.parameters.JIPipeParameterTree;

import java.util.HashSet;
import java.util.Set;

public class TableColumnValuesExpressionParameterVariablesInfo implements ExpressionParameterVariablesInfo {
    private final static Set<JIPipeExpressionParameterVariableInfo> VARIABLES;

    static {
        VARIABLES = new HashSet<>();
        VARIABLES.add(new JIPipeExpressionParameterVariableInfo("", "<Column values>", "The values of all other columns"));
        VARIABLES.add(new JIPipeExpressionParameterVariableInfo("column", "Table column index", "The column index"));
        VARIABLES.add(new JIPipeExpressionParameterVariableInfo("column_name", "Table column name", "The column name"));
        VARIABLES.add(new JIPipeExpressionParameterVariableInfo("values", "Column values", "The values of this column as array"));
        VARIABLES.add(new JIPipeExpressionParameterVariableInfo("num_rows", "Number of rows", "The number of rows within the table"));
        VARIABLES.add(new JIPipeExpressionParameterVariableInfo("num_cols", "Number of columns", "The number of columns within the table"));
        VARIABLES.add(new JIPipeExpressionParameterVariableInfo("annotations", "Annotations", "Map of annotations of the current data batch"));
    }

    @Override
    public Set<JIPipeExpressionParameterVariableInfo> getVariables(JIPipeParameterTree parameterTree, JIPipeParameterAccess parameterAccess) {
        return VARIABLES;
    }
}
