package org.hkijena.jipipe.extensions.expressions.functions.math;

import org.hkijena.jipipe.api.JIPipeDocumentation;
import org.hkijena.jipipe.extensions.expressions.ExpressionFunction;
import org.hkijena.jipipe.extensions.expressions.ExpressionVariables;
import org.hkijena.jipipe.extensions.expressions.ParameterInfo;

import java.util.List;

@JIPipeDocumentation(name = "Clamp", description = "Combined Min and Max operation.")
public class ClampFunction extends ExpressionFunction {
    public ClampFunction() {
        super("CLAMP", 3);
    }

    @Override
    public Object evaluate(List<Object> parameters, ExpressionVariables variables) {
        double value = ((Number) parameters.get(0)).doubleValue();
        double min = ((Number) parameters.get(1)).doubleValue();
        double max = ((Number) parameters.get(2)).doubleValue();
        return Math.max(min, Math.min(max, value));
    }

    @Override
    public ParameterInfo getParameterInfo(int index) {
        if (index == 0) {
            return new ParameterInfo("Value", "The value to be clamped.", Number.class);
        } else if (index == 1) {
            return new ParameterInfo("Min", "The minimum value.", Number.class);
        } else {
            return new ParameterInfo("Max", "The maximum value.", Number.class);
        }
    }
}
