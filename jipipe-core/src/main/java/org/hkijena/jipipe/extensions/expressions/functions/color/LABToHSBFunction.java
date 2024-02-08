package org.hkijena.jipipe.extensions.expressions.functions.color;

import org.hkijena.jipipe.api.JIPipeDocumentation;
import org.hkijena.jipipe.extensions.expressions.ExpressionFunction;
import org.hkijena.jipipe.extensions.expressions.JIPipeExpressionVariablesMap;
import org.hkijena.jipipe.extensions.expressions.ParameterInfo;

import java.util.Collection;
import java.util.Collections;
import java.util.List;

@JIPipeDocumentation(name = "LAB to HSB colors", description = "Converts an LAB color array into an HSB color array")
public class LABToHSBFunction extends ExpressionFunction {

    private static final LABToRGBFunction TO_RGB_FUNCTION = new LABToRGBFunction();
    private static final RGBToHSBFunction TO_HSB_FUNCTION = new RGBToHSBFunction();

    public LABToHSBFunction() {
        super("LAB_TO_HSB", 1);
    }

    @Override
    public ParameterInfo getParameterInfo(int index) {
        return new ParameterInfo("LAB", "Array of size 3 containing the LAB components (0-255 per channel)", Collection.class);
    }

    @Override
    public Object evaluate(List<Object> parameters, JIPipeExpressionVariablesMap variables) {
        return TO_HSB_FUNCTION.evaluate(Collections.singletonList(TO_RGB_FUNCTION.evaluate(parameters, variables)), variables);
    }
}
