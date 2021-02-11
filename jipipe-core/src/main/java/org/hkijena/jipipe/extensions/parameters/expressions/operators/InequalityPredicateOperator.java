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

import java.util.*;

@JIPipeDocumentation(name = "Inequality", description = "Returns TRUE if the left and right operands are unequal")
public class InequalityPredicateOperator extends GenericPredicateOperator {

    public InequalityPredicateOperator(String symbol) {
        super(symbol);
    }

    @Override
    public boolean evaluate(Collection<Object> left, Collection<Object> right) {
        return !Objects.equals(new ArrayList<>(left), new ArrayList<>(right));
    }

    @Override
    public boolean evaluate(Map<Object, Object> left, Map<Object, Object> right) {
        return !Objects.equals(new HashMap<>(left), new HashMap<>(right));
    }

    @Override
    public boolean evaluate(double left, double right) {
        return left != right;
    }

    @Override
    public boolean evaluate(String left, String right) {
        return !Objects.equals(left, right);
    }

    @Override
    public ParameterInfo getParameterInfo(int index) {
        switch (index) {
            case 0:
                return new ParameterInfo("value1", "", Boolean.class, Number.class, String.class, Collection.class);
            case 1:
                return new ParameterInfo("value2", "", Boolean.class, Number.class, String.class, Collection.class);
            default:
                return null;
        }
    }
}
