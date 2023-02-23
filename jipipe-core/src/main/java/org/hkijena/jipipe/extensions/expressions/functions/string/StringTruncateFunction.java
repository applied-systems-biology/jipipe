package org.hkijena.jipipe.extensions.expressions.functions.string;

import org.hkijena.jipipe.api.JIPipeDocumentation;
import org.hkijena.jipipe.extensions.expressions.ExpressionFunction;
import org.hkijena.jipipe.extensions.expressions.ExpressionVariables;
import org.hkijena.jipipe.extensions.expressions.ParameterInfo;
import org.hkijena.jipipe.utils.StringUtils;

import java.util.List;

@JIPipeDocumentation(name = "Truncate string", description = "Truncates a string to the specified length")
public class StringTruncateFunction extends ExpressionFunction {

    public StringTruncateFunction() {
        super("STRING_TRUNCATE", 2);
    }

    @Override
    public ParameterInfo getParameterInfo(int index) {
        switch (index) {
            case 0:
                return new ParameterInfo("text", "The text", String.class);
            case 1:
                return new ParameterInfo("max_length", "The maximum length", Integer.class);
            default:
                return null;
        }
    }

    @Override
    public String getSignature() {
        return getName() + "(text, pattern)";
    }

    @Override
    public Object evaluate(List<Object> parameters, ExpressionVariables variables) {
        String text = StringUtils.nullToEmpty(parameters.get(0));
        int maxLength = ((Number) parameters.get(1)).intValue();
        return text.length() > maxLength ? text.substring(0, maxLength) : text;
    }
}
