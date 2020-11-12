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

import org.hkijena.jipipe.extensions.parameters.expressions.ExpressionOperator;

import java.util.Collection;
import java.util.Iterator;
import java.util.Map;

public abstract class GenericPredicateOperator extends ExpressionOperator {

    public GenericPredicateOperator(String symbol) {
        super(symbol, 2, Associativity.LEFT, 5);
    }

    public GenericPredicateOperator(String symbol, int precedence) {
        super(symbol, 2, Associativity.LEFT, precedence);
    }

    @Override
    public Object evaluate(Iterator<Object> operands, Object evaluationContext) {
        Object o1 = operands.next();
        Object o2 = operands.next();

        if ((o1 instanceof Number || o1 instanceof Boolean) && (o2 instanceof Number || o2 instanceof Boolean)) {
            double left;
            double right;
            if (o1 instanceof Number)
                left = ((Number) o1).doubleValue();
            else
                left = (boolean) o1 ? 1 : 0;
            if (o2 instanceof Number)
                right = ((Number) o2).doubleValue();
            else
                right = (boolean) o2 ? 1 : 0;
            return evaluate(left, right);
        } else if (o1 instanceof Collection && o2 instanceof Collection) {
            return evaluate((Collection<Object>) o1, (Collection<Object>) o2);
        } else if (o1 instanceof Map && o2 instanceof Map) {
            return evaluate((Map<Object, Object>) o1, (Map<Object, Object>) o2);
        } else {
            return evaluate("" + o1, "" + o2);
        }
    }

    public abstract boolean evaluate(Collection<Object> left, Collection<Object> right);

    public abstract boolean evaluate(Map<Object, Object> left, Map<Object, Object> right);

    public abstract boolean evaluate(double left, double right);

    public abstract boolean evaluate(String left, String right);
}
