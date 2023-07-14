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

package org.hkijena.jipipe.extensions.imagejalgorithms.nodes.threshold;

import ij.ImagePlus;
import ij.ImageStack;
import ij.plugin.ImageCalculator;
import ij.plugin.filter.RankFilters;
import imagescience.feature.Hessian;
import imagescience.image.Aspects;
import imagescience.image.FloatImage;
import imagescience.image.Image;
import org.hkijena.jipipe.JIPipe;
import org.hkijena.jipipe.api.JIPipeDocumentation;
import org.hkijena.jipipe.api.validation.JIPipeValidationReport;
import org.hkijena.jipipe.api.JIPipeNode;
import org.hkijena.jipipe.api.JIPipeProgressInfo;
import org.hkijena.jipipe.api.nodes.*;
import org.hkijena.jipipe.api.nodes.categories.ImagesNodeTypeCategory;
import org.hkijena.jipipe.api.parameters.JIPipeParameter;
import org.hkijena.jipipe.api.parameters.JIPipeParameterAccess;
import org.hkijena.jipipe.api.parameters.JIPipeParameterTree;
import org.hkijena.jipipe.api.validation.JIPipeValidationReportEntryCause;
import org.hkijena.jipipe.extensions.imagejalgorithms.parameters.EigenvalueSelection2D;
import org.hkijena.jipipe.extensions.imagejdatatypes.datatypes.ImagePlusData;
import org.hkijena.jipipe.extensions.imagejdatatypes.datatypes.d2.greyscale.ImagePlus2DGreyscaleData;
import org.hkijena.jipipe.extensions.imagejdatatypes.datatypes.greyscale.ImagePlusGreyscaleData;
import org.hkijena.jipipe.extensions.imagejdatatypes.datatypes.greyscale.ImagePlusGreyscaleMaskData;
import org.hkijena.jipipe.extensions.imagejdatatypes.util.ImageJUtils;

import java.util.Vector;


/**
 * Segments using a Hessian
 */
@JIPipeDocumentation(name = "Hessian segmentation 2D", description = "Segments by applying a Hessian and morphological postprocessing. " +
        "If higher-dimensional data is provided, the filter is applied to each 2D slice.<br/>" +
        "If you want to further customize all steps, create a group or set of nodes that apply the following operations:" +
        "<ol>" +
        "<li>Calculate Hessian</li>" +
        "<li>Morphological Internal Gradient</li>" +
        "<li>Auto Threshold 2D</li>" +
        "<li>Despeckle</li>" +
        "</ol>")
@JIPipeNode(menuPath = "Threshold", nodeTypeCategory = ImagesNodeTypeCategory.class)
@JIPipeInputSlot(value = ImagePlusGreyscaleData.class, slotName = "Input", autoCreate = true)
@JIPipeOutputSlot(value = ImagePlusGreyscaleMaskData.class, slotName = "Output", autoCreate = true)
public class HessianSegmentation2DAlgorithm extends JIPipeSimpleIteratingAlgorithm {

    private final AutoThreshold2DAlgorithm autoThresholding;
    private double smoothing = 1.0;
    private double gradientRadius = 1;
    private boolean compareAbsolute = true;
    private EigenvalueSelection2D eigenvalueSelection = EigenvalueSelection2D.Largest;
    private boolean applyInternalGradient = true;
    private boolean applyDespeckle = true;
    private int despeckleIterations = 2;

    /**
     * @param info the algorithm info
     */
    public HessianSegmentation2DAlgorithm(JIPipeNodeInfo info) {
        super(info);
        this.autoThresholding = (AutoThreshold2DAlgorithm) JIPipe.getNodes().getInfoById("ij1-threshold-auto2d").newInstance();
    }

    /**
     * Copies the algorithm
     *
     * @param other the original
     */
    public HessianSegmentation2DAlgorithm(HessianSegmentation2DAlgorithm other) {
        super(other);
        this.smoothing = other.smoothing;
        this.gradientRadius = other.gradientRadius;
        this.eigenvalueSelection = other.eigenvalueSelection;
        this.compareAbsolute = other.compareAbsolute;
        this.autoThresholding = (AutoThreshold2DAlgorithm) other.autoThresholding.getInfo().duplicate(other.autoThresholding);
        this.applyInternalGradient = other.applyInternalGradient;
        this.applyDespeckle = other.applyDespeckle;
        this.despeckleIterations = other.despeckleIterations;
    }

    @Override
    public boolean isParameterUIVisible(JIPipeParameterTree tree, JIPipeParameterAccess access) {
        if (access.getSource() == autoThresholding && "source-area".equals(access.getKey())) {
            return false;
        }
        return super.isParameterUIVisible(tree, access);
    }

    private ImagePlus applyHessian(ImagePlus input) {
        final Image image = Image.wrap(input);
        image.aspects(new Aspects());
        final Hessian hessian = new Hessian();
        final Vector<Image> eigenimages = hessian.run(new FloatImage(image), smoothing, compareAbsolute);
        if (eigenvalueSelection == EigenvalueSelection2D.Largest)
            return eigenimages.get(0).imageplus();
        else
            return eigenimages.get(1).imageplus();
    }

    private void applyInternalGradient(ImagePlus img) {
        // Erode the original image
        ImagePlus eroded = ImageJUtils.duplicate(img);
        RankFilters erosionFilter = new RankFilters();
        erosionFilter.rank(eroded.getProcessor(), gradientRadius, RankFilters.MIN); //TODO: Set element to octagon

        // Apply image calculator
        ImageCalculator calculator = new ImageCalculator();
        calculator.run("Subtract", img, eroded);
    }

    private void applyDespeckle(ImagePlus img, int iterations) {
        RankFilters rankFilters = new RankFilters();
        for (int i = 0; i < iterations; ++i) {
            rankFilters.rank(img.getProcessor(), 1, RankFilters.MEDIAN);
        }
    }

    @Override
    public boolean supportsParallelization() {
        return true;
    }

    @Override
    protected void runIteration(JIPipeDataBatch dataBatch, JIPipeProgressInfo progressInfo) {
        ImagePlus img = dataBatch.getInputData(getFirstInputSlot(), ImagePlusGreyscaleData.class, progressInfo).getImage();
        ImageStack stack = new ImageStack(img.getWidth(), img.getHeight(), img.getProcessor().getColorModel());
        AutoThreshold2DAlgorithm autoThresholdingCopy = new AutoThreshold2DAlgorithm(autoThresholding);

        ImageJUtils.forEachIndexedSlice(img, (imp, index) -> {
            progressInfo.log("Slice " + index + "/" + img.getStackSize());
            ImagePlus slice = new ImagePlus("slice", imp.duplicate());
            // Apply hessian
            ImagePlus processedSlice = applyHessian(slice);

            // Apply morphological filters
            if (applyInternalGradient)
                applyInternalGradient(processedSlice);

            // Convert to mask
            autoThresholdingCopy.clearSlotData();
            autoThresholdingCopy.getFirstInputSlot().addData(new ImagePlus2DGreyscaleData(processedSlice), progressInfo);
            autoThresholdingCopy.run(progressInfo);
            processedSlice = autoThresholdingCopy.getFirstOutputSlot().getData(0, ImagePlusData.class, progressInfo).getImage();

            // Despeckle x2
            if (applyDespeckle)
                applyDespeckle(processedSlice, despeckleIterations);
            stack.addSlice("slice" + index, processedSlice.getProcessor());
        }, progressInfo);
        ImagePlus result = new ImagePlus("Segmented Image", stack);
        result.setDimensions(img.getNChannels(), img.getNSlices(), img.getNFrames());
        result.copyScale(img);

        dataBatch.addOutputData(getFirstOutputSlot(), new ImagePlusGreyscaleMaskData(result), progressInfo);
    }

    @JIPipeParameter("smoothing")
    @JIPipeDocumentation(name = "Smoothing", description = "The smoothing scale at which the required image derivatives are computed. " +
            "The scale is equal to the standard deviation of the Gaussian kernel used for differentiation and must be larger than zero. " +
            "In order to enforce physical isotropy, for each dimension, the scale is divided by the size of the image elements (aspect ratio) in that dimension.")
    public double getSmoothing() {
        return smoothing;
    }

    @JIPipeParameter("smoothing")
    public void setSmoothing(double smoothing) {
        this.smoothing = smoothing;

    }

    @JIPipeParameter("gradient-radius")
    @JIPipeDocumentation(name = "Gradient radius", description = "Radius of the internal gradient filter.")
    public double getGradientRadius() {
        return gradientRadius;
    }

    @JIPipeParameter("gradient-radius")
    public void setGradientRadius(double gradientRadius) {
        this.gradientRadius = gradientRadius;

    }

    @JIPipeParameter(value = "auto-thresholding")
    @JIPipeDocumentation(name = "Auto thresholding", description = "Parameters for underlying auto thresholding")
    public AutoThreshold2DAlgorithm getAutoThresholding() {
        return autoThresholding;
    }

    @Override
    public void reportValidity(JIPipeValidationReportEntryCause parentCause, JIPipeValidationReport report) {
        report.checkIfWithin(this, gradientRadius, 0, Double.POSITIVE_INFINITY, false, true);
        report.checkIfWithin(this, smoothing, 0, Double.POSITIVE_INFINITY, false, true);
        report.resolve("Auto thresholding").report(autoThresholding);
    }

    @JIPipeDocumentation(name = "Compare absolute", description = "Determines whether eigenvalues are compared in absolute sense")
    @JIPipeParameter("compare-absolute")
    public boolean isCompareAbsolute() {
        return compareAbsolute;
    }

    @JIPipeParameter("compare-absolute")
    public void setCompareAbsolute(boolean compareAbsolute) {
        this.compareAbsolute = compareAbsolute;

    }

    @JIPipeDocumentation(name = "Eigenvalue", description = "Allows you to choose whether the largest or smallest Eigenvalues are chosen")
    @JIPipeParameter("eigenvalue-selection")
    public EigenvalueSelection2D getEigenvalueSelection() {
        return eigenvalueSelection;
    }

    @JIPipeParameter("eigenvalue-selection")
    public void setEigenvalueSelection(EigenvalueSelection2D eigenvalueSelection) {
        this.eigenvalueSelection = eigenvalueSelection;

    }

    @JIPipeDocumentation(name = "Apply internal gradient filter", description = "If enabled, an internal gradient filter is applied to the hessian image.")
    @JIPipeParameter("apply-internal-gradient")
    public boolean isApplyInternalGradient() {
        return applyInternalGradient;
    }

    @JIPipeParameter("apply-internal-gradient")
    public void setApplyInternalGradient(boolean applyInternalGradient) {
        this.applyInternalGradient = applyInternalGradient;
    }

    @JIPipeDocumentation(name = "Apply despeckle", description = "If enabled, a median filter is applied to the thresholded images.")
    @JIPipeParameter("apply-despeckle")
    public boolean isApplyDespeckle() {
        return applyDespeckle;
    }

    @JIPipeParameter("apply-despeckle")
    public void setApplyDespeckle(boolean applyDespeckle) {
        this.applyDespeckle = applyDespeckle;
    }

    @JIPipeDocumentation(name = "Despeckle iterations", description = "How many times the despeckle is applied.")
    @JIPipeParameter("despeckle-iterations")
    public int getDespeckleIterations() {
        return despeckleIterations;
    }

    @JIPipeParameter("despeckle-iterations")
    public void setDespeckleIterations(int despeckleIterations) {
        this.despeckleIterations = despeckleIterations;
    }

}