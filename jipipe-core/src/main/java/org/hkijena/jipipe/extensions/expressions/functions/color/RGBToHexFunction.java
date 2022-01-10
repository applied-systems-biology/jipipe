package org.hkijena.jipipe.extensions.expressions.functions.color;

import com.google.common.collect.ImmutableList;
import org.hkijena.jipipe.api.JIPipeDocumentation;
import org.hkijena.jipipe.extensions.expressions.ExpressionFunction;
import org.hkijena.jipipe.extensions.expressions.ExpressionVariables;
import org.hkijena.jipipe.extensions.expressions.ParameterInfo;
import org.hkijena.jipipe.utils.ColorUtils;

import java.awt.Color;
import java.util.Collection;
import java.util.List;

@JIPipeDocumentation(name = "RGB to Hex color", description = "Converts an RGB color into a hex string #rrggbb")
public class RGBToHexFunction extends ExpressionFunction {
    public RGBToHexFunction() {
        super("RGB_TO_HEX", 1);
    }

    @Override
    public ParameterInfo getParameterInfo(int index) {
        return new ParameterInfo("RGB", "Array of size 3 containing the RGB components (0-255 per channel)", Collection.class);
    }

    @Override
    public Object evaluate(List<Object> parameters, ExpressionVariables variables) {
        List<?> collection = ImmutableList.copyOf((Collection<?>) parameters.get(0));
        int r = ((Number)collection.get(0)).intValue();
        int g = ((Number)collection.get(1)).intValue();
        int b = ((Number)collection.get(2)).intValue();
        return ColorUtils.colorToHexString(new Color(r,g,b));
    }
}