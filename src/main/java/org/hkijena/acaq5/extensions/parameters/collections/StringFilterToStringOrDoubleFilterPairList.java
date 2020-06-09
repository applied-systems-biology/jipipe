package org.hkijena.acaq5.extensions.parameters.collections;

import org.hkijena.acaq5.extensions.parameters.filters.StringFilter;

import java.util.function.Predicate;

/**
 * A collection of multiple {@link StringFilterToStringOrDoubleFilterPair}
 * The filters are connected via "OR"
 */
public class StringFilterToStringOrDoubleFilterPairList extends ListParameter<StringFilterToStringOrDoubleFilterPair> {
    /**
     * Creates a new instance
     */
    public StringFilterToStringOrDoubleFilterPairList() {
        super(StringFilterToStringOrDoubleFilterPair.class);

    }

    /**
     * Creates a copy
     *
     * @param other the original
     */
    public StringFilterToStringOrDoubleFilterPairList(StringFilterToStringOrDoubleFilterPairList other) {
        super(StringFilterToStringOrDoubleFilterPair.class);
        for (StringFilterToStringOrDoubleFilterPair filter : other) {
            add(new StringFilterToStringOrDoubleFilterPair(filter));
        }
    }
}
