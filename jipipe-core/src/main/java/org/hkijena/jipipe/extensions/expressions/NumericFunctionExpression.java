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

import org.hkijena.jipipe.api.parameters.JIPipeParameterAccess;
import org.hkijena.jipipe.api.parameters.JIPipeParameterTree;

import java.util.HashSet;
import java.util.Set;

/**
 * A parameter that is intended to set or scale an integer
 */
@JIPipeExpressionParameterSettings(variableSource = NumericFunctionExpression.VariablesInfo.class)
public class NumericFunctionExpression extends JIPipeExpressionParameter {
    public NumericFunctionExpression() {
        super("default");
    }

    public NumericFunctionExpression(String expression) {
        super(expression);
    }

    public NumericFunctionExpression(NumericFunctionExpression other) {
        super(other.getExpression());
    }

    public double apply(double defaultValue, JIPipeExpressionVariablesMap parameters) {
        parameters.set("x", defaultValue);
        parameters.set("default", defaultValue);
        return evaluateToDouble(parameters);
    }

    /**
     * Modifies the expression to 0 or x depending on whether an exact value should be set
     *
     * @param exactValue if an exact value is expected
     */
    public void ensureExactValue(boolean exactValue) {
        if (exactValue) {
            if (getExpression().contains("x") || getExpression().contains("default"))
                setExpression("0");
        }
    }

    /**
     * Sets the expression to an exact value
     *
     * @param x the exact value
     */
    public void setExactValue(double x) {
        setExpression(x + "");
    }

    public static class VariablesInfo implements ExpressionParameterVariablesInfo {

        private static final Set<JIPipeExpressionParameterVariableInfo> VARIABLES = new HashSet<>();

        static {
            VARIABLES.add(JIPipeExpressionParameterVariableInfo.ANNOTATIONS_VARIABLE);
            VARIABLES.add(new JIPipeExpressionParameterVariableInfo("x", "x", "The current input value [DEPRECATED]"));
            VARIABLES.add(new JIPipeExpressionParameterVariableInfo("default", "default", "The current input value"));
        }

        @Override
        public Set<JIPipeExpressionParameterVariableInfo> getVariables(JIPipeParameterTree parameterTree, JIPipeParameterAccess parameterAccess) {
            return VARIABLES;
        }
    }
}
