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

/**
 * A parameter that renames an integer into another integer
 */
public class StringAndStringPair extends Pair<String, String> {

    /**
     * Creates a new instance
     */
    public StringAndStringPair() {
        super(String.class, String.class);
    }

    /**
     * Creates a copy
     *
     * @param other the original
     */
    public StringAndStringPair(StringAndStringPair other) {
        super(other);
    }

    /**
     * A collection of multiple {@link StringAndStringPair}
     */
    public static class List extends ListParameter<StringAndStringPair> {
        /**
         * Creates a new instance
         */
        public List() {
            super(StringAndStringPair.class);
        }

        /**
         * Creates a copy
         *
         * @param other the original
         */
        public List(List other) {
            super(StringAndStringPair.class);
            for (StringAndStringPair filter : other) {
                add(new StringAndStringPair(filter));
            }
        }
    }
}
