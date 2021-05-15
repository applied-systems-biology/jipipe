package org.hkijena.jipipe.extensions.expressions.functions;

import org.hkijena.jipipe.api.JIPipeDocumentation;
import org.hkijena.jipipe.extensions.expressions.ExpressionFunction;
import org.hkijena.jipipe.extensions.expressions.ExpressionParameters;

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