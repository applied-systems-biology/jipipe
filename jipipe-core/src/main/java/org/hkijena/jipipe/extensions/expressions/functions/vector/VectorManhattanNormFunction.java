package org.hkijena.jipipe.extensions.expressions.functions.vector;

import org.hkijena.jipipe.api.JIPipeDocumentation;
import org.hkijena.jipipe.extensions.expressions.ExpressionFunction;
import org.hkijena.jipipe.extensions.expressions.JIPipeExpressionVariablesMap;
import org.hkijena.jipipe.utils.VectorUtils;

import java.util.Collection;
import java.util.List;

@JIPipeDocumentation(name = "L1 norm", description = "The Manhattan norm of a vector")
public class VectorManhattanNormFunction extends ExpressionFunction {
    public VectorManhattanNormFunction() {
        super("VEC_NORM_L1", 1);
    }

    @Override
    public Object evaluate(List<Object> parameters, JIPipeExpressionVariablesMap variables) {
        return VectorUtils.l1Norm(VectorUtils.objectListToNumericVector((Collection<?>) parameters.get(0)));
    }
}
