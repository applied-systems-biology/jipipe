package org.hkijena.acaq5.extensions.parameters.pairs;

import org.hkijena.acaq5.extensions.imagejalgorithms.ij1.measure.SortOrder;
import org.hkijena.acaq5.extensions.parameters.collections.KeyValuePairParameter;
import org.hkijena.acaq5.extensions.parameters.filters.StringFilter;

import java.util.function.Function;
import java.util.function.Predicate;

/**
 * A pair of {@link StringFilter} and {@link org.hkijena.acaq5.extensions.imagejalgorithms.ij1.measure.SortOrder}
 */
public class StringFilterSortOrderPair extends KeyValuePairParameter<StringFilter, SortOrder>{

    /**
     * Creates a new instance
     */
    public StringFilterSortOrderPair() {
        super(StringFilter.class, SortOrder.class);
        setKey(new StringFilter());
        setValue(SortOrder.Ascending);
    }

    /**
     * Creates a copy
     *
     * @param other the original
     */
    public StringFilterSortOrderPair(StringFilterSortOrderPair other) {
        super(other);
    }
}
