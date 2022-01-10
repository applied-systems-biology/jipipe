package org.hkijena.jipipe.extensions.expressions.functions.math;

import org.hkijena.jipipe.api.JIPipeDocumentation;
import org.hkijena.jipipe.extensions.expressions.ExpressionFunction;
import org.hkijena.jipipe.extensions.expressions.ExpressionVariables;
import org.hkijena.jipipe.extensions.expressions.ParameterInfo;

import java.util.List;

@JIPipeDocumentation(name = "Calculate percentage", description = "Calculates the percentage of a value between a minimum and a maximum. The output is always between zero and one.")
public class PercentageFunction extends ExpressionFunction {
    public PercentageFunction() {
        super("PERC", 3);
    }

    @Override
    public Object evaluate(List<Object> parameters, ExpressionVariables variables) {
        double value = ((Number)parameters.get(0)).doubleValue();
        double min = ((Number)parameters.get(1)).doubleValue();
        double max = ((Number)parameters.get(2)).doubleValue();
        return Math.max(0, Math.min(1, (value - min) / (max - min)));
    }

    @Override
    public ParameterInfo getParameterInfo(int index) {
        if(index == 0) {
            return new ParameterInfo("Value", "The value", Number.class);
        }
        else if(index == 1) {
            return new ParameterInfo("Minimum", "The minimum", Number.class);
        }
        else {
            return new ParameterInfo("Maximum", "The maximum", Number.class);
        }
    }
}