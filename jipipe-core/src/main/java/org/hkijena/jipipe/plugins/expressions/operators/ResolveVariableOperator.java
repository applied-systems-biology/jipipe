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

import java.util.Iterator;

@SetJIPipeDocumentation(name = "Resolve variable/Escape expression", description = "If followed by a string $\"variable name\", the value of the variable with the name 'variable name' is returned. " +
        "If used in conjunction with braces ${ }, all parts within the braces are turned into a string as-is.")
public class ResolveVariableOperator extends ExpressionOperator {
    public ResolveVariableOperator(int precedence) {
        super("$", 1, Associativity.RIGHT, precedence);
    }

    @Override
    public Object evaluate(Iterator<Object> operands, JIPipeExpressionVariablesMap variables) {
        String right = operands.next() + "";
        return ((JIPipeExpressionVariablesMap) variables).get(right);
    }

    @Override
    public ParameterInfo getParameterInfo(int index) {
        if (index == 0) {
            return new ParameterInfo("name", "The variable name", String.class);
        }
        return null;
    }
}
