package org.hkijena.jipipe.extensions.expressions.functions;

import org.hkijena.jipipe.api.JIPipeDocumentation;
import org.hkijena.jipipe.extensions.expressions.ExpressionFunction;
import org.hkijena.jipipe.extensions.expressions.ExpressionParameters;
import org.hkijena.jipipe.extensions.expressions.ParameterInfo;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

@JIPipeDocumentation(name = "Calculate histogram (non-negative integers)", description = "Calculates the histogram of the incoming list of numbers. The calculation will ignore negative integer values." +
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
    public Object evaluate(List<Object> parameters, ExpressionParameters variables) {
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
