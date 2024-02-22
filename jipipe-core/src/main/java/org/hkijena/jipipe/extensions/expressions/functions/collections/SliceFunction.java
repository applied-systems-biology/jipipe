package org.hkijena.jipipe.extensions.expressions.functions.collections;

import org.hkijena.jipipe.api.SetJIPipeDocumentation;
import org.hkijena.jipipe.extensions.expressions.ExpressionFunction;
import org.hkijena.jipipe.extensions.expressions.JIPipeExpressionVariablesMap;
import org.hkijena.jipipe.extensions.expressions.ParameterInfo;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@SetJIPipeDocumentation(name = "Slice", description = "Returns a sublist/substring of an array/string/map keys defined by the two indices. The start and end point to the (N-1)th item in the input. If N is negative, the (-N)th last item is referred.")
public class SliceFunction extends ExpressionFunction {

    public SliceFunction() {
        super("SLICE", 3);
    }

    @Override
    public ParameterInfo getParameterInfo(int index) {
        switch (index) {
            case 0:
                return new ParameterInfo("array", "An array, map, or a string", String.class, List.class, Map.class);
            case 1:
                return new ParameterInfo("start", "The first item/character index (inclusive)", Integer.class, String.class);
            case 2:
                return new ParameterInfo("end", "The second item/character index (exclusive)", Integer.class, String.class);
            default:
                return null;
        }
    }

    @Override
    public Object evaluate(List<Object> parameters, JIPipeExpressionVariablesMap variables) {
        Object list_ = parameters.get(0);
        List<Object> src;
        if (list_ instanceof List) {
            src = (List<Object>) list_;
        } else if (list_ instanceof String) {
            src = new ArrayList<>();
            String str = (String) list_;
            for (int i = 0; i < str.length(); i++) {
                src.add(str.charAt(i));
            }
        } else if (list_ instanceof Map) {
            src = new ArrayList<>(((Map<?, ?>) list_).keySet());
        } else {
            throw new UnsupportedOperationException("Element slicing does not support " + list_);
        }

        int i1 = ((Number) parameters.get(1)).intValue();
        int i2 = ((Number) parameters.get(2)).intValue();
        while (!src.isEmpty() && i1 < 0) {
            i1 += src.size();
        }
        while (!src.isEmpty() && i2 < 0) {
            i2 += src.size();
        }

        List<Object> result = new ArrayList<>();
        if (i1 <= i2) {
            for (int i = i1; i < i2; i++) {
                result.add(src.get(i));
            }
        } else {
            for (int i = i2; i < i1; i--) {
                result.add(src.get(i));
            }
        }

        if (list_ instanceof String) {
            return result.stream().map(Object::toString).collect(Collectors.joining(""));
        } else {
            return result;
        }
    }
}
