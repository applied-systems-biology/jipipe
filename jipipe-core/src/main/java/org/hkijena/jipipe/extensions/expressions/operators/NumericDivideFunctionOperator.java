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

package org.hkijena.jipipe.extensions.expressions.operators;

import org.hkijena.jipipe.api.JIPipeDocumentation;
import org.hkijena.jipipe.extensions.expressions.ParameterInfo;

@JIPipeDocumentation(name = "Divide", description = "Divides the left operand by the right operand")
public class NumericDivideFunctionOperator extends NumericFunctionOperator {
    public NumericDivideFunctionOperator() {
        super("/", 7);
    }

    @Override
    public double evaluate(double left, double right) {
        return left / right;
    }

    @Override
    public ParameterInfo getParameterInfo(int index) {
        switch (index) {
            case 0:
                return new ParameterInfo("dividend", "", Number.class);
            case 1:
                return new ParameterInfo("divisor", "", Number.class);
            default:
                return null;
        }
    }
}
