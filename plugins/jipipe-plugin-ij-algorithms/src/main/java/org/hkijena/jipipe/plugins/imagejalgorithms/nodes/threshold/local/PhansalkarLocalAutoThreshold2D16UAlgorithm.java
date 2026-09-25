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
@SetJIPipeDocumentation(name = "Local auto threshold 2D (Phansalkar, 16-bit)", description = "Applies a local auto-thresholding algorithm. " +
        "The threshold is calculated as <code>t = mean * (1 + p * exp(-q * mean) + k * ((stdev / r) - 1))</code>.\n\n" +
        "If higher-dimensional data is provided, the filter is applied to each 2D slice.")
@ConfigureJIPipeNode(menuPath = "Threshold\nLocal", nodeTypeCategory = ImagesNodeTypeCategory.class)
@AddJIPipeInputSlot(value = ImagePlusGreyscale16UData.class, name = "Input", create = true)
@AddJIPipeOutputSlot(value = ImagePlusGreyscaleMaskData.class, name = "Output", create = true)
@AddJIPipeCitation("Phansalskar N. et al. Adaptive local thresholding for detection of nuclei in diversity stained cytology images. " +
        "International Conference on Communications and Signal Processing (ICCSP), 2011, 218 - 220.")
public class PhansalkarLocalAutoThreshold2D16UAlgorithm extends JIPipeSimpleIteratingAlgorithm {
    private boolean darkBackground = true;
    private double k = 0.25;
    private double r = 0.5;
    private double p = 2;
    private double q = 10;
    private int radius = 15;

    /**
     * @param info the info
     */
    public PhansalkarLocalAutoThreshold2D16UAlgorithm(JIPipeNodeInfo info) {
        super(info);
    }

    public PhansalkarLocalAutoThreshold2D16UAlgorithm(PhansalkarLocalAutoThreshold2D16UAlgorithm other) {
        super(other);
        this.darkBackground = other.darkBackground;
        this.k = other.k;
        this.r = other.r;
        this.p = other.p;
        this.q = other.q;
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
            LocalAutoThreshold16UUtils.Phansalkar(processor, radius, k, r, p, q, true, out);
            ByteProcessor target = (ByteProcessor) (outputImage.hasImageStack()
                    ? outputImage.getStack().getProcessor(outputImage.getStackIndex(index.getC() + 1, index.getZ() + 1, index.getT() + 1))
                    : outputImage.getProcessor());
            target.copyBits(out, 0, 0, Blitter.COPY);
        }, progressInfo);

        iterationStep.addOutputData(getFirstOutputSlot(), new ImagePlusGreyscaleMaskData(outputImage), progressInfo);
    }

    @SetJIPipeDocumentation(name = "K", description = "Value of the parameter 'k' in the threshold formula (see Phansalkar et al., 2011). A recommended value is 0.25.")
    @JIPipeParameter("k")
    public double getK() {
        return k;
    }

    @JIPipeParameter("k")
    public void setK(double k) {
        this.k = k;
    }

    @SetJIPipeDocumentation(name = "R", description = "Value of the parameter 'r' in the threshold formula (see Phansalkar et al., 2011). A recommended value is 0.5.")
    @JIPipeParameter("r")
    public double getR() {
        return r;
    }

    @JIPipeParameter("r")
    public void setR(double r) {
        this.r = r;
    }

    @SetJIPipeDocumentation(name = "P", description = "Value of the parameter 'p' in the threshold formula (see Phansalkar et al., 2011). A recommended value is 2.")
    @JIPipeParameter("p")
    public double getP() {
        return p;
    }

    @JIPipeParameter("p")
    public void setP(double p) {
        this.p = p;
    }

    @SetJIPipeDocumentation(name = "Q", description = "Value of the parameter 'q' in the threshold formula (see Phansalkar et al., 2011). A recommended value is 10.")
    @JIPipeParameter("q")
    public double getQ() {
        return q;
    }

    @JIPipeParameter("q")
    public void setQ(double q) {
        this.q = q;
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
