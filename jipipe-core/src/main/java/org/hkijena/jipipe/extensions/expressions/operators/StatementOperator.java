package org.hkijena.jipipe.extensions.expressions.operators;

import org.hkijena.jipipe.api.JIPipeDocumentation;
import org.hkijena.jipipe.extensions.expressions.ExpressionOperator;
import org.hkijena.jipipe.extensions.expressions.JIPipeExpressionVariablesMap;

import java.util.Iterator;

/**
 * Operator that is used to make different statements
 * The return value is the right statement
 */
@JIPipeDocumentation(name = "Statement", description = "Returns the right parameter.")
public class StatementOperator extends ExpressionOperator {
    public StatementOperator(int precedence) {
        super(";", 2, Associativity.LEFT, precedence);
    }

    @Override
    public Object evaluate(Iterator<Object> operands, JIPipeExpressionVariablesMap variables) {
        Object o1 = operands.next();
        Object o2 = operands.next();
        return o2;
    }
}
