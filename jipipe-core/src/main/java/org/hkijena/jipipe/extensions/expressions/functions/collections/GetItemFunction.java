package org.hkijena.jipipe.extensions.expressions.functions.collections;

import org.hkijena.jipipe.api.JIPipeDocumentation;
import org.hkijena.jipipe.extensions.expressions.ExpressionFunction;
import org.hkijena.jipipe.extensions.expressions.ExpressionVariables;
import org.hkijena.jipipe.extensions.expressions.ParameterInfo;

import java.util.List;
import java.util.Map;

@JIPipeDocumentation(name = "Gets item with index/key", description = "Gets the item of an array by index N (first is zero) or the (N+1)th character of a string. " +
        "If the first parameter is a map, the entry with the provided key is returned.")
public class GetItemFunction extends ExpressionFunction {

    public GetItemFunction() {
        super("GET_ITEM", 2);
    }

    @Override
    public ParameterInfo getParameterInfo(int index) {
        switch (index) {
            case 0:
                return new ParameterInfo("array", "An array, map, or a string", String.class, List.class, Map.class);
            case 1:
                return new ParameterInfo("index", "The item/character index or key", Integer.class, String.class);
            default:
                return null;
        }
    }

    @Override
    public Object evaluate(List<Object> parameters, ExpressionVariables variables) {
        Object target = parameters.get(0);
        Object index = parameters.get(1);
        if (target instanceof List) {
            return ((List<?>) target).get(((Number) index).intValue());
        } else if (target instanceof String) {
            return "" + target.toString().charAt(((Number) index).intValue());
        } else if (target instanceof Map) {
            return ((Map<?, ?>) target).getOrDefault(index, null);
        } else {
            throw new UnsupportedOperationException("Element access does not support " + target);
        }
    }
}
