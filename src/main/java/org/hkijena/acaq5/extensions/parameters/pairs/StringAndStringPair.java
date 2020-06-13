package org.hkijena.acaq5.extensions.parameters.pairs;

import org.hkijena.acaq5.extensions.parameters.collections.ListParameter;

/**
 * A parameter that renames an integer into another integer
 */
public class StringAndStringPair extends Pair<String, String> {

    /**
     * Creates a new instance
     */
    public StringAndStringPair() {
        super(String.class, String.class);
    }

    /**
     * Creates a copy
     *
     * @param other the original
     */
    public StringAndStringPair(StringAndStringPair other) {
        super(other);
    }

    /**
     * A collection of multiple {@link StringAndStringPair}
     */
    public static class List extends ListParameter<StringAndStringPair> {
        /**
         * Creates a new instance
         */
        public List() {
            super(StringAndStringPair.class);
        }

        /**
         * Creates a copy
         *
         * @param other the original
         */
        public List(List other) {
            super(StringAndStringPair.class);
            for (StringAndStringPair filter : other) {
                add(new StringAndStringPair(filter));
            }
        }
    }
}
