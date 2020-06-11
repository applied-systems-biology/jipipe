package org.hkijena.acaq5.extensions.parameters.pairs;

import org.hkijena.acaq5.extensions.parameters.collections.ListParameter;
import org.hkijena.acaq5.extensions.parameters.predicates.StringPredicate;

import java.util.function.Function;
import java.util.function.Predicate;

/**
 * A parameter that renames a matching string into another string
 */
public class StringFilterAndStringPair extends Pair<StringPredicate, String> implements Predicate<String>, Function<String, String> {

    /**
     * Creates a new instance
     */
    public StringFilterAndStringPair() {
        super(StringPredicate.class, String.class);
    }

    /**
     * Creates a copy
     *
     * @param other the original
     */
    public StringFilterAndStringPair(StringFilterAndStringPair other) {
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

    /**
     * A collection of multiple {@link StringFilterAndStringPair}
     */
    public static class List extends ListParameter<StringFilterAndStringPair> {
        /**
         * Creates a new instance
         */
        public List() {
            super(StringFilterAndStringPair.class);
        }

        /**
         * Creates a copy
         *
         * @param other the original
         */
        public List(List other) {
            super(StringFilterAndStringPair.class);
            for (StringFilterAndStringPair filter : other) {
                add(new StringFilterAndStringPair(filter));
            }
        }
    }
}
