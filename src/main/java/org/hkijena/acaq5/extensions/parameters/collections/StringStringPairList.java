package org.hkijena.acaq5.extensions.parameters.collections;

import org.hkijena.acaq5.extensions.parameters.pairs.StringFilterStringPair;

/**
 * A collection of multiple {@link StringFilterStringPair}
 */
public class StringStringPairList extends ListParameter<StringFilterStringPair> {
    /**
     * Creates a new instance
     */
    public StringStringPairList() {
        super(StringFilterStringPair.class);
    }

    /**
     * Creates a copy
     *
     * @param other the original
     */
    public StringStringPairList(StringStringPairList other) {
        super(StringFilterStringPair.class);
        for (StringFilterStringPair filter : other) {
            add(new StringFilterStringPair(filter));
        }
    }
}
