package org.hkijena.jipipe.extensions.expressions.functions.string;

import org.hkijena.jipipe.api.JIPipeDocumentation;
import org.hkijena.jipipe.extensions.expressions.ExpressionFunction;
import org.hkijena.jipipe.extensions.expressions.ExpressionVariables;
import org.hkijena.jipipe.utils.StringUtils;

import java.util.List;

@JIPipeDocumentation(name = "Trim string", description = "Removes whitespace characters at the start and the end of the string. Uses the Java trim algorithm.")
public class StringTrimFunction extends ExpressionFunction {

    public StringTrimFunction() {
        super("STRING_TRIM", 1);
    }

    @Override
    public Object evaluate(List<Object> parameters, ExpressionVariables variables) {
        String fileName = StringUtils.nullToEmpty(parameters.get(0));
        return fileName.trim();
    }
}
