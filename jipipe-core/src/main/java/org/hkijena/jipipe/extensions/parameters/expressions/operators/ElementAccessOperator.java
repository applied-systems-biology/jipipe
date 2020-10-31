package org.hkijena.jipipe.extensions.parameters.expressions.operators;

import com.google.common.collect.ImmutableList;
import org.hkijena.jipipe.api.JIPipeDocumentation;
import org.hkijena.jipipe.extensions.parameters.expressions.ExpressionOperator;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;

@JIPipeDocumentation(name = "Element access", description = "Access the array element at given position. The first position is zero. If a string is provided, the character at the given position is returned instead.")
public class ElementAccessOperator extends ExpressionOperator {

    public ElementAccessOperator() {
        super("@", 2, Associativity.RIGHT, 9);
    }

    @Override
    public Object evaluate(Iterator<Object> operands, Object evaluationContext) {
        Object array = operands.next();
        Object indices = operands.next();
        if(indices instanceof Number) {
            int index = ((Number) indices).intValue();
            if (array instanceof List) {
                return ((List<?>) array).get(index);
            } else if (array instanceof Collection) {
                return ImmutableList.copyOf((Collection<?>) array).get(index);
            } else if (array instanceof String) {
                return "" + array.toString().charAt(index);
            } else {
                throw new UnsupportedOperationException("Element access does not support target " + indices);
            }
        }
        else if(indices instanceof Collection) {
            List<Object> result = new ArrayList<>();
            if (array instanceof List) {
                List<?> targetList = (List<?>) array;
                for (Object indexItem : (Collection<?>) indices) {
                    int index = ((Number)indexItem).intValue();
                    result.add(targetList.get(index));
                }
            } else if (array instanceof Collection) {
                List<?> targetList = ImmutableList.copyOf((Collection<?>)array);
                for (Object indexItem : (Collection<?>) indices) {
                    int index = ((Number)indexItem).intValue();
                    result.add(targetList.get(index));
                }
            } else if (array instanceof String) {
                String s = array.toString();
                for (Object indexItem : (Collection<?>) indices) {
                    int index = ((Number)indexItem).intValue();
                    result.add("" + s.charAt(index));
                }
            } else {
                throw new UnsupportedOperationException("Element access does not support array " + array);
            }
            return result;
        }
        else {
            throw new UnsupportedOperationException("Element access does not support indices " + indices);
        }
    }
}
