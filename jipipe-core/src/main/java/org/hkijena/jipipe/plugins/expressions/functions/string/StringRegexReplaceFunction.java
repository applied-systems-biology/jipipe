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

package org.hkijena.jipipe.plugins.expressions.functions.string;

import org.hkijena.jipipe.api.SetJIPipeDocumentation;
import org.hkijena.jipipe.plugins.expressions.ExpressionFunction;
import org.hkijena.jipipe.plugins.expressions.JIPipeExpressionVariablesMap;
import org.hkijena.jipipe.plugins.expressions.ParameterInfo;

import java.util.List;

@SetJIPipeDocumentation(name = "Replace in string (Regex)", description = "Replaces one string in another. Finds pattern via RegEx.")
public class StringRegexReplaceFunction extends ExpressionFunction {

    public StringRegexReplaceFunction() {
        super("REGEX_REPLACE_IN_STRING", 3);
    }

    @Override
    public ParameterInfo getParameterInfo(int index) {
        switch (index) {
            case 0:
                return new ParameterInfo("text", "The text", String.class);
            case 1:
                return new ParameterInfo("pattern", "The pattern to replace", String.class);
            case 2:
                return new ParameterInfo("replacement", "The string that will replace the pattern", String.class);
            default:
                return null;
        }
    }

    @Override
    public String getSignature() {
        return getName() + "(text, pattern, replacement)";
    }

    @Override
    public Object evaluate(List<Object> parameters, JIPipeExpressionVariablesMap variables) {
        String text = "" + parameters.get(0);
        String pattern = "" + parameters.get(1);
        String replacement = "" + parameters.get(2);
        return text.replaceAll(pattern, replacement);
    }
}
