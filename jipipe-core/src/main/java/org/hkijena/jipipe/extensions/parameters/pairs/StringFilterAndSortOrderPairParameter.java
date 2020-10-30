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
import org.hkijena.jipipe.extensions.parameters.predicates.StringPredicate;
import org.hkijena.jipipe.extensions.parameters.util.SortOrder;

/**
 * A pair of {@link StringPredicate} and {@link SortOrder}
 */
public class StringFilterAndSortOrderPairParameter extends PairParameter<StringPredicate, SortOrder> {

    /**
     * Creates a new instance
     */
    public StringFilterAndSortOrderPairParameter() {
        super(StringPredicate.class, SortOrder.class);
        setKey(new StringPredicate());
        setValue(SortOrder.Ascending);
    }

    /**
     * Creates a copy
     *
     * @param other the original
     */
    public StringFilterAndSortOrderPairParameter(StringFilterAndSortOrderPairParameter other) {
        super(other);
    }

    /**
     * A collection of multiple {@link StringFilterAndSortOrderPairParameter}
     */
    public static class List extends ListParameter<StringFilterAndSortOrderPairParameter> {
        /**
         * Creates a new instance
         */
        public List() {
            super(StringFilterAndSortOrderPairParameter.class);
        }

        /**
         * Creates a copy
         *
         * @param other the original
         */
        public List(List other) {
            super(StringFilterAndSortOrderPairParameter.class);
            for (StringFilterAndSortOrderPairParameter filter : other) {
                add(new StringFilterAndSortOrderPairParameter(filter));
            }
        }
    }
}
