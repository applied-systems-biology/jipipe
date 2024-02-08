package org.hkijena.jipipe.extensions.expressions.functions.collections;

import org.hkijena.jipipe.api.JIPipeDocumentation;
import org.hkijena.jipipe.extensions.expressions.ExpressionFunction;
import org.hkijena.jipipe.extensions.expressions.JIPipeExpressionVariablesMap;
import org.hkijena.jipipe.extensions.expressions.ParameterInfo;

import java.util.List;
import java.util.Map;

@JIPipeDocumentation(name = "Gets item with index/key or default", description = "Gets the item of an array by index N (first is zero) or the (N+1)th character of a string. If N is negative, the (-N)th last item is returned." +
        "If the first parameter is a map, the entry with the provided key is returned. If the key does not exist, then the default value is returned")
public class GetItemOrDefaultFunction extends ExpressionFunction {

    public GetItemOrDefaultFunction() {
        super("GET_ITEM_OR_DEFAULT", 3);
    }

    @Override
    public ParameterInfo getParameterInfo(int index) {
        switch (index) {
            case 0:
                return new ParameterInfo("array", "An array, map, or a string", String.class, List.class, Map.class);
            case 1:
                return new ParameterInfo("index", "The item/character index or key", Integer.class, String.class);
            case 2:
                return new ParameterInfo("default", "The defautl value", Object.class);
            default:
                return null;
        }
    }

    @Override
    public Object evaluate(List<Object> parameters, JIPipeExpressionVariablesMap variables) {
        try {
            Object target = parameters.get(0);
            Object index = parameters.get(1);
            if (target instanceof List) {
                int i = ((Number) index).intValue();
                List<?> list = (List<?>) target;
                if (i < 0) {
                    i += list.size();
                }
                return list.get(i);
            } else if (target instanceof String) {
                int i = ((Number) index).intValue();
                String string = (String) target;
                if (i < 0) {
                    i += string.length();
                }
                return string.charAt(i) + "";
            } else if (target instanceof Map) {
                return ((Map<?, ?>) target).getOrDefault(index, null);
            } else {
                throw new UnsupportedOperationException("Element access does not support " + target);
            }
        }
        catch (Throwable e) {
            return parameters.get(2);
        }
    }
}
