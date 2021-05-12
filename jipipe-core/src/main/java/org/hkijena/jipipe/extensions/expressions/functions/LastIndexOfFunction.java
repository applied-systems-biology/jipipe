package org.hkijena.jipipe.extensions.expressions.functions;

import org.hkijena.jipipe.api.JIPipeDocumentation;
import org.hkijena.jipipe.api.exceptions.UserFriendlyRuntimeException;
import org.hkijena.jipipe.extensions.expressions.DefaultExpressionParameter;
import org.hkijena.jipipe.extensions.expressions.ExpressionFunction;
import org.hkijena.jipipe.extensions.expressions.ExpressionParameters;
import org.hkijena.jipipe.extensions.expressions.ParameterInfo;

import java.util.Collection;
import java.util.List;
import java.util.Map;

import static org.hkijena.jipipe.extensions.expressions.functions.EvaluateFunction.parseVariableAssignment;

@JIPipeDocumentation(name = "Last index of", description = "Finds the last index of the array where the condition applies. Returns -1 if none match.")
public class LastIndexOfFunction extends ExpressionFunction {
    public LastIndexOfFunction() {
        super("LAST_INDEX_WHERE", 2, Integer.MAX_VALUE);
    }

    @Override
    public Object evaluate(List<Object> parameters, ExpressionParameters variables) {
        ExpressionParameters localVariables = new ExpressionParameters();
        for (Map.Entry<String, Object> entry : variables.entrySet()) {
            localVariables.put(entry.getKey(), entry.getValue());
        }

        // Evaluate other parameters
        for (int i = 2; i < parameters.size(); i++) {
            String parameter = (String) parameters.get(i);
            parseVariableAssignment(variables, localVariables, parameter);
        }

        // Evaluate iterated
        List<?> sequence;
        String variableName;
        if (parameters.get(1) instanceof String) {
            String assignment = (String) parameters.get(1);
            int separatorIndex = assignment.indexOf('=');
            if (separatorIndex < 0) {
                throw new UserFriendlyRuntimeException("Variable assignment '" + assignment + "' is invalid: Missing '='.",
                        "Invalid variable assignment expression!",
                        "Assignment '" + assignment + "'",
                        "You used an expression function that assigns variables. Variable assignments always have following format: [Variable name]=[Expression]. " +
                                "For example: var1=x+1",
                        "Insert a correct variable assignment");
            }
            variableName = assignment.substring(0, separatorIndex);
            String expression = assignment.substring(separatorIndex + 1);
            sequence = (List<?>) DefaultExpressionParameter.getEvaluatorInstance().evaluate(expression, variables);
        } else {
            variableName = "item";
            sequence = (List<?>) parameters.get(1);
        }

        String loopedExpression = (String) parameters.get(0);
        int result = -1;
        for (int i = 0; i < sequence.size(); ++i) {
            localVariables.put(variableName, sequence.get(i));
            localVariables.put("index", i);
            boolean test = DefaultExpressionParameter.getEvaluatorInstance().test(loopedExpression, localVariables);
            if (test) {
                result = i;
            }
        }
        return result;
    }

    @Override
    public ParameterInfo getParameterInfo(int index) {
        if (index == 0) {
            return new ParameterInfo("Condition", "String that contains an expression for the condition", String.class);
        } else if (index == 1) {
            return new ParameterInfo("Sequence", "The sequence to be looped. Can be a string or array. " +
                    "If it is an array, the current item is assigned to a variable 'item'." +
                    "If it is a string, it must have following format: [Variable name]=[Expression]." +
                    " The result of [Condition] is looped through. The item is assigned to the variable [Variable name].", Collection.class, String.class);
        } else {
            return new ParameterInfo("Variable " + index, "String that has following format: [Variable name]=[Expression]." +
                    " The result of [Condition] is assigned to [Variable name] for the evaluated expression", String.class);
        }
    }
}
