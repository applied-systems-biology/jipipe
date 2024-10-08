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

@SetJIPipeDocumentation(name = "Calculate percentage", description = "Calculates the percentage of a value between a minimum and a maximum. The output is always between zero and one.")
public class PercentageFunction extends ExpressionFunction {
    public PercentageFunction() {
        super("PERC", 3, 4);
    }

    @Override
    public Object evaluate(List<Object> parameters, JIPipeExpressionVariablesMap variables) {
        double value = ((Number) parameters.get(0)).doubleValue();
        double min = ((Number) parameters.get(1)).doubleValue();
        double max = ((Number) parameters.get(2)).doubleValue();
        boolean wrap = parameters.size() >= 4 && (boolean) parameters.get(3);
        if (wrap) {
            double x = (value - min) / (max - min);
            while (x < 0)
                x += 1.0;
            while (x > 1)
                x -= 1.0;
            return x;
        } else {
            return Math.max(0, Math.min(1, (value - min) / (max - min)));
        }
    }

    @Override
    public ParameterInfo getParameterInfo(int index) {
        if (index == 0) {
            return new ParameterInfo("Value", "The value", Number.class);
        } else if (index == 1) {
            return new ParameterInfo("Minimum", "The minimum", Number.class);
        } else if (index == 2) {
            return new ParameterInfo("Maximum", "The maximum", Number.class);
        } else {
            return new ParameterInfo("Wrap", "If true, wrap values outside the specified range (default FALSE)", Boolean.class);
        }
    }
}
