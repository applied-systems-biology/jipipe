package org.hkijena.jipipe.extensions.parameters.expressions.functions;

import com.fathzer.soft.javaluator.StaticVariableSet;
import com.google.common.collect.ImmutableList;
import org.hkijena.jipipe.api.JIPipeDocumentation;
import org.hkijena.jipipe.extensions.parameters.expressions.ExpressionFunction;

import java.util.Collection;
import java.util.List;

@JIPipeDocumentation(name = "Gets first item", description = "Gets the first item of an array or the first character of a string")
public class GetFirstItemFunction extends ExpressionFunction {

    public GetFirstItemFunction() {
        super("FIRST_OF", 1);
    }

    @Override
    public Object evaluate(List<Object> parameters, StaticVariableSet<Object> variables) {
        Object target = parameters.get(0);
        if(target instanceof Collection) {
            return ((Collection<?>)target).iterator().next();
        }
        else if(target instanceof String) {
            return "" + target.toString().charAt(0);
        }
        else {
            throw new UnsupportedOperationException("Element access does not support " + target);
        }
    }
}
