package org.hkijena.jipipe.extensions.parameters.expressions.functions;

import org.hkijena.jipipe.api.JIPipeDocumentation;
import org.hkijena.jipipe.extensions.parameters.expressions.ExpressionFunction;
import org.hkijena.jipipe.extensions.parameters.expressions.ExpressionParameters;

import java.time.LocalDateTime;
import java.util.List;

@JIPipeDocumentation(name = "Get current time (hour)", description = "Returns the hour of the current time as integer (0-23)")
public class GetTimeMinutesFunction extends ExpressionFunction {

    public GetTimeMinutesFunction() {
        super("GET_TIME_HOUR", 0);
    }

    @Override
    public Object evaluate(List<Object> parameters, ExpressionParameters variables) {
        return LocalDateTime.now().getHour();
    }
}
