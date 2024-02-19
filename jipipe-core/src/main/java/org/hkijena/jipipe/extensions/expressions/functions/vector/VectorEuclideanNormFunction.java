package org.hkijena.jipipe.extensions.expressions.functions.vector;

import org.hkijena.jipipe.api.SetJIPipeDocumentation;
import org.hkijena.jipipe.extensions.expressions.ExpressionFunction;
import org.hkijena.jipipe.extensions.expressions.JIPipeExpressionVariablesMap;
import org.hkijena.jipipe.utils.VectorUtils;

import java.util.Collection;
import java.util.List;

@SetJIPipeDocumentation(name = "L2 norm", description = "The Euclidean norm (length) of a vector")
public class VectorEuclideanNormFunction extends ExpressionFunction {
    public VectorEuclideanNormFunction() {
        super("VEC_NORM_L2", 1);
    }

    @Override
    public Object evaluate(List<Object> parameters, JIPipeExpressionVariablesMap variables) {
        return VectorUtils.l2Norm(VectorUtils.objectListToNumericVector((Collection<?>) parameters.get(0)));
    }
}
