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

package org.hkijena.jipipe.plugins.expressions.functions.convert;

import org.hkijena.jipipe.api.SetJIPipeDocumentation;
import org.hkijena.jipipe.plugins.expressions.ExpressionFunction;
import org.hkijena.jipipe.plugins.expressions.JIPipeExpressionVariablesMap;
import org.hkijena.jipipe.plugins.expressions.ParameterInfo;
import org.hkijena.jipipe.utils.StringUtils;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

@SetJIPipeDocumentation(name = "Convert to number (alternative)", description = "Converts the input to a number. Strings must be formatted correctly. Boolean values are converted to TRUE = 1 and FALSE = 0. Behaves exactly as TO_NUMBER.")
public class ToNumber2Function extends ExpressionFunction {

    public ToNumber2Function() {
        super("NUM", 1);
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
            return value;
        else if (value instanceof Boolean)
            return (boolean) value ? 1 : 0;
        else
            return StringUtils.parseDouble(StringUtils.nullToEmpty(value));
    }
}
