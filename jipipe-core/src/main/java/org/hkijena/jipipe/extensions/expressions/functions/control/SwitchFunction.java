package org.hkijena.jipipe.extensions.expressions.functions.control;

import org.hkijena.jipipe.api.SetJIPipeDocumentation;
import org.hkijena.jipipe.extensions.expressions.ExpressionFunction;
import org.hkijena.jipipe.extensions.expressions.JIPipeExpressionVariablesMap;

import java.util.List;

@SetJIPipeDocumentation(name = "Switch", description = "Multiple IF_ELSE instructions flattened into a function. Pass CASE(condition, value) items into this function. The cases are evaluated in order. " +
        "The value of the first case where the condition is true is returned.")
public class SwitchFunction extends ExpressionFunction {

    public SwitchFunction() {
        super("SWITCH", 0, Integer.MAX_VALUE);
    }

    @Override
    public Object evaluate(List<Object> parameters, JIPipeExpressionVariablesMap variables) {
        for (Object parameter : parameters) {
            List<Object> pair = (List<Object>) parameter;
            if (pair.get(0) instanceof Boolean) {
                if ((Boolean) pair.get(0)) {
                    return pair.get(1);
                }
            } else if (pair.get(0) instanceof Number) {
                if (((Number) pair.get(0)).doubleValue() > 0) {
                    return pair.get(1);
                }
            }
        }
        return null;
    }
}
