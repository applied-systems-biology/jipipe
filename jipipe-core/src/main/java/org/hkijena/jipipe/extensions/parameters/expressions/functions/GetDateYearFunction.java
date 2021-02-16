package org.hkijena.jipipe.extensions.parameters.expressions.functions;

import com.fathzer.soft.javaluator.StaticVariableSet;
import org.hkijena.jipipe.api.JIPipeDocumentation;
import org.hkijena.jipipe.extensions.parameters.expressions.ExpressionFunction;

import java.time.LocalDateTime;
import java.util.List;

@JIPipeDocumentation(name = "Get current year", description = "Returns the current year as integer")
public class GetDateYearFunction extends ExpressionFunction {

    public GetDateYearFunction() {
        super("GET_DATE_YEAR", 0);
    }

    @Override
    public Object evaluate(List<Object> parameters, StaticVariableSet<Object> variables) {
        return LocalDateTime.now().getYear();
    }
}
