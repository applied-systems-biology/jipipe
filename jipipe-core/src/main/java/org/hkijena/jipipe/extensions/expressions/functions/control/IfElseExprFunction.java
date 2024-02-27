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

package org.hkijena.jipipe.extensions.expressions.functions.control;

import org.hkijena.jipipe.api.SetJIPipeDocumentation;
import org.hkijena.jipipe.extensions.expressions.JIPipeExpressionParameter;
import org.hkijena.jipipe.extensions.expressions.ExpressionFunction;
import org.hkijena.jipipe.extensions.expressions.JIPipeExpressionVariablesMap;
import org.hkijena.jipipe.extensions.expressions.ParameterInfo;

import java.util.List;

@SetJIPipeDocumentation(name = "If-Else condition (lazy)", description = "Returns the second parameter if the first is true, otherwise return the third parameter. Compared to IF_ELSE, the if_true and if_false parameters are expected to be expressions.")
public class IfElseExprFunction extends ExpressionFunction {

    public IfElseExprFunction() {
        super("IF_ELSE_EXPR", 3);
    }

    @Override
    public ParameterInfo getParameterInfo(int index) {
        switch (index) {
            case 0:
                return new ParameterInfo("boolean", "The condition", Boolean.class);
            case 1:
                return new ParameterInfo("if_true", "Evaluated if the boolean is true. Please note that you must be a string with an expression. Tip: use the ${ [your expression here] } operator to avoid escaping.", String.class);
            case 2:
                return new ParameterInfo("if_false", "Evaluated if the boolean is false. Please note that you must be a string with an expression. Tip: use the ${ [your expression here] } operator to avoid escaping.", String.class);
            default:
                return null;
        }
    }

    @Override
    public Object evaluate(List<Object> parameters, JIPipeExpressionVariablesMap variables) {
        boolean condition = (boolean) parameters.get(0);
        JIPipeExpressionVariablesMap copy = new JIPipeExpressionVariablesMap(variables);
        if (condition)
            return JIPipeExpressionParameter.getEvaluatorInstance().evaluate(parameters.get(1) + "", copy);
        else
            return JIPipeExpressionParameter.getEvaluatorInstance().evaluate(parameters.get(2) + "", copy);
    }

    @Override
    public String getSignature() {
        return String.format("%s(%s, %s, %s)", getName(), "boolean", "if_true", "if_false");
    }
}
