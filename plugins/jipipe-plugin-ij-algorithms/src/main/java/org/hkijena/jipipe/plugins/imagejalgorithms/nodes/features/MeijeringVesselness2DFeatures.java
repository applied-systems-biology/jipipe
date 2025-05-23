/*
 * Copyright by Zoltán Cseresnyés, Ruman Gerst
 *
 * Research Group Applied Systems Biology - Head: Prof. Dr. Marc Thilo Figge
 * https://www.leibniz-hki.de/en/applied-systems-biology.html
 * HKI-Center for Systems Biology of Infection
 * Leibniz Institute for Natural Product Research and Infection Biology - Hans Knöll Institute (HKI)
 * Adolf-Reichwein-Straße 23, 07745 Jena, Germany
 *
 * The project code is licensed under MIT.
 * See the LICENSE file provided with the code for the full license.
 */

package org.hkijena.jipipe.plugins.imagejalgorithms.nodes.features;

import ij.ImagePlus;
import ij.ImageStack;
import ij.plugin.ZProjector;
import ij.process.FloatProcessor;
import ij.process.ImageProcessor;
import imagescience.feature.Hessian;
import imagescience.image.Aspects;
import imagescience.image.FloatImage;
import imagescience.image.Image;
import org.hkijena.jipipe.api.ConfigureJIPipeNode;
import org.hkijena.jipipe.api.JIPipeProgressInfo;
import org.hkijena.jipipe.api.SetJIPipeDocumentation;
import org.hkijena.jipipe.api.nodes.AddJIPipeInputSlot;
import org.hkijena.jipipe.api.nodes.AddJIPipeOutputSlot;
import org.hkijena.jipipe.api.nodes.JIPipeGraphNodeRunContext;
import org.hkijena.jipipe.api.nodes.JIPipeNodeInfo;
import org.hkijena.jipipe.api.nodes.algorithm.JIPipeSimpleIteratingAlgorithm;
import org.hkijena.jipipe.api.nodes.categories.ImagesNodeTypeCategory;
import org.hkijena.jipipe.api.nodes.iterationstep.JIPipeIterationContext;
import org.hkijena.jipipe.api.nodes.iterationstep.JIPipeSingleIterationStep;
import org.hkijena.jipipe.api.parameters.JIPipeParameter;
import org.hkijena.jipipe.api.validation.JIPipeValidationReport;
import org.hkijena.jipipe.api.validation.JIPipeValidationReportContext;
import org.hkijena.jipipe.api.validation.JIPipeValidationReportEntry;
import org.hkijena.jipipe.api.validation.JIPipeValidationReportEntryLevel;
import org.hkijena.jipipe.api.validation.contexts.ParameterValidationReportContext;
import org.hkijena.jipipe.plugins.imagejdatatypes.datatypes.ImagePlusData;
import org.hkijena.jipipe.plugins.imagejdatatypes.datatypes.greyscale.ImagePlusGreyscale32FData;
import org.hkijena.jipipe.plugins.imagejdatatypes.util.ImageJIterationUtils;
import org.hkijena.jipipe.plugins.imagejdatatypes.util.ImageJUtils;
import org.hkijena.jipipe.plugins.parameters.library.primitives.NumberParameterSettings;
import org.hkijena.jipipe.plugins.parameters.library.primitives.list.DoubleList;

import java.util.Vector;


/**
 * Applies CLAHE image enhancing
 */
@SetJIPipeDocumentation(name = "Meijering vesselness 2D", description = "Applies the vesselness filter developed by Meijering et al. " +
        "This filter only implements the first algorithm part that responds to neurite-like features, similar to the Frangi vesselness filter. " +
        "If higher-dimensional data is provided, the filter is applied to each 2D slice.")
@ConfigureJIPipeNode(nodeTypeCategory = ImagesNodeTypeCategory.class, menuPath = "Features")


@AddJIPipeInputSlot(value = ImagePlusGreyscale32FData.class, name = "Input", create = true)
@AddJIPipeOutputSlot(value = ImagePlusGreyscale32FData.class, name = "Output", create = true)
public class MeijeringVesselness2DFeatures extends JIPipeSimpleIteratingAlgorithm {

    private DoubleList scales = new DoubleList();
    private boolean invert = false;
    private double alpha = 0.5;


    /**
     * @param info the info
     */
    public MeijeringVesselness2DFeatures(JIPipeNodeInfo info) {
        super(info);

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
    protected void runIteration(JIPipeSingleIterationStep iterationStep, JIPipeIterationContext iterationContext, JIPipeGraphNodeRunContext runContext, JIPipeProgressInfo progressInfo) {
        ImagePlusGreyscale32FData inputData = iterationStep.getInputData(getFirstInputSlot(), ImagePlusGreyscale32FData.class, progressInfo);
        ImagePlus img = inputData.getImage();

        if (invert) {
            img = ImageJUtils.duplicate(img);
            ImageJIterationUtils.forEachSlice(img, ImageProcessor::invert, progressInfo);
        }

        ImageStack stack = new ImageStack(img.getWidth(), img.getHeight(), img.getProcessor().getColorModel());
        ImagePlus finalImg = img;
        ImageJIterationUtils.forEachIndexedSlice(img, (imp, index) -> {
            progressInfo.log("Slice " + index + "/" + finalImg.getStackSize());
            ImagePlus slice = new ImagePlus("slice", imp);
            ImagePlus processedSlice = processSlice(slice);
            stack.addSlice("slice" + index, processedSlice.getProcessor());
        }, progressInfo);
        ImagePlus result = new ImagePlus("Vesselness", stack);
        result.setDimensions(img.getNChannels(), img.getNSlices(), img.getNFrames());
        result.copyScale(img);
        iterationStep.addOutputData(getFirstOutputSlot(), new ImagePlusData(result), progressInfo);
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
    public void reportValidity(JIPipeValidationReportContext reportContext, JIPipeValidationReport report) {
        if (scales.isEmpty()) {
            report.add(new JIPipeValidationReportEntry(JIPipeValidationReportEntryLevel.Error,
                    new ParameterValidationReportContext(reportContext, this, "Scales", "scales"),
                    "No scales provided!",
                    "You have to provide a list of scales to test",
                    "Please add at least one entry."));
        }
    }

    @SetJIPipeDocumentation(name = "Invert colors", description = "Invert colors before applying the filter. This is useful if you look for bright structures within a dark background.")
    @JIPipeParameter("invert")
    public boolean isInvert() {
        return invert;
    }

    @JIPipeParameter("invert")
    public void setInvert(boolean invert) {
        this.invert = invert;

    }

    @NumberParameterSettings(step = 0.1)
    @SetJIPipeDocumentation(name = "Correction constant", description = "Adjusts the sensitivity to deviation from a plate-like structure.")
    @JIPipeParameter("alpha")
    public double getAlpha() {
        return alpha;
    }

    @JIPipeParameter("alpha")
    public void setAlpha(double alpha) {
        this.alpha = alpha;

    }

    @SetJIPipeDocumentation(name = "Scales", description = "List of scales to test. They are also referenced as 'Sigmas'.")
    @JIPipeParameter("scales")
    public DoubleList getScales() {
        return scales;
    }

    @JIPipeParameter("scales")
    public void setScales(DoubleList scales) {
        this.scales = scales;
    }
}