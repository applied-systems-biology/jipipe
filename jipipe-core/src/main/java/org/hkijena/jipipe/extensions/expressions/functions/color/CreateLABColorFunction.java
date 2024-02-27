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

@SetJIPipeDocumentation(name = "LAB Color", description = "Creates an array that represents a LAB color (0-255 per channel)")
public class CreateLABColorFunction extends ExpressionFunction {
    public CreateLABColorFunction() {
        super("LAB_COLOR", 3);
    }

    @Override
    public ParameterInfo getParameterInfo(int index) {
        if (index == 0) {
            return new ParameterInfo("L*", "The L* component", Number.class);
        } else if (index == 1) {
            return new ParameterInfo("a*", "The a* component", Number.class);
        } else if (index == 2) {
            return new ParameterInfo("b*", "The b* component", Number.class);
        } else {
            return null;
        }
    }

    @Override
    public Object evaluate(List<Object> parameters, JIPipeExpressionVariablesMap variables) {
        int h = ((Number) parameters.get(0)).intValue();
        int s = ((Number) parameters.get(1)).intValue();
        int b = ((Number) parameters.get(2)).intValue();
        return Arrays.asList(h, s, b);
    }
}
