package org.hkijena.acaq5.extensions.parameters.pairs;

import org.hkijena.acaq5.extensions.parameters.collections.KeyValuePairParameter;
import org.hkijena.acaq5.extensions.parameters.filters.StringFilter;

import java.util.function.Function;
import java.util.function.Predicate;

/**
 * A parameter that renames a matching string into another string
 */
public class StringFilterStringPair extends KeyValuePairParameter<StringFilter, String> implements Predicate<String>, Function<String, String> {

    /**
     * Creates a new instance
     */
    public StringFilterStringPair() {
        super(StringFilter.class, String.class);
    }

    /**
     * Creates a copy
     *
     * @param other the original
     */
    public StringFilterStringPair(StringFilterStringPair other) {
        super(other);
    }

    @Override
    public String apply(String s) {
        if (test(s)) {
            return getValue();
        } else {
            return s;
        }
    }

    @Override
    public boolean test(String s) {
        return getKey().test(s);
    }
}
