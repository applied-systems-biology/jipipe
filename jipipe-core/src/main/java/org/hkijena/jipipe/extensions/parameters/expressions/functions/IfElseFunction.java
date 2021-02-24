package org.hkijena.jipipe.extensions.parameters.expressions.functions;

import org.hkijena.jipipe.extensions.parameters.expressions.ExpressionParameters;
import org.hkijena.jipipe.api.JIPipeDocumentation;
import org.hkijena.jipipe.extensions.parameters.expressions.ExpressionFunction;
import org.hkijena.jipipe.extensions.parameters.expressions.ParameterInfo;

import java.util.List;

@JIPipeDocumentation(name = "If-Else condition", description = "Returns the second parameter if the first is true, otherwise return the third parameter.")
public class IfElseFunction extends ExpressionFunction {

    public IfElseFunction() {
        super("IF_ELSE", 3);
    }

    @Override
    public ParameterInfo getParameterInfo(int index) {
        switch (index) {
            case 0:
                return new ParameterInfo("boolean", "The condition", Boolean.class);
            case 1:
                return new ParameterInfo("if_true", "Returned if the boolean is true");
            case 2:
                return new ParameterInfo("if_false", "Returned if the boolean is false");
            default:
                return null;
        }
    }

    @Override
    public Object evaluate(List<Object> parameters, ExpressionParameters variables) {
        boolean condition = (boolean) parameters.get(0);
        if (condition)
            return parameters.get(1);
        else
            return parameters.get(2);
    }

    @Override
    public String getSignature() {
        return String.format("%s(%s, %s, %s)", getName(), "boolean", "if_true", "if_false");
    }
}
