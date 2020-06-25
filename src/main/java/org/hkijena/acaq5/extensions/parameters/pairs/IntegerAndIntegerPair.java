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

/**
 * A parameter that renames an integer into another integer
 */
public class IntegerAndIntegerPair extends Pair<Integer, Integer> {

    /**
     * Creates a new instance
     */
    public IntegerAndIntegerPair() {
        super(Integer.class, Integer.class);
    }

    /**
     * Creates a copy
     *
     * @param other the original
     */
    public IntegerAndIntegerPair(IntegerAndIntegerPair other) {
        super(other);
    }

    /**
     * A collection of multiple {@link IntegerAndIntegerPair}
     */
    public static class List extends ListParameter<IntegerAndIntegerPair> {
        /**
         * Creates a new instance
         */
        public List() {
            super(IntegerAndIntegerPair.class);
        }

        /**
         * Creates a copy
         *
         * @param other the original
         */
        public List(List other) {
            super(IntegerAndIntegerPair.class);
            for (IntegerAndIntegerPair filter : other) {
                add(new IntegerAndIntegerPair(filter));
            }
        }
    }
}
