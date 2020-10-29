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

@JIPipeDocumentation(name = "Greater than", description = "Returns TRUE if the left operand is greater than the right operand")
public class NumericGreaterThanPredicateOperator extends NumericPredicateOperator {
    public NumericGreaterThanPredicateOperator() {
        super(">");
    }

    @Override
    public boolean evaluate(double left, double right) {
        return left > right;
    }
}
