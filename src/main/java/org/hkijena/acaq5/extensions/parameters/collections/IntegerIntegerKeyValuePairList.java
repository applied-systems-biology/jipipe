package org.hkijena.acaq5.extensions.parameters.collections;

import org.hkijena.acaq5.extensions.parameters.pairs.IntegerIntegerPair;

/**
 * A collection of multiple {@link IntegerIntegerPair}
 */
public class IntegerIntegerKeyValuePairList extends ListParameter<IntegerIntegerPair> {
    /**
     * Creates a new instance
     */
    public IntegerIntegerKeyValuePairList() {
        super(IntegerIntegerPair.class);
    }

    /**
     * Creates a copy
     *
     * @param other the original
     */
    public IntegerIntegerKeyValuePairList(IntegerIntegerKeyValuePairList other) {
        super(IntegerIntegerPair.class);
        for (IntegerIntegerPair filter : other) {
            add(new IntegerIntegerPair(filter));
        }
    }
}
