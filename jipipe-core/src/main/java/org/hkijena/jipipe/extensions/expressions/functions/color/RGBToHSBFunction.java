package org.hkijena.jipipe.extensions.expressions.functions.color;

import com.google.common.collect.ImmutableList;
import org.hkijena.jipipe.api.JIPipeDocumentation;
import org.hkijena.jipipe.extensions.expressions.ExpressionFunction;
import org.hkijena.jipipe.extensions.expressions.ExpressionVariables;
import org.hkijena.jipipe.extensions.expressions.ParameterInfo;
import org.hkijena.jipipe.utils.ColorUtils;

import java.awt.Color;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;

@JIPipeDocumentation(name = "RGB to HSB colors", description = "Converts an RGB color array into an HSB color array")
public class RGBToHSBFunction extends ExpressionFunction {

    public RGBToHSBFunction() {
        super("RGB_TO_HSB", 1);
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
        float[] hsb = Color.RGBtoHSB(r, g, b, null);
        int H = ((int) (hsb[0] * 255.0));
        int S = ((int) (hsb[1] * 255.0));
        int B = ((int) (hsb[2] * 255.0));
        return Arrays.asList(H,S,B);
    }
}
