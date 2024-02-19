package org.hkijena.jipipe.extensions.expressions.functions.control;

import org.hkijena.jipipe.api.SetJIPipeDocumentation;
import org.hkijena.jipipe.extensions.expressions.ExpressionFunction;
import org.hkijena.jipipe.extensions.expressions.JIPipeExpressionVariablesMap;

import java.util.List;
import java.util.Objects;

@SetJIPipeDocumentation(name = "Switch (map value)", description = "Maps the first parameter to another value as defined by all the other parameters. " +
        "Pass CASE(input, output) items into this function to generate the output if the first parameter is equal to the input. The cases are evaluated in order. " +
        "Pass a non-list value as last case to define a default value. If no matching case is found, the original value is returned.")
public class SwitchMapFunction extends ExpressionFunction {

    public SwitchMapFunction() {
        super("SWITCH_MAP", 1, Integer.MAX_VALUE);
    }

    @Override
    public Object evaluate(List<Object> parameters, JIPipeExpressionVariablesMap variables) {
        Object input = parameters.get(0);
        for (int i = 1; i < parameters.size(); i++) {
            Object parameter = parameters.get(i);
            if(parameter instanceof List) {
                List<Object> pair = (List<Object>) parameter;
                if(Objects.equals(pair.get(0), input)) {
                    return pair.get(1);
                }
            }
            else {
                return parameter;
            }
        }
        return input;
    }
}
