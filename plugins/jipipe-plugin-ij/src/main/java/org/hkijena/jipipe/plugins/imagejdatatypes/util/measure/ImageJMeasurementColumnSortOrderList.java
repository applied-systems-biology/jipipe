package org.hkijena.jipipe.plugins.imagejdatatypes.util.measure;

import org.hkijena.jipipe.plugins.parameters.api.collections.JIPipeListParameter;

/**
 * A list of {@link ImageJMeasurementColumnSortOrder}
 */
public class ImageJMeasurementColumnSortOrderList extends JIPipeListParameter<ImageJMeasurementColumnSortOrder> {

    /**
     * Creates a new instance
     */
    public ImageJMeasurementColumnSortOrderList() {
        super(ImageJMeasurementColumnSortOrder.class);
    }

    /**
     * Creates a copy
     *
     * @param other the original
     */
    public ImageJMeasurementColumnSortOrderList(ImageJMeasurementColumnSortOrderList other) {
        super(ImageJMeasurementColumnSortOrder.class);
        for (ImageJMeasurementColumnSortOrder measurementFilter : other) {
            add(new ImageJMeasurementColumnSortOrder(measurementFilter));
        }
    }

    /**
     * Returns the integer value that describes which measurements to extract
     *
     * @return the integer value that describes which measurements to extract
     */
    public int getNativeMeasurementEnumValue() {
        int result = 0;
        for (ImageJMeasurementColumnSortOrder measurementFilter : this) {
            result |= measurementFilter.getKey().getNativeValue();
        }
        return result;
    }
}
