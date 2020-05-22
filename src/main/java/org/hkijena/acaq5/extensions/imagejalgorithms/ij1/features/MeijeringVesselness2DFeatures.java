package org.hkijena.acaq5.extensions.imagejalgorithms.ij1.features;

import ij.ImagePlus;
import ij.ImageStack;
import ij.process.FloatProcessor;
import ij.process.ImageProcessor;
import imagescience.feature.Hessian;
import imagescience.image.Aspects;
import imagescience.image.FloatImage;
import imagescience.image.Image;
import org.hkijena.acaq5.api.ACAQDocumentation;
import org.hkijena.acaq5.api.ACAQOrganization;
import org.hkijena.acaq5.api.ACAQRunnerSubStatus;
import org.hkijena.acaq5.api.ACAQValidityReport;
import org.hkijena.acaq5.api.algorithm.*;
import org.hkijena.acaq5.api.data.ACAQMutableSlotConfiguration;
import org.hkijena.acaq5.api.events.ParameterChangedEvent;
import org.hkijena.acaq5.api.parameters.ACAQParameter;
import org.hkijena.acaq5.extensions.imagejalgorithms.ij1.ImageJ1Algorithm;
import org.hkijena.acaq5.extensions.imagejdatatypes.datatypes.ImagePlusData;
import org.hkijena.acaq5.extensions.imagejdatatypes.datatypes.greyscale.ImagePlusGreyscale32FData;
import org.hkijena.acaq5.utils.ImageJUtils;

import java.util.Vector;
import java.util.function.Consumer;
import java.util.function.Supplier;

import static org.hkijena.acaq5.extensions.imagejalgorithms.ImageJAlgorithmsExtension.REMOVE_MASK_QUALIFIER;

/**
 * Applies CLAHE image enhancing
 */
@ACAQDocumentation(name = "Meijering vesselness 2D", description = "Applies the vesselness filter developed by Meijering et al. " +
        "This filter only implements the first algorithm part that responds to neurite-like features, similar to the Frangi vesselness filter. " +
        "If higher-dimensional data is provided, the filter is applied to each 2D slice.")
@ACAQOrganization(algorithmCategory = ACAQAlgorithmCategory.Processor, menuPath = "Features")

// Algorithm flow
@AlgorithmInputSlot(value = ImagePlusGreyscale32FData.class, slotName = "Input")
@AlgorithmOutputSlot(value = ImagePlusGreyscale32FData.class, slotName = "Output")
public class MeijeringVesselness2DFeatures extends ImageJ1Algorithm {

    private int numScales = 1;
    private double minimumScale = 3.0;
    private double maximumScale = 3.0;
    private boolean invert = false;
    private double alpha = 0.5;


    /**
     * @param declaration the declaration
     */
    public MeijeringVesselness2DFeatures(ACAQAlgorithmDeclaration declaration) {
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
    public MeijeringVesselness2DFeatures(MeijeringVesselness2DFeatures other) {
        super(other);
        this.numScales = other.numScales;
        this.minimumScale = other.minimumScale;
        this.maximumScale = other.maximumScale;
        this.invert = other.invert;
        this.alpha = other.alpha;
    }

    @Override
    protected void runIteration(ACAQDataInterface dataInterface, ACAQRunnerSubStatus subProgress, Consumer<ACAQRunnerSubStatus> algorithmProgress, Supplier<Boolean> isCancelled) {
        ImagePlusGreyscale32FData inputData = dataInterface.getInputData(getFirstInputSlot(), ImagePlusGreyscale32FData.class);
        ImagePlus img = inputData.getImage();

        if (invert) {
            img = img.duplicate();
            ImageJUtils.forEachSlice(img, ImageProcessor::invert);
        }

        ImageStack stack = new ImageStack(img.getWidth(), img.getHeight(), img.getProcessor().getColorModel());
        ImageJUtils.forEachIndexedSlice(img, (imp, index) -> {
            ImagePlus slice = new ImagePlus("slice", imp);
            ImagePlus processedSlice = processSlice(slice);
            stack.addSlice("slice" + index, processedSlice.getProcessor());
        });
        ImagePlus result = new ImagePlus("Vesselness", stack);
        result.setDimensions(img.getNChannels(), img.getNSlices(), img.getNFrames());

        dataInterface.addOutputData(getFirstOutputSlot(), new ImagePlusData(result));
    }

    private ImagePlus processSlice(ImagePlus slice) {
        // Implementation taken from Skimage
        for(double sigma = minimumScale; sigma <= maximumScale; sigma += (maximumScale - minimumScale) / numScales) {
            FloatProcessor eigenValues = (FloatProcessor) getLargestEigenImage(slice, sigma).getProcessor();

        }
        return null;
    }

    /**
     *
     * @param input input image
     * @param smoothing sigma
     * @return largest eigenvalue by absolute
     */
    private ImagePlus getLargestEigenImage(ImagePlus input, double smoothing) {
        final Image image = Image.wrap(input);
        image.aspects(new Aspects());
        final Hessian hessian = new Hessian();
        final Vector<Image> eigenimages = hessian.run(new FloatImage(image), smoothing, true);
        Image largest = eigenimages.get(0);
        return largest.imageplus();
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

    @ACAQDocumentation(name = "Correction constant", description = "Adjusts the sensitivity to deviation from a plate-like structure.")
    @ACAQParameter("alpha")
    public double getAlpha() {
        return alpha;
    }

    @ACAQParameter("alpha")
    public void setAlpha(double alpha) {
        this.alpha = alpha;
    }

    /**
     * Available ways how to handle higher-dimensional images
     */
    public enum SlicingMode {
        Unchanged,
        ApplyPer2DSlice
    }
}