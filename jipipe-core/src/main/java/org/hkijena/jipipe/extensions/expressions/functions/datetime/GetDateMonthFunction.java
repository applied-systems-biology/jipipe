package org.hkijena.jipipe.extensions.expressions.functions.datetime;

import org.hkijena.jipipe.api.JIPipeDocumentation;
import org.hkijena.jipipe.extensions.expressions.ExpressionFunction;
import org.hkijena.jipipe.extensions.expressions.ExpressionVariables;

import java.time.LocalDateTime;
import java.util.List;

@JIPipeDocumentation(name = "Get current month", description = "Returns the current month as integer (January = 1, December = 12)")
public class GetDateMonthFunction extends ExpressionFunction {

    public GetDateMonthFunction() {
        super("GET_DATE_MONTH", 0);
    }

    @Override
    public Object evaluate(List<Object> parameters, ExpressionVariables variables) {
        return LocalDateTime.now().getMonth().getValue();
    }
}