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

package org.hkijena.jipipe.plugins.imagejalgorithms.nodes.threshold.local;

import ij.IJ;
import ij.ImagePlus;
import ij.process.Blitter;
import ij.process.ByteProcessor;
import org.hkijena.jipipe.api.ConfigureJIPipeNode;
import org.hkijena.jipipe.api.JIPipeProgressInfo;
import org.hkijena.jipipe.api.SetJIPipeDocumentation;
import org.hkijena.jipipe.api.nodes.*;
import org.hkijena.jipipe.api.nodes.algorithm.JIPipeSimpleIteratingAlgorithm;
import org.hkijena.jipipe.api.nodes.categories.ImagesNodeTypeCategory;
import org.hkijena.jipipe.api.nodes.iterationstep.JIPipeIterationContext;
import org.hkijena.jipipe.api.nodes.iterationstep.JIPipeSingleIterationStep;
import org.hkijena.jipipe.api.parameters.JIPipeParameter;
import org.hkijena.jipipe.plugins.imagejdatatypes.datatypes.ImagePlusData;
import org.hkijena.jipipe.plugins.imagejdatatypes.datatypes.greyscale.ImagePlusGreyscale16UData;
import org.hkijena.jipipe.plugins.imagejdatatypes.datatypes.greyscale.ImagePlusGreyscaleMaskData;
import org.hkijena.jipipe.plugins.imagejdatatypes.util.ImageJIterationUtils;
import org.scijava.Priority;


/**
 * Segmenter node that thresholds via an auto threshold
 * Based on code from {@link fiji.threshold.Auto_Local_Threshold}
 */
@SetJIPipeDocumentation(name = "Local auto threshold 2D (Mean/Median/MidGrey/Otsu, 16-bit)", description = "Applies a local auto-thresholding algorithm.\n\n" +
        "This node supports various methods:\n\nMean (threshold is mean local pixel value)\n" +
        "Median (threshold is median local pixel value)\n" +
        "MidGray (threshold is average of min and max pixel values)\n" +
        "Otsu (threshold is the local otsu threshold)\n\n" +
        "If higher-dimensional data is provided, the filter is applied to each 2D slice.")
@ConfigureJIPipeNode(menuPath = "Threshold\nLocal", nodeTypeCategory = ImagesNodeTypeCategory.class)
@AddJIPipeInputSlot(value = ImagePlusGreyscale16UData.class, name = "Input", create = true)
@AddJIPipeOutputSlot(value = ImagePlusGreyscaleMaskData.class, name = "Output", create = true)
public class LocalAutoThreshold2D16UAlgorithm extends JIPipeSimpleIteratingAlgorithm {

    private Method method = Method.Otsu;
    private boolean darkBackground = true;
    private int radius = 15;
    private int modifier = 0;

    /**
     * @param info the info
     */
    public LocalAutoThreshold2D16UAlgorithm(JIPipeNodeInfo info) {
        super(info);
    }

    /**
     * Copies the algorithm
     *
     * @param other the original
     */
    public LocalAutoThreshold2D16UAlgorithm(LocalAutoThreshold2D16UAlgorithm other) {
        super(other);
        this.method = other.method;
        this.darkBackground = other.darkBackground;
        this.modifier = other.modifier;
        this.radius = other.radius;
    }

    @Override
    public boolean supportsParallelization() {
        return true;
    }

    @Override
    protected void runIteration(JIPipeSingleIterationStep iterationStep, JIPipeIterationContext iterationContext, JIPipeGraphNodeRunContext runContext, JIPipeProgressInfo progressInfo) {
        ImagePlusData inputData = iterationStep.getInputData(getFirstInputSlot(), ImagePlusGreyscale16UData.class, progressInfo);
        ImagePlus img = inputData.getDuplicateImage();
        ImagePlus outputImage = IJ.createHyperStack(img.getTitle() + " Thresholded",
                img.getWidth(), img.getHeight(), img.getNChannels(), img.getNSlices(), img.getNFrames(), 8);
        ImageJIterationUtils.forEachIndexedZCTSlice(img, (processor, index) -> {
            if (!darkBackground) {
                LocalAutoThreshold16UUtils.invertFullRange(processor);
            }
            ByteProcessor out = new ByteProcessor(processor.getWidth(), processor.getHeight());
            switch (method) {
                case Mean:
                    LocalAutoThreshold16UUtils.Mean(processor, radius, modifier, true, out);
                    break;
                case Otsu:
                    LocalAutoThreshold16UUtils.Otsu(processor, radius, modifier, true, out);
                    break;
                case Median:
                    LocalAutoThreshold16UUtils.Median(processor, radius, modifier, true, out);
                    break;
                case MidGrey:
                    LocalAutoThreshold16UUtils.MidGrey(processor, radius, modifier, true, out);
                    break;
            }
            ByteProcessor target = (ByteProcessor) (outputImage.hasImageStack()
                    ? outputImage.getStack().getProcessor(outputImage.getStackIndex(index.getC() + 1, index.getZ() + 1, index.getT() + 1))
                    : outputImage.getProcessor());
            target.copyBits(out, 0, 0, Blitter.COPY);
        }, progressInfo);

        iterationStep.addOutputData(getFirstOutputSlot(), new ImagePlusGreyscaleMaskData(outputImage), progressInfo);
    }

    @JIPipeParameter(value = "method", priority = Priority.HIGH)
    @SetJIPipeDocumentation(name = "Method", description = "Determines the thresholding method:\n\n" +
            "Mean (threshold is mean local pixel value)\n" +
            "Median (threshold is median local pixel value)\n" +
            "MidGray (threshold is average of min and max pixel values)\n" +
            "Otsu (threshold is the local otsu threshold)")
    public Method getMethod() {
        return method;
    }

    @JIPipeParameter("method")
    public void setMethod(Method method) {
        this.method = method;
    }

    @SetJIPipeDocumentation(name = "Dark background", description = "If the background color is dark. Disable this if your image has a bright background.")
    @JIPipeParameter("dark-background")
    public boolean isDarkBackground() {
        return darkBackground;
    }

    @JIPipeParameter("dark-background")
    public void setDarkBackground(boolean darkBackground) {
        this.darkBackground = darkBackground;
    }

    @SetJIPipeDocumentation(name = "Modifier", description = "This value is subtracted from each calculated local threshold.")
    @JIPipeParameter("modifier")
    public int getModifier() {
        return modifier;
    }

    @JIPipeParameter("modifier")
    public void setModifier(int modifier) {
        this.modifier = modifier;
    }

    @SetJIPipeDocumentation(name = "Radius", description = "The radius of the circular local window.")
    @JIPipeParameter("radius")
    public int getRadius() {
        return radius;
    }

    @JIPipeParameter("radius")
    public boolean setRadius(int radius) {
        if (radius <= 0)
            return false;
        this.radius = radius;
        return true;
    }

    public enum Method {
        Mean,
        Median,
        MidGrey,
        Otsu
    }
}
