/*
 * Copyright by Zoltán Cseresnyés, Ruman Gerst
 *
 * Research Group Applied Systems Biology - Head: Prof. Dr. Marc Thilo Figge
 * https://www.leibniz-hki.de/en/applied-systems-biology.html
 * HKI-Center for Systems Biology of Infection
 * Leibniz Institute for Natural Product Research and Infection Biology - Hans Knöll Institute (HKI)
 * Adolf-Reichwein-Straße 23, 07745 Jena, Germany
 *
 * The project code is licensed under MIT.
 * See the LICENSE file provided with the code for the full license.
 */

package org.hkijena.jipipe.plugins.ij3d.utils;

import org.hkijena.jipipe.plugins.parameters.api.options.JIPipeDynamicSetParameter;

import java.util.Arrays;
import java.util.Set;

public class Roi3dRelationMeasurementSetParameter extends JIPipeDynamicSetParameter<Roi3dRelationMeasurement> {
    public Roi3dRelationMeasurementSetParameter() {
        initialize();
        setNativeValue(1 + 2 + 4 + 8 + 16 + 32 + 64 + 128 + 256 + 512 + 2024); // Fast standard set
    }

    public Roi3dRelationMeasurementSetParameter(JIPipeDynamicSetParameter<Roi3dRelationMeasurement> other) {
        super(other);
        initialize();
    }

    public Roi3dRelationMeasurementSetParameter(Set<Roi3dRelationMeasurement> values) {
        super(values);
        initialize();
    }

    private void initialize() {
        getAllowedValues().addAll(Arrays.asList(Roi3dRelationMeasurement.values()));
    }

    public int getNativeValue() {
        int result = 0;
        for (Roi3dRelationMeasurement value : getValues()) {
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
        for (Roi3dRelationMeasurement value : getAllowedValues()) {
            if ((value.getNativeValue() & nativeValue) == value.getNativeValue()) {
                getValues().add(value);
            }
        }
    }
}
