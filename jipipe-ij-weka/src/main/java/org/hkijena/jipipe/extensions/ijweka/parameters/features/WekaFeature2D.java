/*
 * Copyright by Zoltán Cseresnyés, Ruman Gerst
 *
 * Research Group Applied Systems Biology - Head: Prof. Dr. Marc Thilo Figge
 * https://www.leibniz-hki.de/en/applied-systems-biology.html
 * HKI-Center for Systems Biology of Infection
 * Leibniz Institute for Natural Product Research and Infection Biology - Hans Knöll Institute (HKI)
 * Adolf-Reichwein-Straße 23, 07745 Jena, Germany
 *
 * The project code is licensed under BSD 2-Clause.
 * See the LICENSE file provided with the code for the full license.
 *
 */

package org.hkijena.jipipe.extensions.ijweka.parameters.features;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.text.WordUtils;
import org.hkijena.jipipe.api.JIPipeDocumentationDescription;
import org.hkijena.jipipe.api.data.JIPipeDataStorageDocumentation;

/**
 * Enum of the standard {@link trainableSegmentation.WekaSegmentation} features supported by {@link trainableSegmentation.FeatureStack}
 */
@JIPipeDocumentationDescription(description = "<ul>" +
        "<li>Gaussian blur: performs \uD835\uDC5B individual convolutions with Gaussian kernels with the normal \uD835\uDC5B variations of \uD835\uDF0E. The larger the radius the more blurred the image becomes until the pixels are homogeneous.</li>" +
        "<li>Sobel filter: calculates an approximation of the gradient of the image intensity at each pixel. Gaussian blurs with \uD835\uDF0E varying as usual are performed prior to the filter.</li>" +
        "<li>Hessian: Calculates a Hessian matrix \uD835\uDC3B at each pixel. Prior to the application of any filters, a Gaussian blur with varying \uD835\uDF0E is performed. </li>" +
        "<li>Difference of gaussians: calculates two Gaussian blur images from the original image and subtracts one from the other. \uD835\uDF0E values are varied as usual, so \uD835\uDC5B(\uD835\uDC5B−1)2 feature images are added to the stack.</li>" +
        "<li>Membrane projections: enhances membrane-like structures of the image through directional filtering. The initial kernel for this operation is hardcoded as a 19×19 zero matrix with the middle column entries set to 1. " +
        "Multiple kernels are created by rotating the original kernel by 6 degrees up to a total rotation of 180 degrees, giving 30 kernels. Each kernel is convolved with the image and then the set of 30 images are Z-projected into a single image via 6 methods (sum of the pixels in each image, \n" +
        "mean of the pixels in each image, \n" +
        "standard deviation of the pixels in each image, \n" +
        "median of the pixels in each image, \n" +
        "maximum of the pixels in each image, \n" +
        "minimum of the pixels in each image). Each of the 6 resulting images is a feature. Hence pixels in lines of similarly valued pixels in the image that are different from the average image intensity will stand out in the Z-projections.</li>" +
        "<li>Mean, Variance, Median, Minimum, Maximum: the pixels within a radius of \uD835\uDF0E pixels from the target pixel are subjected to the pertinent operation (mean/min etc.) and the target pixel is set to that value.</li>" +
        "<li>Anisotropic diffusion: the anisotropic diffusion filtering from Fiji with 20 iterations, \uD835\uDF0E smoothing per iterations, \uD835\uDC4E1=0.10,0.35, \uD835\uDC4E2=0.9, and an edge threshold set to the membrane size.</li>" +
        "<li>Bilateral filter: is very similar to the Mean filter but better preserves edges while averaging/blurring other parts of the image. The filter accomplishes this task by only averaging the values around the current pixel that are close in color value to the current pixel. " +
        "The ‘closeness’ of other neighborhood pixels to the current pixels is determined by the specified threshold. I.e. for a value of 10 each pixel that contributes to the current mean have to be within 10 values of the current pixel. In our case, we combine spatial radius of 5 and 10, with a range radius of 50 and 100.</li>" +
        "<li>Lipschitz filter: from Mikulas Stencel plugin. This plugin implements Lipschitz cover of an image that is equivalent to a grayscale opening by a cone. The Lipschitz cover can be applied for the elimination of a slowly varying image background by subtraction of the lower Lipschitz cover (a top-hat procedure). A sequential double scan algorithm is used. We use down and top hats combination, with slope \uD835\uDC60=5,10,15,20,25.</li>" +
        "<li>Kuwahara filter: another noise-reduction filter that preserves edges. This is a version of the Kuwahara filter that uses linear kernels rather than square ones. We use the membrane patch size as kernel size, 30 angles and the three different criteria (Variance, Variance / Mean and Variance / Mean^2).\n</li>" +
        "<li>abor filter: at the moment this option may take some time and memory because it generates a very diverse range of Gabor filters (22). ’ This may undergo changes in the future’. The implementation details are included in this script. The Gabor filter is an edge detection and texture filter, which convolves several kernels at different angles with an image. Gabor filters are band-pass filters and therefore implement a frequency transformation.</li>" +
        "<li>Derivatives filter: calculates high order (4, 6, 8, 10) derivatives of the input image.</li>" +
        "<li>Laplacian filter: computes the Laplacian of the input image using FeatureJ (it requires enabling the ImageScience update site in the updater). It uses smoothing scale \uD835\uDF0E.</li>" +
        "<li>Structure filter: calculates for all elements in the input image, the eigenvalues (smallest and largest) of the so-called structure tensor using FeatureJ (it requires enabling the ImageScience update site in the updater). It uses smoothing scale \uD835\uDF0E and integration scales 1 and 3.</li>" +
        "<li>Entropy: draws a circle of radius \uD835\uDC5F around each pixel; gets the histogram of that circle split in numBins chunks; then calculates the entropy as ∑\uD835\uDC5D in histogram−\uD835\uDC5D∗log2(\uD835\uDC5D), where \uD835\uDC5D is the probability of each chunk in the histogram. numBins is equal to 32,64,128,256. \uD835\uDC5F is equal to \uD835\uDF0E.</li>" +
        "<li>Neighbors: shifts the image in 8 directions by an certain number of pixel, \uD835\uDF0E. Therefore creates 8\uD835\uDC5B feature images.\n</li>" +
        "</ul>")
@JIPipeDocumentationDescription(description = "More information: https://imagej.net/plugins/tws/")
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
