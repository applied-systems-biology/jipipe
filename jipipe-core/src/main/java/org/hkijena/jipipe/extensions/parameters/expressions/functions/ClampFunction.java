package org.hkijena.jipipe.extensions.parameters.expressions.functions;

import org.hkijena.jipipe.api.JIPipeDocumentation;
import org.hkijena.jipipe.extensions.parameters.expressions.DefaultExpressionParameter;
import org.hkijena.jipipe.extensions.parameters.expressions.ExpressionFunction;
import org.hkijena.jipipe.extensions.parameters.expressions.ExpressionParameters;
import org.hkijena.jipipe.extensions.parameters.expressions.ParameterInfo;
import org.hkijena.jipipe.utils.StringUtils;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

@JIPipeDocumentation(name = "Clamp", description = "Combined Min and Max operation.")
public class ClampFunction extends ExpressionFunction {
    public ClampFunction() {
        super("CLAMP", 3);
    }

    @Override
    public Object evaluate(List<Object> parameters, ExpressionParameters variables) {
       double value = ((Number)parameters.get(0)).doubleValue();
        double min = ((Number)parameters.get(1)).doubleValue();
        double max = ((Number)parameters.get(2)).doubleValue();
        return Math.max(min, Math.min(max, value));
    }

    @Override
    public ParameterInfo getParameterInfo(int index) {
        if (index == 0) {
            return new ParameterInfo("Value", "The value to be clamped.", Number.class);
        } else if(index == 1) {
            return new ParameterInfo("Min", "The minimum value.", Number.class);
        }
        else {
            return new ParameterInfo("Max", "The maximum value.", Number.class);
        }
    }
}
