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
import org.hkijena.jipipe.plugins.expressions.ParameterInfo;

@SetJIPipeDocumentation(name = "Less than or equal", description = "Returns TRUE if the left operand is less than or equal to the right operand")
public class NumericLessThanOrEqualPredicateOperator extends NumericPredicateOperator {
    public NumericLessThanOrEqualPredicateOperator(int precedence) {
        super("<=", precedence);
    }

    @Override
    public boolean evaluate(double left, double right) {
        return left <= right;
    }

    @Override
    public ParameterInfo getParameterInfo(int index) {
        switch (index) {
            case 0:
                return new ParameterInfo("value1", "", Number.class);
            case 1:
                return new ParameterInfo("value2", "", Number.class);
            default:
                return null;
        }
    }
}
