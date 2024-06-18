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

package org.hkijena.jipipe.plugins.imagejalgorithms.nodes.threshold;

import ij.ImagePlus;
import ij.ImageStack;
import ij.plugin.filter.BackgroundSubtracter;
import ij.plugin.filter.Binary;
import ij.plugin.filter.GaussianBlur;
import org.hkijena.jipipe.JIPipe;
import org.hkijena.jipipe.api.ConfigureJIPipeNode;
import org.hkijena.jipipe.api.JIPipeProgressInfo;
import org.hkijena.jipipe.api.SetJIPipeDocumentation;
import org.hkijena.jipipe.api.nodes.AddJIPipeInputSlot;
import org.hkijena.jipipe.api.nodes.AddJIPipeOutputSlot;
import org.hkijena.jipipe.api.nodes.JIPipeGraphNodeRunContext;
import org.hkijena.jipipe.api.nodes.JIPipeNodeInfo;
import org.hkijena.jipipe.api.nodes.algorithm.JIPipeSimpleIteratingAlgorithm;
import org.hkijena.jipipe.api.nodes.categories.ImagesNodeTypeCategory;
import org.hkijena.jipipe.api.nodes.iterationstep.JIPipeIterationContext;
import org.hkijena.jipipe.api.nodes.iterationstep.JIPipeSingleIterationStep;
import org.hkijena.jipipe.api.parameters.JIPipeParameter;
import org.hkijena.jipipe.api.parameters.JIPipeParameterAccess;
import org.hkijena.jipipe.api.parameters.JIPipeParameterTree;
import org.hkijena.jipipe.plugins.imagejdatatypes.datatypes.ImagePlusData;
import org.hkijena.jipipe.plugins.imagejdatatypes.datatypes.d2.greyscale.ImagePlus2DGreyscaleData;
import org.hkijena.jipipe.plugins.imagejdatatypes.datatypes.greyscale.ImagePlusGreyscale8UData;
import org.hkijena.jipipe.plugins.imagejdatatypes.datatypes.greyscale.ImagePlusGreyscaleData;
import org.hkijena.jipipe.plugins.imagejdatatypes.datatypes.greyscale.ImagePlusGreyscaleMaskData;
import org.hkijena.jipipe.plugins.imagejdatatypes.util.ImageJUtils;


/**
 * Applies Bright spots segmentation
 */
@SetJIPipeDocumentation(name = "Bright spots segmentation 2D", description = "Applies thresholding by applying a background subtraction, auto thresholding, and " +
        "various morphological operations. If higher-dimensional data is provided, the filter is applied to each 2D slice.<br/>" +
        "If you want to further customize all steps, create a group or set of nodes that apply the following operations:" +
        "<ol>" +
        "<li>Subtract background (rolling ball): default radius = 20</li>" +
        "<li>Auto threshold 2D</li>" +
        "<li>Morphological dilation</li>" +
        "<li>Morphological hole filling</li>" +
        "<li>Morphological erosion</li>" +
        "<li>Optional: Gaussian blur</li>" +
        "</ol>")
@ConfigureJIPipeNode(menuPath = "Threshold", nodeTypeCategory = ImagesNodeTypeCategory.class)


@AddJIPipeInputSlot(value = ImagePlusGreyscale8UData.class, slotName = "Input", create = true)
@AddJIPipeOutputSlot(value = ImagePlusGreyscaleMaskData.class, slotName = "Output", create = true)
public class BrightSpotsSegmentation2DAlgorithm extends JIPipeSimpleIteratingAlgorithm {

    private final AutoThreshold2DAlgorithm autoThresholding;
    private int rollingBallRadius = 20;
    private int dilationErodeSteps = 2;
    private double gaussianSigma = 3;

    /**
     * @param info the info
     */
    public BrightSpotsSegmentation2DAlgorithm(JIPipeNodeInfo info) {
        super(info);
        this.autoThresholding = (AutoThreshold2DAlgorithm) JIPipe.getNodes().getInfoById("ij1-threshold-auto2d").newInstance();
    }

    /**
     * Copies the algorithm
     *
     * @param other the original
     */
    public BrightSpotsSegmentation2DAlgorithm(BrightSpotsSegmentation2DAlgorithm other) {
        super(other);
        this.rollingBallRadius = other.rollingBallRadius;
        this.dilationErodeSteps = other.dilationErodeSteps;
        this.gaussianSigma = other.gaussianSigma;
        this.autoThresholding = (AutoThreshold2DAlgorithm) other.autoThresholding.getInfo().duplicate(other.autoThresholding);
    }

    @Override
    public boolean isParameterUIVisible(JIPipeParameterTree tree, JIPipeParameterAccess access) {
        if (access.getSource() == autoThresholding && "source-area".equals(access.getKey())) {
            return false;
        }
        return super.isParameterUIVisible(tree, access);
    }

    @Override
    public boolean supportsParallelization() {
        return true;
    }

    @Override
    protected void runIteration(JIPipeSingleIterationStep iterationStep, JIPipeIterationContext iterationContext, JIPipeGraphNodeRunContext runContext, JIPipeProgressInfo progressInfo) {
        ImagePlus img = iterationStep.getInputData(getFirstInputSlot(), ImagePlusGreyscaleData.class, progressInfo).getImage();
        ImageStack stack = new ImageStack(img.getWidth(), img.getHeight(), img.getProcessor().getColorModel());
        AutoThreshold2DAlgorithm autoThresholdingCopy = new AutoThreshold2DAlgorithm(autoThresholding);

        ImageJUtils.forEachIndexedSlice(img, (imp, index) -> {
            ImagePlus slice = new ImagePlus("slice", imp);
            ImagePlus processedSlice = ImageJUtils.duplicate(slice);

            // Apply background subtraction
            BackgroundSubtracter backgroundSubtracter = new BackgroundSubtracter();
            backgroundSubtracter.rollingBallBackground(processedSlice.getProcessor(),
                    rollingBallRadius,
                    false,
                    false,
                    false,
                    true,
                    true);

            // Apply auto threshold
            autoThresholdingCopy.clearSlotData(false, progressInfo);
            autoThresholdingCopy.getFirstOutputSlot().addData(new ImagePlus2DGreyscaleData(processedSlice), progressInfo);
            autoThresholdingCopy.run(runContext, progressInfo);
            processedSlice = autoThresholdingCopy.getFirstOutputSlot().getData(0, ImagePlusData.class, progressInfo).getImage();

            // Apply morphologial operations
            Binary binaryFilter = new Binary();

            binaryFilter.setup("dilate", null);
            for (int i = 0; i < dilationErodeSteps; ++i) {
                binaryFilter.run(processedSlice.getProcessor());
            }

            binaryFilter.setup("fill holes", null);
            binaryFilter.run(processedSlice.getProcessor());

            binaryFilter.setup("erode", null);
            for (int i = 0; i < dilationErodeSteps; ++i) {
                binaryFilter.run(processedSlice.getProcessor());
            }

            // Smooth the spots and re-threshold them
            if (gaussianSigma > 0) {
                GaussianBlur gaussianBlur = new GaussianBlur();
                gaussianBlur.blurGaussian(processedSlice.getProcessor(), gaussianSigma);

                autoThresholdingCopy.clearSlotData(false, progressInfo);
                autoThresholdingCopy.getFirstInputSlot().addData(new ImagePlusGreyscaleData(processedSlice), progressInfo);
                autoThresholdingCopy.run(runContext, progressInfo);
                processedSlice = autoThresholdingCopy.getFirstOutputSlot().getData(0, ImagePlusData.class, progressInfo).getImage();
            }
            stack.addSlice("slice" + index, processedSlice.getProcessor());
        }, progressInfo);
        ImagePlus result = new ImagePlus("Segmented Image", stack);
        result.setDimensions(img.getNChannels(), img.getNSlices(), img.getNFrames());
        result.copyScale(img);
        iterationStep.addOutputData(getFirstOutputSlot(), new ImagePlusGreyscaleMaskData(result), progressInfo);
    }

    @JIPipeParameter("rolling-ball-radius")
    @SetJIPipeDocumentation(name = "Rolling ball radius")
    public int getRollingBallRadius() {
        return rollingBallRadius;
    }

    @JIPipeParameter("rolling-ball-radius")
    public void setRollingBallRadius(int rollingBallRadius) {
        this.rollingBallRadius = rollingBallRadius;

    }

    @JIPipeParameter("dilation-erode-steps")
    @SetJIPipeDocumentation(name = "Dilation erode steps")
    public int getDilationErodeSteps() {
        return dilationErodeSteps;
    }

    @JIPipeParameter("dilation-erode-steps")
    public void setDilationErodeSteps(int dilationErodeSteps) {
        this.dilationErodeSteps = dilationErodeSteps;

    }

    @JIPipeParameter("gaussian-sigma")
    @SetJIPipeDocumentation(name = "Gaussian sigma")
    public double getGaussianSigma() {
        return gaussianSigma;
    }

    @JIPipeParameter("gaussian-sigma")
    public void setGaussianSigma(double gaussianSigma) {
        this.gaussianSigma = gaussianSigma;

    }

    @JIPipeParameter(value = "auto-thresholding")
    @SetJIPipeDocumentation(name = "Auto thresholding", description = "Parameters for underlying auto thresholding")
    public AutoThreshold2DAlgorithm getAutoThresholding() {
        return autoThresholding;
    }
}
