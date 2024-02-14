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

package org.hkijena.jipipe.extensions.imagejalgorithms.nodes.threshold.local;

import ij.ImagePlus;
import ij.gui.NewImage;
import ij.plugin.filter.RankFilters;
import ij.process.Blitter;
import ij.process.ImageConverter;
import ij.process.ImageProcessor;
import org.hkijena.jipipe.api.JIPipeCitation;
import org.hkijena.jipipe.api.JIPipeDocumentation;
import org.hkijena.jipipe.api.JIPipeNode;
import org.hkijena.jipipe.api.JIPipeProgressInfo;
import org.hkijena.jipipe.api.nodes.*;
import org.hkijena.jipipe.api.nodes.categories.ImageJNodeTypeCategory;
import org.hkijena.jipipe.api.nodes.categories.ImagesNodeTypeCategory;
import org.hkijena.jipipe.api.nodes.iterationstep.JIPipeIterationContext;
import org.hkijena.jipipe.api.nodes.iterationstep.JIPipeSingleIterationStep;
import org.hkijena.jipipe.api.nodes.algorithm.JIPipeSimpleIteratingAlgorithm;
import org.hkijena.jipipe.api.parameters.JIPipeParameter;
import org.hkijena.jipipe.extensions.imagejdatatypes.datatypes.ImagePlusData;
import org.hkijena.jipipe.extensions.imagejdatatypes.datatypes.greyscale.ImagePlusGreyscale8UData;
import org.hkijena.jipipe.extensions.imagejdatatypes.datatypes.greyscale.ImagePlusGreyscaleMaskData;
import org.hkijena.jipipe.extensions.imagejdatatypes.util.ImageJUtils;


/**
 * Segmenter node that thresholds via an auto threshold
 * Based on code from {@link fiji.threshold.Auto_Local_Threshold}
 */
@JIPipeDocumentation(name = "Local auto threshold 2D (Niblack)", description = "Applies a local auto-thresholding algorithm. " +
        "If higher-dimensional data is provided, the filter is applied to each 2D slice.")
@JIPipeNode(menuPath = "Threshold\nLocal", nodeTypeCategory = ImagesNodeTypeCategory.class)
@JIPipeInputSlot(value = ImagePlusGreyscale8UData.class, slotName = "Input", autoCreate = true)
@JIPipeOutputSlot(value = ImagePlusGreyscaleMaskData.class, slotName = "Output", autoCreate = true)
@JIPipeCitation("Niblack W. (1986) \"An introduction to Digital Image Processing\" Prentice-Hall.")
@JIPipeNodeAlias(nodeTypeCategory = ImageJNodeTypeCategory.class, menuPath = "Image\nAdjust\nAuto Local Threshold")
public class NiblackLocalAutoThreshold2DAlgorithm extends JIPipeSimpleIteratingAlgorithm {

    private int modifier = 0;
    private boolean darkBackground = true;
    private double k = 0.2;
    private int radius = 15;

    /**
     * @param info the info
     */
    public NiblackLocalAutoThreshold2DAlgorithm(JIPipeNodeInfo info) {
        super(info);
    }

    /**
     * Copies the algorithm
     *
     * @param other the original
     */
    public NiblackLocalAutoThreshold2DAlgorithm(NiblackLocalAutoThreshold2DAlgorithm other) {
        super(other);
        this.darkBackground = other.darkBackground;
        this.k = other.k;
        this.radius = other.radius;
        this.modifier = other.modifier;
    }

    public static void Niblack(ImageProcessor ip, int radius, double k_value, int c_value, boolean doIwhite) {
        // Niblack recommends K_VALUE = -0.2 for images with black foreground
        // objects, and K_VALUE = +0.2 for images with white foreground objects.
        // Niblack W. (1986) "An introduction to Digital Image Processing" Prentice-Hall.
        // Ported to ImageJ plugin from E Celebi's fourier_0.8 routines
        // This version uses a circular local window, instead of a rectagular one

        ImagePlus Meanimp, Varimp;
        ImageProcessor ipMean, ipVar;

        byte object;
        byte backg;

        if (doIwhite) {
            object = (byte) 0xff;
            backg = (byte) 0;
        } else {
            object = (byte) 0;
            backg = (byte) 0xff;
        }

        Meanimp = duplicateImage(ip);
        ImageConverter ic = new ImageConverter(Meanimp);
        ic.convertToGray32();

        ipMean = Meanimp.getProcessor();
        RankFilters rf = new RankFilters();
        rf.rank(ipMean, radius, rf.MEAN);// Mean
        //Meanimp.show();
        Varimp = duplicateImage(ip);
        ic = new ImageConverter(Varimp);
        ic.convertToGray32();
        ipVar = Varimp.getProcessor();
        rf.rank(ipVar, radius, rf.VARIANCE); //Variance
        //Varimp.show();
        byte[] pixels = (byte[]) ip.getPixels();
        float[] mean = (float[]) ipMean.getPixels();
        float[] var = (float[]) ipVar.getPixels();

        for (int i = 0; i < pixels.length; i++)
            pixels[i] = ((pixels[i] & 0xff) > (int) (mean[i] + k_value * Math.sqrt(var[i]) - c_value)) ? object : backg;
        //imp.updateAndDraw();
    }

    public static ImagePlus duplicateImage(ImageProcessor iProcessor) {
        int w = iProcessor.getWidth();
        int h = iProcessor.getHeight();
        ImagePlus iPlus = NewImage.createByteImage("Image", w, h, 1, NewImage.FILL_BLACK);
        ImageProcessor imageProcessor = iPlus.getProcessor();
        imageProcessor.copyBits(iProcessor, 0, 0, Blitter.COPY);
        return iPlus;
    }

    @JIPipeDocumentation(name = "K", description = "Value of the parameter 'k' in the threshold formula (see Niblack, 1986). A recommended value is 0.2.")
    @JIPipeParameter("k")
    public double getK() {
        return k;
    }

    @JIPipeParameter("k")
    public void setK(double k) {
        this.k = k;
    }

    @JIPipeDocumentation(name = "Radius", description = "The radius of the circular local window.")
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

    @Override
    public boolean supportsParallelization() {
        return true;
    }

    @Override
    protected void runIteration(JIPipeSingleIterationStep iterationStep, JIPipeIterationContext iterationContext, JIPipeGraphNodeRunContext runContext, JIPipeProgressInfo progressInfo) {
        ImagePlusData inputData = iterationStep.getInputData(getFirstInputSlot(), ImagePlusGreyscale8UData.class, progressInfo);
        ImagePlus img = inputData.getDuplicateImage();
        ImageJUtils.forEachIndexedZCTSlice(img, (processor, index) -> {
            if (!darkBackground) {
                processor.invert();
            }
            Niblack(processor, radius, k, modifier, true);
        }, progressInfo);
        iterationStep.addOutputData(getFirstOutputSlot(), new ImagePlusGreyscaleMaskData(img), progressInfo);
    }

    @JIPipeDocumentation(name = "Dark background", description = "If the background color is dark. Disable this if your image has a bright background.")
    @JIPipeParameter("dark-background")
    public boolean isDarkBackground() {
        return darkBackground;
    }

    @JIPipeParameter("dark-background")
    public void setDarkBackground(boolean darkBackground) {
        this.darkBackground = darkBackground;
    }

    @JIPipeDocumentation(name = "Modifier", description = "This value is subtracted from each calculated local threshold.")
    @JIPipeParameter("modifier")
    public int getModifier() {
        return modifier;
    }

    @JIPipeParameter("modifier")
    public void setModifier(int modifier) {
        this.modifier = modifier;
    }
}
