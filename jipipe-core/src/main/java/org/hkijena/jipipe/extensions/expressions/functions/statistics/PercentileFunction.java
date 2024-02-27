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

package org.hkijena.jipipe.extensions.expressions.functions.statistics;

import com.google.common.primitives.Doubles;
import org.apache.commons.math3.stat.descriptive.rank.Percentile;
import org.hkijena.jipipe.api.SetJIPipeDocumentation;
import org.hkijena.jipipe.extensions.expressions.ExpressionFunction;
import org.hkijena.jipipe.extensions.expressions.JIPipeExpressionVariablesMap;
import org.hkijena.jipipe.extensions.expressions.ParameterInfo;

import java.util.Collection;
import java.util.List;

@SetJIPipeDocumentation(name = "Percentile", description = "Calculates the Nth percentile (0-100) of the provided numeric array.")
public class PercentileFunction extends ExpressionFunction {

    public PercentileFunction() {
        super("PERCENTILE", 2);
    }

    @Override
    public ParameterInfo getParameterInfo(int index) {
        if (index == 0)
            return new ParameterInfo("Array", "Array of numbers", Collection.class);
        else
            return new ParameterInfo("N", "The percentile to calculate (0-100)", Number.class);
    }

    @Override
    public Object evaluate(List<Object> parameters, JIPipeExpressionVariablesMap variables) {
        Collection<Number> collection = (Collection<Number>) parameters.get(0);
        double N = ((Number) parameters.get(1)).doubleValue();

        Percentile percentile = new Percentile(N);
        return percentile.evaluate(Doubles.toArray(collection));
    }
}
