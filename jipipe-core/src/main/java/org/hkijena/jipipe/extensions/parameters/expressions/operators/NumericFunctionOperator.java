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

import java.util.Iterator;

public abstract class NumericFunctionOperator extends ExpressionOperator {

    public NumericFunctionOperator(String symbol, int precedence) {
        super(symbol, 2, Associativity.LEFT, precedence);
    }

    @Override
    public Object evaluate(Iterator<Object> operands, Object evaluationContext) {
        Object o1 = operands.next();
        Object o2 = operands.next();
        double left;
        double right;
        if(o1 instanceof Number)
            left = ((Number)o1).doubleValue();
        else if(o1 instanceof String)
            left = Double.parseDouble((String)o1);
        else if(o1 instanceof Boolean)
            left = (boolean)o1 ? 1 : 0;
        else
            throw new UnsupportedOperationException("Cannot convert " + o1.getClass() + " to a number!");

        if(o2 instanceof Number)
            right = ((Number)o2).doubleValue();
        else if(o2 instanceof String)
            right = Double.parseDouble((String)o2);
        else if(o2 instanceof Boolean)
            right = (boolean)o2 ? 1 : 0;
        else
            throw new UnsupportedOperationException("Cannot convert " + o2.getClass() + " to a number!");
        return evaluate(left, right);
    }

    public abstract double evaluate(double left, double right);
}
