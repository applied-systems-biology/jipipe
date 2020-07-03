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

package org.hkijena.acaq5.extensions.imagejalgorithms.ij1.threshold;

import ij.ImagePlus;
import ij.ImageStack;
import ij.plugin.ImageCalculator;
import ij.plugin.filter.RankFilters;
import imagescience.feature.Hessian;
import imagescience.image.Aspects;
import imagescience.image.FloatImage;
import imagescience.image.Image;
import org.hkijena.acaq5.api.ACAQDocumentation;
import org.hkijena.acaq5.api.ACAQOrganization;
import org.hkijena.acaq5.api.ACAQRunnerSubStatus;
import org.hkijena.acaq5.api.ACAQValidityReport;
import org.hkijena.acaq5.api.algorithm.ACAQAlgorithmCategory;
import org.hkijena.acaq5.api.algorithm.ACAQAlgorithmDeclaration;
import org.hkijena.acaq5.api.algorithm.ACAQDataInterface;
import org.hkijena.acaq5.api.algorithm.ACAQSimpleIteratingAlgorithm;
import org.hkijena.acaq5.api.algorithm.AlgorithmInputSlot;
import org.hkijena.acaq5.api.algorithm.AlgorithmOutputSlot;
import org.hkijena.acaq5.api.data.ACAQDefaultMutableSlotConfiguration;
import org.hkijena.acaq5.api.events.ParameterChangedEvent;
import org.hkijena.acaq5.api.parameters.ACAQParameter;
import org.hkijena.acaq5.api.registries.ACAQAlgorithmRegistry;
import org.hkijena.acaq5.extensions.imagejalgorithms.ij1.EigenvalueSelection2D;
import org.hkijena.acaq5.extensions.imagejdatatypes.datatypes.ImagePlusData;
import org.hkijena.acaq5.extensions.imagejdatatypes.datatypes.d2.greyscale.ImagePlus2DGreyscaleData;
import org.hkijena.acaq5.extensions.imagejdatatypes.datatypes.greyscale.ImagePlusGreyscale8UData;
import org.hkijena.acaq5.extensions.imagejdatatypes.datatypes.greyscale.ImagePlusGreyscaleData;
import org.hkijena.acaq5.extensions.imagejdatatypes.datatypes.greyscale.ImagePlusGreyscaleMaskData;
import org.hkijena.acaq5.utils.ImageJUtils;

import java.util.Vector;
import java.util.function.Consumer;
import java.util.function.Supplier;

import static org.hkijena.acaq5.extensions.imagejalgorithms.ImageJAlgorithmsExtension.ADD_MASK_QUALIFIER;

/**
 * Segments using a Hessian
 */
@ACAQDocumentation(name = "Hessian segmentation 2D", description = "Segments by applying a Hessian and morphological postprocessing. " +
        "If higher-dimensional data is provided, the filter is applied to each 2D slice.")
@ACAQOrganization(menuPath = "Threshold", algorithmCategory = ACAQAlgorithmCategory.Processor)
@AlgorithmInputSlot(value = ImagePlusGreyscaleData.class, slotName = "Input")
@AlgorithmOutputSlot(value = ImagePlusGreyscaleMaskData.class, slotName = "Output")
public class HessianSegmentation2DAlgorithm extends ACAQSimpleIteratingAlgorithm {

    private double smoothing = 1.0;
    private double gradientRadius = 1;
    private boolean compareAbsolute = true;
    private EigenvalueSelection2D eigenvalueSelection = EigenvalueSelection2D.Largest;
    private AutoThreshold2DAlgorithm autoThresholding;
    private boolean applyInternalGradient = true;
    private boolean applyDespeckle = true;
    private int despeckleIterations = 2;

    /**
     * @param declaration the algorithm declaration
     */
    public HessianSegmentation2DAlgorithm(ACAQAlgorithmDeclaration declaration) {
        super(declaration, ACAQDefaultMutableSlotConfiguration.builder().addInputSlot("Input", ImagePlusGreyscale8UData.class)
                .addOutputSlot("Output", ImagePlusGreyscaleMaskData.class, "Input", ADD_MASK_QUALIFIER)
                .allowOutputSlotInheritance(true)
                .seal()
                .build());
        this.autoThresholding = (AutoThreshold2DAlgorithm) ACAQAlgorithmRegistry.getInstance().getDeclarationById("ij1-threshold-auto2d").newInstance();
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
        this.autoThresholding = (AutoThreshold2DAlgorithm) other.autoThresholding.getDeclaration().clone(other.autoThresholding);
        this.applyInternalGradient = other.applyInternalGradient;
        this.applyDespeckle = other.applyDespeckle;
        this.despeckleIterations = other.despeckleIterations;
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
        ImagePlus eroded = img.duplicate();
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
    protected void runIteration(ACAQDataInterface dataInterface, ACAQRunnerSubStatus subProgress, Consumer<ACAQRunnerSubStatus> algorithmProgress, Supplier<Boolean> isCancelled) {
        ImagePlus img = dataInterface.getInputData(getFirstInputSlot(), ImagePlusGreyscaleData.class).getImage();
        ImageStack stack = new ImageStack(img.getWidth(), img.getHeight(), img.getProcessor().getColorModel());
        AutoThreshold2DAlgorithm autoThresholdingCopy = new AutoThreshold2DAlgorithm(autoThresholding);

        ImageJUtils.forEachIndexedSlice(img, (imp, index) -> {
            algorithmProgress.accept(subProgress.resolve("Slice " + index + "/" + img.getStackSize()));
            ImagePlus slice = new ImagePlus("slice", imp.duplicate());
            // Apply hessian
            ImagePlus processedSlice = applyHessian(slice);

            // Apply morphological filters
            if (applyInternalGradient)
                applyInternalGradient(processedSlice);

            // Convert to mask
            autoThresholdingCopy.clearSlotData();
            autoThresholdingCopy.getFirstInputSlot().addData(new ImagePlus2DGreyscaleData(processedSlice));
            autoThresholdingCopy.run(subProgress.resolve("Auto-thresholding"), algorithmProgress, isCancelled);
            processedSlice = autoThresholdingCopy.getFirstOutputSlot().getData(0, ImagePlusData.class).getImage();

            // Despeckle x2
            if (applyDespeckle)
                applyDespeckle(processedSlice, despeckleIterations);
            stack.addSlice("slice" + index, processedSlice.getProcessor());
        });
        ImagePlus result = new ImagePlus("Segmented Image", stack);
        result.setDimensions(img.getNChannels(), img.getNSlices(), img.getNFrames());

        dataInterface.addOutputData(getFirstOutputSlot(), new ImagePlusGreyscaleMaskData(result));
    }

    @ACAQParameter("smoothing")
    @ACAQDocumentation(name = "Smoothing", description = "The smoothing scale at which the required image derivatives are computed. " +
            "The scale is equal to the standard deviation of the Gaussian kernel used for differentiation and must be larger than zero. " +
            "In order to enforce physical isotropy, for each dimension, the scale is divided by the size of the image elements (aspect ratio) in that dimension.")
    public double getSmoothing() {
        return smoothing;
    }

    @ACAQParameter("smoothing")
    public void setSmoothing(double smoothing) {
        this.smoothing = smoothing;

    }

    @ACAQParameter("gradient-radius")
    @ACAQDocumentation(name = "Gradient radius", description = "Radius of the internal gradient filter.")
    public double getGradientRadius() {
        return gradientRadius;
    }

    @ACAQParameter("gradient-radius")
    public void setGradientRadius(double gradientRadius) {
        this.gradientRadius = gradientRadius;

    }

    @ACAQParameter("auto-thresholding")
    @ACAQDocumentation(name = "Auto thresholding", description = "Parameters for underlying auto thresholding")
    public AutoThreshold2DAlgorithm getAutoThresholding() {
        return autoThresholding;
    }

    @Override
    public void reportValidity(ACAQValidityReport report) {
        report.checkIfWithin(this, gradientRadius, 0, Double.POSITIVE_INFINITY, false, true);
        report.checkIfWithin(this, smoothing, 0, Double.POSITIVE_INFINITY, false, true);
        report.forCategory("Auto thresholding").report(autoThresholding);
    }

    @ACAQDocumentation(name = "Compare absolute", description = "Determines whether eigenvalues are compared in absolute sense")
    @ACAQParameter("compare-absolute")
    public boolean isCompareAbsolute() {
        return compareAbsolute;
    }

    @ACAQParameter("compare-absolute")
    public void setCompareAbsolute(boolean compareAbsolute) {
        this.compareAbsolute = compareAbsolute;

    }

    @ACAQDocumentation(name = "Eigenvalue", description = "Allows you to choose whether the largest or smallest Eigenvalues are chosen")
    @ACAQParameter("eigenvalue-selection")
    public EigenvalueSelection2D getEigenvalueSelection() {
        return eigenvalueSelection;
    }

    @ACAQParameter("eigenvalue-selection")
    public void setEigenvalueSelection(EigenvalueSelection2D eigenvalueSelection) {
        this.eigenvalueSelection = eigenvalueSelection;

    }

    @ACAQDocumentation(name = "Apply internal gradient filter", description = "If enabled, an internal gradient filter is applied to the hessian image.")
    @ACAQParameter("apply-internal-gradient")
    public boolean isApplyInternalGradient() {
        return applyInternalGradient;
    }

    @ACAQParameter("apply-internal-gradient")
    public void setApplyInternalGradient(boolean applyInternalGradient) {
        this.applyInternalGradient = applyInternalGradient;
    }

    @ACAQDocumentation(name = "Apply despeckle", description = "If enabled, a median filter is applied to the thresholded images.")
    @ACAQParameter("apply-despeckle")
    public boolean isApplyDespeckle() {
        return applyDespeckle;
    }

    @ACAQParameter("apply-despeckle")
    public void setApplyDespeckle(boolean applyDespeckle) {
        this.applyDespeckle = applyDespeckle;
    }

    @ACAQDocumentation(name = "Despeckle iterations", description = "How many times the despeckle is applied.")
    @ACAQParameter("despeckle-iterations")
    public int getDespeckleIterations() {
        return despeckleIterations;
    }

    @ACAQParameter("despeckle-iterations")
    public void setDespeckleIterations(int despeckleIterations) {
        this.despeckleIterations = despeckleIterations;
    }

}