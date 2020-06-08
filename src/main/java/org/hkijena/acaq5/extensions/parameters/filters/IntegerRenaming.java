package org.hkijena.acaq5.extensions.parameters.filters;

import org.hkijena.acaq5.extensions.parameters.collections.KeyValuePairParameter;
import org.hkijena.acaq5.extensions.parameters.generators.IntRangeStringParameter;

import java.util.function.Function;
import java.util.function.Predicate;

/**
 * A parameter that renames an integer into another integer
 */
public class IntegerRenaming extends KeyValuePairParameter<IntRangeStringParameter, Integer> implements Predicate<Integer>, Function<Integer, Integer> {

    /**
     * Creates a new instance
     */
    public IntegerRenaming() {
        super(IntRangeStringParameter.class, Integer.class);
    }

    /**
     * Creates a copy
     *
     * @param other the original
     */
    public IntegerRenaming(IntegerRenaming other) {
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
}
