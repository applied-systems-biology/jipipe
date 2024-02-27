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

package org.hkijena.jipipe.extensions.imagejalgorithms.parameters;

import org.hkijena.jipipe.extensions.imagejdatatypes.util.measure.Measurement;
import org.hkijena.jipipe.extensions.parameters.api.enums.DynamicSetParameter;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

/**
 * A {@link DynamicSetParameter} that contains image statistics measurements.
 * Contains {@link Measurement} items
 */
public class OverlapStatisticsSetParameter extends DynamicSetParameter<OverlapStatistics> {
    public OverlapStatisticsSetParameter() {
        super(new HashSet<>(Arrays.asList(OverlapStatistics.values())));
        setCollapsed(true);
        initialize();
    }

    public OverlapStatisticsSetParameter(OverlapStatisticsSetParameter other) {
        super(other);
    }

    public OverlapStatisticsSetParameter(Set<OverlapStatistics> values) {
        super(values);
        initialize();
    }

    private void initialize() {
        getAllowedValues().addAll(Arrays.asList(OverlapStatistics.values()));
    }
}
