package org.hkijena.jipipe.extensions.expressions.functions;

import org.hkijena.jipipe.api.JIPipeDocumentation;
import org.hkijena.jipipe.extensions.expressions.ExpressionFunction;
import org.hkijena.jipipe.extensions.expressions.ExpressionVariables;
import org.hkijena.jipipe.extensions.expressions.ParameterInfo;

import java.util.Arrays;
import java.util.List;

@JIPipeDocumentation(name = "Case", description = "Creates a pair of a a condition and value to be used inside a SWITCH function.")
public class CaseFunction extends ExpressionFunction {

    public CaseFunction() {
        super("CASE", 2);
    }

    @Override
    public ParameterInfo getParameterInfo(int index) {
        if (index == 0) {
            return new ParameterInfo("Condition", "The condition. Must evaluate to a boolean or a number (numbers larger than zero are considered as true, otherwise false)", Boolean.class, Number.class);
        } else {
            return new ParameterInfo("Value", "Value to be returned if the condition is true", Object.class);
        }
    }

    @Override
    public Object evaluate(List<Object> parameters, ExpressionVariables variables) {
        if (parameters.size() == 1) {
            return Arrays.asList(true, parameters.get(0));
        } else {
            return Arrays.asList(parameters.get(0), parameters.get(1));
        }
    }
}
