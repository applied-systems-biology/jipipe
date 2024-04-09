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

import com.google.common.collect.ImmutableList;
import org.hkijena.jipipe.api.SetJIPipeDocumentation;
import org.hkijena.jipipe.plugins.expressions.ExpressionFunction;
import org.hkijena.jipipe.plugins.expressions.JIPipeExpressionVariablesMap;
import org.hkijena.jipipe.plugins.expressions.ParameterInfo;

import java.util.Collection;
import java.util.List;

@SetJIPipeDocumentation(name = "Gets last item", description = "Gets the last item of an array or the last character of a string")
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
    public Object evaluate(List<Object> parameters, JIPipeExpressionVariablesMap variables) {
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
