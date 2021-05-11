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
public class StringAndStringPairParameter extends PairParameter<String, String> {

    /**
     * Creates a new instance
     */
    public StringAndStringPairParameter() {
        super(String.class, String.class);
    }

    public StringAndStringPairParameter(String key, String value) {
        super(String.class, String.class);
        this.setKey(key);
        this.setValue(value);
    }

    /**
     * Creates a copy
     *
     * @param other the original
     */
    public StringAndStringPairParameter(StringAndStringPairParameter other) {
        super(other);
    }

    /**
     * A collection of multiple {@link StringAndStringPairParameter}
     */
    public static class List extends ListParameter<StringAndStringPairParameter> {
        /**
         * Creates a new instance
         */
        public List() {
            super(StringAndStringPairParameter.class);
        }

        /**
         * Creates a copy
         *
         * @param other the original
         */
        public List(List other) {
            super(StringAndStringPairParameter.class);
            for (StringAndStringPairParameter filter : other) {
                add(new StringAndStringPairParameter(filter));
            }
        }
    }
}
