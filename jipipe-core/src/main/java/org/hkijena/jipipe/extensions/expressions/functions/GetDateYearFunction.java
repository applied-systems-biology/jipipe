package org.hkijena.jipipe.extensions.expressions.functions;

import org.hkijena.jipipe.api.JIPipeDocumentation;
import org.hkijena.jipipe.extensions.expressions.ExpressionFunction;
import org.hkijena.jipipe.extensions.expressions.ExpressionParameters;

import java.time.LocalDateTime;
import java.util.List;

@JIPipeDocumentation(name = "Get current year", description = "Returns the current year as integer")
public class GetDateYearFunction extends ExpressionFunction {

    public GetDateYearFunction() {
        super("GET_DATE_YEAR", 0);
    }

    @Override
    public Object evaluate(List<Object> parameters, ExpressionParameters variables) {
        return LocalDateTime.now().getYear();
    }
}
