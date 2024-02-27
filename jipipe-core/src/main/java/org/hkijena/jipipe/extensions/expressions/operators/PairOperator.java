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

package org.hkijena.jipipe.extensions.expressions.operators;

import org.hkijena.jipipe.api.SetJIPipeDocumentation;
import org.hkijena.jipipe.extensions.expressions.ExpressionOperator;
import org.hkijena.jipipe.extensions.expressions.JIPipeExpressionVariablesMap;

import java.util.Arrays;
import java.util.Iterator;

/**
 * Operator that is used to make different statements
 * The return value is the right statement
 */
@SetJIPipeDocumentation(name = "Pair", description = "Creates an array of the left and the right operand (equivalent to PAIR(x, y))")
public class PairOperator extends ExpressionOperator {
    public PairOperator(int precedence) {
        super(":", 2, Associativity.LEFT, precedence);
    }

    @Override
    public Object evaluate(Iterator<Object> operands, JIPipeExpressionVariablesMap variables) {
        Object o1 = operands.next();
        Object o2 = operands.next();
        return Arrays.asList(o1, o2);
    }
}
