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

package org.hkijena.jipipe.extensions.expressions.functions.string;

import org.hkijena.jipipe.api.SetJIPipeDocumentation;
import org.hkijena.jipipe.extensions.expressions.ExpressionFunction;
import org.hkijena.jipipe.extensions.expressions.JIPipeExpressionVariablesMap;
import org.hkijena.jipipe.extensions.expressions.ParameterInfo;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@SetJIPipeDocumentation(name = "Match RegEx groups", description = "Function that returns all RegEx groups. The first group (index 0) is always the whole string if it matches. Returns an empty array if no match is found.")
public class ExtractRegexMatchesFunction extends ExpressionFunction {

    public ExtractRegexMatchesFunction() {
        super("REGEX_EXTRACT_MATCHES", 2);
    }

    @Override
    public String getSignature() {
        return getName() + "(text, pattern)";
    }

    @Override
    public ParameterInfo getParameterInfo(int index) {
        switch (index) {
            case 0:
                return new ParameterInfo("text", "The text to search in", String.class);
            case 1:
                return new ParameterInfo("pattern", "The pattern to search in the text", String.class);
            default:
                return null;
        }
    }

    @Override
    public Object evaluate(List<Object> parameters, JIPipeExpressionVariablesMap variables) {
        String haystack = "" + parameters.get(0);
        String pattern = "" + parameters.get(1);
        Matcher matcher = Pattern.compile(pattern).matcher(haystack);
        if (matcher.find()) {
            ArrayList<Object> result = new ArrayList<>();
            for (int i = 0; i <= matcher.groupCount(); i++) {
                result.add(matcher.group(i));
            }
            return result;
        } else {
            return new ArrayList<>();
        }
    }
}
