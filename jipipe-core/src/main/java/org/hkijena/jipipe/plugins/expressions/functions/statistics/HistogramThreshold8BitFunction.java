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

package org.hkijena.jipipe.plugins.expressions.functions.statistics;

import org.hkijena.jipipe.plugins.expressions.ExpressionFunction;
import org.hkijena.jipipe.plugins.expressions.JIPipeExpressionVariablesMap;
import org.hkijena.jipipe.plugins.expressions.ParameterInfo;
import org.hkijena.jipipe.utils.threshold.AutoThresholdMethod;
import org.hkijena.jipipe.utils.threshold.NBinsAutoThresholder;

import java.util.Collection;
import java.util.List;

/**
 * Base of 8-bit histogram threshold functions. The histogram must have at most 256 bins;
 * the bin index is the pixel value.
 */
public abstract class HistogramThreshold8BitFunction extends ExpressionFunction {

    public HistogramThreshold8BitFunction(String name) {
        super(name, 1);
    }

    protected abstract AutoThresholdMethod getMethod();

    @Override
    public Object evaluate(List<Object> parameters, JIPipeExpressionVariablesMap variables) {
        Collection<Number> numbers = (Collection<Number>) parameters.get(0);
        if (numbers.size() > 256)
            throw new IllegalArgumentException(getName() + ": the histogram has " + numbers.size()
                    + " bins, but at most 256 bins are allowed for this function");
        int[] histogram = new int[256];
        int i = 0;
        for (Number number : numbers) {
            histogram[i] = number.intValue();
            ++i;
            if (i >= 256)
                break;
        }
        return NBinsAutoThresholder.getThreshold8U(getMethod(), histogram);
    }

    @Override
    public ParameterInfo getParameterInfo(int index) {
        return new ParameterInfo("Histogram", "Array of size 256 containing numbers. The array index " +
                "represents the bin (pixel value) and the array value represents the count for this bin.", Collection.class);
    }
}
