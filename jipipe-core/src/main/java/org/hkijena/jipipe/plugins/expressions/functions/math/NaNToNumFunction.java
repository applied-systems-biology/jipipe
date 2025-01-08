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
import org.hkijena.jipipe.utils.StringUtils;
import org.jetbrains.annotations.NotNull;

import java.util.List;

@SetJIPipeDocumentation(name = "NaN to number", description = "Returns the first parameter if it is not NaN and (convertible to) a valid number. Otherwise returns the second parameter.")
public class NaNToNumFunction extends ExpressionFunction {
    public NaNToNumFunction() {
        super("NAN2D", 2);
    }

    private static @NotNull Object calculateFallback(List<Object> parameters) {
        Object fallback = parameters.get(1);
        if (fallback instanceof Number) {
            return fallback;
        } else {
            return StringUtils.parseDouble(fallback.toString());
        }
    }

    @Override
    public Object evaluate(List<Object> parameters, JIPipeExpressionVariablesMap variables) {
        if (parameters.get(0) instanceof Number) {
            double value = ((Number) parameters.get(0)).doubleValue();
            if (Double.isNaN(value)) {
                return calculateFallback(parameters);
            } else {
                return value;
            }
        } else {
            try {
                double value = StringUtils.parseDouble(parameters.get(0).toString());
                if (Double.isNaN(value)) {
                    return calculateFallback(parameters);
                } else {
                    return value;
                }
            } catch (NumberFormatException e) {
                return calculateFallback(parameters);
            }
        }
    }

    @Override
    public ParameterInfo getParameterInfo(int index) {
        if (index == 0) {
            return new ParameterInfo("Value", "The value to be checked.", Number.class);
        } else if (index == 1) {
            return new ParameterInfo("Replacement", "If the first parameter is NaN", Number.class);
        } else {
            return null;
        }
    }
}
