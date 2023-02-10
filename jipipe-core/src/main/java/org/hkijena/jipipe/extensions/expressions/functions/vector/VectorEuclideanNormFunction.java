package org.hkijena.jipipe.extensions.expressions.functions.vector;

import org.hkijena.jipipe.api.JIPipeDocumentation;
import org.hkijena.jipipe.extensions.expressions.ExpressionFunction;
import org.hkijena.jipipe.extensions.expressions.ExpressionVariables;
import org.hkijena.jipipe.utils.VectorUtils;

import java.util.Collection;
import java.util.List;

@JIPipeDocumentation(name = "L2 norm", description = "The Euclidean norm (length) of a vector")
public class VectorEuclideanNormFunction extends ExpressionFunction {
    public VectorEuclideanNormFunction() {
        super("VEC_NORM_L2", 1);
    }

    @Override
    public Object evaluate(List<Object> parameters, ExpressionVariables variables) {
        return VectorUtils.l2Norm(VectorUtils.objectListToNumericVector((Collection<?>) parameters.get(0)));
    }
}
