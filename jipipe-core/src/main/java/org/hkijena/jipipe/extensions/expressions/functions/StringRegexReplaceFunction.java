package org.hkijena.jipipe.extensions.expressions.functions;

import org.hkijena.jipipe.api.JIPipeDocumentation;
import org.hkijena.jipipe.extensions.expressions.ExpressionFunction;
import org.hkijena.jipipe.extensions.expressions.ExpressionParameters;
import org.hkijena.jipipe.extensions.expressions.ParameterInfo;

import java.util.List;

@JIPipeDocumentation(name = "Replace in string (Regex)", description = "Replaces one string in another. Finds pattern via RegEx.")
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
    public Object evaluate(List<Object> parameters, ExpressionParameters variables) {
        String text = "" + parameters.get(0);
        String pattern = "" + parameters.get(1);
        String replacement = "" + parameters.get(2);
        return text.replaceAll(pattern, replacement);
    }
}
