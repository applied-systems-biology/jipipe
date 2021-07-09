package org.hkijena.jipipe.extensions.expressions.functions;

import org.hkijena.jipipe.api.JIPipeDocumentation;
import org.hkijena.jipipe.extensions.expressions.ExpressionFunction;
import org.hkijena.jipipe.extensions.expressions.ExpressionVariables;
import org.hkijena.jipipe.extensions.expressions.ParameterInfo;

import java.util.Arrays;
import java.util.List;

@JIPipeDocumentation(name = "Split string", description = "Splits a string by the right parameter into an array. For example you can split a string 'a_b_c' by '_' into 'a', 'b', and 'c'")
public class StringSplitFunction extends ExpressionFunction {

    public StringSplitFunction() {
        super("SPLIT_STRING", 2);
    }

    @Override
    public ParameterInfo getParameterInfo(int index) {
        switch (index) {
            case 0:
                return new ParameterInfo("text", "The text to split", String.class);
            case 1:
                return new ParameterInfo("pattern", "The pattern where the string is split", String.class);
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
        String text = "" + parameters.get(0);
        String pattern = "" + parameters.get(1);
        return Arrays.asList(text.split(pattern));
    }
}
