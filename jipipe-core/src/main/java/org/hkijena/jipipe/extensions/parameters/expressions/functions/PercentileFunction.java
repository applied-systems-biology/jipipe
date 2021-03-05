package org.hkijena.jipipe.extensions.parameters.expressions.functions;

import com.google.common.primitives.Doubles;
import org.apache.commons.math3.stat.descriptive.rank.Percentile;
import org.hkijena.jipipe.api.JIPipeDocumentation;
import org.hkijena.jipipe.extensions.parameters.expressions.ExpressionFunction;
import org.hkijena.jipipe.extensions.parameters.expressions.ExpressionParameters;
import org.hkijena.jipipe.extensions.parameters.expressions.ParameterInfo;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

@JIPipeDocumentation(name = "Percentile", description = "Calculates the Nth percentile (0-100) of the provided numeric array.")
public class PercentileFunction extends ExpressionFunction {

    public PercentileFunction() {
        super("PERCENTILE", 2);
    }

    @Override
    public ParameterInfo getParameterInfo(int index) {
        if(index == 0)
            return new ParameterInfo("Array", "Array of numbers", Collection.class);
        else
            return new ParameterInfo("N", "The percentile to calculate (0-100)", Number.class);
    }

    @Override
    public Object evaluate(List<Object> parameters, ExpressionParameters variables) {
        Collection<Number> collection = (Collection<Number>) parameters.get(0);
        double N = ((Number)parameters.get(1)).doubleValue();

        Percentile percentile = new Percentile(N);
        return percentile.evaluate(Doubles.toArray(collection));
    }
}
