/*
 * Copyright by Zoltán Cseresnyés, Ruman Gerst
 *
 * Research Group Applied Systems Biology - Head: Prof. Dr. Marc Thilo Figge
 * https://www.leibniz-hki.de/en/applied-systems-biology.html
 * HKI-Center for Systems Biology of Infection
 * Leibniz Institute for Natural Product Research and Infection Biology - Hans Knöll Institute (HKI)
 * Adolf-Reichwein-Straße 23, 07745 Jena, Germany
 *
 * The project code is licensed under MIT.
 * See the LICENSE file provided with the code for the full license.
 */

package org.hkijena.jipipe.plugins.expressions.functions.vector;

import org.hkijena.jipipe.api.SetJIPipeDocumentation;
import org.hkijena.jipipe.plugins.expressions.ExpressionFunction;
import org.hkijena.jipipe.plugins.expressions.JIPipeExpressionVariablesMap;
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
