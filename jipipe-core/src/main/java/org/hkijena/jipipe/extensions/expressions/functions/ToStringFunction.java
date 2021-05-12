/*
 * Copyright by Zoltán Cseresnyés, Ruman Gerst
 *
 * Research Group Applied Systems Biology - Head: Prof. Dr. Marc Thilo Figge
 * https://www.leibniz-hki.de/en/applied-systems-biology.html
 * HKI-Center for Systems Biology of Infection
 * Leibniz Institute for Natural Product Research and Infection Biology - Hans Knöll Institute (HKI)
 * Adolf-Reichwein-Straße 23, 07745 Jena, Germany
 *
 * The project code is licensed under BSD 2-Clause.
 * See the LICENSE file provided with the code for the full license.
 */

package org.hkijena.jipipe.extensions.expressions.functions;

import org.hkijena.jipipe.api.JIPipeDocumentation;
import org.hkijena.jipipe.extensions.expressions.ExpressionFunction;
import org.hkijena.jipipe.extensions.expressions.ExpressionParameters;
import org.hkijena.jipipe.extensions.expressions.ParameterInfo;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

@JIPipeDocumentation(name = "Convert to string", description = "Converts the input to a string.")
public class ToStringFunction extends ExpressionFunction {

    public ToStringFunction() {
        super("TO_STRING", 1);
    }

    @Override
    public ParameterInfo getParameterInfo(int index) {
        if (index == 0) {
            return new ParameterInfo("value", "The value to convert");
        }
        return null;
    }

    @Override
    public Object evaluate(List<Object> parameters, ExpressionParameters variables) {
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

    private Object convert(Object item) {
        return "" + item;
    }
}
