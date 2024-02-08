/*
 * Copyright by Zoltán Cseresnyés, Ruman Gerst
 *
 * Research Group Applied Systems Biology - Head: Prof. Dr. Marc Thilo Figge
 * https://www.leibniz-hki.de/en/applied-systems-biology.html
 * HKI-Center for Systems Biology of Infection
 * Leibniz Institute for Natural Product Research and Infection Biology - Hans Knöll Institute (HKI)
 * Adolf-Reichwein-Straße 23, 07745 Jena, Germany
 *
 * The project code is licensed under BSD 2-Clause.
 * See the LICENSE file provided with the code for the full license.
 */

package org.hkijena.jipipe.extensions.expressions.functions.string;

import org.hkijena.jipipe.api.JIPipeDocumentation;
import org.hkijena.jipipe.extensions.expressions.ExpressionFunction;
import org.hkijena.jipipe.extensions.expressions.JIPipeExpressionVariablesMap;
import org.hkijena.jipipe.extensions.expressions.ParameterInfo;

import java.util.List;

@JIPipeDocumentation(name = "String matches (Regex)", description = "Tests if the left operand matches the pattern described within the right operand. " +
        "There can be multiple right operands; in such case, the function returns true if any matches.")
public class RegexStringPredicateFunction extends ExpressionFunction {

    public RegexStringPredicateFunction() {
        super("STRING_MATCHES_REGEX", 2, Integer.MAX_VALUE);
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
