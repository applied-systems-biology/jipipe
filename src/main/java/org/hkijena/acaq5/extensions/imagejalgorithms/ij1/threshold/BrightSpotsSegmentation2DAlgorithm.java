/*
 * Copyright by Zoltán Cseresnyés, Ruman Gerst
 *
 * Research Group Applied Systems Biology - Head: Prof. Dr. Marc Thilo Figge
 * https://www.leibniz-hki.de/en/applied-systems-biology.html
 * HKI-Center for Systems Biology of Infection
 * Leibniz Institute for Natural Product Research and Infection Biology - Hans Knöll Institute (HKI)
 * Adolf-Reichwein-Straße 23, 07745 Jena, Germany
 *
 * The project code is licensed under BSD 2-Clause.
 * See the LICENSE file provided with the code for the full license.
 */

package org.hkijena.acaq5.extensions.imagejalgorithms.ij1.threshold;

import ij.ImagePlus;
import ij.ImageStack;
import ij.plugin.filter.BackgroundSubtracter;
import ij.plugin.filter.Binary;
import ij.plugin.filter.GaussianBlur;
import org.hkijena.acaq5.api.ACAQDocumentation;
import org.hkijena.acaq5.api.ACAQOrganization;
import org.hkijena.acaq5.api.ACAQRunnerSubStatus;
import org.hkijena.acaq5.api.ACAQValidityReport;
import org.hkijena.acaq5.api.algorithm.*;
import org.hkijena.acaq5.api.data.ACAQDefaultMutableSlotConfiguration;
import org.hkijena.acaq5.api.events.ParameterChangedEvent;
import org.hkijena.acaq5.api.parameters.ACAQParameter;
import org.hkijena.acaq5.api.registries.ACAQAlgorithmRegistry;
import org.hkijena.acaq5.extensions.imagejdatatypes.datatypes.ImagePlusData;
import org.hkijena.acaq5.extensions.imagejdatatypes.datatypes.d2.greyscale.ImagePlus2DGreyscaleData;
import org.hkijena.acaq5.extensions.imagejdatatypes.datatypes.greyscale.ImagePlusGreyscale8UData;
import org.hkijena.acaq5.extensions.imagejdatatypes.datatypes.greyscale.ImagePlusGreyscaleData;
import org.hkijena.acaq5.extensions.imagejdatatypes.datatypes.greyscale.ImagePlusGreyscaleMaskData;
import org.hkijena.acaq5.utils.ImageJUtils;

import java.util.function.Consumer;
import java.util.function.Supplier;

import static org.hkijena.acaq5.extensions.imagejalgorithms.ImageJAlgorithmsExtension.ADD_MASK_QUALIFIER;

/**
 * Applies Bright spots segmentation
 */
@ACAQDocumentation(name = "Bright spots segmentation 2D", description = "Applies thresholding by applying a background subtraction, auto thresholding, and " +
        "various morphological operations. If higher-dimensional data is provided, the filter is applied to each 2D slice.")
@ACAQOrganization(menuPath = "Threshold", algorithmCategory = ACAQAlgorithmCategory.Processor)

// Algorithm flow
@AlgorithmInputSlot(value = ImagePlusGreyscaleData.class, slotName = "Input")
@AlgorithmOutputSlot(value = ImagePlusGreyscaleMaskData.class, slotName = "Output")

// Trait matching

// Trait configuration
public class BrightSpotsSegmentation2DAlgorithm extends ACAQSimpleIteratingAlgorithm {

    private int rollingBallRadius = 20;
    private int dilationErodeSteps = 2;
    private double gaussianSigma = 3;
    private AutoThreshold2DAlgorithm autoThresholding;

    /**
     * @param declaration the declaration
     */
    public BrightSpotsSegmentation2DAlgorithm(ACAQAlgorithmDeclaration declaration) {
        super(declaration, ACAQDefaultMutableSlotConfiguration.builder().addInputSlot("Input", ImagePlusGreyscale8UData.class)
                .addOutputSlot("Output", ImagePlusGreyscaleMaskData.class, "Input", ADD_MASK_QUALIFIER)
                .allowOutputSlotInheritance(true)
                .seal()
                .build());
        this.autoThresholding = (AutoThreshold2DAlgorithm) ACAQAlgorithmRegistry.getInstance().getDeclarationById("ij1-threshold-auto2d").newInstance();
    }

    /**
     * Copies the algorithm
     *
     * @param other the original
     */
    public BrightSpotsSegmentation2DAlgorithm(BrightSpotsSegmentation2DAlgorithm other) {
        super(other);
        this.rollingBallRadius = other.rollingBallRadius;
        this.dilationErodeSteps = other.dilationErodeSteps;
        this.gaussianSigma = other.gaussianSigma;
        this.autoThresholding = (AutoThreshold2DAlgorithm) other.autoThresholding.getDeclaration().clone(other.autoThresholding);
    }

    @Override
    public boolean supportsParallelization() {
        return true;
    }

    @Override
    protected void runIteration(ACAQDataInterface dataInterface, ACAQRunnerSubStatus subProgress, Consumer<ACAQRunnerSubStatus> algorithmProgress, Supplier<Boolean> isCancelled) {
        ImagePlus img = dataInterface.getInputData(getFirstInputSlot(), ImagePlusGreyscaleData.class).getImage();
        ImageStack stack = new ImageStack(img.getWidth(), img.getHeight(), img.getProcessor().getColorModel());
        AutoThreshold2DAlgorithm autoThresholdingCopy = new AutoThreshold2DAlgorithm(autoThresholding);

        ImageJUtils.forEachIndexedSlice(img, (imp, index) -> {
            ImagePlus slice = new ImagePlus("slice", imp);
            ImagePlus processedSlice = slice.duplicate();

            // Apply background subtraction
            BackgroundSubtracter backgroundSubtracter = new BackgroundSubtracter();
            backgroundSubtracter.rollingBallBackground(processedSlice.getProcessor(),
                    rollingBallRadius,
                    false,
                    false,
                    false,
                    true,
                    true);

            // Apply auto threshold
            autoThresholdingCopy.clearSlotData();
            autoThresholdingCopy.getFirstOutputSlot().addData(new ImagePlus2DGreyscaleData(processedSlice));
            autoThresholdingCopy.run(subProgress.resolve("Auto-thresholding"), algorithmProgress, isCancelled);
            processedSlice = autoThresholdingCopy.getFirstOutputSlot().getData(0, ImagePlusData.class).getImage();

            // Apply morphologial operations
            Binary binaryFilter = new Binary();

            binaryFilter.setup("dilate", null);
            for (int i = 0; i < dilationErodeSteps; ++i) {
                binaryFilter.run(processedSlice.getProcessor());
            }

            binaryFilter.setup("fill holes", null);
            binaryFilter.run(processedSlice.getProcessor());

            binaryFilter.setup("erode", null);
            for (int i = 0; i < dilationErodeSteps; ++i) {
                binaryFilter.run(processedSlice.getProcessor());
            }

            // Smooth the spots and re-threshold them
            if (gaussianSigma > 0) {
                GaussianBlur gaussianBlur = new GaussianBlur();
                gaussianBlur.blurGaussian(processedSlice.getProcessor(), gaussianSigma);

                autoThresholdingCopy.clearSlotData();
                autoThresholdingCopy.getFirstInputSlot().addData(new ImagePlusGreyscaleData(processedSlice));
                autoThresholdingCopy.run(subProgress.resolve("Auto-thresholding (2)"), algorithmProgress, isCancelled);
                processedSlice = autoThresholdingCopy.getFirstOutputSlot().getData(0, ImagePlusData.class).getImage();
            }
            stack.addSlice("slice" + index, processedSlice.getProcessor());
        });
        ImagePlus result = new ImagePlus("Segmented Image", stack);
        result.setDimensions(img.getNChannels(), img.getNSlices(), img.getNFrames());

        dataInterface.addOutputData(getFirstOutputSlot(), new ImagePlusGreyscaleMaskData(result));
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

    @ACAQParameter("auto-thresholding")
    @ACAQDocumentation(name = "Auto thresholding", description = "Parameters for underlying auto thresholding")
    public AutoThreshold2DAlgorithm getAutoThresholding() {
        return autoThresholding;
    }

    @Override
    public void reportValidity(ACAQValidityReport report) {

    }
}
