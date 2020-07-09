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

package org.hkijena.pipelinej.extensions.imagejalgorithms.ij1.measure;

import org.hkijena.pipelinej.extensions.parameters.collections.ListParameter;
import org.hkijena.pipelinej.extensions.parameters.pairs.Pair;
import org.hkijena.pipelinej.extensions.parameters.predicates.DoublePredicate;

/**
 * A key-value pair structure that allows to model filtering by measurements
 */
public class MeasurementFilter extends Pair<MeasurementColumn, DoublePredicate> {

    /**
     * Creates a new instance
     */
    public MeasurementFilter() {
        super(MeasurementColumn.class, DoublePredicate.class);
        this.setKey(MeasurementColumn.Area);
        this.setValue(new DoublePredicate());
    }

    /**
     * Creates a copy
     *
     * @param other the original
     */
    public MeasurementFilter(MeasurementFilter other) {
        super(other);
    }

    /**
     * A list of {@link MeasurementFilter}
     */
    public static class List extends ListParameter<MeasurementFilter> {

        /**
         * Creates a new instance
         */
        public List() {
            super(MeasurementFilter.class);
        }

        /**
         * Creates a copy
         *
         * @param other the original
         */
        public List(List other) {
            super(MeasurementFilter.class);
            for (MeasurementFilter measurementFilter : other) {
                add(new MeasurementFilter(measurementFilter));
            }
        }

        /**
         * Returns the integer value that describes which measurements to extract
         *
         * @return the integer value that describes which measurements to extract
         */
        public int getNativeMeasurementEnumValue() {
            int result = 0;
            for (MeasurementFilter measurementFilter : this) {
                result |= measurementFilter.getKey().getNativeValue();
            }
            return result;
        }
    }
}
