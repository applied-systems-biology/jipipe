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

import org.apache.commons.lang3.StringUtils;
import org.hkijena.jipipe.api.SetJIPipeDocumentation;
import org.hkijena.jipipe.plugins.expressions.ExpressionFunction;
import org.hkijena.jipipe.plugins.expressions.JIPipeExpressionVariablesMap;
import org.hkijena.jipipe.plugins.expressions.ParameterInfo;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

@SetJIPipeDocumentation(name = "Invert", description = "If the parameter is an array, the order is reversed. If a string, the string is reversed. Numbers and booleans are negated.")
public class InvertFunction extends ExpressionFunction {

    public InvertFunction() {
        super("INVERT", 1);
    }

    @Override
    public ParameterInfo getParameterInfo(int index) {
        if (index == 0) {
            return new ParameterInfo("value", "The value", String.class, Collection.class, Number.class);
        }
        return null;
    }

    @Override
    public Object evaluate(List<Object> parameters, JIPipeExpressionVariablesMap variables) {
        Object value = parameters.get(0);
        if (value instanceof Collection) {
            ArrayList<?> list = new ArrayList<>((Collection<?>) value);
            Collections.reverse(list);
            return list;
        } else if (value instanceof Number) {
            return -((Number) value).doubleValue();
        } else if (value instanceof Boolean) {
            return !((boolean) value);
        } else {
            return StringUtils.reverse("" + value);
        }
    }
}
