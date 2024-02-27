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

import java.util.Collection;
import java.util.Collections;
import java.util.List;

@SetJIPipeDocumentation(name = "LAB to HSB colors", description = "Converts an LAB color array into an HSB color array")
public class LABToHSBFunction extends ExpressionFunction {

    private static final LABToRGBFunction TO_RGB_FUNCTION = new LABToRGBFunction();
    private static final RGBToHSBFunction TO_HSB_FUNCTION = new RGBToHSBFunction();

    public LABToHSBFunction() {
        super("LAB_TO_HSB", 1);
    }

    @Override
    public ParameterInfo getParameterInfo(int index) {
        return new ParameterInfo("LAB", "Array of size 3 containing the LAB components (0-255 per channel)", Collection.class);
    }

    @Override
    public Object evaluate(List<Object> parameters, JIPipeExpressionVariablesMap variables) {
        return TO_HSB_FUNCTION.evaluate(Collections.singletonList(TO_RGB_FUNCTION.evaluate(parameters, variables)), variables);
    }
}
