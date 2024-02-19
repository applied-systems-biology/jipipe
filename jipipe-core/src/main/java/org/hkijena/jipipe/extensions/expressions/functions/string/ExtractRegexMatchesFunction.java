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
