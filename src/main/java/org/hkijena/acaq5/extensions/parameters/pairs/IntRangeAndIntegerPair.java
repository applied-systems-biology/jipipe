package org.hkijena.acaq5.extensions.parameters.pairs;

import org.hkijena.acaq5.extensions.parameters.collections.ListParameter;
import org.hkijena.acaq5.extensions.parameters.generators.IntegerRange;

import java.util.function.Function;
import java.util.function.Predicate;

/**
 * A parameter that renames an integer into another integer
 */
public class IntRangeAndIntegerPair extends Pair<IntegerRange, Integer> {

    /**
     * Creates a new instance
     */
    public IntRangeAndIntegerPair() {
        super(IntegerRange.class, Integer.class);
    }

    /**
     * Creates a copy
     *
     * @param other the original
     */
    public IntRangeAndIntegerPair(IntRangeAndIntegerPair other) {
        super(other);
    }

    /**
     * A collection of multiple {@link IntRangeAndIntegerPair}
     */
    public static class List extends ListParameter<IntRangeAndIntegerPair> {
        /**
         * Creates a new instance
         */
        public List() {
            super(IntRangeAndIntegerPair.class);
        }

        /**
         * Creates a copy
         *
         * @param other the original
         */
        public List(List other) {
            super(IntRangeAndIntegerPair.class);
            for (IntRangeAndIntegerPair filter : other) {
                add(new IntRangeAndIntegerPair(filter));
            }
        }
    }
}
