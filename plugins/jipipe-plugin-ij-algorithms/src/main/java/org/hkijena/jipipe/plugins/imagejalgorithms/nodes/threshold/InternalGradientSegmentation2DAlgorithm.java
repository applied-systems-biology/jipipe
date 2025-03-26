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
import ij.plugin.ImageCalculator;
import ij.plugin.filter.Binary;
import ij.plugin.filter.GaussianBlur;
import ij.plugin.filter.RankFilters;
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
import org.hkijena.jipipe.api.validation.JIPipeValidationReport;
import org.hkijena.jipipe.api.validation.JIPipeValidationReportContext;
import org.hkijena.jipipe.api.validation.contexts.ParameterValidationReportContext;
import org.hkijena.jipipe.plugins.imagejalgorithms.nodes.contrast.CLAHEContrastEnhancer;
import org.hkijena.jipipe.plugins.imagejdatatypes.datatypes.ImagePlusData;
import org.hkijena.jipipe.plugins.imagejdatatypes.datatypes.greyscale.ImagePlusGreyscaleData;
import org.hkijena.jipipe.plugins.imagejdatatypes.datatypes.greyscale.ImagePlusGreyscaleMaskData;
import org.hkijena.jipipe.plugins.imagejdatatypes.util.ImageJIterationUtils;
import org.hkijena.jipipe.plugins.imagejdatatypes.util.ImageJUtils;


/**
 * Internal gradient segmenter
 */
@SetJIPipeDocumentation(name = "Internal gradient segmentation 2D", description = "Segments objects by finding the internal gradients. " +
        "If higher-dimensional data is provided, the filter is applied to each 2D slice.<br/>" +
        "If you want to further customize all steps, create a group or set of nodes that apply the following operations:" +
        "<ol>" +
        "<li>Optional: First CLAHE</li>" +
        "<li>Optional: Gaussian filter</li>" +
        "<li>Optional: Second CLAHE</li>" +
        "<li>Auto Threshold 2D</li>" +
        "<li>Morphological dilation</li>" +
        "<li>Morphological hole filling</li>" +
        "<li>Morphological erosion</li>" +
        "</ol>")
@ConfigureJIPipeNode(menuPath = "Threshold", nodeTypeCategory = ImagesNodeTypeCategory.class)
@AddJIPipeInputSlot(value = ImagePlusGreyscaleData.class, name = "Input", create = true)
@AddJIPipeOutputSlot(value = ImagePlusGreyscaleMaskData.class, name = "Output", create = true)
public class InternalGradientSegmentation2DAlgorithm extends JIPipeSimpleIteratingAlgorithm {

    private final AutoThreshold2DAlgorithm autoThresholding;
    private final CLAHEContrastEnhancer contrastEnhancer;
    private double gaussSigma = 3;
    private int internalGradientRadius = 25;
    private int dilationIterations = 3;
    private int erosionIterations = 2;
    private boolean applyFirstCLAHE = true;
    private boolean applySecondCLAHE = true;
    private boolean applyGaussian = true;

    /**
     * @param info the algorithm info
     */
    public InternalGradientSegmentation2DAlgorithm(JIPipeNodeInfo info) {
        super(info);
        this.contrastEnhancer = (CLAHEContrastEnhancer) JIPipe.getNodes().getInfoById("ij1-contrast-clahe").newInstance();
        this.autoThresholding = (AutoThreshold2DAlgorithm) JIPipe.getNodes().getInfoById("ij1-threshold-auto2d").newInstance();
    }

    /**
     * Copies the algorithm
     *
     * @param other the original
     */
    public InternalGradientSegmentation2DAlgorithm(InternalGradientSegmentation2DAlgorithm other) {
        super(other);
        this.gaussSigma = other.gaussSigma;
        this.internalGradientRadius = other.internalGradientRadius;
        this.dilationIterations = other.dilationIterations;
        this.erosionIterations = other.erosionIterations;
        this.autoThresholding = (AutoThreshold2DAlgorithm) other.autoThresholding.getInfo().duplicate(other.autoThresholding);
        this.contrastEnhancer = (CLAHEContrastEnhancer) other.contrastEnhancer.getInfo().duplicate(other.contrastEnhancer);
        this.applyFirstCLAHE = other.applyFirstCLAHE;
        this.applySecondCLAHE = other.applySecondCLAHE;
        this.applyGaussian = other.applyGaussian;
    }

    @Override
    public boolean supportsParallelization() {
        return true;
    }

    private void applyInternalGradient(ImagePlus img) {
        // Erode the original image
        ImagePlus eroded = ImageJUtils.duplicate(img);
        RankFilters erosionFilter = new RankFilters();
        erosionFilter.rank(eroded.getProcessor(), internalGradientRadius, RankFilters.MIN); //TODO: Set element to octagon

        // Apply image calculator
        ImageCalculator calculator = new ImageCalculator();
        calculator.run("Subtract", img, eroded);
    }

    @Override
    protected void runIteration(JIPipeSingleIterationStep iterationStep, JIPipeIterationContext iterationContext, JIPipeGraphNodeRunContext runContext, JIPipeProgressInfo progressInfo) {

        ImagePlus img = iterationStep.getInputData(getFirstInputSlot(), ImagePlusGreyscaleData.class, progressInfo).getImage();
        ImageStack stack = new ImageStack(img.getWidth(), img.getHeight(), img.getProcessor().getColorModel());

        CLAHEContrastEnhancer contrastEnhancerCopy = new CLAHEContrastEnhancer(contrastEnhancer);
        AutoThreshold2DAlgorithm autoThresholdingCopy = new AutoThreshold2DAlgorithm(autoThresholding);

        ImageJIterationUtils.forEachIndexedSlice(img, (imp, index) -> {
            ImagePlus slice = new ImagePlus("slice", imp);
            ImagePlus processedSlice = ImageJUtils.duplicate(slice);

            // Apply CLAHE enhancer
            if (applyFirstCLAHE) {
                contrastEnhancerCopy.clearSlotData(false, progressInfo);
                contrastEnhancerCopy.getFirstInputSlot().addData(new ImagePlusGreyscaleData(processedSlice), progressInfo);
                contrastEnhancerCopy.run(runContext, progressInfo);
                processedSlice = contrastEnhancerCopy.getFirstOutputSlot().getData(0, ImagePlusData.class, progressInfo).getImage();
            }

            if (applyGaussian) {
                (new GaussianBlur()).blurGaussian(processedSlice.getProcessor(), gaussSigma);
                ImageJUtils.runOnImage(processedSlice, "8-bit");
                applyInternalGradient(processedSlice);
            }

            if (applySecondCLAHE) {
                contrastEnhancerCopy.clearSlotData(false, progressInfo);
                contrastEnhancerCopy.getFirstInputSlot().addData(new ImagePlusGreyscaleData(processedSlice), progressInfo);
                contrastEnhancerCopy.run(runContext, progressInfo);
                processedSlice = contrastEnhancerCopy.getFirstOutputSlot().getData(0, ImagePlusData.class, progressInfo).getImage();
            }

            // Convert image to mask and threshold with given auto threshold method
            autoThresholdingCopy.clearSlotData(false, progressInfo);
            autoThresholdingCopy.getFirstInputSlot().addData(new ImagePlusGreyscaleData(processedSlice), progressInfo);
            autoThresholdingCopy.run(runContext, progressInfo);
            processedSlice = autoThresholdingCopy.getFirstOutputSlot().getData(0, ImagePlusData.class, progressInfo).getImage();

            // Apply set of rank filters
            Binary binaryFilter = new Binary();

            binaryFilter.setup("dilate", null);
            binaryFilter.run(processedSlice.getProcessor());

            binaryFilter.setup("fill holes", null);
            binaryFilter.run(processedSlice.getProcessor());

            binaryFilter.setup("dilate", null);
            for (int i = 0; i < dilationIterations; ++i) {
                binaryFilter.run(processedSlice.getProcessor());
            }

            binaryFilter.setup("fill holes", null);
            binaryFilter.run(processedSlice.getProcessor());

            binaryFilter.setup("erode", null);
            for (int i = 0; i < erosionIterations; ++i) {
                binaryFilter.run(processedSlice.getProcessor());
            }
            stack.addSlice("slice" + index, processedSlice.getProcessor());
        }, progressInfo);
        ImagePlus result = new ImagePlus("Segmented Image", stack);
        result.setDimensions(img.getNChannels(), img.getNSlices(), img.getNFrames());
        result.copyScale(img);

        iterationStep.addOutputData(getFirstOutputSlot(), new ImagePlusGreyscaleMaskData(result), progressInfo);
    }

    @JIPipeParameter("gauss-sigma")
    @SetJIPipeDocumentation(name = "Gauss sigma", description = "Standard deviation of the Gaussian (pixels)")
    public double getGaussSigma() {
        return gaussSigma;
    }

    @JIPipeParameter("gauss-sigma")
    public void setGaussSigma(double gaussSigma) {
        this.gaussSigma = gaussSigma;

    }

    @JIPipeParameter("internal-gradient-radius")
    @SetJIPipeDocumentation(name = "Internal gradient radius", description = "Radius for the internal gradient radius calculation")
    public int getInternalGradientRadius() {
        return internalGradientRadius;
    }

    @JIPipeParameter("internal-gradient-radius")
    public void setInternalGradientRadius(int internalGradientRadius) {
        this.internalGradientRadius = internalGradientRadius;

    }

    @JIPipeParameter("dilation-iterations")
    @SetJIPipeDocumentation(name = "Dilation iterations", description = "Number of dilation iterations after filling holes")
    public int getDilationIterations() {
        return dilationIterations;
    }

    @JIPipeParameter("dilation-iterations")
    public void setDilationIterations(int dilationIterations) {
        this.dilationIterations = dilationIterations;

    }

    @JIPipeParameter("erosion-iterations")
    @SetJIPipeDocumentation(name = "Erosion iterations", description = "Number of erosion iterations after filling holes. If you do not want to change " +
            "the object sizes, keep this the same as the dilation iterations.")
    public int getErosionIterations() {
        return erosionIterations;
    }

    @JIPipeParameter("erosion-iterations")
    public void setErosionIterations(int erosionIterations) {
        this.erosionIterations = erosionIterations;

    }

    @JIPipeParameter(value = "auto-thresholding")
    @SetJIPipeDocumentation(name = "Auto thresholding", description = "Parameters for underlying auto thresholding")
    public AutoThreshold2DAlgorithm getAutoThresholding() {
        return autoThresholding;
    }

    @JIPipeParameter(value = "clahe-enhancing")
    @SetJIPipeDocumentation(name = "CLAHE", description = "Parameters for underlying CLAHE Enhancing algorithm")
    public CLAHEContrastEnhancer getContrastEnhancer() {
        return contrastEnhancer;
    }

    @SetJIPipeDocumentation(name = "Apply first CLAHE")
    @JIPipeParameter("apply-first-clahe")
    public boolean isApplyFirstCLAHE() {
        return applyFirstCLAHE;
    }

    @JIPipeParameter("apply-first-clahe")
    public void setApplyFirstCLAHE(boolean applyFirstCLAHE) {
        this.applyFirstCLAHE = applyFirstCLAHE;
    }

    @SetJIPipeDocumentation(name = "Apply second CLAHE")
    @JIPipeParameter("apply-second-clahe")
    public boolean isApplySecondCLAHE() {
        return applySecondCLAHE;
    }

    @JIPipeParameter("apply-second-clahe")
    public void setApplySecondCLAHE(boolean applySecondCLAHE) {
        this.applySecondCLAHE = applySecondCLAHE;
    }

    @SetJIPipeDocumentation(name = "Apply Gaussian")
    @JIPipeParameter("apply-gaussian")
    public boolean isApplyGaussian() {
        return applyGaussian;
    }

    @JIPipeParameter("apply-gaussian")
    public void setApplyGaussian(boolean applyGaussian) {
        this.applyGaussian = applyGaussian;
    }

    @Override
    public boolean isParameterUIVisible(JIPipeParameterTree tree, JIPipeParameterAccess access) {
        if (access.getSource() == autoThresholding && "source-area".equals(access.getKey())) {
            return false;
        }
        return super.isParameterUIVisible(tree, access);
    }

    @Override
    public void reportValidity(JIPipeValidationReportContext reportContext, JIPipeValidationReport report) {
        super.reportValidity(reportContext, report);
        report.report(new ParameterValidationReportContext(this, "Auto thresholding", "auto-thresholding"), autoThresholding);
        report.report(new ParameterValidationReportContext(this, "CLAHE", "clahe-enhancing"), contrastEnhancer);
    }
}
