/*
 * Copyright by Zoltán Cseresnyés, Ruman Gerst
 *
 * Research Group Applied Systems Biology - Head: Prof. Dr. Marc Thilo Figge
 * https://www.leibniz-hki.de/en/applied-systems-biology.html
 * HKI-Center for Systems Biology of Infection
 * Leibniz Institute for Natural Product Research and Infection Biology - Hans Knöll Institute (HKI)
 * Adolf-Reichwein-Straße 23, 07745 Jena, Germany
 *
 * The project code is licensed under BSD 2-Clause.
 * See the LICENSE file provided with the code for the full license.
 */

package org.hkijena.acaq5.extensions.parameters.pairs;

import org.hkijena.acaq5.extensions.parameters.collections.ListParameter;
import org.hkijena.acaq5.extensions.parameters.predicates.StringOrDoublePredicate;
import org.hkijena.acaq5.extensions.parameters.predicates.StringPredicate;

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