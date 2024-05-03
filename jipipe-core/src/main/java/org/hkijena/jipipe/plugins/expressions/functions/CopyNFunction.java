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
import org.hkijena.jipipe.plugins.expressions.JIPipeExpressionEvaluator;
import org.hkijena.jipipe.plugins.expressions.JIPipeExpressionVariablesMap;
import org.hkijena.jipipe.plugins.expressions.ParameterInfo;

import java.util.ArrayList;
import java.util.List;

@SetJIPipeDocumentation(name = "Copy N times", description = "Copies the left operand N times, as defined by the right operand. " +
        "If N == 0, NULL is returned. If N == 1, the left operand is returned. If N > 1, an array of N copies is returned.")
public class CopyNFunction extends ExpressionFunction {
    public CopyNFunction() {
        super("COPY_N", 2);
    }

    @Override
    public ParameterInfo getParameterInfo(int index) {
        if (index == 0) {
            return new ParameterInfo("Object", "The object to be copied");
        } else {
            return new ParameterInfo("Copies", "Number of copies to make", Number.class);
        }
    }

    @Override
    public Object evaluate(List<Object> parameters, JIPipeExpressionVariablesMap variables) {
        Object source = parameters.get(0);
        int n = ((Number) parameters.get(1)).intValue();

        if (n <= 0)
            return null;
        if (n == 1)
            return source;
        List<Object> result = new ArrayList<>();
        for (int i = 0; i < n; i++) {
            result.add(JIPipeExpressionEvaluator.deepCopyObject(source));
        }
        return result;
    }
}
