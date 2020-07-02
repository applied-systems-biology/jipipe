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
public class DoubleAndDoublePair extends Pair<Double, Double> {

    /**
     * Creates a new instance
     */
    public DoubleAndDoublePair() {
        super(Double.class, Double.class);
    }

    /**
     * Creates a copy
     *
     * @param other the original
     */
    public DoubleAndDoublePair(DoubleAndDoublePair other) {
        super(other);
    }

    /**
     * A collection of multiple {@link DoubleAndDoublePair}
     */
    public static class List extends ListParameter<DoubleAndDoublePair> {
        /**
         * Creates a new instance
         */
        public List() {
            super(DoubleAndDoublePair.class);
        }

        /**
         * Creates a copy
         *
         * @param other the original
         */
        public List(List other) {
            super(DoubleAndDoublePair.class);
            for (DoubleAndDoublePair filter : other) {
                add(new DoubleAndDoublePair(filter));
            }
        }
    }
}
