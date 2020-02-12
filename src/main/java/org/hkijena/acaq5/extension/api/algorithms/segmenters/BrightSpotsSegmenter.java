package org.hkijena.acaq5.extension.api.algorithms.segmenters;

import ij.ImagePlus;
import ij.plugin.filter.BackgroundSubtracter;
import ij.plugin.filter.Binary;
import ij.plugin.filter.GaussianBlur;
import org.hkijena.acaq5.api.*;
import org.hkijena.acaq5.extension.api.dataslots.ACAQGreyscaleImageDataSlot;
import org.hkijena.acaq5.extension.api.dataslots.ACAQMaskDataSlot;
import org.hkijena.acaq5.extension.api.datatypes.ACAQGreyscaleImageData;
import org.hkijena.acaq5.extension.api.datatypes.ACAQMaskData;

// TODO: Suggest CLAHE
@ACAQDocumentation(name = "Bright spots segmentation")
@ACAQAlgorithmMetadata(category = ACAQAlgorithmCategory.Segmenter)
public class BrightSpotsSegmenter extends ACAQSimpleAlgorithm<ACAQGreyscaleImageData, ACAQMaskData> {

    private int rollingBallRadius = 20;
    private int dilationErodeSteps = 2;
    private double gaussianSigma = 3;
    private AutoThresholdSegmenter autoThresholdSegmenter = new AutoThresholdSegmenter();

    public BrightSpotsSegmenter() {
        super("Image", ACAQGreyscaleImageDataSlot.class,
                "Mask", ACAQMaskDataSlot.class);
    }

    @Override
    public void run() {
        ImagePlus img = getInputSlot().getData().getImage();

        ImagePlus result = img.duplicate();

        // Apply background subtraction
        BackgroundSubtracter backgroundSubtracter = new BackgroundSubtracter();
        backgroundSubtracter.rollingBallBackground(result.getProcessor(),
                rollingBallRadius,
                false,
                false,
                false,
                true,
                true);

        // Apply auto threshold
        autoThresholdSegmenter.getInputSlot().setData(new ACAQGreyscaleImageData(result));
        autoThresholdSegmenter.run();
        result = autoThresholdSegmenter.getOutputSlot().getData().getImage();

        // Apply morphologial operations
        Binary binaryFilter = new Binary();

        binaryFilter.setup("dilate", null);
        for(int i = 0; i < dilationErodeSteps; ++i) {
            binaryFilter.run(result.getProcessor());
        }

        binaryFilter.setup("fill holes", null);
        binaryFilter.run(result.getProcessor());

        binaryFilter.setup("erode", null);
        for(int i = 0; i < dilationErodeSteps; ++i) {
            binaryFilter.run(result.getProcessor());
        }

        // Smooth the spots and re-threshold them
        if(gaussianSigma > 0) {
            GaussianBlur gaussianBlur = new GaussianBlur();
            gaussianBlur.blurGaussian(result.getProcessor(), gaussianSigma);

            autoThresholdSegmenter.getInputSlot().setData(new ACAQGreyscaleImageData(result));
            autoThresholdSegmenter.run();
            result = autoThresholdSegmenter.getOutputSlot().getData().getImage();
        }

        getOutputSlot().setData(new ACAQMaskData(result));
    }

    @ACAQParameter("rolling-ball-radius")
    @ACAQDocumentation(name = "Rolling ball radius")
    public int getRollingBallRadius() {
        return rollingBallRadius;
    }

    @ACAQParameter("rolling-ball-radius")
    public void setRollingBallRadius(int rollingBallRadius) {
        this.rollingBallRadius = rollingBallRadius;
    }

    @ACAQParameter("dilation-erode-steps")
    @ACAQDocumentation(name = "Dilation erode steps")
    public int getDilationErodeSteps() {
        return dilationErodeSteps;
    }

    @ACAQParameter("dilation-erode-steps")
    public void setDilationErodeSteps(int dilationErodeSteps) {
        this.dilationErodeSteps = dilationErodeSteps;
    }

    @ACAQParameter("gaussian-sigma")
    @ACAQDocumentation(name = "Gaussian sigma")
    public double getGaussianSigma() {
        return gaussianSigma;
    }

    @ACAQParameter("gaussian-sigma")
    public void setGaussianSigma(double gaussianSigma) {
        this.gaussianSigma = gaussianSigma;
    }

    @ACAQSubAlgorithm("auto-thresholding")
    public AutoThresholdSegmenter getAutoThresholdSegmenter() {
        return autoThresholdSegmenter;
    }
}
