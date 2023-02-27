package org.hkijena.jipipe.extensions.ij3d.utils;

import org.hkijena.jipipe.extensions.imagejdatatypes.util.measure.Measurement;
import org.hkijena.jipipe.extensions.parameters.api.enums.DynamicSetParameter;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

public class Measurements3DSetParameter extends DynamicSetParameter<Measurement3D> {
    public Measurements3DSetParameter() {
        super(new HashSet<>(Arrays.asList(Measurement3D.values())));
        initialize();
    }

    public Measurements3DSetParameter(DynamicSetParameter<Measurement3D> other) {
        super(other);
        initialize();
    }

    public Measurements3DSetParameter(Set<Measurement3D> values) {
        super(values);
        initialize();
    }

    private void initialize() {
        getAllowedValues().addAll(Arrays.asList(Measurement3D.values()));
    }

    public int getNativeValue() {
        int result = 0;
        for (Measurement3D value : getValues()) {
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
        for (Measurement3D value : getAllowedValues()) {
            if ((value.getNativeValue() & nativeValue) == value.getNativeValue()) {
                getValues().add(value);
            }
        }
    }
}
