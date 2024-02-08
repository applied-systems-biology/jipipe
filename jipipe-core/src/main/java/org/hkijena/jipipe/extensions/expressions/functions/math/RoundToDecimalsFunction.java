package org.hkijena.jipipe.extensions.expressions.functions.math;

import org.apache.commons.math3.util.Precision;
import org.hkijena.jipipe.api.JIPipeDocumentation;
import org.hkijena.jipipe.extensions.expressions.ExpressionFunction;
import org.hkijena.jipipe.extensions.expressions.JIPipeExpressionVariablesMap;
import org.hkijena.jipipe.extensions.expressions.ParameterInfo;

import java.util.List;

@JIPipeDocumentation(name = "Round to decimals", description = "Rounds a number to a specified number of decimals")
public class RoundToDecimalsFunction extends ExpressionFunction {
    public RoundToDecimalsFunction() {
        super("ROUNDD", 2);
    }

    @Override
    public Object evaluate(List<Object> parameters, JIPipeExpressionVariablesMap variables) {
        double value = ((Number) parameters.get(0)).doubleValue();
        int decimals = ((Number) parameters.get(1)).intValue();
        return Precision.round(value, decimals);
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
