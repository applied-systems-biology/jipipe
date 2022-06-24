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

import java.util.HashSet;
import java.util.Set;

/**
 * A parameter that is intended to set or scale an integer
 */
@ExpressionParameterSettings(variableSource = NumericFunctionExpression.VariableSource.class)
public class NumericFunctionExpression extends DefaultExpressionParameter {
    public NumericFunctionExpression() {
        super("x");
    }

    public NumericFunctionExpression(String expression) {
        super(expression);
    }

    public NumericFunctionExpression(NumericFunctionExpression other) {
        super(other.getExpression());
    }

    public double apply(double x, ExpressionVariables parameters) {
        parameters.set("x", x);
        return ((Number) evaluate(parameters)).doubleValue();
    }

    /**
     * Modifies the expression to 0 or x depending on whether an exact value should be set
     *
     * @param exactValue if an exact value is expected
     */
    public void ensureExactValue(boolean exactValue) {
        if (exactValue) {
            if (getExpression().contains("x"))
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

    public static class VariableSource implements ExpressionParameterVariableSource {

        private static final Set<ExpressionParameterVariable> VARIABLES = new HashSet<>();

        static {
            VARIABLES.add(ExpressionParameterVariable.ANNOTATIONS_VARIABLE);
            VARIABLES.add(new ExpressionParameterVariable("x", "The input value", "x"));
        }

        @Override
        public Set<ExpressionParameterVariable> getVariables(JIPipeParameterAccess parameterAccess) {
            return VARIABLES;
        }
    }
}
