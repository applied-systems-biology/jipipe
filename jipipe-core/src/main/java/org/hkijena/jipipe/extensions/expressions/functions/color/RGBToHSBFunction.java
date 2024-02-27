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

package org.hkijena.jipipe.extensions.expressions.functions.color;

import com.google.common.collect.ImmutableList;
import org.hkijena.jipipe.api.SetJIPipeDocumentation;
import org.hkijena.jipipe.extensions.expressions.ExpressionFunction;
import org.hkijena.jipipe.extensions.expressions.JIPipeExpressionVariablesMap;
import org.hkijena.jipipe.extensions.expressions.ParameterInfo;

import java.awt.*;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;

@SetJIPipeDocumentation(name = "RGB to HSB colors", description = "Converts an RGB color array into an HSB color array")
public class RGBToHSBFunction extends ExpressionFunction {

    public RGBToHSBFunction() {
        super("RGB_TO_HSB", 1);
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
        float[] hsb = Color.RGBtoHSB(r, g, b, null);
        int H = ((int) (hsb[0] * 255.0));
        int S = ((int) (hsb[1] * 255.0));
        int B = ((int) (hsb[2] * 255.0));
        return Arrays.asList(H, S, B);
    }
}
