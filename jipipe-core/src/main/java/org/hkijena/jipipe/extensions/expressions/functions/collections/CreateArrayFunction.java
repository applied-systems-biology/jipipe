package org.hkijena.jipipe.extensions.expressions.functions.collections;

import org.hkijena.jipipe.api.JIPipeDocumentation;
import org.hkijena.jipipe.extensions.expressions.ExpressionFunction;
import org.hkijena.jipipe.extensions.expressions.JIPipeExpressionVariablesMap;

import java.util.List;

@JIPipeDocumentation(name = "Create array", description = "Creates a new array that contains the parameters.")
public class CreateArrayFunction extends ExpressionFunction {

    public CreateArrayFunction() {
        super("ARRAY", 0, Integer.MAX_VALUE);
    }

    @Override
    public Object evaluate(List<Object> parameters, JIPipeExpressionVariablesMap variables) {
        return parameters;
    }
}
