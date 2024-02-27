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

package org.hkijena.jipipe.extensions.expressions.functions;

import org.hkijena.jipipe.api.SetJIPipeDocumentation;
import org.hkijena.jipipe.extensions.expressions.ExpressionFunction;
import org.hkijena.jipipe.extensions.expressions.JIPipeExpressionVariablesMap;
import org.hkijena.jipipe.extensions.expressions.ParameterInfo;

import java.util.Collection;
import java.util.List;
import java.util.Map;

@SetJIPipeDocumentation(name = "Get length", description = "For arrays, maps, and strings, this function returns their length/size. For numbers and boolean values, this function will throw an error")
public class LengthFunction extends ExpressionFunction {

    public LengthFunction() {
        super("LENGTH", 1);
    }

    @Override
    public ParameterInfo getParameterInfo(int index) {
        if (index == 0) {
            return new ParameterInfo("value", "The value", String.class, Collection.class);
        }
        return null;
    }

    @Override
    public Object evaluate(List<Object> parameters, JIPipeExpressionVariablesMap variables) {
        Object value = parameters.get(0);
        if (value instanceof Collection)
            return ((Collection<?>) value).size();
        if (value instanceof Map)
            return ((Map<?, ?>) value).size();
        else if (value instanceof String)
            return ((String) value).length();
        else
            throw new UnsupportedOperationException("Cannot get length of '" + value + "'");
    }
}
