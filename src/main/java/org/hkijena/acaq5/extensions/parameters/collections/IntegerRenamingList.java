package org.hkijena.acaq5.extensions.parameters.collections;

import org.hkijena.acaq5.extensions.parameters.pairs.IntRangeIntegerPair;

/**
 * A collection of multiple {@link IntRangeIntegerPair}
 */
public class IntegerRenamingList extends ListParameter<IntRangeIntegerPair> {
    /**
     * Creates a new instance
     */
    public IntegerRenamingList() {
        super(IntRangeIntegerPair.class);
    }

    /**
     * Creates a copy
     *
     * @param other the original
     */
    public IntegerRenamingList(IntegerRenamingList other) {
        super(IntRangeIntegerPair.class);
        for (IntRangeIntegerPair filter : other) {
            add(new IntRangeIntegerPair(filter));
        }
    }
}
