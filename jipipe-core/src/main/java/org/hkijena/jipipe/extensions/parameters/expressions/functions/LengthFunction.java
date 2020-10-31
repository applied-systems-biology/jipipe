package org.hkijena.jipipe.extensions.parameters.expressions.functions;

import com.fathzer.soft.javaluator.StaticVariableSet;
import org.hkijena.jipipe.api.JIPipeDocumentation;
import org.hkijena.jipipe.extensions.parameters.expressions.ExpressionFunction;

import java.util.Collection;
import java.util.List;

@JIPipeDocumentation(name = "Get length", description = "For arrays and strings, this function returns their length/size. For numbers and boolean values, this function will throw an error")
public class LengthFunction extends ExpressionFunction {

    public LengthFunction() {
        super("LENGTH", 1);
    }

    @Override
    public Object evaluate(List<Object> parameters, StaticVariableSet<Object> variables) {
        Object value = parameters.get(0);
        if(value instanceof Collection)
            return ((Collection<?>) value).size();
        else if(value instanceof String)
            return ((String) value).length();
        else
            throw new UnsupportedOperationException("Cannot get length of '" + value + "'");
    }
}
