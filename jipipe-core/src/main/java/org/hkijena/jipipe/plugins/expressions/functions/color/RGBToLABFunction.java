/*
 * Copyright by Zoltán Cseresnyés, Ruman Gerst
 *
 * Research Group Applied Systems Biology - Head: Prof. Dr. Marc Thilo Figge
 * https://www.leibniz-hki.de/en/applied-systems-biology.html
 * HKI-Center for Systems Biology of Infection
 * Leibniz Institute for Natural Product Research and Infection Biology - Hans Knöll Institute (HKI)
 * Adolf-Reichwein-Straße 23, 07745 Jena, Germany
 *
 * The project code is licensed under MIT.
 * See the LICENSE file provided with the code for the full license.
 */

package org.hkijena.jipipe.plugins.expressions.functions.color;

import com.google.common.collect.ImmutableList;
import ij.process.ColorSpaceConverter;
import org.hkijena.jipipe.api.SetJIPipeDocumentation;
import org.hkijena.jipipe.plugins.expressions.ExpressionFunction;
import org.hkijena.jipipe.plugins.expressions.JIPipeExpressionVariablesMap;
import org.hkijena.jipipe.plugins.expressions.ParameterInfo;

import java.util.Arrays;
import java.util.Collection;
import java.util.List;

@SetJIPipeDocumentation(name = "RGB to LAB colors", description = "Converts an RGB color array into an LAB color array")
public class RGBToLABFunction extends ExpressionFunction {

    private static final ColorSpaceConverter CONVERTER = new ColorSpaceConverter();

    public RGBToLABFunction() {
        super("RGB_TO_LAB", 1);
    }

    @Override
    public ParameterInfo getParameterInfo(int index) {
        return new ParameterInfo("RGB", "Array of size 3 containing the RGB components (0-255 per channel)", Collection.class);
    }

    @Override
    public Object evaluate(List<Object> parameters, JIPipeExpressionVariablesMap variables) {
        List<?> collection = ImmutableList.copyOf((Collection<?>) parameters.get(0));
        int r = ((Number) collection.get(0)).intValue();
        int g = ((Number) collection.get(1)).intValue();
        int b = ((Number) collection.get(2)).intValue();
        int pixel = (r << 16) + (g << 8) + b;
        double[] lab = CONVERTER.RGBtoLAB(pixel);
        int l_ = (int) Math.max(0, Math.min(255, lab[0]));
        int a_ = (int) Math.max(Byte.MIN_VALUE, Math.min(Byte.MAX_VALUE, lab[1])) - (int) Byte.MIN_VALUE;
        int b_ = (int) Math.max(Byte.MIN_VALUE, Math.min(Byte.MAX_VALUE, lab[2])) - (int) Byte.MIN_VALUE;
        return Arrays.asList(l_, a_, b_);
    }
}
