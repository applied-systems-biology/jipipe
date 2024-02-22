/*
 * Copyright by Zoltán Cseresnyés, Ruman Gerst
 *
 * Research Group Applied Systems Biology - Head: Prof. Dr. Marc Thilo Figge
 * https://www.leibniz-hki.de/en/applied-systems-biology.html
 * HKI-Center for Systems Biology of Infection
 * Leibniz Institute for Natural Product Research and Infection Biology - Hans Knöll Institute (HKI)
 * Adolf-Reichwein-Straße 23, 07745 Jena, Germany
 *
 * The project code is licensed under BSD 2-Clause.
 * See the LICENSE file provided with the code for the full license.
 */

package org.hkijena.jipipe.extensions.expressions.functions.variables;

import org.hkijena.jipipe.api.SetJIPipeDocumentation;
import org.hkijena.jipipe.extensions.expressions.ExpressionFunction;
import org.hkijena.jipipe.extensions.expressions.JIPipeExpressionVariablesMap;
import org.hkijena.jipipe.extensions.expressions.ParameterInfo;

import java.util.List;

@SetJIPipeDocumentation(name = "Set variable if", description = "Creates a variable with the first parameter as name and sets its value as the second parameter. " +
        "Will only set the variable if the third parameter is true.. " +
        "Returns if the variable was set.")
public class SetVariableIfFunction extends ExpressionFunction {

    public SetVariableIfFunction() {
        super("SET_VARIABLE_IF", 3);
    }

    @Override
    public ParameterInfo getParameterInfo(int index) {
        if (index == 0) {
            return new ParameterInfo("name", "The variable name", String.class);
        } else if (index == 1) {
            return new ParameterInfo("value", "The variable name", Object.class);
        } else if (index == 2) {
            return new ParameterInfo("condition", "If true, the variable will be written", Boolean.class);
        }
        return null;
    }

    @Override
    public Object evaluate(List<Object> parameters, JIPipeExpressionVariablesMap variables) {
        if ((boolean) parameters.get(2)) {
            variables.set("" + parameters.get(0), parameters.get(1));
            return true;
        } else {
            return false;
        }
    }
}
