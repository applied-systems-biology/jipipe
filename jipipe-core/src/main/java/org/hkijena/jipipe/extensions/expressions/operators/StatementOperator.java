package org.hkijena.jipipe.extensions.expressions.operators;

import org.hkijena.jipipe.api.JIPipeDocumentation;
import org.hkijena.jipipe.extensions.expressions.ExpressionOperator;
import org.hkijena.jipipe.extensions.expressions.ExpressionVariables;

import java.util.Iterator;

/**
 * Operator that is used to make different statements
 * The return value is the right statement
 */
@JIPipeDocumentation(name = "Statement", description = "Returns the right parameter.")
public class StatementOperator extends ExpressionOperator {
    public StatementOperator() {
        super(";", 2, Associativity.LEFT, -99999);
    }

    @Override
    public Object evaluate(Iterator<Object> operands, ExpressionVariables variables) {
        Object o1 = operands.next();
        Object o2 = operands.next();
        return o2;
    }
}
