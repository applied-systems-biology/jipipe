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

public class Roi3DMeasurementSetParameter extends JIPipeDynamicSetParameter<Roi3DMeasurement> {
    public Roi3DMeasurementSetParameter() {
        super(new HashSet<>(Arrays.asList(Roi3DMeasurement.values())));
        setCollapsed(true);
        initialize();
    }

    public Roi3DMeasurementSetParameter(JIPipeDynamicSetParameter<Roi3DMeasurement> other) {
        super(other);
        initialize();
    }

    public Roi3DMeasurementSetParameter(Set<Roi3DMeasurement> values) {
        super(values);
        initialize();
    }

    private void initialize() {
        getAllowedValues().addAll(Arrays.asList(Roi3DMeasurement.values()));
    }

    public int getNativeValue() {
        int result = 0;
        for (Roi3DMeasurement value : getValues()) {
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
        for (Roi3DMeasurement value : getAllowedValues()) {
            if ((value.getNativeValue() & nativeValue) == value.getNativeValue()) {
                getValues().add(value);
            }
        }
    }
}
