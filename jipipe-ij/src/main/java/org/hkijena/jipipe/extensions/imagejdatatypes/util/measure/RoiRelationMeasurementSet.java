package org.hkijena.jipipe.extensions.imagejdatatypes.util.measure;

import org.hkijena.jipipe.extensions.parameters.api.enums.DynamicSetParameter;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

public class RoiRelationMeasurementSet extends DynamicSetParameter<RoiRelationMeasurement> {

    public RoiRelationMeasurementSet() {
        super(new HashSet<>(Arrays.asList(RoiRelationMeasurement.values())));
        setCollapsed(true);
        initialize();
    }

    public RoiRelationMeasurementSet(RoiRelationMeasurementSet other) {
        super(other);
    }

    public RoiRelationMeasurementSet(Set<RoiRelationMeasurement> values) {
        super(values);
        initialize();
    }

    private void initialize() {
        getAllowedValues().addAll(Arrays.asList(RoiRelationMeasurement.values()));
    }

    public int getNativeValue() {
        int result = 0;
        for (RoiRelationMeasurement value : getValues()) {
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
        for (RoiRelationMeasurement value : getAllowedValues()) {
            if ((value.getNativeValue() & nativeValue) == value.getNativeValue()) {
                getValues().add(value);
            }
        }
    }
}

