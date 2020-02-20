package org.hkijena.acaq5.extension.api.algorithms.segmenters;

import ij.ImagePlus;
import ij.plugin.ImageCalculator;
import ij.plugin.filter.Binary;
import ij.plugin.filter.GaussianBlur;
import ij.plugin.filter.RankFilters;
import org.hkijena.acaq5.api.*;
import org.hkijena.acaq5.api.traits.GoodForTrait;
import org.hkijena.acaq5.extension.api.algorithms.enhancers.CLAHEImageEnhancer;
import org.hkijena.acaq5.extension.api.dataslots.ACAQGreyscaleImageDataSlot;
import org.hkijena.acaq5.extension.api.dataslots.ACAQMaskDataSlot;
import org.hkijena.acaq5.extension.api.datatypes.ACAQGreyscaleImageData;
import org.hkijena.acaq5.extension.api.datatypes.ACAQMaskData;
import org.hkijena.acaq5.extension.api.traits.bioobject.MembraneLabeledBioObjects;
import org.hkijena.acaq5.utils.ImageJUtils;

@ACAQDocumentation(name = "Internal gradient segmentation")
@ACAQAlgorithmMetadata(category = ACAQAlgorithmCategory.Segmenter)
@GoodForTrait(MembraneLabeledBioObjects.class)
public class InternalGradientSegmenter extends ACAQSimpleAlgorithm<ACAQGreyscaleImageData, ACAQMaskData> {

    private double gaussSigma = 3;
    private int internalGradientRadius = 25;
    private int dilationIterations = 3;
    private int erosionIterations = 2;

    private AutoThresholdSegmenter autoThresholdSegmenter = new AutoThresholdSegmenter();
    private CLAHEImageEnhancer claheImageEnhancer = new CLAHEImageEnhancer();

    public InternalGradientSegmenter() {
        super("Image", ACAQGreyscaleImageDataSlot.class,
                "Mask", ACAQMaskDataSlot.class);
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
    public void run() {
        claheImageEnhancer.getInputSlot().setData(getInputSlot().getData());
        claheImageEnhancer.run();

        ImagePlus img = claheImageEnhancer.getOutputSlot().getData().getImage();
        (new GaussianBlur()).blurGaussian(img.getProcessor(), gaussSigma);
        ImageJUtils.runOnImage(img, "8-bit");
        applyInternalGradient(img);

        claheImageEnhancer.getInputSlot().setData(new ACAQGreyscaleImageData(img));
        claheImageEnhancer.run();
        img = claheImageEnhancer.getOutputSlot().getData().getImage();

        // Convert image to mask and threshold with given auto threshold method
        autoThresholdSegmenter.getInputSlot().setData(new ACAQGreyscaleImageData(img));
        autoThresholdSegmenter.run();
        img = autoThresholdSegmenter.getOutputSlot().getData().getImage();

        // Apply set of rank filters
        Binary binaryFilter = new Binary();

        binaryFilter.setup("dilate", null);
        binaryFilter.run(img.getProcessor());

        binaryFilter.setup("fill holes", null);
        binaryFilter.run(img.getProcessor());

        binaryFilter.setup("dilate", null);
        for(int i = 0; i < dilationIterations; ++i) {
            binaryFilter.run(img.getProcessor());
        }

        binaryFilter.setup("fill holes", null);
        binaryFilter.run(img.getProcessor());

        binaryFilter.setup("erode", null);
        for(int i = 0; i < erosionIterations; ++i) {
            binaryFilter.run(img.getProcessor());
        }
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

    @ACAQSubAlgorithm("auto-thresholding")
    public AutoThresholdSegmenter getAutoThresholdSegmenter() {
        return autoThresholdSegmenter;
    }

    @ACAQSubAlgorithm("clahe-enhancing")
    public CLAHEImageEnhancer getClaheImageEnhancer() {
        return claheImageEnhancer;
    }
}
