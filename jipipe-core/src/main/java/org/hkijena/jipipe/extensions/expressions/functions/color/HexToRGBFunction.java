package org.hkijena.jipipe.extensions.expressions.functions.color;

import org.hkijena.jipipe.api.JIPipeDocumentation;
import org.hkijena.jipipe.extensions.expressions.ExpressionFunction;
import org.hkijena.jipipe.extensions.expressions.ExpressionVariables;
import org.hkijena.jipipe.extensions.expressions.ParameterInfo;
import org.hkijena.jipipe.utils.ColorUtils;

import java.awt.*;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;

@JIPipeDocumentation(name = "Hex to RGB color", description = "Converts a hex string #rrggbb into an RGB color array")
public class HexToRGBFunction extends ExpressionFunction {
    public HexToRGBFunction() {
        super("HEX_TO_RGB", 1);
    }

    @Override
    public ParameterInfo getParameterInfo(int index) {
        return new ParameterInfo("RGB", "Array of size 3 containing the RGB components (0-255 per channel)", Collection.class);
    }

    @Override
    public Object evaluate(List<Object> parameters, ExpressionVariables variables) {
        String hex = parameters.get(0) + "";
        Color color = ColorUtils.parseColor(hex);
        return Arrays.asList(color.getRed(), color.getGreen(), color.getBlue());
    }
}
