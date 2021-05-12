package org.hkijena.jipipe.extensions.expressions.functions;

import com.google.common.collect.ImmutableList;
import org.hkijena.jipipe.api.JIPipeDocumentation;
import org.hkijena.jipipe.extensions.expressions.ExpressionFunction;
import org.hkijena.jipipe.extensions.expressions.ExpressionParameters;
import org.hkijena.jipipe.extensions.expressions.ParameterInfo;

import java.util.Collection;
import java.util.List;

@JIPipeDocumentation(name = "Gets last item", description = "Gets the last item of an array or the last character of a string")
public class GetLastItemFunction extends ExpressionFunction {

    public GetLastItemFunction() {
        super("LAST_OF", 1, 2);
    }

    @Override
    public ParameterInfo getParameterInfo(int index) {
        switch (index) {
            case 0:
                return new ParameterInfo("array", "An array or a string", String.class, Collection.class);
            case 1:
                return new ParameterInfo("N", "Allows to select the Nth last item", Integer.class);
            default:
                return null;
        }
    }

    @Override
    public Object evaluate(List<Object> parameters, ExpressionParameters variables) {
        Object target = parameters.get(0);
        int n = parameters.size() > 1 ? ((Number) parameters.get(1)).intValue() : 0;
        if (target instanceof List) {
            List<?> list = (List<?>) target;
            return (list).get(list.size() - 1 - n);
        } else if (target instanceof Collection) {
            ImmutableList<?> list = ImmutableList.copyOf((Collection<?>) target);
            return list.get(list.size() - 1 - n);
        } else if (target instanceof String) {
            String s = target.toString();
            return "" + s.charAt(s.length() - 1 - n);
        } else {
            throw new UnsupportedOperationException("Element access does not support " + target);
        }
    }
}
