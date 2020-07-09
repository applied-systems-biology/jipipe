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

package org.hkijena.pipelinej.extensions.parameters.pairs;

import org.hkijena.pipelinej.extensions.parameters.collections.ListParameter;
import org.hkijena.pipelinej.extensions.parameters.predicates.StringPredicate;
import org.hkijena.pipelinej.extensions.parameters.util.SortOrder;

/**
 * A pair of {@link StringPredicate} and {@link SortOrder}
 */
public class StringFilterAndSortOrderPair extends Pair<StringPredicate, SortOrder> {

    /**
     * Creates a new instance
     */
    public StringFilterAndSortOrderPair() {
        super(StringPredicate.class, SortOrder.class);
        setKey(new StringPredicate());
        setValue(SortOrder.Ascending);
    }

    /**
     * Creates a copy
     *
     * @param other the original
     */
    public StringFilterAndSortOrderPair(StringFilterAndSortOrderPair other) {
        super(other);
    }

    /**
     * A collection of multiple {@link StringFilterAndSortOrderPair}
     */
    public static class List extends ListParameter<StringFilterAndSortOrderPair> {
        /**
         * Creates a new instance
         */
        public List() {
            super(StringFilterAndSortOrderPair.class);
        }

        /**
         * Creates a copy
         *
         * @param other the original
         */
        public List(List other) {
            super(StringFilterAndSortOrderPair.class);
            for (StringFilterAndSortOrderPair filter : other) {
                add(new StringFilterAndSortOrderPair(filter));
            }
        }
    }
}
