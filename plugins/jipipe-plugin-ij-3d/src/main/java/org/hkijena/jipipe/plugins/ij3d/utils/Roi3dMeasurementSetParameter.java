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
import java.util.HashSet;
import java.util.Set;

public class Roi3dMeasurementSetParameter extends JIPipeDynamicSetParameter<Roi3dMeasurement> {
    public Roi3dMeasurementSetParameter() {
        super(new HashSet<>(Arrays.asList(Roi3dMeasurement.values())));
        setCollapsed(false);
        initialize();
    }

    public Roi3dMeasurementSetParameter(JIPipeDynamicSetParameter<Roi3dMeasurement> other) {
        super(other);
        initialize();
    }

    public Roi3dMeasurementSetParameter(Set<Roi3dMeasurement> values) {
        super(values);
        initialize();
    }

    private void initialize() {
        getAllowedValues().addAll(Arrays.asList(Roi3dMeasurement.values()));
    }

    public int getNativeValue() {
        int result = 0;
        for (Roi3dMeasurement value : getValues()) {
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
        for (Roi3dMeasurement value : getAllowedValues()) {
            if ((value.getNativeValue() & nativeValue) == value.getNativeValue()) {
                getValues().add(value);
            }
        }
    }
}
