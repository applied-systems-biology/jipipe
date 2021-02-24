package org.hkijena.jipipe.extensions.parameters.expressions.functions;

import org.hkijena.jipipe.extensions.parameters.expressions.ExpressionParameters;
import org.hkijena.jipipe.api.JIPipeDocumentation;
import org.hkijena.jipipe.extensions.parameters.expressions.ExpressionFunction;
import org.hkijena.jipipe.extensions.parameters.expressions.ParameterInfo;

import java.util.Collection;
import java.util.List;

@JIPipeDocumentation(name = "Gets first item", description = "Gets the first item of an array or the first character of a string")
public class GetFirstItemFunction extends ExpressionFunction {

    public GetFirstItemFunction() {
        super("FIRST_OF", 1);
    }

    @Override
    public ParameterInfo getParameterInfo(int index) {
        switch (index) {
            case 0:
                return new ParameterInfo("array", "An array or a string", String.class, Collection.class);
            default:
                return null;
        }
    }

    @Override
    public Object evaluate(List<Object> parameters, ExpressionParameters variables) {
        Object target = parameters.get(0);
        if (target instanceof Collection) {
            return ((Collection<?>) target).iterator().next();
        } else if (target instanceof String) {
            return "" + target.toString().charAt(0);
        } else {
            throw new UnsupportedOperationException("Element access does not support " + target);
        }
    }
}
