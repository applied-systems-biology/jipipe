package org.hkijena.acaq5.extensions.parameters.filters;

import org.hkijena.acaq5.extensions.parameters.collections.KeyValuePairParameter;

import java.util.Objects;
import java.util.function.Function;
import java.util.function.Predicate;

/**
 * A parameter that renames an integer into another integer
 */
public class IntegerIntegerKeyValuePair extends KeyValuePairParameter<Integer, Integer> implements Predicate<Integer>, Function<Integer, Integer> {

    /**
     * Creates a new instance
     */
    public IntegerIntegerKeyValuePair() {
        super(Integer.class, Integer.class);
    }

    /**
     * Creates a copy
     *
     * @param other the original
     */
    public IntegerIntegerKeyValuePair(IntegerIntegerKeyValuePair other) {
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
        return Objects.equals(getKey(), s);
    }
}
