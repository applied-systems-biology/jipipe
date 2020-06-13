package org.hkijena.acaq5.extensions.imagejalgorithms.ij1.threshold;

import ij.ImagePlus;
import ij.ImageStack;
import ij.plugin.ImageCalculator;
import ij.plugin.filter.Binary;
import ij.plugin.filter.GaussianBlur;
import ij.plugin.filter.RankFilters;
import org.hkijena.acaq5.api.ACAQDocumentation;
import org.hkijena.acaq5.api.ACAQOrganization;
import org.hkijena.acaq5.api.ACAQRunnerSubStatus;
import org.hkijena.acaq5.api.ACAQValidityReport;
import org.hkijena.acaq5.api.algorithm.*;
import org.hkijena.acaq5.api.data.ACAQMutableSlotConfiguration;
import org.hkijena.acaq5.api.events.ParameterChangedEvent;
import org.hkijena.acaq5.api.parameters.ACAQParameter;
import org.hkijena.acaq5.api.registries.ACAQAlgorithmRegistry;
import org.hkijena.acaq5.extensions.imagejalgorithms.ij1.contrast.CLAHEContrastEnhancer;
import org.hkijena.acaq5.extensions.imagejdatatypes.datatypes.ImagePlusData;
import org.hkijena.acaq5.extensions.imagejdatatypes.datatypes.greyscale.ImagePlusGreyscale8UData;
import org.hkijena.acaq5.extensions.imagejdatatypes.datatypes.greyscale.ImagePlusGreyscaleData;
import org.hkijena.acaq5.extensions.imagejdatatypes.datatypes.greyscale.ImagePlusGreyscaleMaskData;
import org.hkijena.acaq5.utils.ImageJUtils;

import java.util.function.Consumer;
import java.util.function.Supplier;

import static org.hkijena.acaq5.extensions.imagejalgorithms.ImageJAlgorithmsExtension.ADD_MASK_QUALIFIER;

/**
 * Internal gradient segmenter
 */
@ACAQDocumentation(name = "Internal gradient segmentation 2D", description = "Segments objects by finding the internal gradients. " +
        "If higher-dimensional data is provided, the filter is applied to each 2D slice.")
@ACAQOrganization(menuPath = "Threshold", algorithmCategory = ACAQAlgorithmCategory.Processor)

// Algorithm flow
@AlgorithmInputSlot(value = ImagePlusGreyscaleData.class, slotName = "Input")
@AlgorithmOutputSlot(value = ImagePlusGreyscaleMaskData.class, slotName = "Output")

// Trait matching

// Trait configuration
public class InternalGradientSegmentation2DAlgorithm extends ACAQSimpleIteratingAlgorithm {

    private double gaussSigma = 3;
    private int internalGradientRadius = 25;
    private int dilationIterations = 3;
    private int erosionIterations = 2;
    private boolean applyFirstCLAHE = true;
    private boolean applySecondCLAHE = true;
    private boolean applyGaussian = true;

    private AutoThreshold2DAlgorithm autoThresholding;
    private CLAHEContrastEnhancer contrastEnhancer;

    /**
     * @param declaration the algorithm declaration
     */
    public InternalGradientSegmentation2DAlgorithm(ACAQAlgorithmDeclaration declaration) {
        super(declaration, ACAQMutableSlotConfiguration.builder().addInputSlot("Input", ImagePlusGreyscale8UData.class)
                .addOutputSlot("Output", ImagePlusGreyscaleMaskData.class, "Input", ADD_MASK_QUALIFIER)
                .allowOutputSlotInheritance(true)
                .seal()
                .build());
        this.contrastEnhancer = (CLAHEContrastEnhancer) ACAQAlgorithmRegistry.getInstance().getDeclarationById("ij1-contrast-clahe").newInstance();
        this.autoThresholding = (AutoThreshold2DAlgorithm) ACAQAlgorithmRegistry.getInstance().getDeclarationById("ij1-threshold-auto2d").newInstance();
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
        this.autoThresholding = (AutoThreshold2DAlgorithm) other.autoThresholding.getDeclaration().clone(other.autoThresholding);
        this.contrastEnhancer = (CLAHEContrastEnhancer) other.contrastEnhancer.getDeclaration().clone(other.contrastEnhancer);
        this.applyFirstCLAHE = other.applyFirstCLAHE;
        this.applySecondCLAHE = other.applySecondCLAHE;
        this.applyGaussian = other.applyGaussian;
    }

    private void applyInternalGradient(ImagePlus img) {
        // Erode the original image
        ImagePlus eroded = img.duplicate();
        RankFilters erosionFilter = new RankFilters();
        erosionFilter.rank(eroded.getProcessor(), internalGradientRadius, RankFilters.MIN); //TODO: Set element to octagon

        // Apply image calculator
        ImageCalculator calculator = new ImageCalculator();
        calculator.run("Subtract", img, eroded);
    }

    @Override
    protected void runIteration(ACAQDataInterface dataInterface, ACAQRunnerSubStatus subProgress, Consumer<ACAQRunnerSubStatus> algorithmProgress, Supplier<Boolean> isCancelled) {

        ImagePlus img = dataInterface.getInputData(getFirstInputSlot(), ImagePlusGreyscaleData.class).getImage();
        ImageStack stack = new ImageStack(img.getWidth(), img.getHeight(), img.getProcessor().getColorModel());

        CLAHEContrastEnhancer contrastEnhancerCopy = new CLAHEContrastEnhancer(contrastEnhancer);
        AutoThreshold2DAlgorithm autoThresholdingCopy = new AutoThreshold2DAlgorithm(autoThresholding);

        ImageJUtils.forEachIndexedSlice(img, (imp, index) -> {
            ImagePlus slice = new ImagePlus("slice", imp);
            ImagePlus processedSlice = slice.duplicate();

            // Apply CLAHE enhancer
            if (applyFirstCLAHE) {
                contrastEnhancerCopy.clearSlotData();
                contrastEnhancerCopy.getFirstInputSlot().addData(new ImagePlusGreyscaleData(processedSlice));
                contrastEnhancerCopy.run(subProgress.resolve("Slice " + index + "/" + img.getStackSize()).resolve("CLAHE Enhancer (1/2)"), algorithmProgress, isCancelled);
                processedSlice = contrastEnhancerCopy.getFirstOutputSlot().getData(0, ImagePlusData.class).getImage();
            }

            if (applyGaussian) {
                (new GaussianBlur()).blurGaussian(processedSlice.getProcessor(), gaussSigma);
                ImageJUtils.runOnImage(processedSlice, "8-bit");
                applyInternalGradient(processedSlice);
            }

            if (applySecondCLAHE) {
                contrastEnhancerCopy.clearSlotData();
                contrastEnhancerCopy.getFirstInputSlot().addData(new ImagePlusGreyscaleData(processedSlice));
                contrastEnhancerCopy.run(subProgress.resolve("Slice " + index + "/" + img.getStackSize()).resolve("CLAHE Enhancer (2/2)"), algorithmProgress, isCancelled);
                processedSlice = contrastEnhancerCopy.getFirstOutputSlot().getData(0, ImagePlusData.class).getImage();
            }

            // Convert image to mask and threshold with given auto threshold method
            autoThresholdingCopy.clearSlotData();
            autoThresholdingCopy.getFirstInputSlot().addData(new ImagePlusGreyscaleData(processedSlice));
            autoThresholdingCopy.run(subProgress.resolve("Slice " + index + "/" + img.getStackSize()).resolve("Auto-thresholding"), algorithmProgress, isCancelled);
            processedSlice = autoThresholdingCopy.getFirstOutputSlot().getData(0, ImagePlusData.class).getImage();

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
        });
        ImagePlus result = new ImagePlus("Segmented Image", stack);
        result.setDimensions(img.getNChannels(), img.getNSlices(), img.getNFrames());

        dataInterface.addOutputData(getFirstOutputSlot(), new ImagePlusGreyscaleMaskData(result));
    }

    @ACAQParameter("gauss-sigma")
    @ACAQDocumentation(name = "Gauss sigma", description = "Standard deviation of the Gaussian (pixels)")
    public double getGaussSigma() {
        return gaussSigma;
    }

    @ACAQParameter("gauss-sigma")
    public void setGaussSigma(double gaussSigma) {
        this.gaussSigma = gaussSigma;
        getEventBus().post(new ParameterChangedEvent(this, "gauss-sigma"));
    }

    @ACAQParameter("internal-gradient-radius")
    @ACAQDocumentation(name = "Internal gradient radius", description = "Radius for the internal gradient radius calculation")
    public int getInternalGradientRadius() {
        return internalGradientRadius;
    }

    @ACAQParameter("internal-gradient-radius")
    public void setInternalGradientRadius(int internalGradientRadius) {
        this.internalGradientRadius = internalGradientRadius;
        getEventBus().post(new ParameterChangedEvent(this, "internal-gradient-radius"));
    }

    @ACAQParameter("dilation-iterations")
    @ACAQDocumentation(name = "Dilation iterations", description = "Number of dilation iterations after filling holes")
    public int getDilationIterations() {
        return dilationIterations;
    }

    @ACAQParameter("dilation-iterations")
    public void setDilationIterations(int dilationIterations) {
        this.dilationIterations = dilationIterations;
        getEventBus().post(new ParameterChangedEvent(this, "dilation-iterations"));
    }

    @ACAQParameter("erosion-iterations")
    @ACAQDocumentation(name = "Erosion iterations", description = "Number of erosion iterations after filling holes. If you do not want to change " +
            "the object sizes, keep this the same as the dilation iterations.")
    public int getErosionIterations() {
        return erosionIterations;
    }

    @ACAQParameter("erosion-iterations")
    public void setErosionIterations(int erosionIterations) {
        this.erosionIterations = erosionIterations;
        getEventBus().post(new ParameterChangedEvent(this, "erosion-iterations"));
    }

    @ACAQParameter("auto-thresholding")
    @ACAQDocumentation(name = "Auto thresholding", description = "Parameters for underlying auto thresholding")
    public AutoThreshold2DAlgorithm getAutoThresholding() {
        return autoThresholding;
    }

    @ACAQParameter("clahe-enhancing")
    @ACAQDocumentation(name = "CLAHE Enhancer", description = "Parameters for underlying CLAHE Enhancing algorithm")
    public CLAHEContrastEnhancer getContrastEnhancer() {
        return contrastEnhancer;
    }

    @ACAQDocumentation(name = "Apply first CLAHE")
    @ACAQParameter("apply-first-clahe")
    public boolean isApplyFirstCLAHE() {
        return applyFirstCLAHE;
    }

    @ACAQParameter("apply-first-clahe")
    public void setApplyFirstCLAHE(boolean applyFirstCLAHE) {
        this.applyFirstCLAHE = applyFirstCLAHE;
    }

    @ACAQDocumentation(name = "Apply second CLAHE")
    @ACAQParameter("apply-second-clahe")
    public boolean isApplySecondCLAHE() {
        return applySecondCLAHE;
    }

    @ACAQParameter("apply-second-clahe")
    public void setApplySecondCLAHE(boolean applySecondCLAHE) {
        this.applySecondCLAHE = applySecondCLAHE;
    }

    @ACAQDocumentation(name = "Apply Gaussian")
    @ACAQParameter("apply-gaussian")
    public boolean isApplyGaussian() {
        return applyGaussian;
    }

    @ACAQParameter("apply-gaussian")
    public void setApplyGaussian(boolean applyGaussian) {
        this.applyGaussian = applyGaussian;
    }

    @Override
    public void reportValidity(ACAQValidityReport report) {
        report.forCategory("Auto Threshold Segmenter").report(autoThresholding);
        report.forCategory("CLAHE Enhancer").report(contrastEnhancer);
    }
}
