package org.hkijena.jipipe.extensions.expressions.functions.math;

import ij.IJ;
import org.apache.commons.math3.util.Precision;
import org.hkijena.jipipe.api.JIPipeDocumentation;
import org.hkijena.jipipe.extensions.expressions.ExpressionFunction;
import org.hkijena.jipipe.extensions.expressions.ExpressionVariables;
import org.hkijena.jipipe.extensions.expressions.ParameterInfo;

import java.util.List;

@JIPipeDocumentation(name = "Number to string", description = "Rounds a number to a specified number of decimals and outputs the value as string")
public class DoubleToStringFunction extends ExpressionFunction {
    public DoubleToStringFunction() {
        super("D2S", 2);
    }

    @Override
    public Object evaluate(List<Object> parameters, ExpressionVariables variables) {
        double value = ((Number) parameters.get(0)).doubleValue();
        int decimals = ((Number) parameters.get(1)).intValue();
        return IJ.d2s(value, decimals);
    }

    @Override
    public ParameterInfo getParameterInfo(int index) {
        if (index == 0) {
            return new ParameterInfo("Value", "The value to be rounded.", Number.class);
        } else {
            return new ParameterInfo("Decimals", "The number of decimals.", Integer.class);
        }
    }
}
