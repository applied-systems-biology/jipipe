package org.hkijena.jipipe.extensions.expressions.functions.color;

import org.hkijena.jipipe.api.JIPipeDocumentation;
import org.hkijena.jipipe.extensions.expressions.ExpressionFunction;
import org.hkijena.jipipe.extensions.expressions.JIPipeExpressionVariablesMap;
import org.hkijena.jipipe.extensions.expressions.ParameterInfo;

import java.util.Arrays;
import java.util.List;

@JIPipeDocumentation(name = "RGB Color", description = "Creates an array that represents a RGB color (0-255 per channel)")
public class CreateRGBColorFunction extends ExpressionFunction {
    public CreateRGBColorFunction() {
        super("RGB_COLOR", 3);
    }

    @Override
    public ParameterInfo getParameterInfo(int index) {
        if (index == 0) {
            return new ParameterInfo("Red", "The red component", Number.class);
        } else if (index == 1) {
            return new ParameterInfo("Green", "The green component", Number.class);
        } else if (index == 2) {
            return new ParameterInfo("Blue", "The blue component", Number.class);
        } else {
            return null;
        }
    }

    @Override
    public Object evaluate(List<Object> parameters, JIPipeExpressionVariablesMap variables) {
        int r = ((Number) parameters.get(0)).intValue();
        int g = ((Number) parameters.get(1)).intValue();
        int b = ((Number) parameters.get(2)).intValue();
        return Arrays.asList(r, g, b);
    }
}
