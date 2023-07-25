package org.hkijena.jipipe.extensions.expressions.functions.math;

import org.apache.commons.math3.util.Precision;
import org.hkijena.jipipe.api.JIPipeDocumentation;
import org.hkijena.jipipe.extensions.expressions.ExpressionFunction;
import org.hkijena.jipipe.extensions.expressions.ExpressionVariables;
import org.hkijena.jipipe.extensions.expressions.ParameterInfo;

import java.util.List;

@JIPipeDocumentation(name = "Number is NaN", description = "Returns true if the parameter is NaN")
public class IsNaNFunction extends ExpressionFunction {
    public IsNaNFunction() {
        super("IS_NAN", 1);
    }

    @Override
    public Object evaluate(List<Object> parameters, ExpressionVariables variables) {
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
