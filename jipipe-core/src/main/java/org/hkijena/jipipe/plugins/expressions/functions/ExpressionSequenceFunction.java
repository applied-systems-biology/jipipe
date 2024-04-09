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

package org.hkijena.jipipe.plugins.expressions.functions;

import org.hkijena.jipipe.api.SetJIPipeDocumentation;
import org.hkijena.jipipe.plugins.expressions.*;
import org.hkijena.jipipe.utils.StringUtils;

import java.util.*;

@SetJIPipeDocumentation(name = "Generate sequence by expression", description = "Applies the expression string for each item in the second parameter. The return values are collected and returned as array.")
public class ExpressionSequenceFunction extends ExpressionFunction {
    public ExpressionSequenceFunction() {
        super("MAKE_SEQUENCE_EXPR", 2, 3);
    }

    @Override
    public ParameterInfo getParameterInfo(int index) {
        if (index == 0) {
            return new ParameterInfo("Expression", "String that contains an expression");
        } else if (index == 1) {
            return new ParameterInfo("Indices", "Array of values that are passed into the expression as variable (default 'item')");
        } else {
            return new ParameterInfo("Custom item variable name", "Allows to customize the variable name where the item will be written.");
        }
    }

    @Override
    public Object evaluate(List<Object> parameters, JIPipeExpressionVariablesMap variables) {
        String expression = JIPipeExpressionEvaluator.unescapeString(StringUtils.nullToEmpty(parameters.get(0)));
        Collection<?> indices;
        if (parameters.get(1) instanceof Collection) {
            indices = (Collection<?>) parameters.get(1);
        } else {
            indices = Arrays.asList(parameters.get(1));
        }
        String itemVariable = "item";
        if (parameters.size() >= 3) {
            itemVariable = StringUtils.nullToEmpty(parameters.get(2));
        }

        JIPipeExpressionVariablesMap localVariables = new JIPipeExpressionVariablesMap();
        for (Map.Entry<String, Object> entry : variables.entrySet()) {
            localVariables.put(entry.getKey(), entry.getValue());
        }

        List<Object> result = new ArrayList<>();
        for (Object item : indices) {
            localVariables.set(itemVariable, item);
            result.add(JIPipeExpressionParameter.getEvaluatorInstance().evaluate(expression, localVariables));
        }

        return result;
    }
}
