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
import org.hkijena.jipipe.extensions.parameters.expressions.ParameterInfo;

@JIPipeDocumentation(name = "Multiply", description = "Multiplies the left and right operands")
public class NumericMultiplyFunctionOperator extends NumericFunctionOperator {
    public NumericMultiplyFunctionOperator() {
        super("*", 7);
    }

    @Override
    public double evaluate(double left, double right) {
        return left * right;
    }

    @Override
    public ParameterInfo getParameterInfo(int index) {
        switch (index) {
            case 0:
                return new ParameterInfo("factor1", "", Number.class);
            case 1:
                return new ParameterInfo("factor2", "", Number.class);
            default:
                return null;
        }
    }
}
