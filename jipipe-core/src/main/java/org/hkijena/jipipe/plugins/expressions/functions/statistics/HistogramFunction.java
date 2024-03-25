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

import org.hkijena.jipipe.api.SetJIPipeDocumentation;
import org.hkijena.jipipe.plugins.expressions.ExpressionFunction;
import org.hkijena.jipipe.plugins.expressions.JIPipeExpressionVariablesMap;
import org.hkijena.jipipe.plugins.expressions.ParameterInfo;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

@SetJIPipeDocumentation(name = "Calculate histogram (non-negative integers)", description = "Calculates the histogram of the incoming list of numbers. The calculation will ignore negative integer values." +
        "The result is an array of counts, where the index represents the binned value.")
public class HistogramFunction extends ExpressionFunction {

    public HistogramFunction() {
        super("UINT_HISTOGRAM", 1);
    }

    @Override
    public ParameterInfo getParameterInfo(int index) {
        return new ParameterInfo("Array", "Array of numbers", Collection.class);
    }

    @Override
    public Object evaluate(List<Object> parameters, JIPipeExpressionVariablesMap variables) {
        Collection<Number> collection = (Collection<Number>) parameters.get(0);
        int max = 0;
        for (Number number : collection) {
            max = Math.max(max, number.intValue());
        }
        List<Integer> histogram = new ArrayList<>();
        for (int i = 0; i < max; i++) {
            histogram.add(0);
        }
        for (Number number : collection) {
            int val = number.intValue();
            if (val >= 0) {
                histogram.set(val, histogram.get(val) + 1);
            }
        }
        return histogram;
    }
}
