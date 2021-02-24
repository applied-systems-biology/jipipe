package org.hkijena.jipipe.extensions.parameters.expressions.functions;

import org.hkijena.jipipe.extensions.parameters.expressions.ExpressionParameters;
import org.hkijena.jipipe.api.JIPipeDocumentation;
import org.hkijena.jipipe.extensions.parameters.expressions.ExpressionFunction;

import java.util.List;

@JIPipeDocumentation(name = "Create pair", description = "Creates an array with two items.")
public class CreatePairArrayFunction extends ExpressionFunction {

    public CreatePairArrayFunction() {
        super("PAIR", 2);
    }

    @Override
    public Object evaluate(List<Object> parameters, ExpressionParameters variables) {
        return parameters;
    }
}
