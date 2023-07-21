package org.hkijena.jipipe.extensions.expressions.functions.control;

import org.hkijena.jipipe.api.JIPipeDocumentation;
import org.hkijena.jipipe.api.validation.JIPipeValidationReportEntry;
import org.hkijena.jipipe.api.validation.JIPipeValidationReportEntryLevel;
import org.hkijena.jipipe.api.validation.JIPipeValidationRuntimeException;
import org.hkijena.jipipe.api.validation.contexts.CustomValidationReportContext;
import org.hkijena.jipipe.extensions.expressions.DefaultExpressionParameter;
import org.hkijena.jipipe.extensions.expressions.ExpressionFunction;
import org.hkijena.jipipe.extensions.expressions.ExpressionVariables;
import org.hkijena.jipipe.extensions.expressions.ParameterInfo;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;

import static org.hkijena.jipipe.extensions.expressions.functions.EvaluateFunction.parseVariableAssignment;

@JIPipeDocumentation(name = "Indices where", description = "Finds all indices of the array where the condition applies. Returns an empty array if none match.")
public class WhereFunction extends ExpressionFunction {
    public WhereFunction() {
        super("WHERE", 2, Integer.MAX_VALUE);
    }

    @Override
    public Object evaluate(List<Object> parameters, ExpressionVariables variables) {
        ExpressionVariables localVariables = new ExpressionVariables();
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
                throw new JIPipeValidationRuntimeException(new JIPipeValidationReportEntry(JIPipeValidationReportEntryLevel.Error, new CustomValidationReportContext("Expression"),
                        "Variable assignment '" + assignment + "' is invalid: Missing '='.",
                        "Invalid variable assignment expression!",
                        "You used an expression function that assigns variables. Variable assignments always have following format: [Variable name]=[Expression]. " +
                                "For example: var1=x+1",
                        "Insert a correct variable assignment"));
            }
            variableName = assignment.substring(0, separatorIndex);
            String expression = assignment.substring(separatorIndex + 1);
            sequence = (List<?>) DefaultExpressionParameter.getEvaluatorInstance().evaluate(expression, variables);
        } else {
            variableName = "item";
            sequence = (List<?>) parameters.get(1);
        }

        String loopedExpression = (String) parameters.get(0);
        List<Integer> result = new ArrayList<>();
        for (int i = 0; i < sequence.size(); ++i) {
            localVariables.put(variableName, sequence.get(i));
            localVariables.put("index", i);
            boolean test = DefaultExpressionParameter.getEvaluatorInstance().test(loopedExpression, localVariables);
            if (test) {
                result.add(i);
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
