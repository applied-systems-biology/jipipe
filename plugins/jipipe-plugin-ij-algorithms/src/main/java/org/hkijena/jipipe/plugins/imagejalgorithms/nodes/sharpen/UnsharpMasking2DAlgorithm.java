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

package org.hkijena.jipipe.plugins.imagejalgorithms.nodes.sharpen;

import ij.ImagePlus;
import ij.plugin.filter.GaussianBlur;
import ij.plugin.filter.UnsharpMask;
import ij.process.FloatProcessor;
import ij.process.ImageProcessor;
import org.hkijena.jipipe.api.ConfigureJIPipeNode;
import org.hkijena.jipipe.api.JIPipeProgressInfo;
import org.hkijena.jipipe.api.SetJIPipeDocumentation;
import org.hkijena.jipipe.api.nodes.*;
import org.hkijena.jipipe.api.nodes.algorithm.JIPipeSimpleIteratingAlgorithm;
import org.hkijena.jipipe.api.nodes.categories.ImageJNodeTypeCategory;
import org.hkijena.jipipe.api.nodes.categories.ImagesNodeTypeCategory;
import org.hkijena.jipipe.api.nodes.iterationstep.JIPipeIterationContext;
import org.hkijena.jipipe.api.nodes.iterationstep.JIPipeSingleIterationStep;
import org.hkijena.jipipe.api.parameters.JIPipeParameter;
import org.hkijena.jipipe.plugins.imagejdatatypes.datatypes.ImagePlusData;
import org.hkijena.jipipe.plugins.imagejdatatypes.datatypes.greyscale.ImagePlusGreyscale32FData;
import org.hkijena.jipipe.plugins.imagejdatatypes.util.ImageJUtils;

import java.awt.*;
import java.util.Arrays;


/**
 * Wrapper around {@link ImageProcessor}
 */
@SetJIPipeDocumentation(name = "Unsharp mask 2D", description = "Subtracts a blurred copy of the image and rescales the image to obtain the same contrast of large " +
        "(low-frequency) structures as in the input image. This is equivalent to adding a high-pass filtered image and thus sharpens the image. " +
        "\"Radius (Sigma)\" is the standard deviation (blur radius) of the Gaussian blur that is subtracted. \"Mask Weight\" " +
        "determines the strength of filtering, where \"Mask Weight\"=1 would be an infinite weight of the high-pass filtered image that is added. " +
        "If higher-dimensional data is provided, the filter is applied to each 2D slice.")
@ConfigureJIPipeNode(menuPath = "Sharpen", nodeTypeCategory = ImagesNodeTypeCategory.class)
@AddJIPipeInputSlot(value = ImagePlusGreyscale32FData.class, name = "Input", create = true)
@AddJIPipeOutputSlot(value = ImagePlusGreyscale32FData.class, name = "Output", create = true)
@AddJIPipeNodeAlias(nodeTypeCategory = ImageJNodeTypeCategory.class, menuPath = "Process", aliasName = "Unsharp mask")
public class UnsharpMasking2DAlgorithm extends JIPipeSimpleIteratingAlgorithm {

    private double maskWeight = 0.6;
    private double sigma = 1;

    public UnsharpMasking2DAlgorithm(JIPipeNodeInfo info) {
        super(info);
    }

    public UnsharpMasking2DAlgorithm(UnsharpMasking2DAlgorithm other) {
        super(other);
        this.maskWeight = other.maskWeight;
        this.sigma = other.sigma;
    }

    @Override
    protected void runIteration(JIPipeSingleIterationStep iterationStep, JIPipeIterationContext iterationContext, JIPipeGraphNodeRunContext runContext, JIPipeProgressInfo progressInfo) {
        ImagePlusGreyscale32FData inputData = iterationStep.getInputData(getFirstInputSlot(), ImagePlusGreyscale32FData.class, progressInfo);
        ImagePlus img = inputData.getDuplicateImage();
        ImageJUtils.forEachSlice(img, this::applyUnsharpMask, progressInfo);
        iterationStep.addOutputData(getFirstOutputSlot(), new ImagePlusGreyscale32FData(img), progressInfo);
    }

    private void applyUnsharpMask(ImageProcessor ip) {

        float[] originalPixels = (float[]) ip.getPixels();
        originalPixels = Arrays.copyOf(originalPixels, originalPixels.length);

        GaussianBlur gb = new GaussianBlur();
        gb.blurGaussian(ip, sigma, sigma, 0.01);

        if (Thread.currentThread().isInterrupted()) {
            return;
        }

        float[] pixels = (float[]) ip.getPixels();
        int width = ip.getWidth();
        int height = ip.getHeight();
        for (int y = 0; y < height; y++) {
            for (int x = 0, p = width * y + x; x < width; x++, p++) {
                pixels[p] = (float) ((originalPixels[p] - maskWeight * pixels[p]) / (1f - maskWeight));
            }
        }
    }

    @SetJIPipeDocumentation(name = "Mask weight (0.1-0.9)", description = "Determines the strength of filtering, where a value of 1 would be an infinite weight of the high-pass filtered image that is added")
    @JIPipeParameter("mask-weight")
    public double getMaskWeight() {
        return maskWeight;
    }

    @JIPipeParameter("mask-weight")
    public void setMaskWeight(double maskWeight) {
        this.maskWeight = maskWeight;
    }

    @SetJIPipeDocumentation(name = "Radius (Sigma)", description = "The standard deviation (blur radius) of the Gaussian blur that is subtracted")
    @JIPipeParameter("sigma")
    public double getSigma() {
        return sigma;
    }

    @JIPipeParameter("sigma")
    public void setSigma(double sigma) {
        this.sigma = sigma;
    }

    @Override
    public boolean supportsParallelization() {
        return true;
    }
}
