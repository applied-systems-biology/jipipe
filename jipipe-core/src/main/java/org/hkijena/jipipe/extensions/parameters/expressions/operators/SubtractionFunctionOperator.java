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

package org.hkijena.jipipe.extensions.parameters.expressions.operators;

import org.hkijena.jipipe.api.JIPipeDocumentation;
import org.hkijena.jipipe.extensions.parameters.expressions.ParameterInfo;

import java.util.*;

@JIPipeDocumentation(name = "Subtract", description = "Subtracts the right operand from the left operand. If the left operand is an array, the right operand is removed from the array.")
public class SubtractionFunctionOperator extends GenericOperator {
    public SubtractionFunctionOperator() {
        super("-", 6);
    }

    @Override
    public Object evaluate(Collection<Object> left, Collection<Object> right) {
        List<Object> result = new ArrayList<>(left);
        result.removeAll(right);
        return result;
    }

    @Override
    public Object evaluate(double left, double right) {
        return left - right;
    }

    @Override
    public Object evaluate(String left, String right) {
        return false;
    }

    @Override
    public ParameterInfo getParameterInfo(int index) {
        switch (index) {
            case 0:
                return new ParameterInfo("value1", "", Number.class, Collection.class);
            case 1:
                return new ParameterInfo("value2", "", Number.class, Collection.class);
            default:
                return null;
        }
    }
}
