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

import java.util.ArrayList;
import java.util.Collection;
import java.util.Objects;

@JIPipeDocumentation(name = "Inequality", description = "Returns TRUE if the left and right operands are unequal")
public class InequalityPredicateTextOperator extends GenericPredicateOperator {

    public InequalityPredicateTextOperator() {
        super("UNEQUAL");
    }

    @Override
    public boolean evaluate(Collection<Object> left, Collection<Object> right) {
        return Objects.equals(new ArrayList<>(left), new ArrayList<>(right));
    }

    @Override
    public boolean evaluate(double left, double right) {
        return left != right;
    }

    @Override
    public boolean evaluate(String left, String right) {
        return !Objects.equals(left, right);
    }
}
