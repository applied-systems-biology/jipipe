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

import org.hkijena.jipipe.extensions.expressions.ExpressionOperator;
import org.hkijena.jipipe.extensions.expressions.JIPipeExpressionVariablesMap;

import java.util.Iterator;

public abstract class NumericOrStringFunctionOperator extends ExpressionOperator {

    public NumericOrStringFunctionOperator(String symbol, int precedence) {
        super(symbol, 2, Associativity.LEFT, precedence);
    }

    @Override
    public Object evaluate(Iterator<Object> operands, JIPipeExpressionVariablesMap variables) {
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
        } else {
            return evaluate("" + o1, "" + o2);
        }
    }

    public abstract double evaluate(double left, double right);

    public abstract String evaluate(String left, String right);
}
