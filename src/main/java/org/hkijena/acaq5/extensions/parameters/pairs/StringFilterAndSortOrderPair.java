package org.hkijena.acaq5.extensions.parameters.pairs;

import org.hkijena.acaq5.extensions.imagejalgorithms.ij1.measure.SortOrder;
import org.hkijena.acaq5.extensions.parameters.collections.ListParameter;
import org.hkijena.acaq5.extensions.parameters.predicates.StringPredicate;

/**
 * A pair of {@link StringPredicate} and {@link org.hkijena.acaq5.extensions.imagejalgorithms.ij1.measure.SortOrder}
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
