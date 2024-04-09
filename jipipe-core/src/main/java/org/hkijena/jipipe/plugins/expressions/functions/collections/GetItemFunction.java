/*
 * Copyright by Zoltán Cseresnyés, Ruman Gerst
 *
 * Research Group Applied Systems Biology - Head: Prof. Dr. Marc Thilo Figge
 * https://www.leibniz-hki.de/en/applied-systems-biology.html
 * HKI-Center for Systems Biology of Infection
 * Leibniz Institute for Natural Product Research and Infection Biology - Hans Knöll Institute (HKI)
 * Adolf-Reichwein-Straße 23, 07745 Jena, Germany
 *
 * The project code is licensed under MIT.
 * See the LICENSE file provided with the code for the full license.
 */

package org.hkijena.jipipe.plugins.expressions.functions.collections;

import org.hkijena.jipipe.api.SetJIPipeDocumentation;
import org.hkijena.jipipe.plugins.expressions.ExpressionFunction;
import org.hkijena.jipipe.plugins.expressions.JIPipeExpressionVariablesMap;
import org.hkijena.jipipe.plugins.expressions.ParameterInfo;

import java.util.List;
import java.util.Map;

@SetJIPipeDocumentation(name = "Gets item with index/key", description = "Gets the item of an array by index N (first is zero) or the (N+1)th character of a string. If N is negative, the (-N)th last item is returned." +
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
    public Object evaluate(List<Object> parameters, JIPipeExpressionVariablesMap variables) {
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
}
