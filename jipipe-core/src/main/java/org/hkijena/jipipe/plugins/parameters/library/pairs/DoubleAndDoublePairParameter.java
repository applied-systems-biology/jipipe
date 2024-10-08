/*
 * Copyright by Zoltán Cseresnyés, Ruman Gerst
 *
 * Research Group Applied Systems Biology - Head: Prof. Dr. Marc Thilo Figge
 * https://www.leibniz-hki.de/en/applied-systems-biology.html
 * HKI-Center for Systems Biology of Infection
 * Leibniz Institute for Natural Product Research and Infection Biology - Hans Knöll Institute (HKI)
 * Adolf-Reichwein-Straße 23, 07745 Jena, Germany
 *
 * The project code is licensed under MIT.
 * See the LICENSE file provided with the code for the full license.
 */

package org.hkijena.jipipe.plugins.parameters.library.pairs;

import org.hkijena.jipipe.plugins.parameters.api.collections.ListParameter;
import org.hkijena.jipipe.plugins.parameters.api.pairs.PairParameter;

/**
 * A parameter that renames an integer into another integer
 */
public class DoubleAndDoublePairParameter extends PairParameter<Double, Double> {

    /**
     * Creates a new instance
     */
    public DoubleAndDoublePairParameter() {
        super(Double.class, Double.class);
    }

    /**
     * Creates a copy
     *
     * @param other the original
     */
    public DoubleAndDoublePairParameter(DoubleAndDoublePairParameter other) {
        super(other);
    }

    /**
     * A collection of multiple {@link DoubleAndDoublePairParameter}
     */
    public static class List extends ListParameter<DoubleAndDoublePairParameter> {
        /**
         * Creates a new instance
         */
        public List() {
            super(DoubleAndDoublePairParameter.class);
        }

        /**
         * Creates a copy
         *
         * @param other the original
         */
        public List(List other) {
            super(DoubleAndDoublePairParameter.class);
            for (DoubleAndDoublePairParameter filter : other) {
                add(new DoubleAndDoublePairParameter(filter));
            }
        }
    }
}
