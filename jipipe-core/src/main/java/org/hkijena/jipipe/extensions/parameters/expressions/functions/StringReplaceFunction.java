package org.hkijena.jipipe.extensions.parameters.expressions.functions;

import org.hkijena.jipipe.extensions.parameters.expressions.ExpressionParameters;
import org.hkijena.jipipe.api.JIPipeDocumentation;
import org.hkijena.jipipe.extensions.parameters.expressions.ExpressionFunction;
import org.hkijena.jipipe.extensions.parameters.expressions.ParameterInfo;

import java.util.List;

@JIPipeDocumentation(name = "Replace in string", description = "Replaces one string in another")
public class StringReplaceFunction extends ExpressionFunction {

    public StringReplaceFunction() {
        super("REPLACE_IN_STRING", 3);
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
        return text.replace(pattern, replacement);
    }
}
