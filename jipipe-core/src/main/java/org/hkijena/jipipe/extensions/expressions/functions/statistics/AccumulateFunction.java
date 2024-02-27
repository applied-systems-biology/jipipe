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

import org.hkijena.jipipe.api.SetJIPipeDocumentation;
import org.hkijena.jipipe.extensions.expressions.JIPipeExpressionParameter;
import org.hkijena.jipipe.extensions.expressions.ExpressionFunction;
import org.hkijena.jipipe.extensions.expressions.JIPipeExpressionVariablesMap;
import org.hkijena.jipipe.extensions.expressions.ParameterInfo;
import org.hkijena.jipipe.utils.StringUtils;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

@SetJIPipeDocumentation(name = "Accumulate", description = "Merges multiple array items via an accumulation function (Default: +)")
public class AccumulateFunction extends ExpressionFunction {
    public AccumulateFunction() {
        super("ACCUMULATE", 1, 2);
    }

    @Override
    public Object evaluate(List<Object> parameters, JIPipeExpressionVariablesMap variables) {
        JIPipeExpressionVariablesMap localVariables = new JIPipeExpressionVariablesMap();
        Collection<?> items = (Collection<?>) parameters.get(0);
        List<String> variableNames = new ArrayList<>();
        for (Object item : items) {
            String s = "i" + localVariables.size();
            variableNames.add(s);
            localVariables.set(s, item);
        }

        String operator = parameters.size() > 1 ? StringUtils.nullToEmpty(parameters.get(1)) : "+";
        String expression = String.join(operator, variableNames);

        return JIPipeExpressionParameter.getEvaluatorInstance().evaluate(expression, localVariables);
    }

    @Override
    public ParameterInfo getParameterInfo(int index) {
        if (index == 0) {
            return new ParameterInfo("Sequence", "The array to be accumulated.", Collection.class);
        } else {
            return new ParameterInfo("Operator", "A valid operator string (e.g., +, -, /, ...)", String.class);
        }
    }
}
