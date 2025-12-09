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

package org.hkijena.jipipe.plugins.imagejdatatypes.util.measure;

import org.hkijena.jipipe.plugins.parameters.api.options.JIPipeDynamicSetParameter;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

public class Roi2dRelationMeasurementSet extends JIPipeDynamicSetParameter<Roi2dRelationMeasurement> {

    public Roi2dRelationMeasurementSet() {
        super(new HashSet<>(Arrays.asList(Roi2dRelationMeasurement.values())));
        setCollapsed(true);
        initialize();
    }

    public Roi2dRelationMeasurementSet(Roi2dRelationMeasurementSet other) {
        super(other);
    }

    public Roi2dRelationMeasurementSet(Set<Roi2dRelationMeasurement> values) {
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

