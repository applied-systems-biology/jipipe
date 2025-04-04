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

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Locale;

@SetJIPipeDocumentation(name = "Convert to boolean", description = "Converts the input to a boolean value. If the input is a number, all values >= 1 are converted to TRUE. Values < 1 are converted to FALSE. If the value is a string, then 'TRUE', 'T', 'ON', 'YES' and 'Y' are converted to TRUE (case-insensitive). Other values are converted to FALSE. " +
        "If the parameter is an array, all values are converted to booleans.")
public class ToBooleanFunction extends ExpressionFunction {

    public ToBooleanFunction() {
        super("TO_BOOLEAN", 1);
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
        if (value instanceof Boolean) {
            return value;
        } else if (value instanceof Number) {
            return ((Number) value).doubleValue() >= 1;
        } else {
            String str = ("" + value).toLowerCase(Locale.ROOT);
            return str.equals("true") || str.equals("t") || str.equals("on") || str.equals("yes") || str.equals("y");
        }
    }
}
