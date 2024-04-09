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

import org.hkijena.jipipe.api.SetJIPipeDocumentation;
import org.hkijena.jipipe.plugins.expressions.ExpressionFunction;
import org.hkijena.jipipe.plugins.expressions.JIPipeExpressionVariablesMap;
import org.hkijena.jipipe.plugins.expressions.ParameterInfo;
import org.hkijena.jipipe.utils.ColorUtils;

import java.awt.*;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;

@SetJIPipeDocumentation(name = "Hex to RGB color", description = "Converts a hex string #rrggbb into an RGB color array")
public class HexToRGBFunction extends ExpressionFunction {
    public HexToRGBFunction() {
        super("HEX_TO_RGB", 1);
    }

    @Override
    public ParameterInfo getParameterInfo(int index) {
        return new ParameterInfo("RGB", "Array of size 3 containing the RGB components (0-255 per channel)", Collection.class);
    }

    @Override
    public Object evaluate(List<Object> parameters, JIPipeExpressionVariablesMap variables) {
        String hex = parameters.get(0) + "";
        Color color = ColorUtils.parseColor(hex);
        return Arrays.asList(color.getRed(), color.getGreen(), color.getBlue());
    }
}
