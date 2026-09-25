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

public abstract class HistogramThresholdFunction extends ExpressionFunction {

    /**
     * Default number of bins for backwards compatibility.
     */
    public static final int DEFAULT_NBINS = 256;

    public HistogramThresholdFunction(String name) {
        super(name, 1, 2);
    }

    @Override
    public Object evaluate(List<Object> parameters, JIPipeExpressionVariablesMap variables) {
        Collection<Number> numbers = (Collection<Number>) parameters.get(0);
        int nbins = DEFAULT_NBINS;
        if (parameters.size() > 1) {
            Object nbinsParam = parameters.get(1);
            if (nbinsParam instanceof Number) {
                nbins = ((Number) nbinsParam).intValue();
            } else {
                throw new IllegalArgumentException("The nbins parameter of " + getName()
                        + " must be a number, got: " + nbinsParam);
            }
        }
        if (nbins < 2)
            throw new IllegalArgumentException("The nbins parameter of " + getName()
                    + " must be at least 2, got: " + nbins);
        int[] histogram = new int[nbins];
        int i = 0;
        for (Number number : numbers) {
            histogram[i] = number.intValue();
            ++i;
            if (i >= nbins)
                break;
        }
        return NBinsAutoThresholder.getThreshold(getMethod(), histogram);
    }

    /**
     * @return the auto-threshold method to apply
     */
    protected abstract AutoThresholdMethod getMethod();

    @Override
    public ParameterInfo getParameterInfo(int index) {
        switch (index) {
            case 0:
                return new ParameterInfo("Histogram", "Array containing numbers. The array index represents the " +
                        "bin and the array value represents the count for this bin. " +
                        "The histogram is truncated to the number of bins.", Collection.class);
            case 1:
                return new ParameterInfo("nbins", "Optional. Number of bins (default 256). If provided, the " +
                        "histogram is truncated/padded to this length.", Integer.class);
            default:
                return null;
        }
    }
}
