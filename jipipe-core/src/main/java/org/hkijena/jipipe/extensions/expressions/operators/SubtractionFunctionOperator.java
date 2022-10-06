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

package org.hkijena.jipipe.extensions.expressions.operators;

import org.apache.commons.lang3.math.NumberUtils;
import org.hkijena.jipipe.api.JIPipeDocumentation;
import org.hkijena.jipipe.extensions.expressions.ParameterInfo;

import java.util.*;

@JIPipeDocumentation(name = "Subtract", description = "Subtracts the right operand from the left operand. If the left operand is an array, the right operand is removed from the array. If the left operand is a map, the right operand is removed from the map's keys.")
public class SubtractionFunctionOperator extends GenericOperator {
    public SubtractionFunctionOperator() {
        super("-", 6);
    }

    @Override
    public Object evaluate(Map<Object, Object> left, Object right) {
        Map<Object, Object> result = new HashMap<>(left);
        if (right instanceof Map) {
            for (Map.Entry<?, ?> entry : ((Map<?, ?>) right).entrySet()) {
                result.remove(entry.getKey());
            }
        } else if (right instanceof Collection) {
            Collection<?> pair = (Collection<?>) right;
            Iterator<?> iterator = pair.iterator();
            Object key = iterator.next();
            result.remove(key);
        } else {
            result.remove(right);
        }
        return result;
    }

    @Override
    public Object evaluate(Collection<Object> left, Object right) {
        List<Object> result = new ArrayList<>(left);
        if (right instanceof Collection)
            result.removeAll((Collection<?>) right);
        else
            result.remove(right);
        return result;
    }

    @Override
    public Object evaluate(double left, double right) {
        return left - right;
    }

    @Override
    public Object evaluate(String left, String right) {
        if (NumberUtils.isCreatable(left) && NumberUtils.isCreatable(right)) {
            return NumberUtils.createDouble(left) - NumberUtils.createDouble(right);
        } else {
            throw new IllegalArgumentException("Cannot subtract strings '" + left + "' and '" + right + "'!");
        }
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
