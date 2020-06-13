package org.hkijena.acaq5.extensions.parameters.pairs;

import org.hkijena.acaq5.extensions.parameters.collections.ListParameter;

/**
 * A parameter that renames an integer into another integer
 */
public class IntegerAndIntegerPair extends Pair<Integer, Integer> {

    /**
     * Creates a new instance
     */
    public IntegerAndIntegerPair() {
        super(Integer.class, Integer.class);
    }

    /**
     * Creates a copy
     *
     * @param other the original
     */
    public IntegerAndIntegerPair(IntegerAndIntegerPair other) {
        super(other);
    }

    /**
     * A collection of multiple {@link IntegerAndIntegerPair}
     */
    public static class List extends ListParameter<IntegerAndIntegerPair> {
        /**
         * Creates a new instance
         */
        public List() {
            super(IntegerAndIntegerPair.class);
        }

        /**
         * Creates a copy
         *
         * @param other the original
         */
        public List(List other) {
            super(IntegerAndIntegerPair.class);
            for (IntegerAndIntegerPair filter : other) {
                add(new IntegerAndIntegerPair(filter));
            }
        }
    }
}
