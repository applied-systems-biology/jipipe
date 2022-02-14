package org.hkijena.jipipe.extensions.expressions.functions;

import org.hkijena.jipipe.api.JIPipeDocumentation;
import org.hkijena.jipipe.api.exceptions.UserFriendlyRuntimeException;
import org.hkijena.jipipe.extensions.expressions.DefaultExpressionParameter;
import org.hkijena.jipipe.extensions.expressions.ExpressionFunction;
import org.hkijena.jipipe.extensions.expressions.ExpressionVariables;
import org.hkijena.jipipe.extensions.expressions.ParameterInfo;
import org.hkijena.jipipe.utils.StringUtils;

import java.util.List;
import java.util.Map;

@JIPipeDocumentation(name = "Evaluate", description = "Evaluates a string as expression. The first parameter is the expression, while all other " +
        "parameters assign variables.")
public class EvaluateFunction extends ExpressionFunction {
    public EvaluateFunction() {
        super("EVALUATE", 1, Integer.MAX_VALUE);
    }

    public static void parseVariableAssignment(ExpressionVariables source, ExpressionVariables target, String assignment) {
        int separatorIndex = assignment.indexOf('=');
        if (separatorIndex < 0) {
            throw new UserFriendlyRuntimeException("Variable assignment '" + assignment + "' is invalid: Missing '='.",
                    "Invalid variable assignment expression!",
                    "Assignment '" + assignment + "'",
                    "You used an expression function that assigns variables. Variable assignments always have following format: [Variable name]=[Expression]. " +
                            "For example: var1=x+1",
                    "Insert a correct variable assignment");
        }
        String variableName = assignment.substring(0, separatorIndex);
        String expression = assignment.substring(separatorIndex + 1);
        Object value = DefaultExpressionParameter.getEvaluatorInstance().evaluate(expression, source);
        target.put(variableName, value);
    }

    @Override
    public ParameterInfo getParameterInfo(int index) {
        if (index == 0) {
            return new ParameterInfo("Expression", "String that contains the evaluated expression", String.class);
        } else {
            return new ParameterInfo("Variable " + index, "String that has following format: [Variable name]=[Expression]." +
                    " The result of [Expression] is assigned to [Variable name] for the evaluated expression", String.class);
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
                String parameter = (String) parameters.get(i);
                parseVariableAssignment(variables, localVariables, parameter);
            }
        } else {
            localVariables = variables;
        }

        return DefaultExpressionParameter.getEvaluatorInstance().evaluate(StringUtils.nullToEmpty(parameters.get(0)), localVariables);
    }
}
