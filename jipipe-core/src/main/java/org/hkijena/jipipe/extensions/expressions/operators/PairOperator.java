package org.hkijena.jipipe.extensions.expressions.operators;

import org.hkijena.jipipe.api.JIPipeDocumentation;
import org.hkijena.jipipe.extensions.expressions.ExpressionOperator;
import org.hkijena.jipipe.extensions.expressions.ExpressionVariables;

import java.util.Arrays;
import java.util.Iterator;

/**
 * Operator that is used to make different statements
 * The return value is the right statement
 */
@JIPipeDocumentation(name = "Pair", description = "Creates an array of the left and the right operand (equivalent to PAIR(x, y))")
public class PairOperator extends ExpressionOperator {
    public PairOperator(int precedence) {
        super(":", 2, Associativity.LEFT, precedence);
    }

    @Override
    public Object evaluate(Iterator<Object> operands, ExpressionVariables variables) {
        Object o1 = operands.next();
        Object o2 = operands.next();
        return Arrays.asList(o1, o2);
    }
}
