package org.hkijena.jipipe.extensions.expressions.functions.vector;

import org.hkijena.jipipe.api.JIPipeDocumentation;
import org.hkijena.jipipe.extensions.expressions.ExpressionFunction;
import org.hkijena.jipipe.extensions.expressions.ExpressionVariables;
import org.hkijena.jipipe.utils.VectorUtils;

import java.util.Collection;
import java.util.List;

@JIPipeDocumentation(name = "Normalize vector", description = "Normalizes a vector")
public class VectorNormalizeFunction extends ExpressionFunction {
    public VectorNormalizeFunction() {
        super("VEC_NORMALIZE", 1);
    }

    @Override
    public Object evaluate(List<Object> parameters, ExpressionVariables variables) {
        return VectorUtils.normalize(VectorUtils.objectListToNumericVector((Collection<?>) parameters.get(0)));
    }
}