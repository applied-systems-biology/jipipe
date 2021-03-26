package org.hkijena.jipipe.extensions.parameters.expressions.functions;

import org.hkijena.jipipe.api.JIPipeDocumentation;
import org.hkijena.jipipe.extensions.parameters.expressions.DefaultExpressionParameter;
import org.hkijena.jipipe.extensions.parameters.expressions.ExpressionFunction;
import org.hkijena.jipipe.extensions.parameters.expressions.ExpressionParameters;
import org.hkijena.jipipe.extensions.parameters.expressions.ParameterInfo;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

@JIPipeDocumentation(name = "Run function", description = "Decomposes an array into parameters of a function and applies the function.")
public class RunFunctionFunction extends ExpressionFunction {
    public RunFunctionFunction() {
        super("RUN_FUNCTION", 2);
    }

    @Override
    public Object evaluate(List<Object> parameters, ExpressionParameters variables) {
        ExpressionParameters localVariables = new ExpressionParameters();
        Collection<?> items = (Collection<?>) parameters.get(1);
        List<String> variableNames = new ArrayList<>();
        for (Object item : items) {
            String s = "i" + localVariables.size();
            variableNames.add(s);
            localVariables.set(s, item);
        }

        String functionName = "" + parameters.get(0);
        String expression = functionName + "(" + String.join(", ", variableNames) + ") ";
        return DefaultExpressionParameter.getEvaluatorInstance().evaluate(expression, localVariables);
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
