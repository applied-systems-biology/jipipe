package org.hkijena.acaq5.algorithms.enhancers;

import ij.ImagePlus;
import ij.Prefs;
import ij.plugin.ImageCalculator;
import ij.plugin.Thresholder;
import ij.plugin.filter.RankFilters;
import imagescience.feature.Hessian;
import imagescience.image.Aspects;
import imagescience.image.FloatImage;
import imagescience.image.Image;
import org.hkijena.acaq5.ACAQInputDataSlot;
import org.hkijena.acaq5.ACAQOutputDataSlot;
import org.hkijena.acaq5.ACAQSimpleAlgorithm;
import org.hkijena.acaq5.datatypes.ACAQGreyscaleImageData;
import org.hkijena.acaq5.datatypes.ACAQMaskData;
import org.hkijena.acaq5.utils.ImageJUtils;

import java.util.Vector;

public class HessianImageEnhancer extends ACAQSimpleAlgorithm<ACAQInputDataSlot<ACAQGreyscaleImageData>,
        ACAQOutputDataSlot<ACAQMaskData>> {

    private double smoothing = 1.0;
    private double gradientRadius = 1;

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
        erosionFilter.rank(eroded.getProcessor(), gradientRadius, RankFilters.MIN);

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

    private void convertToMask(ImagePlus img) {
        Prefs.blackBackground = true;
        ImageJUtils.runOnImage(img, new Thresholder());
    }

    @Override
    public void run() {
        ImagePlus img = getInputSlot().getData().getImage();

        // Apply hessian
        ImagePlus result = applyHessian(img);

        // Apply morphological filters
        applyInternalGradient(result);

        // Convert to mask
        convertToMask(result);

        // Despeckle x2
        applyDespeckle(result, 2);

        getOutputSlot().setData(new ACAQMaskData(result));
    }
}