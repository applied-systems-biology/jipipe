package org.hkijena.acaq5.extensions.parameters.collections;

import org.hkijena.acaq5.extensions.parameters.pairs.StringFilterSortOrderPair;
import org.hkijena.acaq5.extensions.parameters.pairs.StringFilterStringPair;

/**
 * A collection of multiple {@link StringFilterSortOrderPair}
 */
public class StringFilterSortOrderPairList extends ListParameter<StringFilterSortOrderPair> {
    /**
     * Creates a new instance
     */
    public StringFilterSortOrderPairList() {
        super(StringFilterSortOrderPair.class);
    }

    /**
     * Creates a copy
     *
     * @param other the original
     */
    public StringFilterSortOrderPairList(StringFilterSortOrderPairList other) {
        super(StringFilterSortOrderPair.class);
        for (StringFilterSortOrderPair filter : other) {
            add(new StringFilterSortOrderPair(filter));
        }
    }
}
