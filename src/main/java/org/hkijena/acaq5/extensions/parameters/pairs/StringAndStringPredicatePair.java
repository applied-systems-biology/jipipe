package org.hkijena.acaq5.extensions.parameters.pairs;

import org.hkijena.acaq5.extensions.parameters.collections.ListParameter;
import org.hkijena.acaq5.extensions.parameters.predicates.StringPredicate;

/**
 * A parameter that renames an integer into another integer
 */
public class StringAndStringPredicatePair extends Pair<String, StringPredicate> {

    /**
     * Creates a new instance
     */
    public StringAndStringPredicatePair() {
        super(String.class, StringPredicate.class);
    }

    /**
     * Creates a copy
     *
     * @param other the original
     */
    public StringAndStringPredicatePair(StringAndStringPredicatePair other) {
        super(other);
    }

    /**
     * A collection of multiple {@link StringAndStringPredicatePair}
     */
    public static class List extends ListParameter<StringAndStringPredicatePair> {
        /**
         * Creates a new instance
         */
        public List() {
            super(StringAndStringPredicatePair.class);
        }

        /**
         * Creates a copy
         *
         * @param other the original
         */
        public List(List other) {
            super(StringAndStringPredicatePair.class);
            for (StringAndStringPredicatePair filter : other) {
                add(new StringAndStringPredicatePair(filter));
            }
        }
    }
}
