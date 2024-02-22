package org.hkijena.jipipe.extensions.expressions.functions.math;

import org.hkijena.jipipe.api.SetJIPipeDocumentation;
import org.hkijena.jipipe.extensions.expressions.ExpressionFunction;
import org.hkijena.jipipe.extensions.expressions.JIPipeExpressionVariablesMap;
import org.hkijena.jipipe.extensions.expressions.ParameterInfo;

import java.util.List;

@SetJIPipeDocumentation(name = "Number is NaN", description = "Returns true if the parameter is NaN")
public class IsNaNFunction extends ExpressionFunction {
    public IsNaNFunction() {
        super("IS_NAN", 1);
    }

    @Override
    public Object evaluate(List<Object> parameters, JIPipeExpressionVariablesMap variables) {
        if(parameters.get(0) instanceof Double) {
            return Double.isNaN((Double) parameters.get(0));
        }
        else if(parameters.get(0) instanceof Float) {
            return Float.isNaN((Float) parameters.get(0));
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
