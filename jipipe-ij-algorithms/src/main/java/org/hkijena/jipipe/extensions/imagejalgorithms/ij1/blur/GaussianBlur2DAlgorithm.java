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

package org.hkijena.jipipe.extensions.imagejalgorithms.ij1.blur;

import ij.ImagePlus;
import ij.plugin.filter.GaussianBlur;
import ij.process.ByteProcessor;
import ij.process.ColorProcessor;
import org.hkijena.jipipe.api.JIPipeDocumentation;
import org.hkijena.jipipe.api.JIPipeOrganization;
import org.hkijena.jipipe.api.JIPipeProgressInfo;
import org.hkijena.jipipe.api.JIPipeValidityReport;
import org.hkijena.jipipe.api.data.JIPipeDefaultMutableSlotConfiguration;
import org.hkijena.jipipe.api.nodes.*;
import org.hkijena.jipipe.api.nodes.categories.ImagesNodeTypeCategory;
import org.hkijena.jipipe.api.parameters.JIPipeParameter;
import org.hkijena.jipipe.extensions.imagejdatatypes.datatypes.ImagePlusData;
import org.hkijena.jipipe.extensions.imagejdatatypes.util.ImageJUtils;

import static org.hkijena.jipipe.extensions.imagejalgorithms.ImageJAlgorithmsExtension.REMOVE_MASK_QUALIFIER;

/**
 * Wrapper around {@link ij.plugin.filter.GaussianBlur}
 */
@JIPipeDocumentation(name = "Gaussian blur 2D", description = "Applies convolution with a Gaussian function for smoothing. " +
        "If higher-dimensional data is provided, the filter is applied to each 2D slice.")
@JIPipeOrganization(menuPath = "Blur", nodeTypeCategory = ImagesNodeTypeCategory.class)
@JIPipeInputSlot(value = ImagePlusData.class, slotName = "Input")
@JIPipeOutputSlot(value = ImagePlusData.class, slotName = "Output")
public class GaussianBlur2DAlgorithm extends JIPipeSimpleIteratingAlgorithm {

    private double sigmaX = 1;
    private double sigmaY = -1;

    /**
     * Instantiates a new node type.
     *
     * @param info the info
     */
    public GaussianBlur2DAlgorithm(JIPipeNodeInfo info) {
        super(info, JIPipeDefaultMutableSlotConfiguration.builder().addInputSlot("Input", ImagePlusData.class)
                .addOutputSlot("Output", ImagePlusData.class, "Input", REMOVE_MASK_QUALIFIER)
                .allowOutputSlotInheritance(true)
                .seal()
                .build());
    }

    /**
     * Instantiates a new node type.
     *
     * @param other the other
     */
    public GaussianBlur2DAlgorithm(GaussianBlur2DAlgorithm other) {
        super(other);
        this.sigmaX = other.sigmaX;
        this.sigmaY = other.sigmaY;
    }

    @Override
    protected void runIteration(JIPipeDataBatch dataBatch, JIPipeProgressInfo progressInfo) {
        ImagePlusData inputData = dataBatch.getInputData(getFirstInputSlot(), ImagePlusData.class, progressInfo);
        ImagePlus img = inputData.getDuplicateImage();
        GaussianBlur gaussianBlur = new GaussianBlur();
        ImageJUtils.forEachSlice(img, ip -> {
            double accuracy = (ip instanceof ByteProcessor || ip instanceof ColorProcessor) ? 0.002 : 0.0002;
            gaussianBlur.blurGaussian(ip, sigmaX, sigmaY > 0 ? sigmaY : sigmaX, accuracy);
        }, progressInfo);
        dataBatch.addOutputData(getFirstOutputSlot(), new ImagePlusData(img), progressInfo);
    }

    @Override
    public boolean supportsParallelization() {
        return true;
    }

    @Override
    public void reportValidity(JIPipeValidityReport report) {
        report.forCategory("Sigma (X)").checkIfWithin(this, sigmaX, 0, Double.POSITIVE_INFINITY, false, true);
    }

    @JIPipeDocumentation(name = "Sigma (X)", description = "Standard deviation of the Gaussian (pixels) in X direction. ")
    @JIPipeParameter("sigma-x")
    public double getSigmaX() {
        return sigmaX;
    }

    @JIPipeParameter("sigma-x")
    public void setSigmaX(double sigmaX) {
        this.sigmaX = sigmaX;

    }

    @JIPipeDocumentation(name = "Sigma (Y)", description = "Standard deviation of the Gaussian (pixels) in Y direction." +
            " If zero or less, sigma in X direction is automatically used instead.")
    @JIPipeParameter("sigma-y")
    public double getSigmaY() {
        return sigmaY;
    }

    @JIPipeParameter("sigma-y")
    public void setSigmaY(double sigmaY) {
        this.sigmaY = sigmaY;

    }
}
