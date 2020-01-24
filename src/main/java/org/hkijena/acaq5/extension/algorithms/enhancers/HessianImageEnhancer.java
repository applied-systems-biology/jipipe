package org.hkijena.acaq5.extension.algorithms.enhancers;

import ij.ImagePlus;
import ij.plugin.ImageCalculator;
import ij.plugin.filter.RankFilters;
import imagescience.feature.Hessian;
import imagescience.image.Aspects;
import imagescience.image.FloatImage;
import imagescience.image.Image;
import org.hkijena.acaq5.api.ACAQInputDataSlot;
import org.hkijena.acaq5.api.ACAQOutputDataSlot;
import org.hkijena.acaq5.api.ACAQSimpleAlgorithm;
import org.hkijena.acaq5.extension.algorithms.segmenters.AutoThresholdSegmenter;
import org.hkijena.acaq5.extension.datatypes.ACAQGreyscaleImageData;
import org.hkijena.acaq5.extension.datatypes.ACAQMaskData;

import java.util.Vector;

public class HessianImageEnhancer extends ACAQSimpleAlgorithm<ACAQGreyscaleImageData, ACAQMaskData> {

    private double smoothing = 1.0;
    private double gradientRadius = 1;
    private AutoThresholdSegmenter autoThresholdSegmenter = new AutoThresholdSegmenter();

    public HessianImageEnhancer() {
        super(new ACAQInputDataSlot<>("Input image", ACAQGreyscaleImageData.class),
                new ACAQOutputDataSlot<>("Output image", ACAQMaskData.class));
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
}