package org.hkijena.jipipe.extensions.expressions.functions.vector;

import org.hkijena.jipipe.api.JIPipeDocumentation;
import org.hkijena.jipipe.extensions.expressions.ExpressionFunction;
import org.hkijena.jipipe.extensions.expressions.JIPipeExpressionVariablesMap;
import org.hkijena.jipipe.utils.VectorUtils;

import java.util.Collection;
import java.util.List;

@JIPipeDocumentation(name = "L infinity norm", description = "The maximum of a vector")
public class VectorMaximumNormFunction extends ExpressionFunction {
    public VectorMaximumNormFunction() {
        super("VEC_NORM_L_INF", 1);
    }

    @Override
    public Object evaluate(List<Object> parameters, JIPipeExpressionVariablesMap variables) {
        return VectorUtils.lInfinityNorm(VectorUtils.objectListToNumericVector((Collection<?>) parameters.get(0)));
    }
}
