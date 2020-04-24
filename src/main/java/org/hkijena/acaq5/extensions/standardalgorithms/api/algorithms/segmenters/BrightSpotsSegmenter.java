package org.hkijena.acaq5.extensions.standardalgorithms.api.algorithms.segmenters;

import ij.ImagePlus;
import ij.plugin.filter.BackgroundSubtracter;
import ij.plugin.filter.Binary;
import ij.plugin.filter.GaussianBlur;
import org.hkijena.acaq5.api.ACAQDocumentation;
import org.hkijena.acaq5.api.ACAQOrganization;
import org.hkijena.acaq5.api.ACAQRunnerSubStatus;
import org.hkijena.acaq5.api.ACAQValidityReport;
import org.hkijena.acaq5.api.algorithm.*;
import org.hkijena.acaq5.api.data.traits.AddsTrait;
import org.hkijena.acaq5.api.data.traits.BadForTrait;
import org.hkijena.acaq5.api.data.traits.RemovesTrait;
import org.hkijena.acaq5.api.events.ParameterChangedEvent;
import org.hkijena.acaq5.api.parameters.ACAQParameter;
import org.hkijena.acaq5.api.parameters.ACAQSubParameters;
import org.hkijena.acaq5.extensions.imagejdatatypes.datatypes.ImagePlusData;
import org.hkijena.acaq5.extensions.imagejdatatypes.datatypes.d2.greyscale.ImagePlus2DGreyscaleData;
import org.hkijena.acaq5.extensions.imagejdatatypes.datatypes.d2.greyscale.ImagePlus2DGreyscaleMaskData;

import java.util.function.Consumer;
import java.util.function.Supplier;

/**
 * Applies Bright spots segmentation
 */
@ACAQDocumentation(name = "Bright spots segmentation")
@ACAQOrganization(menuPath = "Threshold", algorithmCategory = ACAQAlgorithmCategory.Processor)

// Algorithm flow
@AlgorithmInputSlot(value = ImagePlus2DGreyscaleData.class, slotName = "Image", autoCreate = true)
@AlgorithmOutputSlot(value = ImagePlus2DGreyscaleMaskData.class, slotName = "Mask", autoCreate = true)

// Trait matching
@BadForTrait("image-quality-brightness-nonuniform")

// Trait configuration
@RemovesTrait("image-quality")
@AddsTrait("bioobject-count-cluster")
public class BrightSpotsSegmenter extends ACAQIteratingAlgorithm {

    private int rollingBallRadius = 20;
    private int dilationErodeSteps = 2;
    private double gaussianSigma = 3;
    private AutoThresholdSegmenter autoThresholdSegmenter;

    /**
     * @param declaration the declaration
     */
    public BrightSpotsSegmenter(ACAQAlgorithmDeclaration declaration) {
        super(declaration);
        this.autoThresholdSegmenter = new AutoThresholdSegmenter(new ACAQEmptyAlgorithmDeclaration());
    }

    /**
     * Copies the algorithm
     *
     * @param other the original
     */
    public BrightSpotsSegmenter(BrightSpotsSegmenter other) {
        super(other);
        this.rollingBallRadius = other.rollingBallRadius;
        this.dilationErodeSteps = other.dilationErodeSteps;
        this.gaussianSigma = other.gaussianSigma;
        this.autoThresholdSegmenter = new AutoThresholdSegmenter(other.autoThresholdSegmenter);
    }

    @Override
    protected void runIteration(ACAQDataInterface dataInterface, ACAQRunnerSubStatus subProgress, Consumer<ACAQRunnerSubStatus> algorithmProgress, Supplier<Boolean> isCancelled) {
        ImagePlus img = dataInterface.getInputData(getFirstInputSlot(), ImagePlus2DGreyscaleData.class).getImage();

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
        autoThresholdSegmenter.clearSlotData();
        autoThresholdSegmenter.getFirstOutputSlot().addData(new ImagePlus2DGreyscaleData(result));
        autoThresholdSegmenter.run(subProgress.resolve("Auto-thresholding"), algorithmProgress, isCancelled);
        result = autoThresholdSegmenter.getFirstOutputSlot().getData(0, ImagePlusData.class).getImage();

        // Apply morphologial operations
        Binary binaryFilter = new Binary();

        binaryFilter.setup("dilate", null);
        for (int i = 0; i < dilationErodeSteps; ++i) {
            binaryFilter.run(result.getProcessor());
        }

        binaryFilter.setup("fill holes", null);
        binaryFilter.run(result.getProcessor());

        binaryFilter.setup("erode", null);
        for (int i = 0; i < dilationErodeSteps; ++i) {
            binaryFilter.run(result.getProcessor());
        }

        // Smooth the spots and re-threshold them
        if (gaussianSigma > 0) {
            GaussianBlur gaussianBlur = new GaussianBlur();
            gaussianBlur.blurGaussian(result.getProcessor(), gaussianSigma);

            autoThresholdSegmenter.clearSlotData();
            autoThresholdSegmenter.getFirstInputSlot().addData(new ImagePlus2DGreyscaleData(result));
            autoThresholdSegmenter.run(subProgress.resolve("Auto-thresholding (2)"), algorithmProgress, isCancelled);
            result = autoThresholdSegmenter.getFirstOutputSlot().getData(0, ImagePlusData.class).getImage();
        }

        dataInterface.addOutputData(getFirstOutputSlot(), new ImagePlus2DGreyscaleMaskData(result));
    }

    @ACAQParameter("rolling-ball-radius")
    @ACAQDocumentation(name = "Rolling ball radius")
    public int getRollingBallRadius() {
        return rollingBallRadius;
    }

    @ACAQParameter("rolling-ball-radius")
    public void setRollingBallRadius(int rollingBallRadius) {
        this.rollingBallRadius = rollingBallRadius;
        getEventBus().post(new ParameterChangedEvent(this, "rolling-ball-radius"));
    }

    @ACAQParameter("dilation-erode-steps")
    @ACAQDocumentation(name = "Dilation erode steps")
    public int getDilationErodeSteps() {
        return dilationErodeSteps;
    }

    @ACAQParameter("dilation-erode-steps")
    public void setDilationErodeSteps(int dilationErodeSteps) {
        this.dilationErodeSteps = dilationErodeSteps;
        getEventBus().post(new ParameterChangedEvent(this, "dilation-erode-steps"));
    }

    @ACAQParameter("gaussian-sigma")
    @ACAQDocumentation(name = "Gaussian sigma")
    public double getGaussianSigma() {
        return gaussianSigma;
    }

    @ACAQParameter("gaussian-sigma")
    public void setGaussianSigma(double gaussianSigma) {
        this.gaussianSigma = gaussianSigma;
        getEventBus().post(new ParameterChangedEvent(this, "gaussian-sigma"));
    }

    @ACAQSubParameters("auto-thresholding")
    @ACAQDocumentation(name = "Auto thresholding", description = "Parameters for underlying auto thresholding")
    public AutoThresholdSegmenter getAutoThresholdSegmenter() {
        return autoThresholdSegmenter;
    }

    @Override
    public void reportValidity(ACAQValidityReport report) {

    }
}
