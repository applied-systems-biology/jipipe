package org.hkijena.jipipe.extensions.parameters.expressions.functions;

import com.fathzer.soft.javaluator.StaticVariableSet;
import org.hkijena.jipipe.api.JIPipeDocumentation;
import org.hkijena.jipipe.extensions.parameters.expressions.ExpressionFunction;

import java.util.List;

@JIPipeDocumentation(name = "Create array", description = "Creates a new array that contains the parameters.")
public class CreateArrayFunction extends ExpressionFunction {

    public CreateArrayFunction() {
        super("ARRAY", 0, Integer.MAX_VALUE);
    }

    @Override
    public Object evaluate(List<Object> parameters, StaticVariableSet<Object> variables) {
        return parameters;
    }
}
