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
import org.hkijena.jipipe.utils.StringUtils;

import java.util.List;

@SetJIPipeDocumentation(name = "String matches (Glob)", description = "Tests if the left operand matches the pattern described within the right operand. " +
        "There can be multiple right operands; in such case, the function returns true if any matches.")
public class GlobStringPredicateFunction extends ExpressionFunction {

    public GlobStringPredicateFunction() {
        super("STRING_MATCHES_GLOB", 2, Integer.MAX_VALUE);
    }

    @Override
    public ParameterInfo getParameterInfo(int index) {
        switch (index) {
            case 0:
                return new ParameterInfo("text", "The text to search in", String.class);
            case 1:
                return new ParameterInfo("pattern", "The pattern to search in the text", String.class);
            default:
                return new ParameterInfo("pattern " + (index + 1), "The pattern to search in the text", String.class);
        }
    }

    @Override
    public Object evaluate(List<Object> parameters, JIPipeExpressionVariablesMap variables) {
        String text = "" + parameters.get(0);
        for (int i = 1; i < parameters.size(); i++) {
            String pattern = "" + parameters.get(i);
            pattern = StringUtils.convertGlobToRegex(pattern);
            if (text.matches(pattern))
                return true;
        }
        return false;
    }

    @Override
    public String getSignature() {
        return String.format("%s(%s, %s)", getName(), "text", "pattern");
    }
}
