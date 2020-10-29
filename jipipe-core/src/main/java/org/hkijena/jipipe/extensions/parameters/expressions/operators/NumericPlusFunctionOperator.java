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

@JIPipeDocumentation(name = "Addition", description = "Adds two numbers together or concatenates two strings.")
public class NumericPlusFunctionOperator extends NumericOrStringFunctionOperator {
    public NumericPlusFunctionOperator() {
        super("+", 6);
    }

    @Override
    public double evaluate(double left, double right) {
        return left + right;
    }

    @Override
    public String evaluate(String left, String right) {
        return left + right;
    }
}
