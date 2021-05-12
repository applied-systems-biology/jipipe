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

package org.hkijena.jipipe.extensions.parameters.pairs;

import org.hkijena.jipipe.extensions.parameters.collections.ListParameter;
import org.hkijena.jipipe.extensions.expressions.StringQueryExpression;
import org.hkijena.jipipe.extensions.parameters.util.SortOrder;

/**
 * A pair of {@link StringQueryExpression} and {@link SortOrder}
 */
public class StringQueryExpressionAndSortOrderPairParameter extends PairParameter<StringQueryExpression, SortOrder> {

    /**
     * Creates a new instance
     */
    public StringQueryExpressionAndSortOrderPairParameter() {
        super(StringQueryExpression.class, SortOrder.class);
        setKey(new StringQueryExpression());
        setValue(SortOrder.Ascending);
    }

    /**
     * Creates a copy
     *
     * @param other the original
     */
    public StringQueryExpressionAndSortOrderPairParameter(StringQueryExpressionAndSortOrderPairParameter other) {
        super(other);
    }

    /**
     * A collection of multiple {@link StringQueryExpressionAndSortOrderPairParameter}
     */
    public static class List extends ListParameter<StringQueryExpressionAndSortOrderPairParameter> {
        /**
         * Creates a new instance
         */
        public List() {
            super(StringQueryExpressionAndSortOrderPairParameter.class);
        }

        /**
         * Creates a copy
         *
         * @param other the original
         */
        public List(List other) {
            super(StringQueryExpressionAndSortOrderPairParameter.class);
            for (StringQueryExpressionAndSortOrderPairParameter filter : other) {
                add(new StringQueryExpressionAndSortOrderPairParameter(filter));
            }
        }
    }
}
