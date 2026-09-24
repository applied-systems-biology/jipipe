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
import org.hkijena.jipipe.api.AddJIPipeCitation;
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


/**
 * Segmenter node that thresholds via an auto threshold
 * Based on code from {@link fiji.threshold.Auto_Local_Threshold}
 */
@SetJIPipeDocumentation(name = "Local auto threshold 2D (Bernsen, 16-bit)", description = "Applies a local auto-thresholding algorithm. " +
        "If higher-dimensional data is provided, the filter is applied to each 2D slice.\n\n" +
        "Bernsen recommends a radius of 15 and a contrast threshold of 15.")
@ConfigureJIPipeNode(menuPath = "Threshold\nLocal", nodeTypeCategory = ImagesNodeTypeCategory.class)
@AddJIPipeInputSlot(value = ImagePlusGreyscale16UData.class, name = "Input", create = true)
@AddJIPipeOutputSlot(value = ImagePlusGreyscaleMaskData.class, name = "Output", create = true)
@AddJIPipeCitation("Bernsen J. (1986) \"Dynamic Thresholding of Grey-Level Images\" Proc. of the 8th Int. Conf. on Pattern Recognition, pp. 1251-1255")
@AddJIPipeCitation("Sezgin M. and Sankur B. (2004) \"Survey over Image Thresholding Techniques and Quantitative Performance Evaluation\" Journal of Electronic Imaging, 13(1): 146-165")
public class BernsenLocalAutoThreshold2D16UAlgorithm extends JIPipeSimpleIteratingAlgorithm {

    private boolean darkBackground = true;
    private int contrastThreshold = 15;
    private int radius = 15;

    /**
     * @param info the info
     */
    public BernsenLocalAutoThreshold2D16UAlgorithm(JIPipeNodeInfo info) {
        super(info);
    }

    /**
     * Copies the algorithm
     *
     * @param other the original
     */
    public BernsenLocalAutoThreshold2D16UAlgorithm(BernsenLocalAutoThreshold2D16UAlgorithm other) {
        super(other);
        this.darkBackground = other.darkBackground;
        this.contrastThreshold = other.contrastThreshold;
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
            LocalAutoThreshold16UUtils.Bernsen(processor, radius, contrastThreshold, true, out);
            ByteProcessor target = (ByteProcessor) (outputImage.hasImageStack()
                    ? outputImage.getStack().getProcessor(outputImage.getStackIndex(index.getC() + 1, index.getZ() + 1, index.getT() + 1))
                    : outputImage.getProcessor());
            target.copyBits(out, 0, 0, Blitter.COPY);
        }, progressInfo);
        iterationStep.addOutputData(getFirstOutputSlot(), new ImagePlusGreyscaleMaskData(outputImage), progressInfo);
    }

    @SetJIPipeDocumentation(name = "Contrast threshold", description = "The contrast threshold. Bernsen recommends a value of 15.")
    @JIPipeParameter("contrast-threshold")
    public int getContrastThreshold() {
        return contrastThreshold;
    }

    @JIPipeParameter("contrast-threshold")
    public void setContrastThreshold(int contrastThreshold) {
        this.contrastThreshold = contrastThreshold;
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

    @SetJIPipeDocumentation(name = "Dark background", description = "If the background color is dark. Disable this if your image has a bright background.")
    @JIPipeParameter("dark-background")
    public boolean isDarkBackground() {
        return darkBackground;
    }

    @JIPipeParameter("dark-background")
    public void setDarkBackground(boolean darkBackground) {
        this.darkBackground = darkBackground;
    }
}
