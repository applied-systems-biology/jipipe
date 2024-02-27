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

import org.hkijena.jipipe.api.SetJIPipeDocumentation;
import org.hkijena.jipipe.extensions.expressions.ExpressionFunction;
import org.hkijena.jipipe.extensions.expressions.JIPipeExpressionVariablesMap;
import org.hkijena.jipipe.extensions.expressions.ParameterInfo;
import org.hkijena.jipipe.extensions.parameters.library.colors.ColorMap;

import java.awt.*;
import java.util.Arrays;
import java.util.List;

@SetJIPipeDocumentation(name = "False color", description = "Generates an RGB false color for a numeric input. The color is based around a color map. " +
        "The generated color is an array of size 3 containing the RGB components (0-255 per channel).")
public class FalseColorsFunction extends ExpressionFunction {
    public FalseColorsFunction() {
        super("VALUE_TO_COLOR", 2);
    }

    @Override
    public ParameterInfo getParameterInfo(int index) {
        if (index == 0) {
            return new ParameterInfo("Value", "A numeric value between 0 and 1. (Tip: Use PERC(value, min, max))", Number.class);
        } else {
            StringBuilder descriptionBuilder = new StringBuilder();
            descriptionBuilder.append("The color map. Supported maps: ");
            for (ColorMap value : ColorMap.values()) {
                descriptionBuilder.append(value).append(" ");
            }
            return new ParameterInfo("Color map", descriptionBuilder.toString(), String.class);
        }
    }

    @Override
    public Object evaluate(List<Object> parameters, JIPipeExpressionVariablesMap variables) {
        double value = ((Number) parameters.get(0)).doubleValue();
        value = Math.max(0, Math.min(1, value));
        ColorMap map = ColorMap.valueOf(parameters.get(1) + "");
        Color color = map.apply(value);
        return Arrays.asList(color.getRed(), color.getGreen(), color.getBlue());
    }
}
