package org.hkijena.acaq5.extension.api.algorithms.segmenters;

import ij.ImagePlus;
import ij.plugin.ImageCalculator;
import ij.plugin.filter.Binary;
import ij.plugin.filter.GaussianBlur;
import ij.plugin.filter.RankFilters;
import org.hkijena.acaq5.api.ACAQDocumentation;
import org.hkijena.acaq5.api.ACAQValidityReport;
import org.hkijena.acaq5.api.algorithm.ACAQAlgorithmCategory;
import org.hkijena.acaq5.api.algorithm.ACAQAlgorithmDeclaration;
import org.hkijena.acaq5.api.algorithm.ACAQDataInterface;
import org.hkijena.acaq5.api.algorithm.ACAQEmptyAlgorithmDeclaration;
import org.hkijena.acaq5.api.algorithm.ACAQIteratingAlgorithm;
import org.hkijena.acaq5.api.algorithm.AlgorithmInputSlot;
import org.hkijena.acaq5.api.algorithm.AlgorithmMetadata;
import org.hkijena.acaq5.api.algorithm.AlgorithmOutputSlot;
import org.hkijena.acaq5.api.data.traits.GoodForTrait;
import org.hkijena.acaq5.api.data.traits.RemovesTrait;
import org.hkijena.acaq5.api.parameters.ACAQParameter;
import org.hkijena.acaq5.api.parameters.ACAQSubParameters;
import org.hkijena.acaq5.extension.api.algorithms.enhancers.CLAHEImageEnhancer;
import org.hkijena.acaq5.extension.api.datatypes.ACAQGreyscaleImageData;
import org.hkijena.acaq5.extension.api.datatypes.ACAQMaskData;
import org.hkijena.acaq5.extension.api.traits.bioobject.preparations.labeling.MembraneLabeledBioObjects;
import org.hkijena.acaq5.extension.api.traits.quality.ImageQuality;
import org.hkijena.acaq5.utils.ImageJUtils;

@ACAQDocumentation(name = "Internal gradient segmentation")
@AlgorithmMetadata(category = ACAQAlgorithmCategory.Segmenter)

// Algorithm flow
@AlgorithmInputSlot(value = ACAQGreyscaleImageData.class, slotName = "Image", autoCreate = true)
@AlgorithmOutputSlot(value = ACAQMaskData.class, slotName = "Mask", autoCreate = true)

// Trait matching
@GoodForTrait(MembraneLabeledBioObjects.class)

// Trait configuration
@RemovesTrait(ImageQuality.class)
public class InternalGradientSegmenter extends ACAQIteratingAlgorithm {

    private double gaussSigma = 3;
    private int internalGradientRadius = 25;
    private int dilationIterations = 3;
    private int erosionIterations = 2;

    private AutoThresholdSegmenter autoThresholdSegmenter = new AutoThresholdSegmenter(new ACAQEmptyAlgorithmDeclaration());
    private CLAHEImageEnhancer claheImageEnhancer = new CLAHEImageEnhancer(new ACAQEmptyAlgorithmDeclaration());

    public InternalGradientSegmenter(ACAQAlgorithmDeclaration declaration) {
        super(declaration);
    }

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

        ImagePlus img = ((ACAQMaskData) claheImageEnhancer.getFirstOutputSlot().getData(0)).getImage().duplicate();
        (new GaussianBlur()).blurGaussian(img.getProcessor(), gaussSigma);
        ImageJUtils.runOnImage(img, "8-bit");
        applyInternalGradient(img);

        claheImageEnhancer.getFirstInputSlot().addData(new ACAQGreyscaleImageData(img));
        claheImageEnhancer.run();
        img = ((ACAQGreyscaleImageData) claheImageEnhancer.getFirstOutputSlot().getData(0)).getImage();

        // Convert image to mask and threshold with given auto threshold method
        autoThresholdSegmenter.getFirstInputSlot().addData(new ACAQGreyscaleImageData(img));
        autoThresholdSegmenter.run();
        img = ((ACAQMaskData) autoThresholdSegmenter.getFirstOutputSlot().getData(0)).getImage();

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

        dataInterface.addOutputData(getFirstOutputSlot(), new ACAQMaskData(img));
    }

    @ACAQParameter("gauss-sigma")
    @ACAQDocumentation(name = "Gauss sigma")
    public double getGaussSigma() {
        return gaussSigma;
    }

    @ACAQParameter("gauss-sigma")
    public void setGaussSigma(double gaussSigma) {
        this.gaussSigma = gaussSigma;
    }

    @ACAQParameter("internal-gradient-radius")
    @ACAQDocumentation(name = "Internal gradient radius")
    public int getInternalGradientRadius() {
        return internalGradientRadius;
    }

    @ACAQParameter("internal-gradient-radius")
    public void setInternalGradientRadius(int internalGradientRadius) {
        this.internalGradientRadius = internalGradientRadius;
    }

    @ACAQParameter("dilation-iterations")
    @ACAQDocumentation(name = "Dilation iterations")
    public int getDilationIterations() {
        return dilationIterations;
    }

    @ACAQParameter("dilation-iterations")
    public void setDilationIterations(int dilationIterations) {
        this.dilationIterations = dilationIterations;
    }

    @ACAQParameter("erosion-iterations")
    @ACAQDocumentation(name = "Erosion iterations")
    public int getErosionIterations() {
        return erosionIterations;
    }

    @ACAQParameter("erosion-iterations")
    public void setErosionIterations(int erosionIterations) {
        this.erosionIterations = erosionIterations;
    }

    @ACAQSubParameters("auto-thresholding")
    public AutoThresholdSegmenter getAutoThresholdSegmenter() {
        return autoThresholdSegmenter;
    }

    @ACAQSubParameters("clahe-enhancing")
    public CLAHEImageEnhancer getClaheImageEnhancer() {
        return claheImageEnhancer;
    }

    @Override
    public void reportValidity(ACAQValidityReport report) {
        report.forCategory("Auto Threshold Segmenter").report(autoThresholdSegmenter);
        report.forCategory("CLAHE Enhancer").report(claheImageEnhancer);
    }
}
