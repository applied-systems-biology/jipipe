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

package org.hkijena.jipipe.plugins.imagejalgorithms.nodes.features;

import ij.ImagePlus;
import ij.process.ImageProcessor;
import imagescience.ImageScience;
import imagescience.feature.Differentiator;
import imagescience.image.*;
import org.hkijena.jipipe.api.*;
import org.hkijena.jipipe.api.nodes.AddJIPipeInputSlot;
import org.hkijena.jipipe.api.nodes.AddJIPipeOutputSlot;
import org.hkijena.jipipe.api.nodes.JIPipeGraphNodeRunContext;
import org.hkijena.jipipe.api.nodes.JIPipeNodeInfo;
import org.hkijena.jipipe.api.nodes.algorithm.JIPipeSimpleIteratingAlgorithm;
import org.hkijena.jipipe.api.nodes.categories.ImagesNodeTypeCategory;
import org.hkijena.jipipe.api.nodes.iterationstep.JIPipeIterationContext;
import org.hkijena.jipipe.api.nodes.iterationstep.JIPipeSingleIterationStep;
import org.hkijena.jipipe.api.parameters.JIPipeParameter;
import org.hkijena.jipipe.plugins.imagejalgorithms.utils.ImageScienceUtils;
import org.hkijena.jipipe.plugins.imagejdatatypes.datatypes.ImagePlusData;
import org.hkijena.jipipe.plugins.imagejdatatypes.datatypes.greyscale.ImagePlusGreyscaleData;
import org.hkijena.jipipe.plugins.imagejdatatypes.util.ImageJUtils;
import org.hkijena.jipipe.plugins.imagejdatatypes.util.ImageSliceIndex;

import java.util.HashMap;
import java.util.Map;

@SetJIPipeDocumentation(name = "Derivatives features 2D/3D (FeatureJ)", description = "Multi-dimensional, Gaussian-scaled derivatives of images, which form the basis of differential-geometric descriptions of image features found in literature on human and computer vision. " +
        "The differentiation operation is carried out for each time frame and channel in a 5D image." +
        "Derivative images are obtained by separable convolution with derivatives of the multi-dimensional Gaussian function. " +
        "This necessarily involves sampling and truncation of the continuous Gaussian function. At very small smoothing scales (less than about half the derivative order), " +
        "this causes sampling artifacts, whereas at very large scales (where the kernel would have to be larger than twice the image size in the corresponding dimension), this causes truncation artifacts. " +
        "In both cases, the sampled convolution kernel does not faithfully represent the shape of the original continuous function (due to undersampling or truncation), and the results become meaningless. " +
        "The algorithm uses mirror-boundary conditions to obtain values outside the image at (positions close to) the boundaries.")
@AddJIPipeCitation("See https://imagescience.org/meijering/software/featurej/derivatives/")
@AddJIPipeCitation("J. J. Koenderink and A. J. van Doorn Representation of Local Geometry in the Visual System Biological Cybernetics, vol. 55, no. 6, March 1987, pp. 367-375")
@AddJIPipeCitation("T. Lindeberg Scale-Space Theory in Computer Vision Kluwer Academic Publishers, 1994")
@ConfigureJIPipeNode(nodeTypeCategory = ImagesNodeTypeCategory.class, menuPath = "Features")
@AddJIPipeInputSlot(value = ImagePlusGreyscaleData.class, name = "Input", create = true)
@AddJIPipeOutputSlot(value = ImagePlusGreyscaleData.class, name = "Output", create = true)
public class DerivativesFeaturesAlgorithm extends JIPipeSimpleIteratingAlgorithm {

    private int xOrder = 0;
    private int yOrder = 0;
    private int zOrder = 0;
    private double smoothingScale = 1;
    private boolean isotropicGaussian = false;
    private boolean force2D = false;

    public DerivativesFeaturesAlgorithm(JIPipeNodeInfo info) {
        super(info);
    }

    public DerivativesFeaturesAlgorithm(DerivativesFeaturesAlgorithm other) {
        super(other);
        this.xOrder = other.xOrder;
        this.yOrder = other.yOrder;
        this.zOrder = other.zOrder;
        this.smoothingScale = other.smoothingScale;
        this.isotropicGaussian = other.isotropicGaussian;
        this.force2D = other.force2D;
    }

    public static Image run(final Image image, final double scale, final int xorder, final int yorder, final int zorder, JIPipeProgressInfo progressInfo) {

        progressInfo.log(ImageScience.prelude() + "Differentiator");

        // Initialize:
        check(scale, xorder, yorder, zorder);

        final Dimensions dims = image.dimensions();
        progressInfo.log("Input image dimensions: (x,y,z,t,c) = (" + dims.x + "," + dims.y + "," + dims.z + "," + dims.t + "," + dims.c + ")");

        final Aspects asps = image.aspects();
        progressInfo.log("Element aspect ratios: (" + asps.x + "," + asps.y + "," + asps.z + "," + asps.t + "," + asps.c + ")");
        if (asps.x <= 0) throw new IllegalStateException("Aspect ratio in x-dimension less than or equal to 0");
        if (asps.y <= 0) throw new IllegalStateException("Aspect ratio in y-dimension less than or equal to 0");
        if (asps.z <= 0) throw new IllegalStateException("Aspect ratio in z-dimension less than or equal to 0");

        final Image deriv = (image instanceof FloatImage) ? image : new FloatImage(image);

        final int maxSteps = (dims.x > 1 ? dims.c * dims.t * dims.z * dims.y : 0) +
                (dims.y > 1 ? dims.c * dims.t * dims.z * dims.x : 0) +
                (dims.z > 1 ? dims.c * dims.t * dims.z * dims.y : 0);
        int numSteps = 0;
        JIPipePercentageProgressInfo percentageProgress = progressInfo.percentage("Calculating");

        // Differentiation in x-dimension:
        if (dims.x == 1) {
            if (xorder == 0) {
                progressInfo.log("No operation in x-dimension");
            } else {
                progressInfo.log("Zeroing in x-dimension");
                deriv.set(0);
            }
        } else {
            final double xscale = scale / asps.x;
            progressInfo.log("Operating at scale " + scale + "/" + asps.x + " = " + xscale + " pixels");
            progressInfo.log(info(xorder) + " in x-dimension");
            final double[] kernel = kernel(xscale, xorder, dims.x);
            final int klenm1 = kernel.length - 1;
            final double[] ain = new double[dims.x + 2 * klenm1];
            final double[] aout = new double[dims.x];
            final Coordinates coords = new Coordinates();
            deriv.axes(Axes.X);
            for (coords.c = 0; coords.c < dims.c; ++coords.c)
                for (coords.t = 0; coords.t < dims.t; ++coords.t)
                    for (coords.z = 0; coords.z < dims.z; ++coords.z)
                        for (coords.y = 0; coords.y < dims.y; ++coords.y) {
                            coords.x = -klenm1;
                            deriv.get(coords, ain);
                            convolve(ain, aout, kernel);
                            coords.x = 0;
                            deriv.set(coords, aout);
                            percentageProgress.logPercentage(numSteps++, maxSteps);
                        }
        }

        // Differentiation in y-dimension:
        if (dims.y == 1) {
            if (yorder == 0) {
                progressInfo.log("No operation in y-dimension");
            } else {
                progressInfo.log("Zeroing in y-dimension");
                deriv.set(0);
            }
        } else {
            final double yscale = scale / asps.y;
            progressInfo.log("Operating at scale " + scale + "/" + asps.y + " = " + yscale + " pixels");
            progressInfo.log(info(yorder) + " in y-dimension");
            final double[] kernel = kernel(yscale, yorder, dims.y);
            final int klenm1 = kernel.length - 1;
            final double[] ain = new double[dims.y + 2 * klenm1];
            final double[] aout = new double[dims.y];
            final Coordinates coords = new Coordinates();
            deriv.axes(Axes.Y);
            for (coords.c = 0; coords.c < dims.c; ++coords.c)
                for (coords.t = 0; coords.t < dims.t; ++coords.t)
                    for (coords.z = 0; coords.z < dims.z; ++coords.z)
                        for (coords.x = 0; coords.x < dims.x; ++coords.x) {
                            coords.y = -klenm1;
                            deriv.get(coords, ain);
                            convolve(ain, aout, kernel);
                            coords.y = 0;
                            deriv.set(coords, aout);
                            percentageProgress.logPercentage(numSteps++, maxSteps);
                        }
        }

        // Differentiation in z-dimension:
        if (dims.z == 1) {
            if (zorder == 0) {
                progressInfo.log("No operation in z-dimension");
            } else {
                progressInfo.log("Zeroing in z-dimension");
                deriv.set(0);
            }
        } else {
            final double zscale = scale / asps.z;
            progressInfo.log("Operating at scale " + scale + "/" + asps.z + " = " + zscale + " slices");
            progressInfo.log(info(zorder) + " in z-dimension");
            final double[] kernel = kernel(zscale, zorder, dims.z);
            final int klenm1 = kernel.length - 1;
            final double[] ain = new double[dims.z + 2 * klenm1];
            final double[] aout = new double[dims.z];
            final Coordinates coords = new Coordinates();
            deriv.axes(Axes.Z);
            for (coords.c = 0; coords.c < dims.c; ++coords.c)
                for (coords.t = 0; coords.t < dims.t; ++coords.t)
                    for (coords.y = 0; coords.y < dims.y; ++coords.y) {
                        for (coords.x = 0; coords.x < dims.x; ++coords.x) {
                            coords.z = -klenm1;
                            deriv.get(coords, ain);
                            convolve(ain, aout, kernel);
                            coords.z = 0;
                            deriv.set(coords, aout);
                        }
                        percentageProgress.logPercentage(numSteps++, maxSteps);
                    }
        }

        deriv.name(image.name() + " dx" + xorder + " dy" + yorder + " dz" + zorder);

        return deriv;
    }

    private static double[] kernel(final double s, final int d, final int m) {

        // Initialize:
        double r = 5;
        if (d == 0) r = 3;
        else if (d <= 2) r = 4;
        int h = (int) (s * r) + 1;
        if (h > m) h = m;
        final double[] kernel = new double[h];
        kernel[0] = (d == 0) ? 1 : 0;

        // Compute kernel:
        if (h > 1) {
            final double is2 = 1 / (s * s);
            final double is4 = is2 * is2;
            final double is6 = is4 * is2;
            final double is8 = is6 * is2;
            final double is10 = is8 * is2;
            final double mis2 = -0.5 * is2;
            final double sq2pi = Math.sqrt(2 * Math.PI);
            switch (d) {
                case 0: {
                    double integral = 0;
                    for (int k = 0; k < h; ++k) {
                        kernel[k] = Math.exp(k * k * mis2);
                        integral += kernel[k];
                    }
                    integral *= 2.0;
                    integral -= kernel[0];
                    for (int k = 0; k < h; ++k)
                        kernel[k] /= integral;
                    break;
                }
                case 1: {
                    final double c = -is2 / (sq2pi * s);
                    for (int k = 1; k < h; ++k) {
                        final double k2 = k * k;
                        kernel[k] = c * k * Math.exp(k2 * mis2);
                    }
                    break;
                }
                case 2: {
                    final double c = is2 / (sq2pi * s);
                    for (int k = 0; k < h; ++k) {
                        final double k2 = k * k;
                        kernel[k] = c * (k2 * is2 - 1) * Math.exp(k2 * mis2);
                    }
                    break;
                }
                case 3: {
                    final double c = -is4 / (sq2pi * s);
                    for (int k = 1; k < h; ++k) {
                        final double k2 = k * k;
                        kernel[k] = c * k * (k2 * is2 - 3) * Math.exp(k2 * mis2);
                    }
                    break;
                }
                case 4: {
                    final double c = is4 / (sq2pi * s);
                    for (int k = 0; k < h; ++k) {
                        final double k2 = k * k;
                        kernel[k] = c * (k2 * k2 * is4 - 6 * k2 * is2 + 3) * Math.exp(k2 * mis2);
                    }
                    break;
                }
                case 5: {
                    final double c = -is6 / (sq2pi * s);
                    for (int k = 1; k < h; ++k) {
                        final double k2 = k * k;
                        kernel[k] = c * k * (k2 * k2 * is4 - 10 * k2 * is2 + 15) * Math.exp(k2 * mis2);
                    }
                    break;
                }
                case 6: {
                    final double c = is6 / (sq2pi * s);
                    for (int k = 0; k < h; ++k) {
                        final double k2 = k * k;
                        final double k4 = k2 * k2;
                        kernel[k] = c * (k4 * k2 * is6 - 15 * k4 * is4 + 45 * k2 * is2 - 15) * Math.exp(k2 * mis2);
                    }
                    break;
                }
                case 7: {
                    final double c = -is8 / (sq2pi * s);
                    for (int k = 1; k < h; ++k) {
                        final double k2 = k * k;
                        final double k4 = k2 * k2;
                        kernel[k] = c * k * (k4 * k2 * is6 - 21 * k4 * is4 + 105 * k2 * is2 - 105) * Math.exp(k2 * mis2);
                    }
                    break;
                }
                case 8: {
                    final double c = is8 / (sq2pi * s);
                    for (int k = 0; k < h; ++k) {
                        final double k2 = k * k;
                        final double k4 = k2 * k2;
                        kernel[k] = c * (k4 * k4 * is8 - 28 * k4 * k2 * is6 + 210 * k4 * is4 - 420 * k2 * is2 + 105) * Math.exp(k2 * mis2);
                    }
                    break;
                }
                case 9: {
                    final double c = -is10 / (sq2pi * s);
                    for (int k = 1; k < h; ++k) {
                        final double k2 = k * k;
                        final double k4 = k2 * k2;
                        kernel[k] = c * k * (k4 * k4 * is8 - 36 * k4 * k2 * is6 + 378 * k4 * is4 - 1260 * k2 * is2 + 945) * Math.exp(k2 * mis2);
                    }
                    break;
                }
                case 10: {
                    final double c = is10 / (sq2pi * s);
                    for (int k = 0; k < h; ++k) {
                        final double k2 = k * k;
                        final double k4 = k2 * k2;
                        final double k6 = k4 * k2;
                        kernel[k] = c * (k6 * k4 * is10 - 45 * k4 * k4 * is8 + 630 * k6 * is6 - 3150 * k4 * is4 + 4725 * k2 * is2 - 945) * Math.exp(k2 * mis2);
                    }
                    break;
                }
            }
        }

        return kernel;
    }

    private static void convolve(final double[] ain, final double[] aout, final double[] kernel) {

        // Mirror borders in input array:
        final int khlenm1 = kernel.length - 1;
        final int aolenm1 = aout.length - 1;
        for (int k = 0, lm = khlenm1, lp = khlenm1, hm = khlenm1 + aolenm1, hp = khlenm1 + aolenm1; k < khlenm1; ++k) {
            ain[--lm] = ain[++lp];
            ain[++hp] = ain[--hm];
        }

        // Convolve with kernel:
        final double sign = (kernel[0] == 0) ? -1 : 1;
        for (int io = 0, ii = khlenm1; io <= aolenm1; ++io, ++ii) {
            double convres = ain[ii] * kernel[0];
            for (int k = 1, iimk = ii, iipk = ii; k <= khlenm1; ++k)
                convres += (ain[--iimk] + sign * ain[++iipk]) * kernel[k];
            aout[io] = convres;
        }
    }

    private static String info(final int d) {

        String info = null;
        switch (d) {
            case 0:
                info = "Smoothing";
                break;
            case 1:
                info = "First";
                break;
            case 2:
                info = "Second";
                break;
            case 3:
                info = "Third";
                break;
            case 4:
                info = "Fourth";
                break;
            case 5:
                info = "Fifth";
                break;
            case 6:
                info = "Sixth";
                break;
            case 7:
                info = "Seventh";
                break;
            case 8:
                info = "Eighth";
                break;
            case 9:
                info = "Nineth";
                break;
            case 10:
                info = "Tenth";
                break;
        }
        if (d > 0) info += "-order differentiation";
        return info;
    }

    private static void check(final double scale, final int xorder, final int yorder, final int zorder) {
        if (scale <= 0) throw new IllegalArgumentException("Smoothing scale less than or equal to 0");
        if (xorder < 0 || xorder > Differentiator.MAX_ORDER)
            throw new IllegalArgumentException("Differentiation order out of range in x-dimension");
        if (yorder < 0 || yorder > Differentiator.MAX_ORDER)
            throw new IllegalArgumentException("Differentiation order out of range in y-dimension");
        if (zorder < 0 || zorder > Differentiator.MAX_ORDER)
            throw new IllegalArgumentException("Differentiation order out of range in z-dimension");
    }

    @Override
    protected void runIteration(JIPipeSingleIterationStep iterationStep, JIPipeIterationContext iterationContext, JIPipeGraphNodeRunContext runContext, JIPipeProgressInfo progressInfo) {
        ImagePlus input = iterationStep.getInputData(getFirstInputSlot(), ImagePlusGreyscaleData.class, progressInfo).getImage();

        if (force2D) {
            Map<ImageSliceIndex, ImageProcessor> resultMap = new HashMap<>();
            ImageJUtils.forEachIndexedZCTSliceWithProgress(input, (ip, index, sliceProgress) -> {
                ImagePlus sliceImp = new ImagePlus("slice", ip);
                sliceImp.copyScale(input);
                ImagePlus resultSlice = applyDerivatives(sliceImp, sliceProgress);
                resultMap.put(index, resultSlice.getProcessor());
            }, progressInfo);
            ImagePlus result = ImageJUtils.mergeMappedSlices(resultMap);
            result.copyScale(input);
            iterationStep.addOutputData(getFirstOutputSlot(), new ImagePlusData(result), progressInfo);
        } else {
            ImagePlus result = applyDerivatives(input, progressInfo);
            iterationStep.addOutputData(getFirstOutputSlot(), new ImagePlusData(result), progressInfo);
        }
    }

    private ImagePlus applyDerivatives(ImagePlus input, JIPipeProgressInfo progressInfo) {
        final Image image = Image.wrap(input);
        final Aspects aspects = image.aspects();
        if (!isotropicGaussian) {
            image.aspects(new Aspects());
        }
        final Image output = new FloatImage(image);
        run(output, smoothingScale, xOrder, yOrder, zOrder, progressInfo);
        output.aspects(aspects);
        return ImageScienceUtils.unwrap(output, input);
    }

    @SetJIPipeDocumentation(name = "Force 2D", description = "If enabled, each 2D image slice is processed individually.")
    @JIPipeParameter(value = "force-2d", important = true)
    public boolean isForce2D() {
        return force2D;
    }

    @JIPipeParameter("force-2d")
    public void setForce2D(boolean force2D) {
        this.force2D = force2D;
    }

    @SetJIPipeDocumentation(name = "Isotropic Gaussian smoothing", description = "Use the voxel width, height, and depth specified in the image properties (calibration). " +
            "That is, each dimension is assigned its own smoothing scale, computed by dividing the scale as specified in the plugin dialog by the specified pixel/voxel size for that dimension.")
    @JIPipeParameter("isotropic-gaussian")
    public boolean isIsotropicGaussian() {
        return isotropicGaussian;
    }

    @JIPipeParameter("isotropic-gaussian")
    public void setIsotropicGaussian(boolean isotropicGaussian) {
        this.isotropicGaussian = isotropicGaussian;
    }

    @SetJIPipeDocumentation(name = "x-Order", description = "The differentiation order (up tp 10) in the x-dimension. " +
            "Order 0 in any dimension implies that only smoothing is applied in that dimension. The differentiation operation is carried out for each time frame and channel in a 5D image.")
    @JIPipeParameter("x-order")
    public int getxOrder() {
        return xOrder;
    }

    @JIPipeParameter("x-order")
    public void setxOrder(int xOrder) {
        this.xOrder = xOrder;
    }

    @SetJIPipeDocumentation(name = "y-Order", description = "The differentiation order (up tp 10) in the y-dimension. " +
            "Order 0 in any dimension implies that only smoothing is applied in that dimension. The differentiation operation is carried out for each time frame and channel in a 5D image.")
    @JIPipeParameter("y-order")
    public int getyOrder() {
        return yOrder;
    }

    @JIPipeParameter("y-order")
    public void setyOrder(int yOrder) {
        this.yOrder = yOrder;
    }

    @SetJIPipeDocumentation(name = "z-Order", description = "The differentiation order (up tp 10) in the z-dimension. " +
            "Order 0 in any dimension implies that only smoothing is applied in that dimension. The differentiation operation is carried out for each time frame and channel in a 5D image.")
    @JIPipeParameter("z-order")
    public int getzOrder() {
        return zOrder;
    }

    @JIPipeParameter("z-order")
    public void setzOrder(int zOrder) {
        this.zOrder = zOrder;
    }

    @SetJIPipeDocumentation(name = "Smoothing scale", description = "The standard deviation of the Gaussian kernel. Must be larger than zero.")
    @JIPipeParameter("smoothing-scale")
    public double getSmoothingScale() {
        return smoothingScale;
    }

    @JIPipeParameter("smoothing-scale")
    public void setSmoothingScale(double smoothingScale) {
        this.smoothingScale = smoothingScale;
    }

}
