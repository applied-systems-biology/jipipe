package org.hkijena.jipipe.extensions.expressions.functions;

import org.hkijena.jipipe.api.JIPipeDocumentation;
import org.hkijena.jipipe.extensions.expressions.ExpressionFunction;
import org.hkijena.jipipe.extensions.expressions.ExpressionVariables;

import java.time.LocalDateTime;
import java.util.List;

@JIPipeDocumentation(name = "Get current time (hour)", description = "Returns the hour of the current time as integer (0-23)")
public class GetTimeMinutesFunction extends ExpressionFunction {

    public GetTimeMinutesFunction() {
        super("GET_TIME_HOUR", 0);
    }

    @Override
    public Object evaluate(List<Object> parameters, ExpressionVariables variables) {
        return LocalDateTime.now().getHour();
    }
}
