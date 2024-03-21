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

package org.hkijena.jipipe.extensions.ijweka.parameters.features;

import com.google.common.collect.Sets;
import org.hkijena.jipipe.api.AddJIPipeDocumentationDescription;
import org.hkijena.jipipe.extensions.parameters.api.enums.DynamicSetParameter;

import java.util.Arrays;

/**
 * Parameter that allows users to select a set of {@link WekaFeature3D}
 */
@AddJIPipeDocumentationDescription(description = "<ul>" +
        "<li>Gaussian blur: performs \uD835\uDC5B individual 3D convolutions with Gaussian kernels with the normal \uD835\uDC5B variations of \uD835\uDF0E. The larger the radius the more blurred the image becomes until the pixels are homogeneous.</li>" +
        "<li>Hessian: using FeatureJ it computes for each image element (voxel) the eigenvalues of the Hessian, which can be used for example to discriminate locally between plate-like, line-like, and blob-like image structures. More specifically, it calculates the magnitude of the largest, middle and smallest eigenvalue of the Hessian tensor. It requires enabling the ImageScience update site in the updater. It uses smoothing scale \uD835\uDF0E.</li>" +
        "<li>Derivatives: calculates high order derivatives of the input image (4, 6, 8, 10)</li>" +
        "<li>Laplacian: computes the Laplacian of the input image using FeatureJ (it requires enabling the ImageScience update site in the updater). It uses smoothing scale \uD835\uDF0E.</li>" +
        "<li>Structure: calculates for all elements in the input image, the eigenvalues (smallest and largest) of the so-called structure tensor using FeatureJ (it requires enabling the ImageScience update site in the updater). It uses smoothing scale \uD835\uDF0E and integration scales 1 and 3.</li>" +
        "<li>Edges: detects edges using Canny edge detection, which involves computation of the gradient magnitude, suppression of locally non-maximum gradient magnitudes, and (hysteresis) thresholding. Again, this feature uses FeatureJ so it requires enabling the ImageScience update site in the updater. It uses smoothing scale \uD835\uDF0E.\n</li>" +
        "<li>Difference of Gaussian: calculates two Gaussian blur images from the original image and subtracts one from the other. \uD835\uDF0E values are varied as usual, so \uD835\uDC5B(\uD835\uDC5B−1)2 feature images are added to the stack.</li>" +
        "<li>Minimum, Maximum, Mean, Variance, Median: the voxels within a radius of \uD835\uDF0E voxels from the target pixel are subjected to the pertinent operation (mean/min etc.) and the target voxel is set to that value.</li>" +
        "</ul>")
@AddJIPipeDocumentationDescription(description = "More information: https://imagej.net/plugins/tws/")
public class WekaFeatureSet3D extends DynamicSetParameter<WekaFeature3D> {
    public WekaFeatureSet3D() {
        setAllowedValues(Arrays.asList(WekaFeature3D.values()));
        // Default features
        setValues(Sets.newHashSet(WekaFeature3D.Mean,
                WekaFeature3D.Variance));
        setCollapsed(true);
    }

    public WekaFeatureSet3D(WekaFeatureSet3D other) {
        super(other);
    }
}
