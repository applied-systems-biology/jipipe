package org.hkijena.jipipe.extensions.expressions.functions.color;

import com.google.common.collect.ImmutableList;
import org.hkijena.jipipe.api.JIPipeDocumentation;
import org.hkijena.jipipe.extensions.expressions.ExpressionFunction;
import org.hkijena.jipipe.extensions.expressions.ExpressionVariables;
import org.hkijena.jipipe.extensions.expressions.ParameterInfo;

import java.awt.*;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;

@JIPipeDocumentation(name = "HSB to RGB colors", description = "Converts an HSB color array into an RGB color array")
public class HSBToRGBFunction extends ExpressionFunction {

    public HSBToRGBFunction() {
        super("HSB_TO_RGB", 1);
    }

    @Override
    public ParameterInfo getParameterInfo(int index) {
        return new ParameterInfo("HSB", "Array of size 3 containing the HSB components (0-255 per channel)", Collection.class);
    }

    @Override
    public Object evaluate(List<Object> parameters, ExpressionVariables variables) {
        List<?> collection = ImmutableList.copyOf((Collection<?>) parameters.get(0));
        int H = ((Number) collection.get(0)).intValue();
        int S = ((Number) collection.get(1)).intValue();
        int B = ((Number) collection.get(2)).intValue();
        Color color = new Color(Color.HSBtoRGB(H / 255.0f, S / 255.0f, B / 255.0f));
        return Arrays.asList(color.getRed(), color.getGreen(), color.getBlue());
    }
}
