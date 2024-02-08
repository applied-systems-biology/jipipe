package org.hkijena.jipipe.extensions.expressions.functions.string;

import org.hkijena.jipipe.api.JIPipeDocumentation;
import org.hkijena.jipipe.extensions.expressions.ExpressionFunction;
import org.hkijena.jipipe.extensions.expressions.JIPipeExpressionVariablesMap;
import org.hkijena.jipipe.extensions.expressions.ParameterInfo;
import org.hkijena.jipipe.utils.StringUtils;

import java.util.List;

@JIPipeDocumentation(name = "Format string", description = "Replaces one string in another")
public class StringFormatFunction extends ExpressionFunction {

    public StringFormatFunction() {
        super("STRING_FORMAT", 1, Integer.MAX_VALUE);
    }

    @Override
    public ParameterInfo getParameterInfo(int index) {
        if (index == 0) {
            return new ParameterInfo("Format string", "String containing the format codes (see https://docs.oracle.com/javase/8/docs/api/java/util/Formatter.html)", String.class);
        } else {
            return new ParameterInfo("Value " + index, "Value that will be passed to the formatter", Object.class);
        }
    }

    @Override
    public Object evaluate(List<Object> parameters, JIPipeExpressionVariablesMap variables) {
        String formatString = StringUtils.nullToEmpty(parameters.get(0));
        Object[] args = new Object[parameters.size() - 1];
        for (int i = 1; i < parameters.size(); i++) {
            args[i - 1] = parameters.get(i);
        }
        return String.format(formatString, args);
    }
}
