package org.hkijena.jipipe.extensions.expressions.functions;

import org.hkijena.jipipe.api.JIPipeDocumentation;
import org.hkijena.jipipe.extensions.expressions.DefaultExpressionParameter;
import org.hkijena.jipipe.extensions.expressions.ExpressionFunction;
import org.hkijena.jipipe.extensions.expressions.ExpressionParameters;
import org.hkijena.jipipe.extensions.expressions.ParameterInfo;
import org.hkijena.jipipe.utils.StringUtils;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

@JIPipeDocumentation(name = "Accumulate", description = "Merges multiple array items via an accumulation function (Default: +)")
public class AccumulateFunction extends ExpressionFunction {
    public AccumulateFunction() {
        super("ACCUMULATE", 1, 2);
    }

    @Override
    public Object evaluate(List<Object> parameters, ExpressionParameters variables) {
        ExpressionParameters localVariables = new ExpressionParameters();
        Collection<?> items = (Collection<?>) parameters.get(0);
        List<String> variableNames = new ArrayList<>();
        for (Object item : items) {
            String s = "i" + localVariables.size();
            variableNames.add(s);
            localVariables.set(s, item);
        }

        String operator = parameters.size() > 1 ? StringUtils.nullToEmpty(parameters.get(1)) : "+";
        String expression = String.join(operator, variableNames);

        return DefaultExpressionParameter.getEvaluatorInstance().evaluate(expression, localVariables);
    }

    @Override
    public ParameterInfo getParameterInfo(int index) {
        if (index == 0) {
            return new ParameterInfo("Sequence", "The array to be accumulated.", Collection.class);
        } else {
            return new ParameterInfo("Operator", "A valid operator string (e.g., +, -, /, ...)", String.class);
        }
    }
}
