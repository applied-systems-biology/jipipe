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

package org.hkijena.jipipe.extensions.expressions.functions.convert;

import org.apache.commons.lang3.math.NumberUtils;
import org.hkijena.jipipe.api.SetJIPipeDocumentation;
import org.hkijena.jipipe.extensions.expressions.ExpressionFunction;
import org.hkijena.jipipe.extensions.expressions.JIPipeExpressionVariablesMap;
import org.hkijena.jipipe.extensions.expressions.ParameterInfo;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

@SetJIPipeDocumentation(name = "Convert to integer", description = "Converts the input to an integral number. Strings must be formatted correctly. Boolean values are converted to TRUE = 1 and FALSE = 0")
public class ToIntegerFunction extends ExpressionFunction {

    public ToIntegerFunction() {
        super("TO_INTEGER", 1);
    }

    @Override
    public ParameterInfo getParameterInfo(int index) {
        if (index == 0) {
            return new ParameterInfo("value", "The value to convert");
        }
        return null;
    }

    @Override
    public Object evaluate(List<Object> parameters, JIPipeExpressionVariablesMap variables) {
        Object value = parameters.get(0);
        if (value instanceof Collection) {
            List<Object> result = new ArrayList<>();
            for (Object item : ((Collection<?>) value)) {
                result.add(convert(item));
            }
            return result;
        } else {
            return convert(value);
        }
    }

    public Object convert(Object value) {
        if (value instanceof Number)
            return ((Number) value).intValue();
        else if (value instanceof Boolean)
            return (boolean) value ? 1 : 0;
        else
            return NumberUtils.createDouble("" + value).intValue();
    }
}
