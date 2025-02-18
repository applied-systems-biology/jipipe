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
import imagescience.feature.Hessian;
import imagescience.image.*;
import org.hkijena.jipipe.api.*;
import org.hkijena.jipipe.api.nodes.*;
import org.hkijena.jipipe.api.nodes.algorithm.JIPipeSimpleIteratingAlgorithm;
import org.hkijena.jipipe.api.nodes.categories.ImageJNodeTypeCategory;
import org.hkijena.jipipe.api.nodes.categories.ImagesNodeTypeCategory;
import org.hkijena.jipipe.api.nodes.iterationstep.JIPipeIterationContext;
import org.hkijena.jipipe.api.nodes.iterationstep.JIPipeSingleIterationStep;
import org.hkijena.jipipe.api.parameters.JIPipeParameter;
import org.hkijena.jipipe.plugins.imagejalgorithms.utils.ImageScienceUtils;
import org.hkijena.jipipe.plugins.imagejdatatypes.datatypes.ImagePlusData;
import org.hkijena.jipipe.plugins.imagejdatatypes.datatypes.greyscale.ImagePlusGreyscale32FData;
import org.hkijena.jipipe.plugins.imagejdatatypes.datatypes.greyscale.ImagePlusGreyscaleData;
import org.hkijena.jipipe.plugins.imagejdatatypes.util.ImageJUtils;
import org.hkijena.jipipe.plugins.imagejdatatypes.util.ImageSliceIndex;

import java.util.HashMap;
import java.util.Map;
import java.util.Vector;

@SetJIPipeDocumentation(name = "Hessian 2D/3D (FeatureJ)", description = "Computes Hessian the eigenvalues of the Hessian, which can be used for example to discriminate locally between plate-like, line-like, and blob-like image structures. " +
        "All largest eigenvalues are put in a separate image, as are all smallest, and in 3D all middle as well. " +
        "If the size of the image is unity in the z-dimension (single slice), the node computes 2D-Hessian eigenvalues, otherwise it computes 3D-Hessian eigenvalues (for each time frame and channel in a 5D image).")
@ConfigureJIPipeNode(menuPath = "Features", nodeTypeCategory = ImagesNodeTypeCategory.class)
@AddJIPipeInputSlot(value = ImagePlusGreyscaleData.class, name = "Input", create = true)
@AddJIPipeOutputSlot(value = ImagePlusGreyscale32FData.class, name = "Smallest", create = true, description = "The largest eigenvalue of Hessian tensor")
@AddJIPipeOutputSlot(value = ImagePlusGreyscale32FData.class, name = "Middle", create = true, description = "The middle eigenvalue of Hessian tensor. Only for 3D data")
@AddJIPipeOutputSlot(value = ImagePlusGreyscale32FData.class, name = "Largest", create = true, description = "The largest eigenvalue of Hessian tensor")
@AddJIPipeNodeAlias(nodeTypeCategory = ImageJNodeTypeCategory.class, menuPath = "Plugins\nFeatureJ", aliasName = "FeatureJ Hessian")
@AddJIPipeCitation("Y. Sato, S. Nakajima, N. Shiraga, H. Atsumi, S. Yoshida, T. Koller, G. Gerig, R. Kikinis Three-Dimensional Multi-Scale Line Filter for Segmentation and Visualization of Curvilinear Structures in Medical Images Medical Image Analysis, vol. 2, no. 2, June 1998, pp. 143-168")
@AddJIPipeCitation("A. F. Frangi, W. J. Niessen, R. M. Hoogeveen, T. van Walsum, M. A. Viergever Model-Based Quantitation of 3D Magnetic Resonance Angiographic Images IEEE Transactions on Medical Imaging, vol. 18, no. 10, October 1999, pp. 946-956")
@AddJIPipeCitation("K. Rohr Landmark-Based Image Analysis using Geometric and Intensity Models Kluwer Academic Publishers, 2001")
@AddJIPipeCitation("see https://imagescience.org/meijering/software/featurej/hessian/")
public class HessianFeatureAlgorithm extends JIPipeSimpleIteratingAlgorithm {

    private double smoothing = 1.0;
    private boolean compareAbsolute = true;
    private boolean force2D = false;
    private boolean isotropicGaussian = false;

    /**
     * Instantiates a new node type.
     *
     * @param info the info
     */
    public HessianFeatureAlgorithm(JIPipeNodeInfo info) {
        super(info);
    }

    /**
     * Instantiates a new node type.
     *
     * @param other the other
     */
    public HessianFeatureAlgorithm(HessianFeatureAlgorithm other) {
        super(other);
        this.smoothing = other.smoothing;
        this.compareAbsolute = other.compareAbsolute;
        this.force2D = other.force2D;
        this.isotropicGaussian = other.isotropicGaussian;
    }

    @Override
    public boolean supportsParallelization() {
        return true;
    }

    @Override
    protected void runIteration(JIPipeSingleIterationStep iterationStep, JIPipeIterationContext iterationContext, JIPipeGraphNodeRunContext runContext, JIPipeProgressInfo progressInfo) {
        ImagePlus input = iterationStep.getInputData(getFirstInputSlot(), ImagePlusGreyscaleData.class, progressInfo).getImage();

        if (force2D) {
            Map<ImageSliceIndex, ImageProcessor> resultSmallestMap = new HashMap<>();
            Map<ImageSliceIndex, ImageProcessor> resultLargestMap = new HashMap<>();
            ImageJUtils.forEachIndexedZCTSliceWithProgress(input, (imp, index, sliceProgress) -> {
                ImagePlus slice = new ImagePlus("slice", imp);
                slice.copyScale(input);
                Vector<Image> eigenImages = applyHessian(slice, sliceProgress);
                resultSmallestMap.put(index, ImageScienceUtils.unwrap(eigenImages.get(1), input).getProcessor());
                resultLargestMap.put(index, ImageScienceUtils.unwrap(eigenImages.get(0), input).getProcessor());
            }, progressInfo);
            ImagePlus resultSmallest = ImageJUtils.mergeMappedSlices(resultSmallestMap);
            ImagePlus resultLargest = ImageJUtils.mergeMappedSlices(resultLargestMap);
            resultSmallest.copyScale(input);
            resultLargest.copyScale(input);
            iterationStep.addOutputData("Smallest", new ImagePlusData(resultSmallest), progressInfo);
            iterationStep.addOutputData("Largest", new ImagePlusData(resultLargest), progressInfo);
        } else {
            Vector<Image> eigenImages = applyHessian(input, progressInfo);
            if(eigenImages.size() == 2) {
                ImagePlus smallest = ImageScienceUtils.unwrap(eigenImages.get(1), input);
                ImagePlus largest = ImageScienceUtils.unwrap(eigenImages.get(0), input);
                iterationStep.addOutputData("Smallest", new ImagePlusData(smallest), progressInfo);
                iterationStep.addOutputData("Largest", new ImagePlusData(largest), progressInfo);
            }
            else if(eigenImages.size() == 3) {
                ImagePlus smallest = ImageScienceUtils.unwrap(eigenImages.get(2), input);
                ImagePlus middle = ImageScienceUtils.unwrap(eigenImages.get(1), input);
                ImagePlus largest = ImageScienceUtils.unwrap(eigenImages.get(0), input);
                iterationStep.addOutputData("Smallest", new ImagePlusData(smallest), progressInfo);
                iterationStep.addOutputData("Middle", new ImagePlusData(middle), progressInfo);
                iterationStep.addOutputData("Largest", new ImagePlusData(largest), progressInfo);
            }
        }
    }

    private Vector<Image> applyHessian(ImagePlus input, JIPipeProgressInfo progressInfo) {
        final Image image = Image.wrap(input);
        if (!isotropicGaussian) {
            image.aspects(new Aspects());
        }
        return run(new FloatImage(image), smoothing, compareAbsolute, progressInfo);
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

    @SetJIPipeDocumentation(name = "Force 2D", description = "If enabled, each 2D image slice is processed individually.")
    @JIPipeParameter(value = "force-2d", important = true)
    public boolean isForce2D() {
        return force2D;
    }

    @JIPipeParameter("force-2d")
    public void setForce2D(boolean force2D) {
        this.force2D = force2D;
    }

    @JIPipeParameter("smoothing-scale")
    @SetJIPipeDocumentation(name = "Smoothing scale", description = "The smoothing scale at which the required image derivatives are computed. " +
            "The scale is equal to the standard deviation of the Gaussian kernel used for differentiation and must be larger than zero. " +
            "In order to enforce physical isotropy, for each dimension, the scale is divided by the size of the image elements (aspect ratio) in that dimension.")
    public double getSmoothing() {
        return smoothing;
    }

    @JIPipeParameter("smoothing-scale")
    public void setSmoothing(double smoothing) {
        this.smoothing = smoothing;

    }

    @SetJIPipeDocumentation(name = "Compare absolute", description = "Determines whether eigenvalues are compared in absolute sense")
    @JIPipeParameter("compare-absolute")
    public boolean isCompareAbsolute() {
        return compareAbsolute;
    }

    @JIPipeParameter("compare-absolute")
    public void setCompareAbsolute(boolean compareAbsolute) {
        this.compareAbsolute = compareAbsolute;

    }

    public static Vector<Image> run(final Image image, final double scale, final boolean absolute, JIPipeProgressInfo progressInfo) {

        progressInfo.log(ImageScience.prelude() + "Hessian");

        // Initialize:
        progressInfo.log("Checking arguments");
        if (scale <= 0) throw new IllegalArgumentException("Smoothing scale less than or equal to 0");

        final Dimensions dims = image.dimensions();
        progressInfo.log("Input image dimensions: (x,y,z,t,c) = (" + dims.x + "," + dims.y + "," + dims.z + "," + dims.t + "," + dims.c + ")");

        final Aspects asps = image.aspects();
        progressInfo.log("Element aspect ratios: (" + asps.x + "," + asps.y + "," + asps.z + "," + asps.t + "," + asps.c + ")");
        if (asps.x <= 0) throw new IllegalStateException("Aspect ratio in x-dimension less than or equal to 0");
        if (asps.y <= 0) throw new IllegalStateException("Aspect ratio in y-dimension less than or equal to 0");
        if (asps.z <= 0) throw new IllegalStateException("Aspect ratio in z-dimension less than or equal to 0");

        final Image smoothImage = (image instanceof FloatImage) ? image : new FloatImage(image);
        Vector<Image> eigenimages = null;
        final String name = image.name();

        // Compute Hessian matrix and eigenimages:
        if (dims.z == 1) { // 2D case

            final double[] pls = {0, 0.32, 0.64, 0.96, 1};
            int pl = 0;

            // Compute Hessian components:
            final Image Hxx = DerivativesFeaturesAlgorithm.run(smoothImage.duplicate(), scale, 2, 0, 0, progressInfo.resolveAndLog("Computing Hxx"));
            final Image Hxy = DerivativesFeaturesAlgorithm.run(smoothImage.duplicate(), scale, 1, 1, 0, progressInfo.resolveAndLog("Computing Hxy"));
            final Image Hyy = DerivativesFeaturesAlgorithm.run(smoothImage, scale, 0, 2, 0, progressInfo.resolveAndLog("Computing Hyy"));

            // Compute eigenimages (Hxx and Hyy are reused to save memory):
            progressInfo.log("Computing eigenimages");
            final int maxNumEigenImages = dims.c * dims.t * dims.y;
            int numEigenImages = 0;
            JIPipePercentageProgressInfo eigenImagesPercentageProgress = progressInfo.percentage("Computing eigenimages");
            Hxx.axes(Axes.X);
            Hxy.axes(Axes.X);
            Hyy.axes(Axes.X);
            final double[] ahxx = new double[dims.x];
            final double[] ahxy = new double[dims.x];
            final double[] ahyy = new double[dims.x];
            final Coordinates coords = new Coordinates();

            if (absolute) {
                progressInfo.log("Comparing and storing absolute eigenvalues");
                for (coords.c = 0; coords.c < dims.c; ++coords.c)
                    for (coords.t = 0; coords.t < dims.t; ++coords.t)
                        for (coords.y = 0; coords.y < dims.y; ++coords.y) {
                            Hxx.get(coords, ahxx);
                            Hxy.get(coords, ahxy);
                            Hyy.get(coords, ahyy);
                            for (int x = 0; x < dims.x; ++x) {
                                final double b = -(ahxx[x] + ahyy[x]);
                                final double c = ahxx[x] * ahyy[x] - ahxy[x] * ahxy[x];
                                final double q = -0.5 * (b + (b < 0 ? -1 : 1) * Math.sqrt(b * b - 4 * c));
                                double absh1, absh2;
                                if (q == 0) {
                                    absh1 = 0;
                                    absh2 = 0;
                                } else {
                                    absh1 = Math.abs(q);
                                    absh2 = Math.abs(c / q);
                                }
                                if (absh1 > absh2) {
                                    ahxx[x] = absh1;
                                    ahyy[x] = absh2;
                                } else {
                                    ahxx[x] = absh2;
                                    ahyy[x] = absh1;
                                }
                            }
                            Hxx.set(coords, ahxx);
                            Hyy.set(coords, ahyy);
                            eigenImagesPercentageProgress.logPercentage(numEigenImages++, maxNumEigenImages);
                        }
            } else {
                progressInfo.log("Comparing and storing actual eigenvalues");
                for (coords.c = 0; coords.c < dims.c; ++coords.c)
                    for (coords.t = 0; coords.t < dims.t; ++coords.t)
                        for (coords.y = 0; coords.y < dims.y; ++coords.y) {
                            Hxx.get(coords, ahxx);
                            Hxy.get(coords, ahxy);
                            Hyy.get(coords, ahyy);
                            for (int x = 0; x < dims.x; ++x) {
                                final double b = -(ahxx[x] + ahyy[x]);
                                final double c = ahxx[x] * ahyy[x] - ahxy[x] * ahxy[x];
                                final double q = -0.5 * (b + (b < 0 ? -1 : 1) * Math.sqrt(b * b - 4 * c));
                                double h1, h2;
                                if (q == 0) {
                                    h1 = 0;
                                    h2 = 0;
                                } else {
                                    h1 = q;
                                    h2 = c / q;
                                }
                                if (h1 > h2) {
                                    ahxx[x] = h1;
                                    ahyy[x] = h2;
                                } else {
                                    ahxx[x] = h2;
                                    ahyy[x] = h1;
                                }
                            }
                            Hxx.set(coords, ahxx);
                            Hyy.set(coords, ahyy);
                            eigenImagesPercentageProgress.logPercentage(numEigenImages++, maxNumEigenImages);
                        }
            }

            Hxx.name(name + " largest Hessian eigenvalues");
            Hyy.name(name + " smallest Hessian eigenvalues");

            Hxx.aspects(asps.duplicate());
            Hyy.aspects(asps.duplicate());

            eigenimages = new Vector<Image>(2);
            eigenimages.add(Hxx);
            eigenimages.add(Hyy);

        } else { // 3D case

            final double[] pls = {0, 0.1, 0.2, 0.3, 0.4, 0.5, 0.6, 1};
            int pl = 0;

            // Compute Hessian components:
            final Image Hxx = DerivativesFeaturesAlgorithm.run(smoothImage.duplicate(), scale, 2, 0, 0, progressInfo.resolveAndLog("Computing Hxx"));
            final Image Hxy = DerivativesFeaturesAlgorithm.run(smoothImage.duplicate(), scale, 1, 1, 0, progressInfo.resolveAndLog("Computing Hxy"));
            final Image Hxz = DerivativesFeaturesAlgorithm.run(smoothImage.duplicate(), scale, 1, 0, 1, progressInfo.resolveAndLog("Computing Hxz"));
            final Image Hyy = DerivativesFeaturesAlgorithm.run(smoothImage.duplicate(), scale, 0, 2, 0, progressInfo.resolveAndLog("Computing Hyy"));
            final Image Hyz = DerivativesFeaturesAlgorithm.run(smoothImage.duplicate(), scale, 0, 1, 1, progressInfo.resolveAndLog("Computing Hyz"));
            final Image Hzz = DerivativesFeaturesAlgorithm.run(smoothImage, scale, 0, 0, 2, progressInfo.resolveAndLog("Computing Hyz"));

            // Compute eigenimages (Hxx, Hyy, Hzz are reused to save memory):
            final int maxNumEigenImages = dims.c * dims.t * dims.z * dims.y;
            int numEigenImages = 0;
            JIPipePercentageProgressInfo eigenImagesPercentageProgress = progressInfo.percentage("Computing eigenimages");

            Hxx.axes(Axes.X);
            Hxy.axes(Axes.X);
            Hxz.axes(Axes.X);
            Hyy.axes(Axes.X);
            Hyz.axes(Axes.X);
            Hzz.axes(Axes.X);
            final double[] ahxx = new double[dims.x];
            final double[] ahxy = new double[dims.x];
            final double[] ahxz = new double[dims.x];
            final double[] ahyy = new double[dims.x];
            final double[] ahyz = new double[dims.x];
            final double[] ahzz = new double[dims.x];
            final Coordinates coords = new Coordinates();

            if (absolute) {
                progressInfo.log("Comparing and storing absolute eigenvalues");
                for (coords.c = 0; coords.c < dims.c; ++coords.c)
                    for (coords.t = 0; coords.t < dims.t; ++coords.t)
                        for (coords.z = 0; coords.z < dims.z; ++coords.z)
                            for (coords.y = 0; coords.y < dims.y; ++coords.y) {
                                Hxx.get(coords, ahxx);
                                Hxy.get(coords, ahxy);
                                Hxz.get(coords, ahxz);
                                Hyy.get(coords, ahyy);
                                Hyz.get(coords, ahyz);
                                Hzz.get(coords, ahzz);
                                for (int x = 0; x < dims.x; ++x) {
                                    final double fhxx = ahxx[x];
                                    final double fhxy = ahxy[x];
                                    final double fhxz = ahxz[x];
                                    final double fhyy = ahyy[x];
                                    final double fhyz = ahyz[x];
                                    final double fhzz = ahzz[x];
                                    final double a = -(fhxx + fhyy + fhzz);
                                    final double b = fhxx * fhyy + fhxx * fhzz + fhyy * fhzz - fhxy * fhxy - fhxz * fhxz - fhyz * fhyz;
                                    final double c = fhxx * (fhyz * fhyz - fhyy * fhzz) + fhyy * fhxz * fhxz + fhzz * fhxy * fhxy - 2 * fhxy * fhxz * fhyz;
                                    final double q = (a * a - 3 * b) / 9;
                                    final double r = (a * a * a - 4.5 * a * b + 13.5 * c) / 27;
                                    final double sqrtq = (q > 0) ? Math.sqrt(q) : 0;
                                    final double sqrtq3 = sqrtq * sqrtq * sqrtq;
                                    double absh1, absh2, absh3;
                                    if (sqrtq3 == 0) {
                                        absh1 = 0;
                                        absh2 = 0;
                                        absh3 = 0;
                                    } else {
                                        final double rsqq3 = r / sqrtq3;
                                        final double angle = (rsqq3 * rsqq3 <= 1) ? Math.acos(rsqq3) : Math.acos(rsqq3 < 0 ? -1 : 1);
                                        absh1 = Math.abs(-2 * sqrtq * Math.cos(angle / 3) - a / 3);
                                        absh2 = Math.abs(-2 * sqrtq * Math.cos((angle + TWOPI) / 3) - a / 3);
                                        absh3 = Math.abs(-2 * sqrtq * Math.cos((angle - TWOPI) / 3) - a / 3);
                                    }
                                    if (absh2 < absh3) {
                                        final double tmp = absh2;
                                        absh2 = absh3;
                                        absh3 = tmp;
                                    }
                                    if (absh1 < absh2) {
                                        final double tmp1 = absh1;
                                        absh1 = absh2;
                                        absh2 = tmp1;
                                        if (absh2 < absh3) {
                                            final double tmp2 = absh2;
                                            absh2 = absh3;
                                            absh3 = tmp2;
                                        }
                                    }
                                    ahxx[x] = absh1;
                                    ahyy[x] = absh2;
                                    ahzz[x] = absh3;
                                }
                                Hxx.set(coords, ahxx);
                                Hyy.set(coords, ahyy);
                                Hzz.set(coords, ahzz);
                                eigenImagesPercentageProgress.logPercentage(numEigenImages++, maxNumEigenImages);
                            }
            } else {
                progressInfo.log("Comparing and storing actual eigenvalues");
                for (coords.c = 0; coords.c < dims.c; ++coords.c)
                    for (coords.t = 0; coords.t < dims.t; ++coords.t)
                        for (coords.z = 0; coords.z < dims.z; ++coords.z)
                            for (coords.y = 0; coords.y < dims.y; ++coords.y) {
                                Hxx.get(coords, ahxx);
                                Hxy.get(coords, ahxy);
                                Hxz.get(coords, ahxz);
                                Hyy.get(coords, ahyy);
                                Hyz.get(coords, ahyz);
                                Hzz.get(coords, ahzz);
                                for (int x = 0; x < dims.x; ++x) {
                                    final double fhxx = ahxx[x];
                                    final double fhxy = ahxy[x];
                                    final double fhxz = ahxz[x];
                                    final double fhyy = ahyy[x];
                                    final double fhyz = ahyz[x];
                                    final double fhzz = ahzz[x];
                                    final double a = -(fhxx + fhyy + fhzz);
                                    final double b = fhxx * fhyy + fhxx * fhzz + fhyy * fhzz - fhxy * fhxy - fhxz * fhxz - fhyz * fhyz;
                                    final double c = fhxx * (fhyz * fhyz - fhyy * fhzz) + fhyy * fhxz * fhxz + fhzz * fhxy * fhxy - 2 * fhxy * fhxz * fhyz;
                                    final double q = (a * a - 3 * b) / 9;
                                    final double r = (a * a * a - 4.5 * a * b + 13.5 * c) / 27;
                                    final double sqrtq = (q > 0) ? Math.sqrt(q) : 0;
                                    final double sqrtq3 = sqrtq * sqrtq * sqrtq;
                                    double h1, h2, h3;
                                    if (sqrtq3 == 0) {
                                        h1 = 0;
                                        h2 = 0;
                                        h3 = 0;
                                    } else {
                                        final double rsqq3 = r / sqrtq3;
                                        final double angle = (rsqq3 * rsqq3 <= 1) ? Math.acos(rsqq3) : Math.acos(rsqq3 < 0 ? -1 : 1);
                                        h1 = -2 * sqrtq * Math.cos(angle / 3) - a / 3;
                                        h2 = -2 * sqrtq * Math.cos((angle + TWOPI) / 3) - a / 3;
                                        h3 = -2 * sqrtq * Math.cos((angle - TWOPI) / 3) - a / 3;
                                    }
                                    if (h2 < h3) {
                                        final double tmp = h2;
                                        h2 = h3;
                                        h3 = tmp;
                                    }
                                    if (h1 < h2) {
                                        final double tmp1 = h1;
                                        h1 = h2;
                                        h2 = tmp1;
                                        if (h2 < h3) {
                                            final double tmp2 = h2;
                                            h2 = h3;
                                            h3 = tmp2;
                                        }
                                    }
                                    ahxx[x] = h1;
                                    ahyy[x] = h2;
                                    ahzz[x] = h3;
                                }
                                Hxx.set(coords, ahxx);
                                Hyy.set(coords, ahyy);
                                Hzz.set(coords, ahzz);
                                eigenImagesPercentageProgress.logPercentage(numEigenImages++, maxNumEigenImages);
                            }
            }

            Hxx.name(name + " largest Hessian eigenvalues");
            Hyy.name(name + " middle Hessian eigenvalues");
            Hzz.name(name + " smallest Hessian eigenvalues");

            Hxx.aspects(asps.duplicate());
            Hyy.aspects(asps.duplicate());
            Hzz.aspects(asps.duplicate());

            eigenimages = new Vector<Image>(3);
            eigenimages.add(Hxx);
            eigenimages.add(Hyy);
            eigenimages.add(Hzz);
        }

        return eigenimages;
    }

    private static final double TWOPI = 2*Math.PI;
}
