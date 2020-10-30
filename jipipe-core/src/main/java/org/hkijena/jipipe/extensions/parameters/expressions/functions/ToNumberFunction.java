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

package org.hkijena.jipipe.extensions.parameters.expressions.functions;

import com.fathzer.soft.javaluator.StaticVariableSet;
import org.apache.commons.lang3.math.NumberUtils;
import org.hkijena.jipipe.api.JIPipeDocumentation;
import org.hkijena.jipipe.extensions.parameters.expressions.ExpressionFunction;

import java.util.List;

@JIPipeDocumentation(name = "Convert to number", description = "Converts the input to a number. Strings must be formatted correctly. Boolean values are converted to TRUE = 1 and FALSE = 0")
public class ToNumberFunction extends ExpressionFunction {

    public ToNumberFunction() {
        super("TO_NUMBER", 1);
    }

    @Override
    public Object evaluate(List<Object> parameters, StaticVariableSet<Object> variables) {
        Object value = parameters.get(0);
        if(value instanceof  Number)
            return value;
        else if(value instanceof Boolean)
            return (boolean)value ? 1 : 0;
        else
            return NumberUtils.createDouble("" + value);
    }
}
