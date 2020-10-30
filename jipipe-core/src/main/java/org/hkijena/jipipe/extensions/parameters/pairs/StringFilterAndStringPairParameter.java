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
 * A parameter that renames a matching string into another string
 */
public class StringFilterAndStringPairParameter extends PairParameter<StringPredicate, String> {

    /**
     * Creates a new instance
     */
    public StringFilterAndStringPairParameter() {
        super(StringPredicate.class, String.class);
    }

    /**
     * Creates a copy
     *
     * @param other the original
     */
    public StringFilterAndStringPairParameter(StringFilterAndStringPairParameter other) {
        super(other);
    }

    /**
     * A collection of multiple {@link StringFilterAndStringPairParameter}
     */
    public static class List extends ListParameter<StringFilterAndStringPairParameter> {
        /**
         * Creates a new instance
         */
        public List() {
            super(StringFilterAndStringPairParameter.class);
        }

        /**
         * Creates a copy
         *
         * @param other the original
         */
        public List(List other) {
            super(StringFilterAndStringPairParameter.class);
            for (StringFilterAndStringPairParameter filter : other) {
                add(new StringFilterAndStringPairParameter(filter));
            }
        }
    }
}
