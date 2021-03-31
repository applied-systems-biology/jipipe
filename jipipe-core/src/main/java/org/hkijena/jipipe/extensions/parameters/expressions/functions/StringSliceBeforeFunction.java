package org.hkijena.jipipe.extensions.parameters.expressions.functions;

import org.hkijena.jipipe.api.JIPipeDocumentation;
import org.hkijena.jipipe.extensions.parameters.expressions.ExpressionFunction;
import org.hkijena.jipipe.extensions.parameters.expressions.ExpressionParameters;
import org.hkijena.jipipe.extensions.parameters.expressions.ParameterInfo;

import java.util.List;

@JIPipeDocumentation(name = "Slice string", description = "Gets a substring. " +
        "This function will return the sub-string between two indices (first index is exclusive, second is inclusive). " +
        "If only one index is provided, all characters before the start index are included.")
public class StringSliceBeforeFunction extends ExpressionFunction {

    public StringSliceBeforeFunction() {
        super("SUBSTRING_BEFORE", 2, 3);
    }

    @Override
    public ParameterInfo getParameterInfo(int index) {
        switch (index) {
            case 0:
                return new ParameterInfo("text", "The text to split", String.class);
            case 1:
                return new ParameterInfo("end", "The end index (exclusive)", Integer.class);
            case 2:
                return new ParameterInfo("start", "The start index (inclusive)", Integer.class);
            default:
                return null;
        }
    }

    @Override
    public String getSignature() {
        return getName() + "(text, pattern)";
    }

    @Override
    public Object evaluate(List<Object> parameters, ExpressionParameters variables) {
        String text = "" + parameters.get(0);
        int end = ((Number)parameters.get(1)).intValue();
        int start = parameters.size() >= 3 ? ((Number)parameters.get(2)).intValue() : 0;

        return text.substring(start, end);
    }
}
