package org.hkijena.jipipe.extensions.ijweka.parameters;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.text.WordUtils;

/**
 * Enum of the standard {@link trainableSegmentation.WekaSegmentation} features supported by {@link trainableSegmentation.FeatureStack}
 */
public enum WekaFeature2D {
    Gaussian_blur,
    Sobel_filter,
    Hessian,
    Difference_of_gaussians,
    Membrane_projections,
    Variance,
    Mean,
    Minimum,
    Maximum,
    Median,
    Anisotropic_diffusion,
    Bilateral,
    Lipschitz,
    Kuwahara,
    Gabor,
    Derivatives,
    Laplacian,
    Structure,
    Entropy,
    Neighbors;


    @Override
    public String toString() {
        return name().replace('_', ' ');
    }
}
