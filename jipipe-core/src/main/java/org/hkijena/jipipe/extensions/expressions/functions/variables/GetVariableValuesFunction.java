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

package org.hkijena.jipipe.extensions.expressions.functions.variables;

import org.hkijena.jipipe.api.SetJIPipeDocumentation;
import org.hkijena.jipipe.extensions.expressions.ExpressionFunction;
import org.hkijena.jipipe.extensions.expressions.JIPipeExpressionVariablesMap;

import java.util.List;
import java.util.stream.Collectors;

@SetJIPipeDocumentation(name = "Get all variable values", description = "Returns all variable values as array. The values are ordered by " +
        "their name.")
public class GetVariableValuesFunction extends ExpressionFunction {

    public GetVariableValuesFunction() {
        super("GET_ALL_VARIABLE_VALUES", 0);
    }

    @Override
    public Object evaluate(List<Object> parameters, JIPipeExpressionVariablesMap variables) {
        return variables.keySet().stream().sorted().map(variables::get).collect(Collectors.toList());
    }
}
