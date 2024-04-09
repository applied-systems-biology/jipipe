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

package org.hkijena.jipipe.plugins.imagejalgorithms.nodes.roi;

import org.hkijena.jipipe.plugins.parameters.api.enums.DynamicSetParameter;

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
