package org.hkijena.jipipe.extensions.parameters.expressions.functions;

import org.hkijena.jipipe.api.JIPipeDocumentation;
import org.hkijena.jipipe.api.exceptions.UserFriendlyRuntimeException;
import org.hkijena.jipipe.extensions.parameters.expressions.DefaultExpressionParameter;
import org.hkijena.jipipe.extensions.parameters.expressions.ExpressionFunction;
import org.hkijena.jipipe.extensions.parameters.expressions.ExpressionParameters;
import org.hkijena.jipipe.extensions.parameters.expressions.ParameterInfo;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;

import static org.hkijena.jipipe.extensions.parameters.expressions.functions.EvaluateFunction.parseVariableAssignment;

@JIPipeDocumentation(name = "Foreach loop", description = "Repeats an expression for all items of the second parameter. ")
public class ForEachFunction extends ExpressionFunction {
    public ForEachFunction() {
        super("FOREACH", 2, Integer.MAX_VALUE);
    }

    @Override
    public Object evaluate(List<Object> parameters, ExpressionParameters variables) {
        ExpressionParameters localVariables = new ExpressionParameters();
        for (Map.Entry<String, Object> entry : variables.entrySet()) {
            localVariables.put(entry.getKey(), entry.getValue());
        }

        // Evaluate other parameters
        for (int i = 2; i < parameters.size(); i++) {
            String parameter = (String)parameters.get(i);
            parseVariableAssignment(variables, localVariables, parameter);
        }

        // Evaluate iterated
        Collection<?> sequence;
        String variableName;
        if(parameters.get(1) instanceof String) {
            String assignment = (String) parameters.get(1);
            int separatorIndex = assignment.indexOf('=');
            if(separatorIndex < 0) {
                throw new UserFriendlyRuntimeException("Variable assignment '" + assignment + "' is invalid: Missing '='.",
                        "Invalid variable assignment expression!",
                        "Assignment '" + assignment + "'",
                        "You used an expression function that assigns variables. Variable assignments always have following format: [Variable name]=[Expression]. " +
                                "For example: var1=x+1",
                        "Insert a correct variable assignment");
            }
            variableName = assignment.substring(0, separatorIndex);
            String expression = assignment.substring(separatorIndex + 1);
            sequence = (Collection<?>) DefaultExpressionParameter.getEvaluatorInstance().evaluate(expression, variables);
        }
        else {
            variableName = "item";
            sequence = (Collection<?>) parameters.get(1);
        }

        String loopedExpression = (String) parameters.get(0);
        List<Object> result = new ArrayList<>();
        for (Object item : sequence) {
            localVariables.put(variableName, item);
            result.add(DefaultExpressionParameter.getEvaluatorInstance().evaluate(loopedExpression, localVariables));
        }

        return result;
    }

    @Override
    public ParameterInfo getParameterInfo(int index) {
        if(index == 0) {
            return new ParameterInfo("Expression", "String that contains the evaluated expression", String.class);
        }
        else if(index == 1) {
            return new ParameterInfo("Sequence", "The sequence to be looped. Can be a string or array. " +
                    "If it is an array, the current item is assigned to a variable 'item'." +
                    "If it is a string, it must have following format: [Variable name]=[Expression]." +
                    " The result of [Expression] is looped through. The item is assigned to the variable [Variable name].", Collection.class, String.class);
        }
        else {
            return new ParameterInfo("Variable " + index, "String that has following format: [Variable name]=[Expression]." +
                    " The result of [Expression] is assigned to [Variable name] for the evaluated expression", String.class);
        }
    }
}
