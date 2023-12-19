package org.hkijena.jipipe.extensions.expressions.functions.functions;

import com.google.common.collect.ImmutableList;
import org.hkijena.jipipe.api.JIPipeDocumentation;
import org.hkijena.jipipe.extensions.expressions.*;
import org.hkijena.jipipe.extensions.expressions.functions.EvaluateFunction;

import java.util.Collection;
import java.util.List;
import java.util.Map;

@JIPipeDocumentation(name = "Run user function", description = "Evaluates a user function as string. The first parameter must be the function name. Use FUNCTION to define new functions.")
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
    public Object evaluate(List<Object> parameters, ExpressionVariables variables) {
        ExpressionVariables localVariables;

        if (parameters.size() > 1) {
            localVariables = new ExpressionVariables();
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
