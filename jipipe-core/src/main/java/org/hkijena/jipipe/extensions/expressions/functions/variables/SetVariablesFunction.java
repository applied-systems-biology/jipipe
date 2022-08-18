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

import org.hkijena.jipipe.api.JIPipeDocumentation;
import org.hkijena.jipipe.extensions.expressions.ExpressionFunction;
import org.hkijena.jipipe.extensions.expressions.ExpressionVariables;
import org.hkijena.jipipe.extensions.expressions.ParameterInfo;
import org.hkijena.jipipe.utils.StringUtils;

import java.util.List;

@JIPipeDocumentation(name = "Set variables", description = "Sets multiple variables. Use the pair operator ':' for convenience.")
public class SetVariablesFunction extends ExpressionFunction {

    public SetVariablesFunction() {
        super("SET_VARIABLES", 0, Integer.MAX_VALUE);
    }

    @Override
    public ParameterInfo getParameterInfo(int index) {
        return new ParameterInfo("x" + index, "An array with two values. Use ARRAY(x,y), PAIR(x,y), or x: y to create one entry", List.class);
    }

    @Override
    public Object evaluate(List<Object> parameters, ExpressionVariables variables) {
        for (Object parameter : parameters) {
            List<?> items = (List<?>) parameter;
            variables.set("" + items.get(0), items.get(1));
        }
        return true;
    }
}
