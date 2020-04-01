package org.hkijena.acaq5.extensions.standardalgorithms.api.algorithms.segmenters;

import ij.ImagePlus;
import ij.plugin.ImageCalculator;
import ij.plugin.filter.Binary;
import ij.plugin.filter.GaussianBlur;
import ij.plugin.filter.RankFilters;
import org.hkijena.acaq5.api.ACAQDocumentation;
import org.hkijena.acaq5.api.ACAQValidityReport;
import org.hkijena.acaq5.api.algorithm.*;
import org.hkijena.acaq5.api.data.traits.GoodForTrait;
import org.hkijena.acaq5.api.data.traits.RemovesTrait;
import org.hkijena.acaq5.api.events.ParameterChangedEvent;
import org.hkijena.acaq5.api.parameters.ACAQParameter;
import org.hkijena.acaq5.api.parameters.ACAQSubParameters;
import org.hkijena.acaq5.extensions.imagejdatatypes.datatypes.d2.greyscale.ImagePlus2DGreyscaleData;
import org.hkijena.acaq5.extensions.imagejdatatypes.datatypes.d2.greyscale.ImagePlus2DGreyscaleMaskData;
import org.hkijena.acaq5.extensions.standardalgorithms.api.algorithms.enhancers.CLAHEImageEnhancer;
import org.hkijena.acaq5.utils.ImageJUtils;

/**
 * Internal gradient segmenter
 */
@ACAQDocumentation(name = "Internal gradient segmentation")
@AlgorithmMetadata(category = ACAQAlgorithmCategory.Segmentation)

// Algorithm flow
@AlgorithmInputSlot(value = ImagePlus2DGreyscaleData.class, slotName = "Image", autoCreate = true)
@AlgorithmOutputSlot(value = ImagePlus2DGreyscaleMaskData.class, slotName = "Mask", autoCreate = true)

// Trait matching
@GoodForTrait("bioobject-preparations-labeling-membrane")

// Trait configuration
@RemovesTrait("image-quality")
public class InternalGradientSegmenter extends ACAQIteratingAlgorithm {

    private double gaussSigma = 3;
    private int internalGradientRadius = 25;
    private int dilationIterations = 3;
    private int erosionIterations = 2;

    private AutoThresholdSegmenter autoThresholdSegmenter = new AutoThresholdSegmenter(new ACAQEmptyAlgorithmDeclaration());
    private CLAHEImageEnhancer claheImageEnhancer = new CLAHEImageEnhancer(new ACAQEmptyAlgorithmDeclaration());

    /**
     * @param declaration the algorithm declaration
     */
    public InternalGradientSegmenter(ACAQAlgorithmDeclaration declaration) {
        super(declaration);
    }

    /**
     * Copies the algorithm
     *
     * @param other the original
     */
    public InternalGradientSegmenter(InternalGradientSegmenter other) {
        super(other);
        this.gaussSigma = other.gaussSigma;
        this.internalGradientRadius = other.internalGradientRadius;
        this.dilationIterations = other.dilationIterations;
        this.erosionIterations = other.erosionIterations;
        this.autoThresholdSegmenter = new AutoThresholdSegmenter(other.autoThresholdSegmenter);
        this.claheImageEnhancer = new CLAHEImageEnhancer(other.claheImageEnhancer);
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
    protected void runIteration(ACAQDataInterface dataInterface) {

        claheImageEnhancer.getFirstInputSlot().addData(dataInterface.getInputData(getFirstInputSlot()));
        claheImageEnhancer.run();

        ImagePlus img = ((ImagePlus2DGreyscaleMaskData) claheImageEnhancer.getFirstOutputSlot().getData(0)).getImage().duplicate();
        (new GaussianBlur()).blurGaussian(img.getProcessor(), gaussSigma);
        ImageJUtils.runOnImage(img, "8-bit");
        applyInternalGradient(img);

        claheImageEnhancer.getFirstInputSlot().addData(new ImagePlus2DGreyscaleData(img));
        claheImageEnhancer.run();
        img = ((ImagePlus2DGreyscaleData) claheImageEnhancer.getFirstOutputSlot().getData(0)).getImage();

        // Convert image to mask and threshold with given auto threshold method
        autoThresholdSegmenter.getFirstInputSlot().addData(new ImagePlus2DGreyscaleData(img));
        autoThresholdSegmenter.run();
        img = ((ImagePlus2DGreyscaleMaskData) autoThresholdSegmenter.getFirstOutputSlot().getData(0)).getImage();

        // Apply set of rank filters
        Binary binaryFilter = new Binary();

        binaryFilter.setup("dilate", null);
        binaryFilter.run(img.getProcessor());

        binaryFilter.setup("fill holes", null);
        binaryFilter.run(img.getProcessor());

        binaryFilter.setup("dilate", null);
        for (int i = 0; i < dilationIterations; ++i) {
            binaryFilter.run(img.getProcessor());
        }

        binaryFilter.setup("fill holes", null);
        binaryFilter.run(img.getProcessor());

        binaryFilter.setup("erode", null);
        for (int i = 0; i < erosionIterations; ++i) {
            binaryFilter.run(img.getProcessor());
        }

        dataInterface.addOutputData(getFirstOutputSlot(), new ImagePlus2DGreyscaleMaskData(img));
    }

    @ACAQParameter("gauss-sigma")
    @ACAQDocumentation(name = "Gauss sigma")
    public double getGaussSigma() {
        return gaussSigma;
    }

    @ACAQParameter("gauss-sigma")
    public void setGaussSigma(double gaussSigma) {
        this.gaussSigma = gaussSigma;
        getEventBus().post(new ParameterChangedEvent(this, "gauss-sigma"));
    }

    @ACAQParameter("internal-gradient-radius")
    @ACAQDocumentation(name = "Internal gradient radius")
    public int getInternalGradientRadius() {
        return internalGradientRadius;
    }

    @ACAQParameter("internal-gradient-radius")
    public void setInternalGradientRadius(int internalGradientRadius) {
        this.internalGradientRadius = internalGradientRadius;
        getEventBus().post(new ParameterChangedEvent(this, "internal-gradient-radius"));
    }

    @ACAQParameter("dilation-iterations")
    @ACAQDocumentation(name = "Dilation iterations")
    public int getDilationIterations() {
        return dilationIterations;
    }

    @ACAQParameter("dilation-iterations")
    public void setDilationIterations(int dilationIterations) {
        this.dilationIterations = dilationIterations;
        getEventBus().post(new ParameterChangedEvent(this, "dilation-iterations"));
    }

    @ACAQParameter("erosion-iterations")
    @ACAQDocumentation(name = "Erosion iterations")
    public int getErosionIterations() {
        return erosionIterations;
    }

    @ACAQParameter("erosion-iterations")
    public void setErosionIterations(int erosionIterations) {
        this.erosionIterations = erosionIterations;
        getEventBus().post(new ParameterChangedEvent(this, "erosion-iterations"));
    }

    @ACAQSubParameters("auto-thresholding")
    @ACAQDocumentation(name = "Auto thresholding", description = "Parameters for underlying auto thresholding")
    public AutoThresholdSegmenter getAutoThresholdSegmenter() {
        return autoThresholdSegmenter;
    }

    @ACAQSubParameters("clahe-enhancing")
    @ACAQDocumentation(name = "CLAHE Enhancer", description = "Parameters for underlying CLAHE Enhancing algorithm")
    public CLAHEImageEnhancer getClaheImageEnhancer() {
        return claheImageEnhancer;
    }

    @Override
    public void reportValidity(ACAQValidityReport report) {
        report.forCategory("Auto Threshold Segmenter").report(autoThresholdSegmenter);
        report.forCategory("CLAHE Enhancer").report(claheImageEnhancer);
    }
}
