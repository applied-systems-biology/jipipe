package org.hkijena.acaq5.extension.algorithms.segmenters;

import ij.ImagePlus;
import ij.plugin.filter.BackgroundSubtracter;
import ij.plugin.filter.Binary;
import ij.plugin.filter.GaussianBlur;
import org.hkijena.acaq5.api.ACAQInputDataSlot;
import org.hkijena.acaq5.api.ACAQOutputDataSlot;
import org.hkijena.acaq5.api.ACAQSimpleAlgorithm;
import org.hkijena.acaq5.extension.datatypes.ACAQGreyscaleImageData;
import org.hkijena.acaq5.extension.datatypes.ACAQMaskData;

// TODO: Suggest CLAHE
public class BrightSpotsSegmenter extends ACAQSimpleAlgorithm<ACAQGreyscaleImageData, ACAQMaskData> {

    private int rollingBallRadius = 20;
    private int dilationErodeSteps = 2;
    private double gaussianSigma = 3;
    private AutoThresholdSegmenter autoThresholdSegmenter = new AutoThresholdSegmenter();

    public BrightSpotsSegmenter() {
        super(new ACAQInputDataSlot<>("Image", ACAQGreyscaleImageData.class),
                new ACAQOutputDataSlot<>("Mask", ACAQMaskData.class));
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
}
