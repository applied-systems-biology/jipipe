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

import ij.process.AutoThresholder;
import org.hkijena.jipipe.extensions.expressions.ExpressionFunction;
import org.hkijena.jipipe.extensions.expressions.JIPipeExpressionVariablesMap;
import org.hkijena.jipipe.extensions.expressions.ParameterInfo;

import java.util.Collection;
import java.util.List;

public abstract class HistogramThresholdFunction extends ExpressionFunction {

    public static final AutoThresholder AUTO_THRESHOLDER = new AutoThresholder();

    public HistogramThresholdFunction(String name) {
        super(name, 1);
    }

    @Override
    public Object evaluate(List<Object> parameters, JIPipeExpressionVariablesMap variables) {
        Collection<Number> numbers = (Collection<Number>) parameters.get(0);
        int[] histogram = new int[256];
        int i = 0;
        for (Number number : numbers) {
            histogram[i] = number.intValue();
            ++i;
            if (i >= 256)
                break;
        }
        return calculateThreshold(histogram);
    }

    protected abstract int calculateThreshold(int[] histogram);

    @Override
    public ParameterInfo getParameterInfo(int index) {
        return new ParameterInfo("Histogram", "Array of size 256 containing numbers. The array index represents the " +
                "bin and the array value represents the count for this bin. Larger arrays are down-sized to 256.", Collection.class);
    }
}
