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

@SetJIPipeDocumentation(name = "Set variables", description = "Sets multiple variables. Use the pair operator ':' for convenience.")
public class SetVariablesFunction extends ExpressionFunction {

    public SetVariablesFunction() {
        super("SET_VARIABLES", 0, Integer.MAX_VALUE);
    }

    @Override
    public ParameterInfo getParameterInfo(int index) {
        return new ParameterInfo("x" + index, "An array with two values. Use ARRAY(x,y), PAIR(x,y), or x: y to create one entry", List.class);
    }

    @Override
    public Object evaluate(List<Object> parameters, JIPipeExpressionVariablesMap variables) {
        for (Object parameter : parameters) {
            List<?> items = (List<?>) parameter;
            variables.set("" + items.get(0), items.get(1));
        }
        return true;
    }
}
