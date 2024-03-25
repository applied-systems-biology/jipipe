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

package org.hkijena.jipipe.plugins.expressions.functions.variables;

import org.hkijena.jipipe.api.SetJIPipeDocumentation;
import org.hkijena.jipipe.plugins.expressions.ExpressionFunction;
import org.hkijena.jipipe.plugins.expressions.JIPipeExpressionVariablesMap;
import org.hkijena.jipipe.plugins.expressions.ParameterInfo;

import java.util.List;

@SetJIPipeDocumentation(name = "Get variable", description = "Returns the variable for given string. Useful if you have variables with spaces or that are equal to operators. " +
        "This function can also return a default value if a variable is not set.")
public class GetVariableFunction extends ExpressionFunction {

    public GetVariableFunction() {
        super("GET_VARIABLE", 1, 2);
    }

    @Override
    public ParameterInfo getParameterInfo(int index) {
        if (index == 0) {
            return new ParameterInfo("name", "The variable name", String.class);
        } else if (index == 1) {
            return new ParameterInfo("default_value", "Value to be used if the variable is not set", Object.class);
        }
        return null;
    }

    @Override
    public Object evaluate(List<Object> parameters, JIPipeExpressionVariablesMap variables) {
        Object defaultValue = null;
        if (parameters.size() > 1)
            defaultValue = parameters.get(1);
        return variables.getOrDefault("" + parameters.get(0), defaultValue);
    }
}
