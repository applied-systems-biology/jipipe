package org.hkijena.jipipe.extensions.expressions.functions;

import com.google.common.collect.ImmutableList;
import org.hkijena.jipipe.api.SetJIPipeDocumentation;
import org.hkijena.jipipe.api.validation.JIPipeValidationReportEntry;
import org.hkijena.jipipe.api.validation.JIPipeValidationReportEntryLevel;
import org.hkijena.jipipe.api.validation.JIPipeValidationRuntimeException;
import org.hkijena.jipipe.api.validation.contexts.CustomValidationReportContext;
import org.hkijena.jipipe.extensions.expressions.JIPipeExpressionParameter;
import org.hkijena.jipipe.extensions.expressions.ExpressionFunction;
import org.hkijena.jipipe.extensions.expressions.JIPipeExpressionVariablesMap;
import org.hkijena.jipipe.extensions.expressions.ParameterInfo;
import org.hkijena.jipipe.utils.StringUtils;

import java.util.Collection;
import java.util.List;
import java.util.Map;

@SetJIPipeDocumentation(name = "Evaluate", description = "Evaluates a string as expression. The first parameter is the expression, while all other " +
        "parameters assign variables.")
public class EvaluateFunction extends ExpressionFunction {
    public EvaluateFunction() {
        super("EVALUATE", 1, Integer.MAX_VALUE);
    }

    public static void parseVariableAssignment(JIPipeExpressionVariablesMap source, JIPipeExpressionVariablesMap target, String assignment) {
        int separatorIndex = assignment.indexOf('=');
        if (separatorIndex < 0) {
            throw new JIPipeValidationRuntimeException(new JIPipeValidationReportEntry(JIPipeValidationReportEntryLevel.Error, new CustomValidationReportContext("Expression"),
                    "Variable assignment '" + assignment + "' is invalid: Missing '='.",
                    "Invalid variable assignment expression!",
                    "You used an expression function that assigns variables. Variable assignments always have following format: [Variable name]=[Expression]. " +
                            "For example: var1=x+1",
                    "Insert a correct variable assignment"));
        }
        String variableName = assignment.substring(0, separatorIndex);
        String expression = assignment.substring(separatorIndex + 1);
        Object value = JIPipeExpressionParameter.getEvaluatorInstance().evaluate(expression, source);
        target.put(variableName, value);
    }

    @Override
    public ParameterInfo getParameterInfo(int index) {
        if (index == 0) {
            return new ParameterInfo("Expression", "String that contains the evaluated expression", String.class);
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
                    parseVariableAssignment(variables, localVariables, parameter);
                }
            }
        } else {
            localVariables = variables;
        }

        return JIPipeExpressionParameter.getEvaluatorInstance().evaluate(StringUtils.nullToEmpty(parameters.get(0)), localVariables);
    }
}
