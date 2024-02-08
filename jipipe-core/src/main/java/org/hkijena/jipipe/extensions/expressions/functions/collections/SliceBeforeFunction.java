package org.hkijena.jipipe.extensions.expressions.functions.collections;

import org.hkijena.jipipe.api.JIPipeDocumentation;
import org.hkijena.jipipe.extensions.expressions.ExpressionFunction;
import org.hkijena.jipipe.extensions.expressions.JIPipeExpressionVariablesMap;
import org.hkijena.jipipe.extensions.expressions.ParameterInfo;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@JIPipeDocumentation(name = "Slice Left", description = "Returns a sublist/substring of an array/string/map keys containing all items before (exclusive) an index.")
public class SliceBeforeFunction extends ExpressionFunction {

    public SliceBeforeFunction() {
        super("SLICE_LEFT", 2);
    }

    @Override
    public ParameterInfo getParameterInfo(int index) {
        switch (index) {
            case 0:
                return new ParameterInfo("array", "An array, map, or a string", String.class, List.class, Map.class);
            case 1:
                return new ParameterInfo("end", "The last item/character index (exlusive)", Integer.class, String.class);
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

        List<Object> result = new ArrayList<>();
        for (int i = 0; i < i1; i++) {
            result.add(src.get(i));
        }

        if (list_ instanceof String) {
            return result.stream().map(Object::toString).collect(Collectors.joining(""));
        } else {
            return result;
        }
    }
}
