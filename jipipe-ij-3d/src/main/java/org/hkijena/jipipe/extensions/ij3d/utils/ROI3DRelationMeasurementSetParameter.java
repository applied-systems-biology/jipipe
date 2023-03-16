package org.hkijena.jipipe.extensions.ij3d.utils;

import org.hkijena.jipipe.extensions.parameters.api.enums.DynamicSetParameter;

import java.util.Arrays;
import java.util.Set;

public class ROI3DRelationMeasurementSetParameter extends DynamicSetParameter<ROI3DRelationMeasurement> {
    public ROI3DRelationMeasurementSetParameter() {
        initialize();
        setNativeValue(32767); // All except intersection stats and roi1, roi2 stats
    }

    public ROI3DRelationMeasurementSetParameter(DynamicSetParameter<ROI3DRelationMeasurement> other) {
        super(other);
        initialize();
    }

    public ROI3DRelationMeasurementSetParameter(Set<ROI3DRelationMeasurement> values) {
        super(values);
        initialize();
    }

    private void initialize() {
        getAllowedValues().addAll(Arrays.asList(ROI3DRelationMeasurement.values()));
    }

    public int getNativeValue() {
        int result = 0;
        for (ROI3DRelationMeasurement value : getValues()) {
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
        for (ROI3DRelationMeasurement value : getAllowedValues()) {
            if ((value.getNativeValue() & nativeValue) == value.getNativeValue()) {
                getValues().add(value);
            }
        }
    }
}
