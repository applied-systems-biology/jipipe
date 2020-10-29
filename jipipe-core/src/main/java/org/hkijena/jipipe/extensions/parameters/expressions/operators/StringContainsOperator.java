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

import org.hkijena.jipipe.api.JIPipeDocumentation;
import org.hkijena.jipipe.extensions.parameters.expressions.ExpressionOperator;

import java.util.Iterator;

/**
 * Operator that tests if the left string is contained in the right string
 */
@JIPipeDocumentation(name = "String contains", description = "Returns true if the left operand is contained in the right operand")
public class StringContainsOperator extends ExpressionOperator {

    public StringContainsOperator() {
        super("IN", 2, Associativity.LEFT, 6);
    }

    @Override
    public Object evaluate(Iterator<Object> operands, Object evaluationContext) {
        String left = "" + operands.next();
        String right = "" + operands.next();
        return right.contains(left);
    }
}
