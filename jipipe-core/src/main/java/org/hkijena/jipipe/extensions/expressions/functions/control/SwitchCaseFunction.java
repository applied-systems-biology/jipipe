package org.hkijena.jipipe.extensions.expressions.functions.control;

import org.hkijena.jipipe.api.JIPipeDocumentation;
import org.hkijena.jipipe.extensions.expressions.ExpressionFunction;
import org.hkijena.jipipe.extensions.expressions.JIPipeExpressionVariablesMap;

import java.util.List;

@JIPipeDocumentation(name = "Switch/case", description = "Multiple IF_ELSE instructions flattened into a function. The function of parameters is defined by if they are even or odd in their order (starting from 1). " +
        "Odd parameters must resolve to Boolean values (TRUE/FALSE). The following even parameter defines the value that is returned if the last parameter is TRUE. The function chooses the first value associated to a TRUE. If you have an uneven number of parameters, then " +
        "the last one is the value returned if none of the conditions is TRUE.")
public class SwitchCaseFunction extends ExpressionFunction {

    public SwitchCaseFunction() {
        super("SWITCH_CASE", 0, Integer.MAX_VALUE);
    }

    @Override
    public Object evaluate(List<Object> parameters, JIPipeExpressionVariablesMap variables) {
        Object result = null;
        boolean found = false;
        for (int i = 0; i < (parameters.size() / 2); i++) {
            Object condition = parameters.get(i * 2);
            Object value = parameters.get(i * 2 + 1);
            if (condition instanceof Boolean) {
                if ((boolean) condition) {
                    result = value;
                    found = true;
                    break;
                }
            } else if (condition instanceof Number) {
                if (((Number) condition).doubleValue() > 0) {
                    found = true;
                    result = value;
                    break;
                }
            }
        }
        if (!found && parameters.size() % 2 != 0) {
            result = parameters.get(parameters.size() - 1);
        }
        return result;
    }
}
