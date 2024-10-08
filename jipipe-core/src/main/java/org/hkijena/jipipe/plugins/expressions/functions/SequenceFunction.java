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

package org.hkijena.jipipe.plugins.expressions.functions;

import org.hkijena.jipipe.api.SetJIPipeDocumentation;
import org.hkijena.jipipe.plugins.expressions.ExpressionFunction;
import org.hkijena.jipipe.plugins.expressions.JIPipeExpressionVariablesMap;
import org.hkijena.jipipe.plugins.expressions.ParameterInfo;

import java.util.ArrayList;
import java.util.List;

@SetJIPipeDocumentation(name = "Generate numeric sequence", description = "Generates an array of numbers. If one parameter is provided, the sequence is generated " +
        "from 0 to the provided number (step 1). If two parameters are provided, the sequence is generated [from, to) (step 1). The third parameter defines the step.")
public class SequenceFunction extends ExpressionFunction {
    public SequenceFunction() {
        super("MAKE_SEQUENCE", 1, 3);
    }

    @Override
    public ParameterInfo getParameterInfo(int index) {
        if (index == 0) {
            return new ParameterInfo("First limit", "If only the first limit is provided, the sequence is generated as [0, First limit). " +
                    "Otherwise, the sequence is generated as [First limit, Second limit)");
        } else if (index == 1) {
            return new ParameterInfo("Second limit", "Upper limit of the sequence. Not inclusive.");
        } else {
            return new ParameterInfo("Step", "The distance between two consecutive sequence items.");
        }
    }

    @Override
    public Object evaluate(List<Object> parameters, JIPipeExpressionVariablesMap variables) {
        double start;
        double end;
        double step;
        if (parameters.size() == 1) {
            start = 0;
            end = ((Number) parameters.get(0)).doubleValue();
            step = 1;
        } else if (parameters.size() == 2) {
            start = ((Number) parameters.get(0)).doubleValue();
            end = ((Number) parameters.get(1)).doubleValue();
            step = 1;
        } else {
            start = ((Number) parameters.get(0)).doubleValue();
            end = ((Number) parameters.get(1)).doubleValue();
            step = ((Number) parameters.get(2)).doubleValue();
        }

        if (step == 0) {
            throw new UnsupportedOperationException("Step variable is zero!");
        }

        List<Double> result = new ArrayList<>();
        if (start < end) {
            if (step < 0)
                step = -step;
            double current = start;
            do {
                result.add(current);
                current += step;
            }
            while (current < end);
        } else {
            if (step > 0)
                step = -step;
            double current = start;
            do {
                result.add(current);
                current += step;
            }
            while (current > end);
        }
        return result;
    }
}
