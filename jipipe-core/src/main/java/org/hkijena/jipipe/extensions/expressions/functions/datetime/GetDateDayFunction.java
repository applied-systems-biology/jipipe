package org.hkijena.jipipe.extensions.expressions.functions.datetime;

import org.hkijena.jipipe.api.JIPipeDocumentation;
import org.hkijena.jipipe.extensions.expressions.ExpressionFunction;
import org.hkijena.jipipe.extensions.expressions.JIPipeExpressionVariablesMap;

import java.time.LocalDateTime;
import java.util.List;

@JIPipeDocumentation(name = "Get current day of the month", description = "Returns the current day of the month as integer")
public class GetDateDayFunction extends ExpressionFunction {

    public GetDateDayFunction() {
        super("GET_DATE_DAY", 0);
    }

    @Override
    public Object evaluate(List<Object> parameters, JIPipeExpressionVariablesMap variables) {
        return LocalDateTime.now().getDayOfMonth();
    }
}
