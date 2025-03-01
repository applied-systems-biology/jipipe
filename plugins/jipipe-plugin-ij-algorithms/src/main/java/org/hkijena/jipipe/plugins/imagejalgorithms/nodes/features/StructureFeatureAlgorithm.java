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

@SetJIPipeDocumentation(name = "Structure 2D/3D (FeatureJ)", description = "Computes for each image element (pixel/voxel) the eigenvalues of the so-called structure tensor, which can be used for example in texture analysis and image enhancement filtering. " +
        "Due to the positive-semi-definiteness of the structure tensor, the eigenvalues are always larger than or equal to zero. If the size of the image is unity in the z-dimension (single slice), the node computes 2D-tensor eigenvalues, otherwise it computes 3D-tensor eigenvalues (for each time frame and channel in a 5D image).")
@ConfigureJIPipeNode(menuPath = "Features", nodeTypeCategory = ImagesNodeTypeCategory.class)
@AddJIPipeInputSlot(value = ImagePlusGreyscaleData.class, name = "Input", create = true)
@AddJIPipeOutputSlot(value = ImagePlusGreyscale32FData.class, name = "Smallest", create = true, description = "The largest eigenvalue of Structure tensor")
@AddJIPipeOutputSlot(value = ImagePlusGreyscale32FData.class, name = "Middle", create = true, description = "The middle eigenvalue of Structure tensor. Only for 3D data")
@AddJIPipeOutputSlot(value = ImagePlusGreyscale32FData.class, name = "Largest", create = true, description = "The largest eigenvalue of Structure tensor")
@AddJIPipeNodeAlias(nodeTypeCategory = ImageJNodeTypeCategory.class, menuPath = "Plugins\nFeatureJ", aliasName = "FeatureJ Structure")
@AddJIPipeCitation("A. R. Rao and B. G. Schunck Computing Oriented Texture Fields CVGIP: Graphical Models and Image Processing, vol. 53, no. 2, March 1991, pp. 157-185")
@AddJIPipeCitation("J. Weickert Coherence-Enhancing Diffusion Filtering International Journal of Computer Vision, vol. 31, no. 2/3, April 1999, pp. 111-127")
@AddJIPipeCitation("see https://imagescience.org/meijering/software/featurej/structure/")
public class StructureFeatureAlgorithm extends JIPipeSimpleIteratingAlgorithm {

    private double smoothing = 1.0;
    private double integrationScale = 3;
    private boolean force2D = false;
    private boolean isotropicGaussian = false;

    /**
     * Instantiates a new node type.
     *
     * @param info the info
     */
    public StructureFeatureAlgorithm(JIPipeNodeInfo info) {
        super(info);
    }

    /**
     * Instantiates a new node type.
     *
     * @param other the other
     */
    public StructureFeatureAlgorithm(StructureFeatureAlgorithm other) {
        super(other);
        this.smoothing = other.smoothing;
        this.integrationScale = other.integrationScale;
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
            if (eigenImages.size() == 2) {
                ImagePlus smallest = ImageScienceUtils.unwrap(eigenImages.get(1), input);
                ImagePlus largest = ImageScienceUtils.unwrap(eigenImages.get(0), input);
                iterationStep.addOutputData("Smallest", new ImagePlusData(smallest), progressInfo);
                iterationStep.addOutputData("Largest", new ImagePlusData(largest), progressInfo);
            } else if (eigenImages.size() == 3) {
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
        return run(new FloatImage(image), smoothing, integrationScale, progressInfo);
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
    @SetJIPipeDocumentation(name = "Smoothing scale", description = "The standard deviation of the Gaussian derivative kernels used for computing the elements of the structure tensor. Must be larger than zero. " +
            "In order to enforce physical isotropy, for each dimension, the scale is divided by the size of the image elements (aspect ratio) in that dimension.")
    public double getSmoothing() {
        return smoothing;
    }

    @JIPipeParameter("smoothing-scale")
    public void setSmoothing(double smoothing) {
        this.smoothing = smoothing;
    }

    @SetJIPipeDocumentation(name = "Integration scale", description = "The standard deviation of the Gaussian kernel used for smoothing the elements of the structure tensor. Must be larger than zero. " +
            "In order to enforce physical isotropy, for each dimension, the scale is divided by the size of the image elements (aspect ratio) in that dimension.")
    @JIPipeParameter("integration-scale")
    public double getIntegrationScale() {
        return integrationScale;
    }

    @JIPipeParameter("integration-scale")
    public void setIntegrationScale(double integrationScale) {
        this.integrationScale = integrationScale;
    }

    public static Vector<Image> run(final Image image, final double sscale, final double iscale, JIPipeProgressInfo progressInfo) {

        progressInfo.log(ImageScience.prelude() + "Structure");


        // Initialize:
        progressInfo.log("Checking arguments");
        if (sscale <= 0) throw new IllegalArgumentException("Smoothing scale less than or equal to 0");
        if (iscale <= 0) throw new IllegalArgumentException("Integration scale less than or equal to 0");

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

        // Compute structure tensor and eigenimages:
        if (dims.z == 1) { // 2D case

            final double[] pls = {0, 0.2, 0.4, 0.45, 0.63, 0.80, 0.95, 1};
            int pl = 0;

            // Compute structure tensor components:
            final Image Ix2 = DerivativesFeaturesAlgorithm.run(smoothImage.duplicate(), sscale, 1, 0, 0, progressInfo.resolveAndLog("Computing Ix"));
            final Image Iy2 = DerivativesFeaturesAlgorithm.run(smoothImage, sscale, 0, 1, 0, progressInfo.resolveAndLog("Computing Iy"));

            final Image IxIy = Ix2.duplicate();
            progressInfo.log("Computing IxIy");
            IxIy.multiply(Iy2);
            progressInfo.log("Squaring Ix");
            Ix2.square();
            progressInfo.log("Squaring Iy");
            Iy2.square();

            // Integrate tensor components:
            progressInfo.log("Gaussian integration at scale " + iscale);
            DerivativesFeaturesAlgorithm.run(Ix2, iscale, 0, 0, 0, progressInfo.resolveAndLog("Integrating IxIx"));
            DerivativesFeaturesAlgorithm.run(IxIy, iscale, 0, 0, 0, progressInfo.resolveAndLog("Integrating IxIy"));
            DerivativesFeaturesAlgorithm.run(Iy2, iscale, 0, 0, 0, progressInfo.resolveAndLog("Integrating IyIy"));

            // Compute eigenimages (Ix2 and Iy2 are reused to save memory):
            JIPipePercentageProgressInfo eigenImagesPercentageProgress = progressInfo.percentage("Computing eigenimages");
            final int maxNumEigenImages = dims.c * dims.t * dims.y;
            int numEigenImages = 0;
            Ix2.axes(Axes.X);
            IxIy.axes(Axes.X);
            Iy2.axes(Axes.X);
            final double[] axx = new double[dims.x];
            final double[] axy = new double[dims.x];
            final double[] ayy = new double[dims.x];
            final Coordinates coords = new Coordinates();
            progressInfo.log("Comparing and storing eigenvalues");

            for (coords.c = 0; coords.c < dims.c; ++coords.c)
                for (coords.t = 0; coords.t < dims.t; ++coords.t)
                    for (coords.y = 0; coords.y < dims.y; ++coords.y) {
                        Ix2.get(coords, axx);
                        IxIy.get(coords, axy);
                        Iy2.get(coords, ayy);
                        for (int x = 0; x < dims.x; ++x) {
                            final double b = -(axx[x] + ayy[x]);
                            final double c = axx[x] * ayy[x] - axy[x] * axy[x];
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
                                axx[x] = absh1;
                                ayy[x] = absh2;
                            } else {
                                axx[x] = absh2;
                                ayy[x] = absh1;
                            }
                        }
                        Ix2.set(coords, axx);
                        Iy2.set(coords, ayy);
                        eigenImagesPercentageProgress.logPercentage(numEigenImages++, maxNumEigenImages);
                    }

            Ix2.name(name + " largest structure eigenvalues");
            Iy2.name(name + " smallest structure eigenvalues");

            Ix2.aspects(asps.duplicate());
            Iy2.aspects(asps.duplicate());

            eigenimages = new Vector<Image>(2);
            eigenimages.add(Ix2);
            eigenimages.add(Iy2);

        } else { // 3D case

            final double[] pls = {0, 0.1, 0.2, 0.3, 0.34, 0.40, 0.46, 0.52, 0.58, 0.64, 0.7, 1};
            int pl = 0;

            // Compute structure tensor components:
            final Image Ix2 = DerivativesFeaturesAlgorithm.run(smoothImage.duplicate(), sscale, 1, 0, 0, progressInfo.resolveAndLog("Computing Ix"));
            final Image Iy2 = DerivativesFeaturesAlgorithm.run(smoothImage.duplicate(), sscale, 0, 1, 0, progressInfo.resolveAndLog("Computing Iy"));
            final Image Iz2 = DerivativesFeaturesAlgorithm.run(smoothImage, sscale, 0, 0, 1, progressInfo.resolveAndLog("Computing Iz"));

            progressInfo.log("Computing IxIy");
            final Image IxIy = Ix2.duplicate();
            IxIy.multiply(Iy2);
            progressInfo.log("Computing IxIz");
            final Image IxIz = Ix2.duplicate();
            IxIz.multiply(Iz2);
            progressInfo.log("Computing IyIz");
            final Image IyIz = Iy2.duplicate();
            IyIz.multiply(Iz2);
            progressInfo.log("Squaring Ix");
            Ix2.square();
            progressInfo.log("Squaring Iy");
            Iy2.square();
            progressInfo.log("Squaring Iz");
            Iz2.square();

            // Integrate tensor components:
            progressInfo.log("Gaussian integration at scale " + iscale);
            DerivativesFeaturesAlgorithm.run(Ix2, iscale, 0, 0, 0, progressInfo.resolveAndLog("Integrating IxIx"));
            DerivativesFeaturesAlgorithm.run(IxIy, iscale, 0, 0, 0, progressInfo.resolveAndLog("Integrating IxIy"));
            DerivativesFeaturesAlgorithm.run(IxIz, iscale, 0, 0, 0, progressInfo.resolveAndLog("Integrating IxIz"));
            DerivativesFeaturesAlgorithm.run(Iy2, iscale, 0, 0, 0, progressInfo.resolveAndLog("Integrating IyIy"));
            DerivativesFeaturesAlgorithm.run(IyIz, iscale, 0, 0, 0, progressInfo.resolveAndLog("Integrating IyIz"));
            DerivativesFeaturesAlgorithm.run(Iz2, iscale, 0, 0, 0, progressInfo.resolveAndLog("Integrating IzIz"));

            // Compute eigenimages (Ix2, Iy2, Iz2 are reused to save memory):
            JIPipePercentageProgressInfo eigenImagesPercentageProgress = progressInfo.percentage("Computing eigenimages");
            final int maxNumEigenImages = dims.c * dims.t * dims.z * dims.y;
            int numEigenImages = 0;

            Ix2.axes(Axes.X);
            IxIy.axes(Axes.X);
            IxIz.axes(Axes.X);
            Iy2.axes(Axes.X);
            IyIz.axes(Axes.X);
            Iz2.axes(Axes.X);
            final double[] axx = new double[dims.x];
            final double[] axy = new double[dims.x];
            final double[] axz = new double[dims.x];
            final double[] ayy = new double[dims.x];
            final double[] ayz = new double[dims.x];
            final double[] azz = new double[dims.x];
            final Coordinates coords = new Coordinates();
            progressInfo.log("Comparing and storing eigenvalues");

            for (coords.c = 0; coords.c < dims.c; ++coords.c)
                for (coords.t = 0; coords.t < dims.t; ++coords.t)
                    for (coords.z = 0; coords.z < dims.z; ++coords.z)
                        for (coords.y = 0; coords.y < dims.y; ++coords.y) {
                            Ix2.get(coords, axx);
                            IxIy.get(coords, axy);
                            IxIz.get(coords, axz);
                            Iy2.get(coords, ayy);
                            IyIz.get(coords, ayz);
                            Iz2.get(coords, azz);
                            for (int x = 0; x < dims.x; ++x) {
                                final double fxx = axx[x];
                                final double fxy = axy[x];
                                final double fxz = axz[x];
                                final double fyy = ayy[x];
                                final double fyz = ayz[x];
                                final double fzz = azz[x];
                                final double a = -(fxx + fyy + fzz);
                                final double b = fxx * fyy + fxx * fzz + fyy * fzz - fxy * fxy - fxz * fxz - fyz * fyz;
                                final double c = fxx * (fyz * fyz - fyy * fzz) + fyy * fxz * fxz + fzz * fxy * fxy - 2 * fxy * fxz * fyz;
                                final double q = (a * a - 3 * b) / 9;
                                final double r = (a * a * a - 4.5f * a * b + 13.5f * c) / 27f;
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
                                axx[x] = absh1;
                                ayy[x] = absh2;
                                azz[x] = absh3;
                            }
                            Ix2.set(coords, axx);
                            Iy2.set(coords, ayy);
                            Iz2.set(coords, azz);
                            eigenImagesPercentageProgress.logPercentage(numEigenImages++, maxNumEigenImages);
                        }

            Ix2.name(name + " largest structure eigenvalues");
            Iy2.name(name + " middle structure eigenvalues");
            Iz2.name(name + " smallest structure eigenvalues");

            Ix2.aspects(asps.duplicate());
            Iy2.aspects(asps.duplicate());
            Iz2.aspects(asps.duplicate());

            eigenimages = new Vector<>(3);
            eigenimages.add(Ix2);
            eigenimages.add(Iy2);
            eigenimages.add(Iz2);
        }


        return eigenimages;
    }

    private static final double TWOPI = 2 * Math.PI;
}
