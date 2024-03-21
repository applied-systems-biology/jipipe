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
