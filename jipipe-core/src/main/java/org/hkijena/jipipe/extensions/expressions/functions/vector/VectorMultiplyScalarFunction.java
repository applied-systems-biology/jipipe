package org.hkijena.jipipe.extensions.expressions.functions.vector;

import org.hkijena.jipipe.api.JIPipeDocumentation;
import org.hkijena.jipipe.extensions.expressions.ExpressionFunction;
import org.hkijena.jipipe.extensions.expressions.JIPipeExpressionVariablesMap;
import org.hkijena.jipipe.utils.StringUtils;
import org.hkijena.jipipe.utils.VectorUtils;

import java.util.Collection;
import java.util.List;

@JIPipeDocumentation(name = "Vector scalar multiplication", description = "Multiplies a vector with a scalar")
public class VectorMultiplyScalarFunction extends ExpressionFunction {
    public VectorMultiplyScalarFunction() {
        super("VEC_MUL_SCALAR", 2);
    }

    @Override
    public Object evaluate(List<Object> parameters, JIPipeExpressionVariablesMap variables) {
        return VectorUtils.multiplyScalar(VectorUtils.objectListToNumericVector((Collection<?>) parameters.get(0)), StringUtils.objectToDouble(parameters.get(1)));
    }
}
