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
import org.hkijena.jipipe.extensions.parameters.expressions.ParameterInfo;

import java.util.Iterator;

@JIPipeDocumentation(name = "Logical OR", description = "Returns TRUE if one of the operands is TRUE")
public class LogicalOrOperator extends ExpressionOperator {

    public LogicalOrOperator(String symbol) {
        super(symbol, 2, Associativity.LEFT, 1);
    }

    @Override
    public Object evaluate(Iterator<Object> operands, Object evaluationContext) {
        return (boolean)operands.next() || (boolean)operands.next();
    }

    @Override
    public ParameterInfo getParameterInfo(int index) {
        switch (index) {
            case 0:
                return new ParameterInfo("value1", "", Boolean.class);
            case 1:
                return new ParameterInfo("value2", "", Boolean.class);
            default:
                return null;
        }
    }
}
