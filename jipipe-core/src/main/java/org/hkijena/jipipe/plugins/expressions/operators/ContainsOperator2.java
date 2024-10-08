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

package org.hkijena.jipipe.plugins.expressions.operators;

import org.hkijena.jipipe.api.SetJIPipeDocumentation;
import org.hkijena.jipipe.plugins.expressions.ExpressionOperator;
import org.hkijena.jipipe.plugins.expressions.JIPipeExpressionVariablesMap;
import org.hkijena.jipipe.plugins.expressions.ParameterInfo;

import java.util.Collection;
import java.util.Iterator;

/**
 * Operator that tests if the left string is contained in the right string
 */
@SetJIPipeDocumentation(name = "Contains", description = "Returns true if the right operand is contained in the left operand. " +
        "The left operand can be a string, where the operator checks if the left string contains the right string. " +
        "If the left operand is an array, the operator checks if the right operand is in the array.")
public class ContainsOperator2 extends ExpressionOperator {

    public ContainsOperator2(int precedence) {
        super("CONTAINS", 2, Associativity.LEFT, precedence);
    }

    @Override
    public Object evaluate(Iterator<Object> operands, JIPipeExpressionVariablesMap variables) {
        String left = "" + operands.next();
        String right = "" + operands.next();
        return left.contains(right);
    }

    @Override
    public ParameterInfo getParameterInfo(int index) {
        switch (index) {
            case 0:
                return new ParameterInfo("array", "", String.class, Collection.class);
            case 1:
                return new ParameterInfo("item", "");
            default:
                return null;
        }
    }
}
