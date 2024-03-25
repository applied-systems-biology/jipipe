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

package org.hkijena.jipipe.plugins.expressions.operators;

import org.hkijena.jipipe.api.SetJIPipeDocumentation;
import org.hkijena.jipipe.plugins.expressions.ParameterInfo;

import java.util.*;

@SetJIPipeDocumentation(name = "Addition", description = "Adds two numbers together, concatenates two strings, or concatenates two arrays.")
public class AdditionFunctionOperator extends GenericOperator {
    public AdditionFunctionOperator(int precedence) {
        super("+", precedence);
    }

    @Override
    public Object evaluate(Map<Object, Object> left, Object right) {
        Map<Object, Object> result = new HashMap<>(left);
        if (right instanceof Map) {
            for (Map.Entry<?, ?> entry : ((Map<?, ?>) right).entrySet()) {
                result.put(entry.getKey(), entry.getValue());
            }
        } else if (right instanceof Collection) {
            Collection<?> pair = (Collection<?>) right;
            Iterator<?> iterator = pair.iterator();
            Object key = iterator.next();
            Object value = iterator.next();
            result.put(key, value);
        } else {
            throw new UnsupportedOperationException("Unsupported right operand for map: " + right);
        }
        return result;
    }

    @Override
    public Object evaluate(Collection<Object> left, Object right) {
        List<Object> result = new ArrayList<>();
        result.addAll(left);
        if (right instanceof Collection)
            result.addAll((Collection<Object>) right);
        else
            result.add(right);
        return result;
    }

    @Override
    public Object evaluate(double left, double right) {
        return left + right;
    }

    @Override
    public Object evaluate(String left, String right) {
        return left + right;
    }

    @Override
    public ParameterInfo getParameterInfo(int index) {
        switch (index) {
            case 0:
                return new ParameterInfo("value1", "", String.class, Number.class, Collection.class);
            case 1:
                return new ParameterInfo("value2", "", String.class, Number.class, Collection.class);
            default:
                return null;
        }
    }
}
