package org.hkijena.jipipe.extensions.expressions.functions.math;

import org.hkijena.jipipe.api.SetJIPipeDocumentation;
import org.hkijena.jipipe.extensions.expressions.ExpressionFunction;
import org.hkijena.jipipe.extensions.expressions.JIPipeExpressionVariablesMap;
import org.hkijena.jipipe.extensions.expressions.ParameterInfo;

import java.util.List;

@SetJIPipeDocumentation(name = "Number is finite", description = "Returns true if the parameter is finite")
public class IsFiniteFunction extends ExpressionFunction {
    public IsFiniteFunction() {
        super("IS_FINITE", 1);
    }

    @Override
    public Object evaluate(List<Object> parameters, JIPipeExpressionVariablesMap variables) {
        if(parameters.get(0) instanceof Double) {
            return Double.isFinite((Double) parameters.get(0));
        }
        else if(parameters.get(0) instanceof Float) {
            return Float.isFinite((Float) parameters.get(0));
        }
        else {
            return true;
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
