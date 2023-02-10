package org.hkijena.jipipe.extensions.expressions.functions.vector;

import org.hkijena.jipipe.api.JIPipeDocumentation;
import org.hkijena.jipipe.extensions.expressions.ExpressionFunction;
import org.hkijena.jipipe.extensions.expressions.ExpressionVariables;
import org.hkijena.jipipe.utils.VectorUtils;

import java.util.Collection;
import java.util.List;

@JIPipeDocumentation(name = "Add vectors", description = "Adds two vectors")
public class VectorAddFunction extends ExpressionFunction {
    public VectorAddFunction() {
        super("VEC_ADD", 2);
    }

    @Override
    public Object evaluate(List<Object> parameters, ExpressionVariables variables) {
        return VectorUtils.add(VectorUtils.objectListToNumericVector((Collection<?>) parameters.get(0)),
                VectorUtils.objectListToNumericVector((Collection<?>) parameters.get(1)));
    }
}
