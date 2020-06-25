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

package org.hkijena.acaq5.extensions.imagejalgorithms.ij1.features;

import ij.ImagePlus;
import ij.ImageStack;
import ij.plugin.ZProjector;
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
import org.hkijena.acaq5.api.data.ACAQDefaultMutableSlotConfiguration;
import org.hkijena.acaq5.api.events.ParameterChangedEvent;
import org.hkijena.acaq5.api.parameters.ACAQParameter;
import org.hkijena.acaq5.extensions.imagejdatatypes.datatypes.ImagePlusData;
import org.hkijena.acaq5.extensions.imagejdatatypes.datatypes.greyscale.ImagePlusGreyscale32FData;
import org.hkijena.acaq5.extensions.parameters.primitives.DoubleList;
import org.hkijena.acaq5.extensions.parameters.primitives.NumberParameterSettings;
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
public class MeijeringVesselness2DFeatures extends ACAQSimpleIteratingAlgorithm {

    private DoubleList scales = new DoubleList();
    private boolean invert = false;
    private double alpha = 0.5;


    /**
     * @param declaration the declaration
     */
    public MeijeringVesselness2DFeatures(ACAQAlgorithmDeclaration declaration) {
        super(declaration, ACAQDefaultMutableSlotConfiguration.builder().addInputSlot("Input", ImagePlusGreyscale32FData.class)
                .addOutputSlot("Output", ImagePlusGreyscale32FData.class, "Input", REMOVE_MASK_QUALIFIER)
                .allowOutputSlotInheritance(true)
                .seal()
                .build());

        // Initialize with a default value
        scales.add(3.0);
    }

    /**
     * Copies the algorithm
     *
     * @param other the original
     */
    public MeijeringVesselness2DFeatures(MeijeringVesselness2DFeatures other) {
        super(other);
        this.scales = new DoubleList(other.scales);
        this.invert = other.invert;
        this.alpha = other.alpha;
    }

    @Override
    public boolean supportsParallelization() {
        return true;
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
        ImagePlus finalImg = img;
        ImageJUtils.forEachIndexedSlice(img, (imp, index) -> {
            algorithmProgress.accept(subProgress.resolve("Slice " + index + "/" + finalImg.getStackSize()));
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
        ImageStack resultStack = new ImageStack(slice.getWidth(), slice.getHeight(), slice.getProcessor().getColorModel());
        for (double sigma : scales) {
            FloatProcessor eigenValues = (FloatProcessor) getLargestEigenImage(slice, sigma).getProcessor();

            // Normalize the Eigen values.
            // To do this first add v[x,y] * alpha to the each pixel x,y
            // This was directly adapted from the Skimage implementation
            float minE = Float.POSITIVE_INFINITY;
            for (int y = 0; y < eigenValues.getHeight(); y++) {
                for (int x = 0; x < eigenValues.getWidth(); x++) {
                    float e = eigenValues.getf(x, y);
                    e += alpha * e;
                    eigenValues.setf(x, y, e);
                    minE = Math.min(e, minE);
                }
            }
            if (minE == 0) {
                minE = 1e-10f; // Set to small non-zero value
            }

            // Rescale intensity and remove background
            for (int y = 0; y < eigenValues.getHeight(); y++) {
                for (int x = 0; x < eigenValues.getWidth(); x++) {
                    float e = eigenValues.getf(x, y);
                    float filtered = e / minE;
                    if (e < 0) {
                        eigenValues.setf(x, y, filtered);
                    } else {
                        eigenValues.setf(x, y, filtered);
                    }
                }
            }

            // Add to stack
            resultStack.addSlice("sigma=" + sigma, eigenValues);
        }

        // Merge stacks into an ImagePlus and apply max projection
        ImagePlus resultStackImage = new ImagePlus("Eigenvalues", resultStack);
        return ZProjector.run(resultStackImage, "MaxIntensity");
    }

    /**
     * @param input     input image
     * @param smoothing sigma
     * @return largest eigenvalue by absolute
     */
    private ImagePlus getLargestEigenImage(ImagePlus input, double smoothing) {
        final Image image = Image.wrap(input);
        image.aspects(new Aspects());
        final Hessian hessian = new Hessian();
        final Vector<Image> eigenimages = hessian.run(new FloatImage(image), smoothing, false);
        Image largest = eigenimages.get(0);
        return largest.imageplus();
    }

    @Override
    public void reportValidity(ACAQValidityReport report) {
        if (scales.isEmpty()) {
            report.forCategory("Scales").reportIsInvalid("No scales provided!",
                    "You have to provide a list of scales to test",
                    "Please add at least one entry.",
                    this);
        }
    }

    @ACAQDocumentation(name = "Invert colors", description = "Invert colors before applying the filter. This is useful if you look for bright structures within a dark background.")
    @ACAQParameter("invert")
    public boolean isInvert() {
        return invert;
    }

    @ACAQParameter("invert")
    public void setInvert(boolean invert) {
        this.invert = invert;
        getEventBus().post(new ParameterChangedEvent(this, "invert"));
    }

    @NumberParameterSettings(step = 0.1)
    @ACAQDocumentation(name = "Correction constant", description = "Adjusts the sensitivity to deviation from a plate-like structure.")
    @ACAQParameter("alpha")
    public double getAlpha() {
        return alpha;
    }

    @ACAQParameter("alpha")
    public void setAlpha(double alpha) {
        this.alpha = alpha;
        getEventBus().post(new ParameterChangedEvent(this, "alpha"));
    }

    @ACAQDocumentation(name = "Scales", description = "List of scales to test. They are also referenced as 'Sigmas'.")
    @ACAQParameter("scales")
    public DoubleList getScales() {
        return scales;
    }

    @ACAQParameter("scales")
    public void setScales(DoubleList scales) {
        this.scales = scales;
    }
}