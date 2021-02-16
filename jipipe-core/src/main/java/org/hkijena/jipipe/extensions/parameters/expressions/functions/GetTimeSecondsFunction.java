package org.hkijena.jipipe.extensions.parameters.expressions.functions;

import com.fathzer.soft.javaluator.StaticVariableSet;
import org.hkijena.jipipe.api.JIPipeDocumentation;
import org.hkijena.jipipe.extensions.parameters.expressions.ExpressionFunction;

import java.time.LocalDateTime;
import java.util.List;

@JIPipeDocumentation(name = "Get current time (second)", description = "Returns the second of the current time as integer (0-59)")
public class GetTimeSecondsFunction extends ExpressionFunction {

    public GetTimeSecondsFunction() {
        super("GET_TIME_SECOND", 0);
    }

    @Override
    public Object evaluate(List<Object> parameters, StaticVariableSet<Object> variables) {
        return LocalDateTime.now().getSecond();
    }
}
