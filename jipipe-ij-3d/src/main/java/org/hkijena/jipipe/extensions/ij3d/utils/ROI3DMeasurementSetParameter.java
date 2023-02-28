package org.hkijena.jipipe.extensions.ij3d.utils;

import org.hkijena.jipipe.extensions.parameters.api.enums.DynamicSetParameter;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

public class ROI3DMeasurementSetParameter extends DynamicSetParameter<ROI3DMeasurement> {
    public ROI3DMeasurementSetParameter() {
        super(new HashSet<>(Arrays.asList(ROI3DMeasurement.values())));
        setCollapsed(true);
        initialize();
    }

    public ROI3DMeasurementSetParameter(DynamicSetParameter<ROI3DMeasurement> other) {
        super(other);
        initialize();
    }

    public ROI3DMeasurementSetParameter(Set<ROI3DMeasurement> values) {
        super(values);
        initialize();
    }

    private void initialize() {
        getAllowedValues().addAll(Arrays.asList(ROI3DMeasurement.values()));
    }

    public int getNativeValue() {
        int result = 0;
        for (ROI3DMeasurement value : getValues()) {
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
        for (ROI3DMeasurement value : getAllowedValues()) {
            if ((value.getNativeValue() & nativeValue) == value.getNativeValue()) {
                getValues().add(value);
            }
        }
    }
}
