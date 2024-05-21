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
import org.hkijena.jipipe.plugins.expressions.JIPipeExpressionParameter;
import org.hkijena.jipipe.plugins.expressions.JIPipeExpressionVariablesMap;
import org.hkijena.jipipe.plugins.expressions.ParameterInfo;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

@SetJIPipeDocumentation(name = "Apply built-in function to array", description = "Decomposes an array into parameters of a function and applies the built-in function. Please note that user-defined functions are not supported.")
public class RunFunctionFunction extends ExpressionFunction {
    public RunFunctionFunction() {
        super("APPLY_FUNCTION_TO_ARRAY", 2);
    }

    @Override
    public Object evaluate(List<Object> parameters, JIPipeExpressionVariablesMap variables) {
        JIPipeExpressionVariablesMap localVariables = new JIPipeExpressionVariablesMap();
        Collection<?> items = (Collection<?>) parameters.get(1);
        List<String> variableNames = new ArrayList<>();
        for (Object item : items) {
            String s = "i" + localVariables.size();
            variableNames.add(s);
            localVariables.set(s, item);
        }

        String functionName = "" + parameters.get(0);
        String expression = functionName + "(" + String.join(", ", variableNames) + ") ";
        return JIPipeExpressionParameter.getEvaluatorInstance().evaluate(expression, localVariables);
    }

    @Override
    public ParameterInfo getParameterInfo(int index) {
        if (index == 0) {
            return new ParameterInfo("Function name", "A valid function name", String.class);
        } else {
            return new ParameterInfo("Sequence", "The sequence to be accumulated.", Collection.class);
        }
    }
}
