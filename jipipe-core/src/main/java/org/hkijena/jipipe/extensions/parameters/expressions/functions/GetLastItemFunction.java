package org.hkijena.jipipe.extensions.parameters.expressions.functions;

import com.fathzer.soft.javaluator.StaticVariableSet;
import com.google.common.collect.ImmutableList;
import org.hkijena.jipipe.api.JIPipeDocumentation;
import org.hkijena.jipipe.extensions.parameters.expressions.ExpressionFunction;
import org.hkijena.jipipe.extensions.parameters.expressions.ParameterInfo;

import java.util.Collection;
import java.util.List;

@JIPipeDocumentation(name = "Gets last item", description = "Gets the last item of an array or the last character of a string")
public class GetLastItemFunction extends ExpressionFunction {

    public GetLastItemFunction() {
        super("LAST_OF", 1);
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
    public Object evaluate(List<Object> parameters, StaticVariableSet<Object> variables) {
        Object target = parameters.get(0);
        if(target instanceof List) {
            List<?> list = (List<?>) target;
            return (list).get(list.size() - 1);
        }
        else if(target instanceof Collection) {
            ImmutableList<?> list = ImmutableList.copyOf((Collection<?>) target);
            return list.get(list.size() - 1);
        }
        else if(target instanceof String) {
            return "" + target.toString().charAt(0);
        }
        else {
            throw new UnsupportedOperationException("Element access does not support " + target);
        }
    }
}
