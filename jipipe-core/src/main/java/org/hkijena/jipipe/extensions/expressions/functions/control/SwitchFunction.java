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

import java.util.List;

@SetJIPipeDocumentation(name = "Switch", description = "Multiple IF_ELSE instructions flattened into a function. Pass CASE(condition, value) items into this function. The cases are evaluated in order. " +
        "The value of the first case where the condition is true is returned.")
public class SwitchFunction extends ExpressionFunction {

    public SwitchFunction() {
        super("SWITCH", 0, Integer.MAX_VALUE);
    }

    @Override
    public Object evaluate(List<Object> parameters, JIPipeExpressionVariablesMap variables) {
        for (Object parameter : parameters) {
            List<Object> pair = (List<Object>) parameter;
            if (pair.get(0) instanceof Boolean) {
                if ((Boolean) pair.get(0)) {
                    return pair.get(1);
                }
            } else if (pair.get(0) instanceof Number) {
                if (((Number) pair.get(0)).doubleValue() > 0) {
                    return pair.get(1);
                }
            }
        }
        return null;
    }
}
