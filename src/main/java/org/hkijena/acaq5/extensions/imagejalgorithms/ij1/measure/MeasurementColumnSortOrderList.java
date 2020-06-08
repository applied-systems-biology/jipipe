package org.hkijena.acaq5.extensions.imagejalgorithms.ij1.measure;

import org.hkijena.acaq5.extensions.parameters.collections.ListParameter;

/**
 * A list of {@link MeasurementColumnSortOrder}
 */
public class MeasurementColumnSortOrderList extends ListParameter<MeasurementColumnSortOrder> {

    /**
     * Creates a new instance
     */
    public MeasurementColumnSortOrderList() {
        super(MeasurementColumnSortOrder.class);
    }

    /**
     * Creates a copy
     *
     * @param other the original
     */
    public MeasurementColumnSortOrderList(MeasurementColumnSortOrderList other) {
        super(MeasurementColumnSortOrder.class);
        for (MeasurementColumnSortOrder measurementFilter : other) {
            add(new MeasurementColumnSortOrder(measurementFilter));
        }
    }

    /**
     * Returns the integer value that describes which measurements to extract
     *
     * @return the integer value that describes which measurements to extract
     */
    public int getNativeMeasurementEnumValue() {
        int result = 0;
        for (MeasurementColumnSortOrder measurementFilter : this) {
            result |= measurementFilter.getKey().getNativeValue();
        }
        return result;
    }
}
