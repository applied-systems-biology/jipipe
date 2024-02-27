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
import org.hkijena.jipipe.extensions.expressions.ParameterInfo;

import java.util.Iterator;

@SetJIPipeDocumentation(name = "Logical NOT", description = "Returns TRUE if the input is FALSE and vice versa.")
public class LogicalNotOperator extends ExpressionOperator {

    public LogicalNotOperator(String symbol, int precedence) {
        super(symbol, 1, Associativity.RIGHT, precedence);
    }

    @Override
    public Object evaluate(Iterator<Object> operands, JIPipeExpressionVariablesMap variables) {
        return !(boolean) operands.next();
    }

    @Override
    public ParameterInfo getParameterInfo(int index) {
        if (index == 0) {
            return new ParameterInfo("value", "", Boolean.class);
        }
        return null;
    }
}
