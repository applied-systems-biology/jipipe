package org.hkijena.acaq5.extensions.parameters.pairs;

import org.hkijena.acaq5.extensions.parameters.collections.ListParameter;
import org.hkijena.acaq5.extensions.parameters.predicates.StringPredicate;
import org.hkijena.acaq5.extensions.parameters.predicates.StringOrDoublePredicate;

/**
 * {@link Pair} from {@link StringPredicate} to {@link StringOrDoublePredicate}
 */
public class StringFilterAndStringOrDoubleFilterPair extends Pair<StringPredicate, StringOrDoublePredicate> {

    /**
     * Creates a new instance
     */
    public StringFilterAndStringOrDoubleFilterPair() {
        super(StringPredicate.class, StringOrDoublePredicate.class);
        setKey(new StringPredicate());
        setValue(new StringOrDoublePredicate());
    }

    /**
     * Creates a copy
     *
     * @param other the original
     */
    public StringFilterAndStringOrDoubleFilterPair(StringFilterAndStringOrDoubleFilterPair other) {
        super(other);
    }

    /**
     * A collection of multiple {@link StringFilterAndStringOrDoubleFilterPair}
     * The filters are connected via "OR"
     */
    public static class List extends ListParameter<StringFilterAndStringOrDoubleFilterPair> {
        /**
         * Creates a new instance
         */
        public List() {
            super(StringFilterAndStringOrDoubleFilterPair.class);

        }

        /**
         * Creates a copy
         *
         * @param other the original
         */
        public List(List other) {
            super(StringFilterAndStringOrDoubleFilterPair.class);
            for (StringFilterAndStringOrDoubleFilterPair filter : other) {
                add(new StringFilterAndStringOrDoubleFilterPair(filter));
            }
        }
    }
}