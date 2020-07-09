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

package org.hkijena.pipelinej.extensions.parameters.pairs;

import org.hkijena.pipelinej.extensions.parameters.collections.ListParameter;
import org.hkijena.pipelinej.extensions.parameters.predicates.StringPredicate;

/**
 * A parameter that renames a matching string into another string
 */
public class StringFilterAndStringPair extends Pair<StringPredicate, String> {

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
