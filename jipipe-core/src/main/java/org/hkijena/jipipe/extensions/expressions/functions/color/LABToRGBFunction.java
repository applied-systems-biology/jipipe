package org.hkijena.jipipe.extensions.expressions.functions.color;

import com.google.common.collect.ImmutableList;
import ij.process.ColorSpaceConverter;
import org.hkijena.jipipe.api.JIPipeDocumentation;
import org.hkijena.jipipe.extensions.expressions.ExpressionFunction;
import org.hkijena.jipipe.extensions.expressions.ExpressionVariables;
import org.hkijena.jipipe.extensions.expressions.ParameterInfo;

import java.util.Arrays;
import java.util.Collection;
import java.util.List;

@JIPipeDocumentation(name = "LAB to RGB colors", description = "Converts an LAB color array into an RGB color array")
public class LABToRGBFunction extends ExpressionFunction {

    private static final ColorSpaceConverter CONVERTER = new ColorSpaceConverter();

    public LABToRGBFunction() {
        super("LAB_TO_RGB", 1);
    }

    @Override
    public ParameterInfo getParameterInfo(int index) {
        return new ParameterInfo("LAB", "Array of size 3 containing the LAB components (0-255 per channel)", Collection.class);
    }

    @Override
    public Object evaluate(List<Object> parameters, ExpressionVariables variables) {
        List<?> collection = ImmutableList.copyOf((Collection<?>) parameters.get(0));
        int l_ = ((Number)collection.get(0)).intValue();
        int a_ = ((Number)collection.get(1)).intValue();
        int b_ = ((Number)collection.get(2)).intValue();
        int[] rgb = CONVERTER.LABtoRGB(l_,a_,b_);
        return Arrays.asList(rgb[0], rgb[1], rgb[2]);
    }
}
