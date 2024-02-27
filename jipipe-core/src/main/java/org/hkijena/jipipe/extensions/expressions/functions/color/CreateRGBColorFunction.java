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

import java.util.Arrays;
import java.util.List;

@SetJIPipeDocumentation(name = "RGB Color", description = "Creates an array that represents a RGB color (0-255 per channel)")
public class CreateRGBColorFunction extends ExpressionFunction {
    public CreateRGBColorFunction() {
        super("RGB_COLOR", 3);
    }

    @Override
    public ParameterInfo getParameterInfo(int index) {
        if (index == 0) {
            return new ParameterInfo("Red", "The red component", Number.class);
        } else if (index == 1) {
            return new ParameterInfo("Green", "The green component", Number.class);
        } else if (index == 2) {
            return new ParameterInfo("Blue", "The blue component", Number.class);
        } else {
            return null;
        }
    }

    @Override
    public Object evaluate(List<Object> parameters, JIPipeExpressionVariablesMap variables) {
        int r = ((Number) parameters.get(0)).intValue();
        int g = ((Number) parameters.get(1)).intValue();
        int b = ((Number) parameters.get(2)).intValue();
        return Arrays.asList(r, g, b);
    }
}
