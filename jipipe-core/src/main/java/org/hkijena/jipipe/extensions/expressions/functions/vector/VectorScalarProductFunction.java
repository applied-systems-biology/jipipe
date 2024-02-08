package org.hkijena.jipipe.extensions.expressions.functions.vector;

import org.hkijena.jipipe.api.JIPipeDocumentation;
import org.hkijena.jipipe.extensions.expressions.ExpressionFunction;
import org.hkijena.jipipe.extensions.expressions.JIPipeExpressionVariablesMap;
import org.hkijena.jipipe.utils.VectorUtils;

import java.util.Collection;
import java.util.List;

@JIPipeDocumentation(name = "Vector scalar product", description = "Calculates the scalar product of two vectors")
public class VectorScalarProductFunction extends ExpressionFunction {
    public VectorScalarProductFunction() {
        super("VEC_SCALAR_PRODUCT", 2);
    }

    @Override
    public Object evaluate(List<Object> parameters, JIPipeExpressionVariablesMap variables) {
        return VectorUtils.scalarProduct(VectorUtils.objectListToNumericVector((Collection<?>) parameters.get(0)),
                VectorUtils.objectListToNumericVector((Collection<?>) parameters.get(1)));
    }
}
