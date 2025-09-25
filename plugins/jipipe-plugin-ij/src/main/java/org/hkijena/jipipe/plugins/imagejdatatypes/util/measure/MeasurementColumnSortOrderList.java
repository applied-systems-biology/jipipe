package org.hkijena.jipipe.plugins.imagejdatatypes.util.measure;

import org.hkijena.jipipe.plugins.parameters.api.collections.JIPipeListParameter;

/**
 * A list of {@link MeasurementColumnSortOrder}
 */
public class MeasurementColumnSortOrderList extends JIPipeListParameter<MeasurementColumnSortOrder> {

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
