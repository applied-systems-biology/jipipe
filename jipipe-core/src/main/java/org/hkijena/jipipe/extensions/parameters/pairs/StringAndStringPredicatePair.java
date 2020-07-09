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

package org.hkijena.jipipe.extensions.parameters.pairs;

import org.hkijena.jipipe.extensions.parameters.collections.ListParameter;
import org.hkijena.jipipe.extensions.parameters.predicates.StringPredicate;

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
