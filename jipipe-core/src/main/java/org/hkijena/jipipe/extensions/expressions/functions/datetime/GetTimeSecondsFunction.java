package org.hkijena.jipipe.extensions.expressions.functions.datetime;

import org.hkijena.jipipe.api.JIPipeDocumentation;
import org.hkijena.jipipe.extensions.expressions.ExpressionFunction;
import org.hkijena.jipipe.extensions.expressions.ExpressionVariables;

import java.time.LocalDateTime;
import java.util.List;

@JIPipeDocumentation(name = "Get current time (second)", description = "Returns the second of the current time as integer (0-59)")
public class GetTimeSecondsFunction extends ExpressionFunction {

    public GetTimeSecondsFunction() {
        super("GET_TIME_SECOND", 0);
    }

    @Override
    public Object evaluate(List<Object> parameters, ExpressionVariables variables) {
        return LocalDateTime.now().getSecond();
    }
}
