package org.hkijena.jipipe.extensions.expressions.functions;

import org.hkijena.jipipe.api.JIPipeDocumentation;
import org.hkijena.jipipe.extensions.expressions.ExpressionFunction;
import org.hkijena.jipipe.extensions.expressions.JIPipeExpressionVariablesMap;

import java.util.List;

@JIPipeDocumentation(name = "Is NULL", description = "Returns true if the parameter is NULL")
public class IsNullFunction extends ExpressionFunction {

    public IsNullFunction() {
        super("IS_NULL", 1);
    }

    @Override
    public Object evaluate(List<Object> parameters, JIPipeExpressionVariablesMap variables) {
        return parameters.get(0) == null;
    }
}
