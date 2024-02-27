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

import com.google.common.collect.ImmutableList;
import org.hkijena.jipipe.api.SetJIPipeDocumentation;
import org.hkijena.jipipe.extensions.expressions.*;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

@SetJIPipeDocumentation(name = "Transform array (cumulative)", description = "Applies an expression for all cumulative sets of an array and stores the output into the index of the highest index. " +
        "For example, <code>TRANSFORM_ARRAY_CUMULATIVE(ARRAY(1,2,3))</code> will yield an array <code>[1,3,6]</code>")
public class CumulativeTransformArrayFunction extends ExpressionFunction {
    public CumulativeTransformArrayFunction() {
        super("TRANSFORM_ARRAY_CUMULATIVE", 1, 2);
    }

    @Override
    public Object evaluate(List<Object> parameters, JIPipeExpressionVariablesMap variables) {
        JIPipeExpressionVariablesMap localVariables = new JIPipeExpressionVariablesMap(variables);
        List<?> items = ImmutableList.copyOf((Collection<?>) parameters.get(0));
        String accumulator = "SUM(values)";
        if (parameters.size() > 1) {
            accumulator = JIPipeExpressionEvaluator.unescapeString("\"" + parameters.get(1) + "\"");
        }
        List<Object> results = new ArrayList<>();
        List<Object> tmp = new ArrayList<>();
        for (int i = 0; i < items.size(); i++) {
            tmp.clear();
            for (int j = 0; j <= i; j++) {
                tmp.add(items.get(j));
            }
            localVariables.set("values", tmp);
            results.add(JIPipeExpressionParameter.getEvaluatorInstance().evaluate(accumulator, localVariables));
        }
        return results;
    }

    @Override
    public ParameterInfo getParameterInfo(int index) {
        if (index == 0) {
            return new ParameterInfo("Sequence", "The array to be accumulated.", Collection.class);
        } else {
            return new ParameterInfo("Operator", "An expression that accumulates an array <code>values</code>. Tip: use ${ [your expression] } to provide the expression. Defaults to SUM(values)", String.class);
        }
    }
}
