package org.hkijena.jipipe.extensions.expressions.functions.math;

import org.hkijena.jipipe.api.JIPipeDocumentation;
import org.hkijena.jipipe.extensions.expressions.ExpressionFunction;
import org.hkijena.jipipe.extensions.expressions.ExpressionVariables;
import org.hkijena.jipipe.extensions.expressions.ParameterInfo;

import java.util.List;

@JIPipeDocumentation(name = "Number is infinite", description = "Returns true if the parameter is infinite")
public class IsInfiniteFunction extends ExpressionFunction {
    public IsInfiniteFunction() {
        super("IS_INFINITE", 1);
    }

    @Override
    public Object evaluate(List<Object> parameters, ExpressionVariables variables) {
        if(parameters.get(0) instanceof Double) {
            return Double.isInfinite((Double) parameters.get(0));
        }
        else if(parameters.get(0) instanceof Float) {
            return Float.isInfinite((Float) parameters.get(0));
        }
        else {
            return false;
        }
    }

    @Override
    public ParameterInfo getParameterInfo(int index) {
        if (index == 0) {
            return new ParameterInfo("Value", "The value to be checked.", Number.class);
        }
        else {
            return null;
        }
    }
}
