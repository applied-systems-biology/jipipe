package org.hkijena.acaq5.extension.api.algorithms.enhancers;

import ij.ImagePlus;
import ij.plugin.ImageCalculator;
import ij.plugin.filter.RankFilters;
import imagescience.feature.Hessian;
import imagescience.image.Aspects;
import imagescience.image.FloatImage;
import imagescience.image.Image;
import org.hkijena.acaq5.api.*;
import org.hkijena.acaq5.extension.api.algorithms.segmenters.AutoThresholdSegmenter;
import org.hkijena.acaq5.extension.api.dataslots.ACAQGreyscaleImageDataSlot;
import org.hkijena.acaq5.extension.api.dataslots.ACAQMaskDataSlot;
import org.hkijena.acaq5.extension.api.datatypes.ACAQGreyscaleImageData;
import org.hkijena.acaq5.extension.api.datatypes.ACAQMaskData;

import java.util.Vector;

@ACAQDocumentation(name = "Hessian enhancer")
@ACAQAlgorithmMetadata(category = ACAQAlgorithmCategory.Enhancer)
public class HessianImageEnhancer extends ACAQSimpleAlgorithm<ACAQGreyscaleImageData, ACAQMaskData> {

    private double smoothing = 1.0;
    private double gradientRadius = 1;
    private AutoThresholdSegmenter autoThresholdSegmenter = new AutoThresholdSegmenter();

    public HessianImageEnhancer() {
        super("Input image", ACAQGreyscaleImageDataSlot.class,
               "Output image", ACAQMaskDataSlot.class);
    }

    public HessianImageEnhancer(HessianImageEnhancer other) {
        this();
        this.getSlotConfiguration().setTo(other.getSlotConfiguration());
        this.smoothing = other.smoothing;
        this.gradientRadius = other.gradientRadius;
        this.autoThresholdSegmenter = other.autoThresholdSegmenter;
    }

    private ImagePlus applyHessian(ImagePlus input) {
        final Image image = Image.wrap(input);
        image.aspects(new Aspects());
        final Hessian hessian = new Hessian();
        final Vector<Image> eigenimages = hessian.run(new FloatImage(image),smoothing,true);
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
        for(int i = 0; i < iterations; ++i) {
            rankFilters.rank(img.getProcessor(), 1, RankFilters.MEDIAN);
        }
    }

    @Override
    public void run() {
        ImagePlus img = getInputSlot().getData().getImage();

        // Apply hessian
        ImagePlus result = applyHessian(img);

        // Apply morphological filters
        applyInternalGradient(result);

        // Convert to mask
        autoThresholdSegmenter.getInputSlot().setData(new ACAQGreyscaleImageData(result));
        autoThresholdSegmenter.run();
        result = autoThresholdSegmenter.getOutputSlot().getData().getImage();

        // Despeckle x2
        applyDespeckle(result, 2);

        getOutputSlot().setData(new ACAQMaskData(result));
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
}