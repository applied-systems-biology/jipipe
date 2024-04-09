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

import java.util.*;

@SetJIPipeDocumentation(name = "Equality", description = "Returns TRUE if the left and right operands are equal")
public class EqualityPredicateOperator extends GenericPredicateOperator {
    public EqualityPredicateOperator(String symbol, int precedence) {
        super(symbol, precedence);
    }

    @Override
    public boolean evaluate(Collection<Object> left, Collection<Object> right) {
        return Objects.equals(new ArrayList<>(left), new ArrayList<>(right));
    }

    @Override
    public boolean evaluate(Map<Object, Object> left, Map<Object, Object> right) {
        return Objects.equals(new HashMap<>(left), new HashMap<>(right));
    }

    @Override
    public boolean evaluate(double left, double right) {
        return left == right;
    }

    @Override
    public boolean evaluate(String left, String right) {
        return Objects.equals(left, right);
    }

    @Override
    public ParameterInfo getParameterInfo(int index) {
        switch (index) {
            case 0:
                return new ParameterInfo("value1", "", Number.class, String.class, Collection.class);
            case 1:
                return new ParameterInfo("value2", "", Number.class, String.class, Collection.class);
            default:
                return null;
        }
    }
}
