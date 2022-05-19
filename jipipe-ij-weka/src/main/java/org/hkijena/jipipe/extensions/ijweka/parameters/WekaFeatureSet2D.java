package org.hkijena.jipipe.extensions.ijweka.parameters;

import com.google.common.collect.Sets;
import org.hkijena.jipipe.extensions.parameters.api.enums.DynamicSetParameter;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.TreeSet;

/**
 * Parameter that allows users to select a set of {@link WekaFeature2D}
 */
public class WekaFeatureSet2D extends DynamicSetParameter<WekaFeature2D> {
    public WekaFeatureSet2D() {
        setAllowedValues(Arrays.asList(WekaFeature2D.values()));
        // Default features
        setValues(Sets.newHashSet(WekaFeature2D.Gaussian_blur,
                WekaFeature2D.Sobel_filter,
                WekaFeature2D.Hessian,
                WekaFeature2D.Difference_of_gaussians,
                WekaFeature2D.Membrane_projections));
        setCollapsed(true);
    }

    public WekaFeatureSet2D(WekaFeatureSet2D other) {
        super(other);
    }
}
