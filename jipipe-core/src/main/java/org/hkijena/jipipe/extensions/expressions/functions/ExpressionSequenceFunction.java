package org.hkijena.jipipe.extensions.expressions.functions;

import org.hkijena.jipipe.api.JIPipeDocumentation;
import org.hkijena.jipipe.extensions.expressions.DefaultExpressionParameter;
import org.hkijena.jipipe.extensions.expressions.ExpressionFunction;
import org.hkijena.jipipe.extensions.expressions.ExpressionVariables;
import org.hkijena.jipipe.extensions.expressions.ParameterInfo;
import org.hkijena.jipipe.utils.StringUtils;

import java.util.*;

@JIPipeDocumentation(name = "Generate sequence by expression", description = "Applies the expression string for each item in the second parameter. The return values are collected and returned as array.")
public class ExpressionSequenceFunction extends ExpressionFunction {
    public ExpressionSequenceFunction() {
        super("MAKE_SEQUENCE_EXPR", 2, 3);
    }

    @Override
    public ParameterInfo getParameterInfo(int index) {
        if (index == 0) {
            return new ParameterInfo("Expression", "String that contains an expression");
        } else if (index == 1) {
            return new ParameterInfo("Indices", "Array of values that are passed into the expression as variable (default 'item')");
        } else {
            return new ParameterInfo("Custom item variable name", "Allows to customize the variable name where the item will be written.");
        }
    }

    @Override
    public Object evaluate(List<Object> parameters, ExpressionVariables variables) {
        String expression = StringUtils.nullToEmpty(parameters.get(0));
        Collection<?> indices;
        if (parameters.get(1) instanceof Collection) {
            indices = (Collection<?>) parameters.get(1);
        } else {
            indices = Arrays.asList(parameters.get(1));
        }
        String itemVariable = "item";
        if (parameters.size() >= 3) {
            itemVariable = StringUtils.nullToEmpty(parameters.get(2));
        }

        ExpressionVariables localVariables = new ExpressionVariables();
        for (Map.Entry<String, Object> entry : variables.entrySet()) {
            localVariables.put(entry.getKey(), entry.getValue());
        }

        List<Object> result = new ArrayList<>();
        for (Object item : indices) {
            localVariables.set(itemVariable, item);
            result.add(DefaultExpressionParameter.getEvaluatorInstance().evaluate(expression, localVariables));
        }

        return result;
    }
}
