package org.hkijena.acaq5.extension.api.algorithms.segmenters;

import ij.ImagePlus;
import ij.plugin.ImageCalculator;
import ij.plugin.filter.RankFilters;
import imagescience.feature.Hessian;
import imagescience.image.Aspects;
import imagescience.image.FloatImage;
import imagescience.image.Image;
import org.hkijena.acaq5.api.ACAQDocumentation;
import org.hkijena.acaq5.api.ACAQValidityReport;
import org.hkijena.acaq5.api.algorithm.*;
import org.hkijena.acaq5.api.data.traits.AutoTransferTraits;
import org.hkijena.acaq5.api.data.traits.RemovesTrait;
import org.hkijena.acaq5.api.parameters.ACAQParameter;
import org.hkijena.acaq5.api.parameters.ACAQSubAlgorithm;
import org.hkijena.acaq5.extension.api.datatypes.ACAQGreyscaleImageData;
import org.hkijena.acaq5.extension.api.datatypes.ACAQMaskData;
import org.hkijena.acaq5.extension.api.traits.quality.ImageQuality;

import java.util.Vector;

@ACAQDocumentation(name = "Hessian segmenter")
@AlgorithmMetadata(category = ACAQAlgorithmCategory.Segmenter)

// Algorithm flow
@AlgorithmInputSlot(value = ACAQGreyscaleImageData.class, slotName = "Image", autoCreate = true)
@AlgorithmOutputSlot(value = ACAQMaskData.class, slotName = "Mask", autoCreate = true)

// Traits
@AutoTransferTraits
@RemovesTrait(ImageQuality.class)
public class HessianSegmenter extends ACAQIteratingAlgorithm {

    private double smoothing = 1.0;
    private double gradientRadius = 1;
    private AutoThresholdSegmenter autoThresholdSegmenter = new AutoThresholdSegmenter(new ACAQEmptyAlgorithmDeclaration());

    public HessianSegmenter(ACAQAlgorithmDeclaration declaration) {
        super(declaration);
    }

    public HessianSegmenter(HessianSegmenter other) {
        super(other);
        this.smoothing = other.smoothing;
        this.gradientRadius = other.gradientRadius;
        this.autoThresholdSegmenter = other.autoThresholdSegmenter;
    }

    private ImagePlus applyHessian(ImagePlus input) {
        final Image image = Image.wrap(input);
        image.aspects(new Aspects());
        final Hessian hessian = new Hessian();
        final Vector<Image> eigenimages = hessian.run(new FloatImage(image), smoothing, true);
        Image largest = eigenimages.get(0);
        return largest.imageplus();
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
    protected void runIteration(ACAQDataInterface dataInterface) {
        ACAQGreyscaleImageData inputImage = dataInterface.getInputData(getFirstInputSlot());
        ImagePlus img = inputImage.getImage();

        // Apply hessian
        ImagePlus result = applyHessian(img);

        // Apply morphological filters
        applyInternalGradient(result);

        // Convert to mask
        autoThresholdSegmenter.getFirstInputSlot().addData(new ACAQGreyscaleImageData(result));
        autoThresholdSegmenter.run();
        result = ((ACAQMaskData) autoThresholdSegmenter.getFirstOutputSlot().getData(0)).getImage();

        // Despeckle x2
        applyDespeckle(result, 2);

        dataInterface.addOutputData(getFirstOutputSlot(), new ACAQMaskData(result));
    }

    @ACAQParameter("smoothing")
    @ACAQDocumentation(name = "Smoothing")
    public double getSmoothing() {
        return smoothing;
    }

    @ACAQParameter("smoothing")
    public void setSmoothing(double smoothing) {
        this.smoothing = smoothing;
    }

    @ACAQParameter("gradient-radius")
    @ACAQDocumentation(name = "Gradient radius")
    public double getGradientRadius() {
        return gradientRadius;
    }

    @ACAQParameter("gradient-radius")
    public void setGradientRadius(double gradientRadius) {
        this.gradientRadius = gradientRadius;
    }

    @ACAQSubAlgorithm("auto-thresholding")
    public AutoThresholdSegmenter getAutoThresholdSegmenter() {
        return autoThresholdSegmenter;
    }

    @Override
    public void reportValidity(ACAQValidityReport report) {
        report.forCategory("Auto threshold segmenter").report(autoThresholdSegmenter);
    }
}