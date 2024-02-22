package org.hkijena.jipipe.extensions.expressions.functions.vector;

import org.hkijena.jipipe.api.SetJIPipeDocumentation;
import org.hkijena.jipipe.extensions.expressions.ExpressionFunction;
import org.hkijena.jipipe.extensions.expressions.JIPipeExpressionVariablesMap;
import org.hkijena.jipipe.utils.VectorUtils;

import java.util.Collection;
import java.util.List;

@SetJIPipeDocumentation(name = "Add vectors", description = "Adds two vectors")
public class VectorSubtractFunction extends ExpressionFunction {
    public VectorSubtractFunction() {
        super("VEC_SUBTRACT", 2);
    }

    @Override
    public Object evaluate(List<Object> parameters, JIPipeExpressionVariablesMap variables) {
        return VectorUtils.subtract(VectorUtils.objectListToNumericVector((Collection<?>) parameters.get(0)),
                VectorUtils.objectListToNumericVector((Collection<?>) parameters.get(1)));
    }
}
