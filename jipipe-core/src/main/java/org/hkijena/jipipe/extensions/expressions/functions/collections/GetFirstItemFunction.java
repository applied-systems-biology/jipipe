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

package org.hkijena.jipipe.extensions.expressions.functions.collections;

import org.hkijena.jipipe.api.SetJIPipeDocumentation;
import org.hkijena.jipipe.extensions.expressions.ExpressionFunction;
import org.hkijena.jipipe.extensions.expressions.JIPipeExpressionVariablesMap;
import org.hkijena.jipipe.extensions.expressions.ParameterInfo;

import java.util.Collection;
import java.util.List;

@SetJIPipeDocumentation(name = "Gets first item", description = "Gets the first item of an array or the first character of a string")
public class GetFirstItemFunction extends ExpressionFunction {

    public GetFirstItemFunction() {
        super("FIRST_OF", 1);
    }

    @Override
    public ParameterInfo getParameterInfo(int index) {
        switch (index) {
            case 0:
                return new ParameterInfo("array", "An array or a string", String.class, Collection.class);
            default:
                return null;
        }
    }

    @Override
    public Object evaluate(List<Object> parameters, JIPipeExpressionVariablesMap variables) {
        Object target = parameters.get(0);
        if (target instanceof Collection) {
            return ((Collection<?>) target).iterator().next();
        } else if (target instanceof String) {
            return "" + target.toString().charAt(0);
        } else {
            throw new UnsupportedOperationException("Element access does not support " + target);
        }
    }
}
