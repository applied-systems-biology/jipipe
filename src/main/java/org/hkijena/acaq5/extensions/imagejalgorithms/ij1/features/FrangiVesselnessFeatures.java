package org.hkijena.acaq5.extensions.imagejalgorithms.ij1.features;

import fiji.features.Frangi_;
import fiji.features.MultiTaskProgress;
import ij.ImagePlus;
import ij.ImageStack;
import ij.process.ImageProcessor;
import mpicbg.imglib.type.numeric.real.FloatType;
import org.hkijena.acaq5.api.ACAQDocumentation;
import org.hkijena.acaq5.api.ACAQOrganization;
import org.hkijena.acaq5.api.ACAQRunnerSubStatus;
import org.hkijena.acaq5.api.ACAQValidityReport;
import org.hkijena.acaq5.api.algorithm.ACAQAlgorithmCategory;
import org.hkijena.acaq5.api.algorithm.ACAQAlgorithmDeclaration;
import org.hkijena.acaq5.api.algorithm.ACAQDataInterface;
import org.hkijena.acaq5.api.algorithm.AlgorithmInputSlot;
import org.hkijena.acaq5.api.algorithm.AlgorithmOutputSlot;
import org.hkijena.acaq5.api.data.ACAQMutableSlotConfiguration;
import org.hkijena.acaq5.api.events.ParameterChangedEvent;
import org.hkijena.acaq5.api.parameters.ACAQParameter;
import org.hkijena.acaq5.extensions.imagejalgorithms.ij1.ImageJ1Algorithm;
import org.hkijena.acaq5.extensions.imagejdatatypes.datatypes.ImagePlusData;
import org.hkijena.acaq5.extensions.imagejdatatypes.datatypes.greyscale.ImagePlusGreyscale32FData;
import org.hkijena.acaq5.utils.ImageJUtils;

import java.util.function.Consumer;
import java.util.function.Supplier;

import static org.hkijena.acaq5.extensions.imagejalgorithms.ImageJAlgorithmsExtension.REMOVE_MASK_QUALIFIER;

/**
 * Applies CLAHE image enhancing
 */
@ACAQDocumentation(name = "Frangi vesselness", description = "Applies the vesselness filter developed by Frangi et al.")
@ACAQOrganization(algorithmCategory = ACAQAlgorithmCategory.Processor, menuPath = "Features")

// Algorithm flow
@AlgorithmInputSlot(value = ImagePlusGreyscale32FData.class, slotName = "Input")
@AlgorithmOutputSlot(value = ImagePlusGreyscale32FData.class, slotName = "Output")
public class FrangiVesselnessFeatures extends ImageJ1Algorithm {

    private int numScales = 1;
    private double minimumScale = 3.0;
    private double maximumScale = 3.0;
    private boolean invert = false;
    private SlicingMode slicingMode = SlicingMode.Unchanged;

    /**
     * @param declaration the declaration
     */
    public FrangiVesselnessFeatures(ACAQAlgorithmDeclaration declaration) {
        super(declaration, ACAQMutableSlotConfiguration.builder().addInputSlot("Input", ImagePlusGreyscale32FData.class)
                .addOutputSlot("Output", ImagePlusGreyscale32FData.class, "Input", REMOVE_MASK_QUALIFIER)
                .allowOutputSlotInheritance(true)
                .seal()
                .build());
    }

    /**
     * Copies the algorithm
     *
     * @param other the original
     */
    public FrangiVesselnessFeatures(FrangiVesselnessFeatures other) {
        super(other);
        this.numScales = other.numScales;
        this.minimumScale = other.minimumScale;
        this.maximumScale = other.maximumScale;
        this.invert = other.invert;
        this.slicingMode = other.slicingMode;
    }

    @Override
    protected void runIteration(ACAQDataInterface dataInterface, ACAQRunnerSubStatus subProgress, Consumer<ACAQRunnerSubStatus> algorithmProgress, Supplier<Boolean> isCancelled) {
        ImagePlusGreyscale32FData inputData = dataInterface.getInputData(getFirstInputSlot(), ImagePlusGreyscale32FData.class);
        ImagePlus img = inputData.getImage();

        if (invert) {
            img = img.duplicate();
            ImageJUtils.forEachSlice(img, ImageProcessor::invert);
        }

        Frangi_<FloatType> frangi = new Frangi_<>();
        MultiTaskProgress multiTaskProgress = new MultiTaskProgress() {
            @Override
            public void updateProgress(double proportionDone, int taskIndex) {
                algorithmProgress.accept(subProgress.resolve("Frangi vesselness " + (int) (proportionDone * 100) + "%"));
            }

            @Override
            public void done() {

            }
        };
        ImagePlus result;

        if (slicingMode == SlicingMode.Unchanged) {
            result = frangi.process(img,
                    numScales,
                    minimumScale,
                    maximumScale,
                    false,
                    false,
                    false,
                    multiTaskProgress);
        } else if (slicingMode == SlicingMode.ApplyPer2DSlice) {
            ImageStack stack = new ImageStack(img.getWidth(), img.getHeight(), img.getProcessor().getColorModel());
            ImageJUtils.forEachIndexedSlice(img, (imp, index) -> {
                ImagePlus slice = new ImagePlus("slice", imp);
                ImagePlus processedSlice = frangi.process(slice,
                        numScales,
                        minimumScale,
                        maximumScale,
                        false,
                        false,
                        false,
                        multiTaskProgress);
                stack.addSlice("slice" + index, processedSlice.getProcessor());
            });
            result = new ImagePlus("Vesselness", stack);
            result.setDimensions(img.getNChannels(), img.getNSlices(), img.getNFrames());
        } else {
            throw new UnsupportedOperationException("Not implemented: " + slicingMode);
        }

        dataInterface.addOutputData(getFirstOutputSlot(), new ImagePlusData(result));
    }

    @ACAQParameter("num-scales")
    @ACAQDocumentation(name = "Scales", description = "How many intermediate steps between minimum and maximum scales should be applied.")
    public int getNumScales() {
        return numScales;
    }

    @ACAQParameter("num-scales")
    public void setNumScales(int numScales) {
        this.numScales = numScales;
        getEventBus().post(new ParameterChangedEvent(this, "num-scales"));
    }

    @ACAQParameter("min-scale")
    @ACAQDocumentation(name = "Minimum scale", description = "The minimum scale that is applied")
    public double getMinimumScale() {
        return minimumScale;
    }

    @ACAQParameter("min-scale")
    public void setMinimumScale(double minimumScale) {
        this.minimumScale = minimumScale;
        getEventBus().post(new ParameterChangedEvent(this, "min-scale"));
    }

    @ACAQParameter("max-scale")
    @ACAQDocumentation(name = "Maximum scale", description = "The maximum scale that is applied")
    public double getMaximumScale() {
        return maximumScale;
    }

    @ACAQParameter("max-scale")
    public void setMaximumScale(double maximumScale) {
        this.maximumScale = maximumScale;
        getEventBus().post(new ParameterChangedEvent(this, "max-scale"));
    }

    @Override
    public void reportValidity(ACAQValidityReport report) {
        report.checkIfWithin(this, minimumScale, 0, Double.POSITIVE_INFINITY, false, true);
        report.checkIfWithin(this, maximumScale, 0, Double.POSITIVE_INFINITY, false, true);
    }

    @ACAQDocumentation(name = "Invert colors", description = "Invert colors before applying the filter. This is useful if you look for bright structures within a dark background.")
    @ACAQParameter("invert")
    public boolean isInvert() {
        return invert;
    }

    @ACAQParameter("invert")
    public void setInvert(boolean invert) {
        this.invert = invert;
    }

    @ACAQDocumentation(name = "Apply per slice", description = "Applies the vesselness filter for each 2D slice instead for the whole multi-dimensional image.")
    @ACAQParameter("per-slice")
    public SlicingMode getSlicingMode() {
        return slicingMode;
    }

    @ACAQParameter("per-slice")
    public void setSlicingMode(SlicingMode slicingMode) {
        this.slicingMode = slicingMode;
    }

    /**
     * Available ways how to handle higher-dimensional images
     */
    public enum SlicingMode {
        Unchanged,
        ApplyPer2DSlice
    }
}