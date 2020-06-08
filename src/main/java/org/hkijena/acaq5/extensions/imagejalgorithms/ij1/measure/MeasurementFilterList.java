package org.hkijena.acaq5.extensions.imagejalgorithms.ij1.measure;

import org.hkijena.acaq5.extensions.parameters.collections.ListParameter;

/**
 * A list of {@link MeasurementFilter}
 */
public class MeasurementFilterList extends ListParameter<MeasurementFilter> {

    /**
     * Creates a new instance
     */
    public MeasurementFilterList() {
        super(MeasurementFilter.class);
    }

    /**
     * Creates a copy
     *
     * @param other the original
     */
    public MeasurementFilterList(MeasurementFilterList other) {
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
