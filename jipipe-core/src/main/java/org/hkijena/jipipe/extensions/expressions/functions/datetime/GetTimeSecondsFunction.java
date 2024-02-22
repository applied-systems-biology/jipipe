package org.hkijena.jipipe.extensions.expressions.functions.datetime;

import org.hkijena.jipipe.api.SetJIPipeDocumentation;
import org.hkijena.jipipe.extensions.expressions.ExpressionFunction;
import org.hkijena.jipipe.extensions.expressions.JIPipeExpressionVariablesMap;

import java.time.LocalDateTime;
import java.util.List;

@SetJIPipeDocumentation(name = "Get current time (second)", description = "Returns the second of the current time as integer (0-59)")
public class GetTimeSecondsFunction extends ExpressionFunction {

    public GetTimeSecondsFunction() {
        super("GET_TIME_SECOND", 0);
    }

    @Override
    public Object evaluate(List<Object> parameters, JIPipeExpressionVariablesMap variables) {
        return LocalDateTime.now().getSecond();
    }
}
