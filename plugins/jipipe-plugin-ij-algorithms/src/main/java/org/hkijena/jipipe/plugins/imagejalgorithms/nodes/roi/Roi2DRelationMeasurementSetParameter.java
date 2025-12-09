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

import org.hkijena.jipipe.plugins.parameters.api.options.JIPipeDynamicSetParameter;

import java.util.Arrays;
import java.util.Set;

public class Roi2dRelationMeasurementSetParameter extends JIPipeDynamicSetParameter<Roi2dRelationMeasurement> {
    public Roi2dRelationMeasurementSetParameter() {
        initialize();
        setNativeValue(32767); // All except intersection stats and roi1, roi2 stats
    }

    public Roi2dRelationMeasurementSetParameter(JIPipeDynamicSetParameter<Roi2dRelationMeasurement> other) {
        super(other);
        initialize();
    }

    public Roi2dRelationMeasurementSetParameter(Set<Roi2dRelationMeasurement> values) {
        super(values);
        initialize();
    }

    private void initialize() {
        getAllowedValues().addAll(Arrays.asList(Roi2dRelationMeasurement.values()));
    }

    public int getNativeValue() {
        int result = 0;
        for (Roi2dRelationMeasurement value : getValues()) {
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
        for (Roi2dRelationMeasurement value : getAllowedValues()) {
            if ((value.getNativeValue() & nativeValue) == value.getNativeValue()) {
                getValues().add(value);
            }
        }
    }
}
