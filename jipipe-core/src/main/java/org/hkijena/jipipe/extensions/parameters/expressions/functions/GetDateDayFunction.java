package org.hkijena.jipipe.extensions.parameters.expressions.functions;

import com.fathzer.soft.javaluator.StaticVariableSet;
import org.hkijena.jipipe.api.JIPipeDocumentation;
import org.hkijena.jipipe.extensions.parameters.expressions.ExpressionFunction;

import java.time.LocalDateTime;
import java.util.List;

@JIPipeDocumentation(name = "Get current day of the month", description = "Returns the current day of the month as integer")
public class GetDateDayFunction extends ExpressionFunction {

    public GetDateDayFunction() {
        super("GET_DATE_DAY", 0);
    }

    @Override
    public Object evaluate(List<Object> parameters, StaticVariableSet<Object> variables) {
        return LocalDateTime.now().getDayOfMonth();
    }
}
