package org.hkijena.acaq5.extensions.parameters.pairs;

import org.hkijena.acaq5.extensions.parameters.collections.ListParameter;
import org.hkijena.acaq5.extensions.parameters.generators.IntRangeStringParameter;

import java.util.function.Function;
import java.util.function.Predicate;

/**
 * A parameter that renames an integer into another integer
 */
public class IntRangeAndIntegerPair extends Pair<IntRangeStringParameter, Integer> implements Predicate<Integer>, Function<Integer, Integer> {

    /**
     * Creates a new instance
     */
    public IntRangeAndIntegerPair() {
        super(IntRangeStringParameter.class, Integer.class);
    }

    /**
     * Creates a copy
     *
     * @param other the original
     */
    public IntRangeAndIntegerPair(IntRangeAndIntegerPair other) {
        super(other);
    }

    @Override
    public Integer apply(Integer s) {
        if (test(s)) {
            return getValue();
        } else {
            return s;
        }
    }

    @Override
    public boolean test(Integer s) {
        return getKey().getIntegers().contains(s);
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
