package org.hkijena.jipipe.extensions.parameters.expressions.functions;

import org.hkijena.jipipe.api.JIPipeDocumentation;
import org.hkijena.jipipe.extensions.parameters.expressions.ExpressionFunction;
import org.hkijena.jipipe.extensions.parameters.expressions.ExpressionParameters;
import org.hkijena.jipipe.extensions.parameters.expressions.ParameterInfo;

import java.util.List;

@JIPipeDocumentation(name = "String to uppercase", description = "Converts a string to a uppercase string")
public class StringToUpperCaseFunction extends ExpressionFunction {

    public StringToUpperCaseFunction() {
        super("STRING_TO_UPPERCASE", 1);
    }

    @Override
    public ParameterInfo getParameterInfo(int index) {
        switch (index) {
            case 0:
                return new ParameterInfo("text", "The text", String.class);
            default:
                return null;
        }
    }

    @Override
    public String getSignature() {
        return getName() + "(text)";
    }

    @Override
    public Object evaluate(List<Object> parameters, ExpressionParameters variables) {
        String text = "" + parameters.get(0);
        return text.toUpperCase();
    }
}
