package org.hkijena.jipipe.extensions.expressions.functions.collections;

import org.hkijena.jipipe.api.JIPipeDocumentation;
import org.hkijena.jipipe.extensions.expressions.ExpressionFunction;
import org.hkijena.jipipe.extensions.expressions.ExpressionVariables;
import org.hkijena.jipipe.extensions.expressions.ParameterInfo;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@JIPipeDocumentation(name = "Slice Right", description = "Returns a sublist/substring of an array/string/map keys containing all items after (inclusive) an index.")
public class SliceAfterFunction extends ExpressionFunction {

    public SliceAfterFunction() {
        super("SLICE_RIGHT", 2);
    }

    @Override
    public ParameterInfo getParameterInfo(int index) {
        switch (index) {
            case 0:
                return new ParameterInfo("array", "An array, map, or a string", String.class, List.class, Map.class);
            case 1:
                return new ParameterInfo("start", "The first item/character index (inclusive)", Integer.class, String.class);
            default:
                return null;
        }
    }

    @Override
    public Object evaluate(List<Object> parameters, ExpressionVariables variables) {
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
        for (int i = i1; i < src.size(); i++) {
            result.add(src.get(i));
        }

        if (list_ instanceof String) {
            return result.stream().map(Object::toString).collect(Collectors.joining(""));
        } else {
            return result;
        }
    }
}
