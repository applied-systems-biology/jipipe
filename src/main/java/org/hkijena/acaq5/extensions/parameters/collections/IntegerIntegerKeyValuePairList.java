package org.hkijena.acaq5.extensions.parameters.collections;

import org.hkijena.acaq5.extensions.parameters.filters.IntegerIntegerKeyValuePair;

/**
 * A collection of multiple {@link IntegerIntegerKeyValuePair}
 */
public class IntegerIntegerKeyValuePairList extends ListParameter<IntegerIntegerKeyValuePair> {
    /**
     * Creates a new instance
     */
    public IntegerIntegerKeyValuePairList() {
        super(IntegerIntegerKeyValuePair.class);
    }

    /**
     * Creates a copy
     *
     * @param other the original
     */
    public IntegerIntegerKeyValuePairList(IntegerIntegerKeyValuePairList other) {
        super(IntegerIntegerKeyValuePair.class);
        for (IntegerIntegerKeyValuePair filter : other) {
            add(new IntegerIntegerKeyValuePair(filter));
        }
    }
}
