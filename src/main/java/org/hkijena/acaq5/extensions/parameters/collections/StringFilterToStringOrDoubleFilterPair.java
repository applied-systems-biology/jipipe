package org.hkijena.acaq5.extensions.parameters.collections;

import org.hkijena.acaq5.extensions.parameters.filters.StringFilter;
import org.hkijena.acaq5.extensions.parameters.filters.StringOrDoubleFilter;

/**
 * {@link KeyValuePairParameter} from {@link StringFilter} to {@link StringOrDoubleFilter}
 */
public class StringFilterToStringOrDoubleFilterPair extends KeyValuePairParameter<StringFilter, StringOrDoubleFilter> {

    /**
     * Creates a new instance
     */
    public StringFilterToStringOrDoubleFilterPair() {
        super(StringFilter.class, StringOrDoubleFilter.class);
        setKey(new StringFilter());
        setValue(new StringOrDoubleFilter());
    }

    /**
     * Creates a copy
     *
     * @param other the original
     */
    public StringFilterToStringOrDoubleFilterPair(StringFilterToStringOrDoubleFilterPair other) {
        super(other);
    }
}