package org.hkijena.jipipe.extensions.expressions.functions;

import org.hkijena.jipipe.api.JIPipeDocumentation;
import org.hkijena.jipipe.extensions.expressions.JIPipeExpressionParameter;
import org.hkijena.jipipe.extensions.expressions.ExpressionFunction;
import org.hkijena.jipipe.extensions.expressions.ExpressionVariables;
import org.hkijena.jipipe.extensions.expressions.ParameterInfo;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

@JIPipeDocumentation(name = "Apply built-in function to array", description = "Decomposes an array into parameters of a function and applies the built-in function. Please note that user-defined functions are not supported.")
public class RunFunctionFunction extends ExpressionFunction {
    public RunFunctionFunction() {
        super("APPLY_FUNCTION_TO_ARRAY", 2);
    }

    @Override
    public Object evaluate(List<Object> parameters, ExpressionVariables variables) {
        ExpressionVariables localVariables = new ExpressionVariables();
        Collection<?> items = (Collection<?>) parameters.get(1);
        List<String> variableNames = new ArrayList<>();
        for (Object item : items) {
            String s = "i" + localVariables.size();
            variableNames.add(s);
            localVariables.set(s, item);
        }

        String functionName = "" + parameters.get(0);
        String expression = functionName + "(" + String.join(", ", variableNames) + ") ";
        return JIPipeExpressionParameter.getEvaluatorInstance().evaluate(expression, localVariables);
    }

    @Override
    public ParameterInfo getParameterInfo(int index) {
        if (index == 0) {
            return new ParameterInfo("Function name", "A valid function name", String.class);
        } else {
            return new ParameterInfo("Sequence", "The sequence to be accumulated.", Collection.class);
        }
    }
}
