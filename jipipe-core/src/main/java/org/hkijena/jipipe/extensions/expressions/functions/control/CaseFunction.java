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

package org.hkijena.jipipe.extensions.expressions.functions.control;

import org.hkijena.jipipe.api.SetJIPipeDocumentation;
import org.hkijena.jipipe.extensions.expressions.ExpressionFunction;
import org.hkijena.jipipe.extensions.expressions.JIPipeExpressionVariablesMap;
import org.hkijena.jipipe.extensions.expressions.ParameterInfo;

import java.util.Arrays;
import java.util.List;

@SetJIPipeDocumentation(name = "Case", description = "Creates a pair of a a condition and value to be used inside a SWITCH function.")
public class CaseFunction extends ExpressionFunction {

    public CaseFunction() {
        super("CASE", 2);
    }

    @Override
    public ParameterInfo getParameterInfo(int index) {
        if (index == 0) {
            return new ParameterInfo("Condition", "The condition. Must evaluate to a boolean or a number (numbers larger than zero are considered as true, otherwise false)", Boolean.class, Number.class);
        } else {
            return new ParameterInfo("Value", "Value to be returned if the condition is true", Object.class);
        }
    }

    @Override
    public Object evaluate(List<Object> parameters, JIPipeExpressionVariablesMap variables) {
        if (parameters.size() == 1) {
            return Arrays.asList(true, parameters.get(0));
        } else {
            return Arrays.asList(parameters.get(0), parameters.get(1));
        }
    }
}
