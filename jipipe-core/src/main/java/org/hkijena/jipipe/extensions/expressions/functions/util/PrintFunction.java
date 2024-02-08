package org.hkijena.jipipe.extensions.expressions.functions.util;

import org.hkijena.jipipe.api.JIPipeDocumentation;
import org.hkijena.jipipe.extensions.expressions.ExpressionFunction;
import org.hkijena.jipipe.extensions.expressions.JIPipeExpressionVariablesMap;

import java.util.List;
import java.util.stream.Collectors;

@JIPipeDocumentation(name = "Print", description = "Outputs the provided values as string into the console. Returns the message.")
public class PrintFunction extends ExpressionFunction {
    public PrintFunction() {
        super("PRINT", 0, Integer.MAX_VALUE);
    }

    @Override
    public Object evaluate(List<Object> parameters, JIPipeExpressionVariablesMap variables) {
        String message = parameters.stream().map(s -> "" + s).collect(Collectors.joining());
        System.out.println(message);
        return message;
    }
}
