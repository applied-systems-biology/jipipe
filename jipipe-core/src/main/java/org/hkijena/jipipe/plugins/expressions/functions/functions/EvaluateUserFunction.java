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

package org.hkijena.jipipe.plugins.expressions.functions.functions;

import com.google.common.collect.ImmutableList;
import org.hkijena.jipipe.api.SetJIPipeDocumentation;
import org.hkijena.jipipe.plugins.expressions.*;
import org.hkijena.jipipe.plugins.expressions.functions.EvaluateFunction;

import java.util.Collection;
import java.util.List;
import java.util.Map;

@SetJIPipeDocumentation(name = "Run user function", description = "Evaluates a user function as string. The first parameter must be the function name. Use FUNCTION to define new functions.")
public class EvaluateUserFunction extends ExpressionFunction {
    public EvaluateUserFunction() {
        super("RUN_FUNCTION", 1, Integer.MAX_VALUE);
    }

    @Override
    public ParameterInfo getParameterInfo(int index) {
        if (index == 0) {
            return new ParameterInfo("Function name", "Name of the function", String.class);
        } else {
            return new ParameterInfo("Variable " + index, "Supports either a string or an array.\nArray: should have two items (key and value). Use ARRAY(key, value), PAIR(key, value), or key: value. The key must be a string." +
                    "\nString: Must following format: [Variable name]=[Expression]." +
                    " The result of [Expression] is assigned to [Variable name] for the evaluated expression", String.class, List.class);
        }
    }

    @Override
    public Object evaluate(List<Object> parameters, JIPipeExpressionVariablesMap variables) {
        JIPipeExpressionVariablesMap localVariables;

        if (parameters.size() > 1) {
            localVariables = new JIPipeExpressionVariablesMap();
            for (Map.Entry<String, Object> entry : variables.entrySet()) {
                localVariables.put(entry.getKey(), entry.getValue());
            }

            // Add other variables
            for (int i = 1; i < parameters.size(); i++) {
                if (parameters.get(i) instanceof Collection) {
                    List<?> items = ImmutableList.copyOf((Collection<?>) parameters.get(i));
                    localVariables.set("" + items.get(0), items.get(1));
                } else {
                    String parameter = (String) parameters.get(i);
                    EvaluateFunction.parseVariableAssignment(variables, localVariables, parameter);
                }
            }
        } else {
            localVariables = variables;
        }

        String functionExpression = (String) variables.get("+function." + parameters.get(0));
        functionExpression = JIPipeExpressionEvaluator.unescapeString(functionExpression); // The function is still escaped as string

        return JIPipeExpressionParameter.getEvaluatorInstance().evaluate(functionExpression, localVariables);
    }
}
