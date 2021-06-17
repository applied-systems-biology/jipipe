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
 */

package org.hkijena.jipipe.extensions.imagejalgorithms.ij1.features;

import ij.ImagePlus;
import ij.plugin.filter.GaussianBlur;
import ij.process.ByteProcessor;
import ij.process.ColorProcessor;
import ij.process.ImageProcessor;
import org.hkijena.jipipe.api.JIPipeDocumentation;
import org.hkijena.jipipe.api.JIPipeOrganization;
import org.hkijena.jipipe.api.JIPipeProgressInfo;
import org.hkijena.jipipe.api.JIPipeValidityReport;
import org.hkijena.jipipe.api.nodes.JIPipeDataBatch;
import org.hkijena.jipipe.api.nodes.JIPipeInputSlot;
import org.hkijena.jipipe.api.nodes.JIPipeNodeInfo;
import org.hkijena.jipipe.api.nodes.JIPipeOutputSlot;
import org.hkijena.jipipe.api.nodes.JIPipeSimpleIteratingAlgorithm;
import org.hkijena.jipipe.api.nodes.categories.ImagesNodeTypeCategory;
import org.hkijena.jipipe.api.parameters.JIPipeParameter;
import org.hkijena.jipipe.extensions.imagejdatatypes.datatypes.ImagePlusData;
import org.hkijena.jipipe.extensions.imagejdatatypes.datatypes.greyscale.ImagePlusGreyscale32FData;
import org.hkijena.jipipe.extensions.imagejdatatypes.util.ImageJUtils;

/**
 * Wrapper around {@link GaussianBlur}
 */
@JIPipeDocumentation(name = "Difference of Gaussian 2D", description = "Applies Gaussian blur to the input image twice with different sigma values resulting in two images which are then subtracted from each other. " +
        "If higher-dimensional data is provided, the filter is applied to each 2D slice.")
@JIPipeOrganization(menuPath = "Blur", nodeTypeCategory = ImagesNodeTypeCategory.class)
@JIPipeInputSlot(value = ImagePlusGreyscale32FData.class, slotName = "Input", autoCreate = true)
@JIPipeOutputSlot(value = ImagePlusGreyscale32FData.class, slotName = "Output", autoCreate = true)
public class DifferenceOfGaussian2DAlgorithm extends JIPipeSimpleIteratingAlgorithm {

    private double sigma0X = 2;
    private double sigma0Y = -1;
    private double sigma1X = 10;
    private double sigma1Y = -1;

    /**
     * Instantiates a new node type.
     *
     * @param info the info
     */
    public DifferenceOfGaussian2DAlgorithm(JIPipeNodeInfo info) {
        super(info);
    }

    public DifferenceOfGaussian2DAlgorithm(DifferenceOfGaussian2DAlgorithm other) {
        super(other);
        this.sigma0X = other.sigma0X;
        this.sigma0Y = other.sigma0Y;
        this.sigma1X = other.sigma1X;
        this.sigma1Y = other.sigma1Y;
    }

    @Override
    protected void runIteration(JIPipeDataBatch dataBatch, JIPipeProgressInfo progressInfo) {
        ImagePlusData inputData = dataBatch.getInputData(getFirstInputSlot(), ImagePlusGreyscale32FData.class, progressInfo);
        ImagePlus img = inputData.getDuplicateImage();
        GaussianBlur gaussianBlur = new GaussianBlur();
        ImageJUtils.forEachSlice(img, ip -> {
            double accuracy = (ip instanceof ByteProcessor || ip instanceof ColorProcessor) ? 0.002 : 0.0002;
            ImageProcessor larger = ip.duplicate();
            gaussianBlur.blurGaussian(ip, sigma0X, sigma0Y > 0 ? sigma0Y : sigma0X, accuracy);
            gaussianBlur.blurGaussian(larger, sigma1X, sigma1Y > 0 ? sigma1Y : sigma1X, accuracy);

            // Subtract smaller - larger
            float[] smallerPixels = (float[]) ip.getPixels();
            float[] largerPixels = (float[]) larger.getPixels();
            for (int i = 0; i < smallerPixels.length; i++) {
                smallerPixels[i] -= largerPixels[i];
            }
        }, progressInfo);
        dataBatch.addOutputData(getFirstOutputSlot(), new ImagePlusData(img), progressInfo);
    }

    @Override
    public boolean supportsParallelization() {
        return true;
    }

    @Override
    public void reportValidity(JIPipeValidityReport report) {
        super.reportValidity(report);
        report.forCategory("First Sigma (X)").checkIfWithin(this, sigma0X, 0, Double.POSITIVE_INFINITY, false, true);
        report.forCategory("Second Sigma (X)").checkIfWithin(this, sigma1X, 0, Double.POSITIVE_INFINITY, false, true);
    }

    @JIPipeDocumentation(name = "First Sigma (X)", description = "Standard deviation of the Gaussian (pixels) in X direction. ")
    @JIPipeParameter(value = "sigma0-x", uiOrder = -20)
    public double getSigma0X() {
        return sigma0X;
    }

    @JIPipeParameter("sigma0-x")
    public void setSigma0X(double sigma0X) {
        this.sigma0X = sigma0X;

    }

    @JIPipeDocumentation(name = "First Sigma (Y)", description = "Standard deviation of the Gaussian (pixels) in Y direction." +
            " If zero or less, sigma in X direction is automatically used instead.")
    @JIPipeParameter(value = "sigma0-y", uiOrder = -19)
    public double getSigma0Y() {
        return sigma0Y;
    }

    @JIPipeParameter("sigma0-y")
    public void setSigma0Y(double sigma0Y) {
        this.sigma0Y = sigma0Y;

    }

    @JIPipeDocumentation(name = "Second Sigma (X)", description = "Standard deviation of the Gaussian (pixels) in X direction. ")
    @JIPipeParameter(value = "sigma1-x", uiOrder = -18)
    public double getSigma1X() {
        return sigma1X;
    }

    @JIPipeParameter("sigma1-x")
    public void setSigma1X(double sigma1X) {
        this.sigma1X = sigma1X;

    }

    @JIPipeDocumentation(name = "Second Sigma (Y)", description = "Standard deviation of the Gaussian (pixels) in Y direction." +
            " If zero or less, sigma in X direction is automatically used instead.")
    @JIPipeParameter(value = "sigma1-y", uiOrder = -17)
    public double getSigma1Y() {
        return sigma1Y;
    }

    @JIPipeParameter("sigma1-y")
    public void setSigma1Y(double sigma1Y) {
        this.sigma1Y = sigma1Y;

    }
}
