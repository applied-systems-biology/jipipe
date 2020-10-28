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

package org.hkijena.jipipe.extensions.parameters.expressions;

import com.fathzer.soft.javaluator.Operator;

import java.util.Iterator;

public abstract class ExpressionOperator extends Operator {

    public ExpressionOperator(String symbol, int operandCount, Associativity associativity, int precedence) {
        super(symbol, operandCount, associativity, precedence);
    }

    /**
     * Evaluates the operator
     * @param operands the operands (contains booleans)
     * @param evaluationContext the evaluation context
     * @return the result (should be a boolean)
     */
    public abstract Object evaluate(Iterator<Object> operands, Object evaluationContext);
}
