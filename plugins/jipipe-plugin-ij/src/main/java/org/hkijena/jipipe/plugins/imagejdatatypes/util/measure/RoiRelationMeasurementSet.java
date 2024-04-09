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

import org.hkijena.jipipe.plugins.parameters.api.enums.DynamicSetParameter;

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

