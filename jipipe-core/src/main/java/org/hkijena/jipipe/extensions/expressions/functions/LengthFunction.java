package org.hkijena.jipipe.extensions.expressions.functions;

import org.hkijena.jipipe.api.JIPipeDocumentation;
import org.hkijena.jipipe.extensions.expressions.ExpressionFunction;
import org.hkijena.jipipe.extensions.expressions.ExpressionVariables;
import org.hkijena.jipipe.extensions.expressions.ParameterInfo;

import java.util.Collection;
import java.util.List;
import java.util.Map;

@JIPipeDocumentation(name = "Get length", description = "For arrays, maps, and strings, this function returns their length/size. For numbers and boolean values, this function will throw an error")
public class LengthFunction extends ExpressionFunction {

    public LengthFunction() {
        super("LENGTH", 1);
    }

    @Override
    public ParameterInfo getParameterInfo(int index) {
        if (index == 0) {
            return new ParameterInfo("value", "The value", String.class, Collection.class);
        }
        return null;
    }

    @Override
    public Object evaluate(List<Object> parameters, ExpressionVariables variables) {
        Object value = parameters.get(0);
        if (value instanceof Collection)
            return ((Collection<?>) value).size();
        if (value instanceof Map)
            return ((Map<?, ?>) value).size();
        else if (value instanceof String)
            return ((String) value).length();
        else
            throw new UnsupportedOperationException("Cannot get length of '" + value + "'");
    }
}
