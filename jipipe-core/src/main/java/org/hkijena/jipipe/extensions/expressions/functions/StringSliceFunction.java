package org.hkijena.jipipe.extensions.expressions.functions;

import org.hkijena.jipipe.api.JIPipeDocumentation;
import org.hkijena.jipipe.extensions.expressions.ExpressionFunction;
import org.hkijena.jipipe.extensions.expressions.ExpressionVariables;
import org.hkijena.jipipe.extensions.expressions.ParameterInfo;

import java.util.List;

@JIPipeDocumentation(name = "Slice string", description = "Gets a substring. " +
        "This function will return the sub-string between two indices (first index is inclusive, second is exclusive). " +
        "If only one index is provided, all characters after the start index are included.")
public class StringSliceFunction extends ExpressionFunction {

    public StringSliceFunction() {
        super("SUBSTRING", 2, 3);
    }

    @Override
    public ParameterInfo getParameterInfo(int index) {
        switch (index) {
            case 0:
                return new ParameterInfo("text", "The text to split", String.class);
            case 1:
                return new ParameterInfo("start", "The start index (inclusive)", Integer.class);
            case 2:
                return new ParameterInfo("end", "The end index (exclusive)", Integer.class);
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
        int start = ((Number) parameters.get(1)).intValue();
        int end = parameters.size() >= 3 ? ((Number) parameters.get(2)).intValue() : text.length();

        return text.substring(start, end);
    }
}
