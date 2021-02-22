package org.hkijena.jipipe.extensions.parameters.expressions.functions;

import com.fathzer.soft.javaluator.StaticVariableSet;
import org.hkijena.jipipe.api.JIPipeDocumentation;
import org.hkijena.jipipe.extensions.parameters.expressions.ExpressionFunction;

import java.time.LocalDateTime;
import java.util.List;

@JIPipeDocumentation(name = "Get current month", description = "Returns the current month as integer (January = 1, December = 12)")
public class GetDateMonthFunction extends ExpressionFunction {

    public GetDateMonthFunction() {
        super("GET_DATE_MONTH", 0);
    }

    @Override
    public Object evaluate(List<Object> parameters, StaticVariableSet<Object> variables) {
        return LocalDateTime.now().getMonth().getValue();
    }
}
