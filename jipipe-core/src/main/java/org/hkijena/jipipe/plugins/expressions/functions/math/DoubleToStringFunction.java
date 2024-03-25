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

import ij.IJ;
import org.hkijena.jipipe.api.SetJIPipeDocumentation;
import org.hkijena.jipipe.plugins.expressions.ExpressionFunction;
import org.hkijena.jipipe.plugins.expressions.JIPipeExpressionVariablesMap;
import org.hkijena.jipipe.plugins.expressions.ParameterInfo;

import java.util.List;

@SetJIPipeDocumentation(name = "Number to string", description = "Rounds a number to a specified number of decimals and outputs the value as string")
public class DoubleToStringFunction extends ExpressionFunction {
    public DoubleToStringFunction() {
        super("D2S", 2);
    }

    @Override
    public Object evaluate(List<Object> parameters, JIPipeExpressionVariablesMap variables) {
        double value = ((Number) parameters.get(0)).doubleValue();
        int decimals = ((Number) parameters.get(1)).intValue();
        return IJ.d2s(value, decimals);
    }

    @Override
    public ParameterInfo getParameterInfo(int index) {
        if (index == 0) {
            return new ParameterInfo("Value", "The value to be rounded.", Number.class);
        } else {
            return new ParameterInfo("Decimals", "The number of decimals.", Integer.class);
        }
    }
}
