package org.hkijena.jipipe.extensions.parameters.expressions.operators;

import com.google.common.collect.ImmutableList;
import org.hkijena.jipipe.api.JIPipeDocumentation;
import org.hkijena.jipipe.extensions.parameters.expressions.ExpressionOperator;
import org.hkijena.jipipe.extensions.parameters.expressions.ParameterInfo;

import java.util.*;

@JIPipeDocumentation(name = "Element access", description = "Access the array element at given position. The first position is zero. If a string is provided, the character at the given position is returned instead.")
public class ElementAccessOperator extends ExpressionOperator {

    public ElementAccessOperator(String symbol) {
        super(symbol, 2, Associativity.RIGHT, 9);
    }

    @Override
    public ParameterInfo getParameterInfo(int index) {
        switch (index) {
            case 0:
                return new ParameterInfo("array", "The array", String.class, Collection.class);
            case 1:
                return new ParameterInfo("index", "The indices", Number.class, Collection.class);
            default:
                return null;
        }
    }

    @Override
    public Object evaluate(Iterator<Object> operands, Object evaluationContext) {
        Object array = operands.next();
        Object indices = operands.next();
        if (indices instanceof Number) {
            int index = ((Number) indices).intValue();
            if (array instanceof Map) {
                return ((Map<?, ?>) array).get(index);
            } else if (array instanceof List) {
                int idx = index;
                while (idx < 0) {
                    idx += ((List<?>) array).size();
                }
                return ((List<?>) array).get(idx);
            } else if (array instanceof Collection) {
                ImmutableList<?> asList = ImmutableList.copyOf((Collection<?>) array);
                int idx = index;
                while (idx < 0) {
                    idx += asList.size();
                }
                return asList.get(idx);
            } else if (array instanceof String) {
                String s = array.toString();
                int idx = index;
                while (idx < 0) {
                    idx += s.length();
                }
                return "" + s.charAt(index);
            } else {
                throw new UnsupportedOperationException("Element access does not support target " + indices);
            }
        } else if (indices instanceof Collection) {
            List<Object> result = new ArrayList<>();
            if (array instanceof Map) {
                Map<?, ?> targetMap = (Map<?, ?>) array;
                for (Object indexItem : (Collection<?>) indices) {
                    result.add(targetMap.get(indexItem));
                }
            } else if (array instanceof List) {
                List<?> targetList = (List<?>) array;
                for (Object indexItem : (Collection<?>) indices) {
                    int index = ((Number) indexItem).intValue();
                    while (index < 0) {
                        index += targetList.size();
                    }
                    result.add(targetList.get(index));
                }
            } else if (array instanceof Collection) {
                List<?> targetList = ImmutableList.copyOf((Collection<?>) array);
                for (Object indexItem : (Collection<?>) indices) {
                    int index = ((Number) indexItem).intValue();
                    while (index < 0) {
                        index += targetList.size();
                    }
                    result.add(targetList.get(index));
                }
            } else if (array instanceof String) {
                String s = array.toString();
                for (Object indexItem : (Collection<?>) indices) {
                    int index = ((Number) indexItem).intValue();
                    while (index < 0) {
                        index += s.length();
                    }
                    result.add("" + s.charAt(index));
                }
            } else {
                throw new UnsupportedOperationException("Element access does not support array " + array);
            }
            return result;
        } else if (array instanceof Map) {
            return ((Map<?, ?>) array).get(indices);
        } else {
            throw new UnsupportedOperationException("Element access does not support indices " + indices);
        }
    }
}
