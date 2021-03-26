package org.hkijena.jipipe.extensions.parameters.expressions.functions;

import org.hkijena.jipipe.api.JIPipeDocumentation;
import org.hkijena.jipipe.extensions.parameters.expressions.ExpressionFunction;
import org.hkijena.jipipe.extensions.parameters.expressions.ExpressionParameters;

import java.time.LocalDateTime;
import java.util.List;

@JIPipeDocumentation(name = "Get current time (minute)", description = "Returns the minute of the current time as integer (0-59)")
public class GetTimeHoursFunction extends ExpressionFunction {

    public GetTimeHoursFunction() {
        super("GET_TIME_MINUTE", 0);
    }

    @Override
    public Object evaluate(List<Object> parameters, ExpressionParameters variables) {
        return LocalDateTime.now().getMinute();
    }
}
