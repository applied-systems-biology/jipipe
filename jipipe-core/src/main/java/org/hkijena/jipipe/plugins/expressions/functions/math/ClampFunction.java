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

package org.hkijena.jipipe.plugins.expressions.functions.math;

import org.hkijena.jipipe.api.SetJIPipeDocumentation;
import org.hkijena.jipipe.plugins.expressions.ExpressionFunction;
import org.hkijena.jipipe.plugins.expressions.JIPipeExpressionVariablesMap;
import org.hkijena.jipipe.plugins.expressions.ParameterInfo;

import java.util.List;

@SetJIPipeDocumentation(name = "Clamp", description = "Combined Min and Max operation.")
public class ClampFunction extends ExpressionFunction {
    public ClampFunction() {
        super("CLAMP", 3);
    }

    @Override
    public Object evaluate(List<Object> parameters, JIPipeExpressionVariablesMap variables) {
        double value = ((Number) parameters.get(0)).doubleValue();
        double min = ((Number) parameters.get(1)).doubleValue();
        double max = ((Number) parameters.get(2)).doubleValue();
        return Math.max(min, Math.min(max, value));
    }

    @Override
    public ParameterInfo getParameterInfo(int index) {
        if (index == 0) {
            return new ParameterInfo("Value", "The value to be clamped.", Number.class);
        } else if (index == 1) {
            return new ParameterInfo("Min", "The minimum value.", Number.class);
        } else {
            return new ParameterInfo("Max", "The maximum value.", Number.class);
        }
    }
}
