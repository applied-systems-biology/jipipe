package org.hkijena.acaq5.extensions.imagejalgorithms.ij1.measure;

import org.hkijena.acaq5.extensions.parameters.collections.KeyValuePairParameter;
import org.hkijena.acaq5.extensions.parameters.filters.DoubleFilter;

/**
 * A key-value pair structure that allows to model filtering by measurements
 */
public class MeasurementFilter extends KeyValuePairParameter<MeasurementColumn, DoubleFilter> {

    /**
     * Creates a new instance
     */
    public MeasurementFilter() {
        super(MeasurementColumn.class, DoubleFilter.class);
        this.setKey(MeasurementColumn.Area);
        this.setValue(new DoubleFilter());
    }

    /**
     * Creates a copy
     *
     * @param other the original
     */
    public MeasurementFilter(MeasurementFilter other) {
        super(other);
    }
}
