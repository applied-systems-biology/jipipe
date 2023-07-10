package org.hkijena.jipipe.extensions.imagejalgorithms.nodes.roi;

import org.hkijena.jipipe.extensions.parameters.api.enums.DynamicSetParameter;

import java.util.Arrays;
import java.util.Set;

public class ROI2DRelationMeasurementSetParameter extends DynamicSetParameter<ROI2DRelationMeasurement> {
    public ROI2DRelationMeasurementSetParameter() {
        initialize();
        setNativeValue(32767); // All except intersection stats and roi1, roi2 stats
    }

    public ROI2DRelationMeasurementSetParameter(DynamicSetParameter<ROI2DRelationMeasurement> other) {
        super(other);
        initialize();
    }

    public ROI2DRelationMeasurementSetParameter(Set<ROI2DRelationMeasurement> values) {
        super(values);
        initialize();
    }

    private void initialize() {
        getAllowedValues().addAll(Arrays.asList(ROI2DRelationMeasurement.values()));
    }

    public int getNativeValue() {
        int result = 0;
        for (ROI2DRelationMeasurement value : getValues()) {
            result |= value.getNativeValue();
        }
        return result;
    }

    /**
     * Sets the values from native values
     *
     * @param nativeValue multiple native values
     */
    public void setNativeValue(int nativeValue) {
        getValues().clear();
        for (ROI2DRelationMeasurement value : getAllowedValues()) {
            if ((value.getNativeValue() & nativeValue) == value.getNativeValue()) {
                getValues().add(value);
            }
        }
    }
}
