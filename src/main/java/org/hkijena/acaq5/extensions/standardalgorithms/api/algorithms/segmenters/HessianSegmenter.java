package org.hkijena.acaq5.extensions.standardalgorithms.api.algorithms.segmenters;

import ij.ImagePlus;
import ij.plugin.ImageCalculator;
import ij.plugin.filter.RankFilters;
import imagescience.feature.Hessian;
import imagescience.image.Aspects;
import imagescience.image.FloatImage;
import imagescience.image.Image;
import org.hkijena.acaq5.api.ACAQDocumentation;
import org.hkijena.acaq5.api.ACAQOrganization;
import org.hkijena.acaq5.api.ACAQRunnerSubStatus;
import org.hkijena.acaq5.api.ACAQValidityReport;
import org.hkijena.acaq5.api.algorithm.ACAQAlgorithmCategory;
import org.hkijena.acaq5.api.algorithm.ACAQAlgorithmDeclaration;
import org.hkijena.acaq5.api.algorithm.ACAQDataInterface;
import org.hkijena.acaq5.api.algorithm.ACAQEmptyAlgorithmDeclaration;
import org.hkijena.acaq5.api.algorithm.ACAQIteratingAlgorithm;
import org.hkijena.acaq5.api.algorithm.AlgorithmInputSlot;
import org.hkijena.acaq5.api.algorithm.AlgorithmOutputSlot;
import org.hkijena.acaq5.api.data.traits.RemovesTrait;
import org.hkijena.acaq5.api.events.ParameterChangedEvent;
import org.hkijena.acaq5.api.parameters.ACAQParameter;
import org.hkijena.acaq5.extensions.imagejdatatypes.datatypes.ImagePlusData;
import org.hkijena.acaq5.extensions.imagejdatatypes.datatypes.d2.greyscale.ImagePlus2DGreyscaleData;
import org.hkijena.acaq5.extensions.imagejdatatypes.datatypes.d2.greyscale.ImagePlus2DGreyscaleMaskData;

import java.util.Vector;
import java.util.function.Consumer;
import java.util.function.Supplier;

/**
 * Segments using a Hessian
 */
@ACAQDocumentation(name = "Hessian segmenter (deprecated)")
@ACAQOrganization(menuPath = "Threshold", algorithmCategory = ACAQAlgorithmCategory.Processor)

// Algorithm flow
@AlgorithmInputSlot(value = ImagePlus2DGreyscaleData.class, slotName = "Image", autoCreate = true)
@AlgorithmOutputSlot(value = ImagePlus2DGreyscaleMaskData.class, slotName = "Mask", autoCreate = true)

// Traits
@RemovesTrait("image-quality")
public class HessianSegmenter extends ACAQIteratingAlgorithm {

    private double smoothing = 1.0;
    private double gradientRadius = 1;
    private AutoThresholdSegmenter autoThresholdSegmenter = new AutoThresholdSegmenter(new ACAQEmptyAlgorithmDeclaration());

    /**
     * @param declaration the algorithm declaration
     */
    public HessianSegmenter(ACAQAlgorithmDeclaration declaration) {
        super(declaration);
    }

    /**
     * Copies the algorithm
     *
     * @param other the original
     */
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
    protected void runIteration(ACAQDataInterface dataInterface, ACAQRunnerSubStatus subProgress, Consumer<ACAQRunnerSubStatus> algorithmProgress, Supplier<Boolean> isCancelled) {
        ImagePlus2DGreyscaleData inputImage = dataInterface.getInputData(getFirstInputSlot(), ImagePlus2DGreyscaleData.class);
        ImagePlus img = inputImage.getImage();

        // Apply hessian
        ImagePlus result = applyHessian(img);

        // Apply morphological filters
        applyInternalGradient(result);

        // Convert to mask
        autoThresholdSegmenter.clearSlotData();
        autoThresholdSegmenter.getFirstInputSlot().addData(new ImagePlus2DGreyscaleData(result));
        autoThresholdSegmenter.run(subProgress.resolve("Auto-thresholding"), algorithmProgress, isCancelled);
        result = autoThresholdSegmenter.getFirstOutputSlot().getData(0, ImagePlusData.class).getImage();

        // Despeckle x2
        applyDespeckle(result, 2);

        dataInterface.addOutputData(getFirstOutputSlot(), new ImagePlus2DGreyscaleMaskData(result));
    }

    @ACAQParameter("smoothing")
    @ACAQDocumentation(name = "Smoothing")
    public double getSmoothing() {
        return smoothing;
    }

    @ACAQParameter("smoothing")
    public void setSmoothing(double smoothing) {
        this.smoothing = smoothing;
        getEventBus().post(new ParameterChangedEvent(this, "smoothing"));
    }

    @ACAQParameter("gradient-radius")
    @ACAQDocumentation(name = "Gradient radius")
    public double getGradientRadius() {
        return gradientRadius;
    }

    @ACAQParameter("gradient-radius")
    public void setGradientRadius(double gradientRadius) {
        this.gradientRadius = gradientRadius;
        getEventBus().post(new ParameterChangedEvent(this, "gradient-radius"));
    }

    @ACAQParameter("auto-thresholding")
    @ACAQDocumentation(name = "Auto thresholding", description = "Parameters for underlying auto thresholding")
    public AutoThresholdSegmenter getAutoThresholdSegmenter() {
        return autoThresholdSegmenter;
    }

    @Override
    public void reportValidity(ACAQValidityReport report) {
        report.forCategory("Auto threshold segmenter").report(autoThresholdSegmenter);
    }
}