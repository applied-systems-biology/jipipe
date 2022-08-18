package org.hkijena.jipipe.extensions.expressions.functions.control;

import org.hkijena.jipipe.api.JIPipeDocumentation;
import org.hkijena.jipipe.extensions.expressions.*;

import java.util.List;

@JIPipeDocumentation(name = "If-Else condition (lazy)", description = "Returns the second parameter if the first is true, otherwise return the third parameter. Compared to IF_ELSE, the if_true and if_false parameters are expected to be expressions.")
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
    public Object evaluate(List<Object> parameters, ExpressionVariables variables) {
        boolean condition = (boolean) parameters.get(0);
        ExpressionVariables copy = new ExpressionVariables(variables);
        if (condition)
            return DefaultExpressionParameter.getEvaluatorInstance().evaluate(parameters.get(1) + "", copy);
        else
            return DefaultExpressionParameter.getEvaluatorInstance().evaluate(parameters.get(2) + "", copy);
    }

    @Override
    public String getSignature() {
        return String.format("%s(%s, %s, %s)", getName(), "boolean", "if_true", "if_false");
    }
}
