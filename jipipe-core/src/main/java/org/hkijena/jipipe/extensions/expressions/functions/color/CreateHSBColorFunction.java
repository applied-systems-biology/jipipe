package org.hkijena.jipipe.extensions.expressions.functions.color;

import org.hkijena.jipipe.api.JIPipeDocumentation;
import org.hkijena.jipipe.extensions.expressions.ExpressionFunction;
import org.hkijena.jipipe.extensions.expressions.JIPipeExpressionVariablesMap;
import org.hkijena.jipipe.extensions.expressions.ParameterInfo;

import java.util.Arrays;
import java.util.List;

@JIPipeDocumentation(name = "HSB Color", description = "Creates an array that represents a HSB color (0-255 per channel)")
public class CreateHSBColorFunction extends ExpressionFunction {
    public CreateHSBColorFunction() {
        super("HSB_COLOR", 3);
    }

    @Override
    public ParameterInfo getParameterInfo(int index) {
        if (index == 0) {
            return new ParameterInfo("Hue", "The hue component", Number.class);
        } else if (index == 1) {
            return new ParameterInfo("Saturation", "The saturation component", Number.class);
        } else if (index == 2) {
            return new ParameterInfo("Brightness", "The brightness component", Number.class);
        } else {
            return null;
        }
    }

    @Override
    public Object evaluate(List<Object> parameters, JIPipeExpressionVariablesMap variables) {
        int h = ((Number) parameters.get(0)).intValue();
        int s = ((Number) parameters.get(1)).intValue();
        int b = ((Number) parameters.get(2)).intValue();
        return Arrays.asList(h, s, b);
    }
}
