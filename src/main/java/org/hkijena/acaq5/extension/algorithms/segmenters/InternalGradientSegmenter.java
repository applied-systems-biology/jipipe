package org.hkijena.acaq5.extension.algorithms.segmenters;

import ij.ImagePlus;
import ij.plugin.ImageCalculator;
import ij.plugin.filter.Binary;
import ij.plugin.filter.GaussianBlur;
import ij.plugin.filter.RankFilters;
import org.hkijena.acaq5.api.ACAQAlgorithmCategory;
import org.hkijena.acaq5.api.ACAQAlgorithmMetadata;
import org.hkijena.acaq5.api.ACAQDocumentation;
import org.hkijena.acaq5.api.ACAQSimpleAlgorithm;
import org.hkijena.acaq5.extension.algorithms.enhancers.CLAHEImageEnhancer;
import org.hkijena.acaq5.extension.dataslots.ACAQGreyscaleImageDataSlot;
import org.hkijena.acaq5.extension.dataslots.ACAQMaskDataSlot;
import org.hkijena.acaq5.extension.datatypes.ACAQGreyscaleImageData;
import org.hkijena.acaq5.extension.datatypes.ACAQMaskData;
import org.hkijena.acaq5.utils.ImageJUtils;

@ACAQDocumentation(name = "Internal gradient segmentation")
@ACAQAlgorithmMetadata(category = ACAQAlgorithmCategory.Segmenter)
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
}
