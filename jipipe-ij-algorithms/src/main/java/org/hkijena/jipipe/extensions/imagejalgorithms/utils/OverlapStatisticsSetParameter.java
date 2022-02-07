package org.hkijena.jipipe.extensions.imagejalgorithms.utils;

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
