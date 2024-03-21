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

package org.hkijena.jipipe.extensions.imagejalgorithms.nodes.threshold.local;

import ij.ImagePlus;
import ij.gui.NewImage;
import ij.plugin.filter.RankFilters;
import ij.process.Blitter;
import ij.process.ImageProcessor;
import org.hkijena.jipipe.api.AddJIPipeCitation;
import org.hkijena.jipipe.api.SetJIPipeDocumentation;
import org.hkijena.jipipe.api.ConfigureJIPipeNode;
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
@SetJIPipeDocumentation(name = "Local auto threshold 2D (Bernsen)", description = "Applies a local auto-thresholding algorithm. " +
        "If higher-dimensional data is provided, the filter is applied to each 2D slice.\n\n" +
        "Bernsen recommends a radius of 15 and a contrast threshold of 15.")
@ConfigureJIPipeNode(menuPath = "Threshold\nLocal", nodeTypeCategory = ImagesNodeTypeCategory.class)
@AddJIPipeInputSlot(value = ImagePlusGreyscale8UData.class, slotName = "Input", create = true)
@AddJIPipeOutputSlot(value = ImagePlusGreyscaleMaskData.class, slotName = "Output", create = true)
@AddJIPipeCitation("Bernsen J. (1986) \"Dynamic Thresholding of Grey-Level Images\" Proc. of the 8th Int. Conf. on Pattern Recognition, pp. 1251-1255")
@AddJIPipeCitation("Sezgin M. and Sankur B. (2004) \"Survey over Image Thresholding Techniques and Quantitative Performance Evaluation\" Journal of Electronic Imaging, 13(1): 146-165")
@AddJIPipeNodeAlias(nodeTypeCategory = ImageJNodeTypeCategory.class, menuPath = "Image\nAdjust\nAuto Local Threshold")
public class BernsenLocalAutoThreshold2DAlgorithm extends JIPipeSimpleIteratingAlgorithm {

    private boolean darkBackground = true;
    private int contrastThreshold = 15;
    private int radius = 15;

    /**
     * @param info the info
     */
    public BernsenLocalAutoThreshold2DAlgorithm(JIPipeNodeInfo info) {
        super(info);
    }

    /**
     * Copies the algorithm
     *
     * @param other the original
     */
    public BernsenLocalAutoThreshold2DAlgorithm(BernsenLocalAutoThreshold2DAlgorithm other) {
        super(other);
        this.darkBackground = other.darkBackground;
        this.contrastThreshold = other.contrastThreshold;
        this.radius = other.radius;
    }

    public static void Bernsen(ImageProcessor ip, int radius, double contrast_threshold, boolean doIwhite) {
        // Bernsen recommends WIN_SIZE = 31 and CONTRAST_THRESHOLD = 15.
        //  1) Bernsen J. (1986) "Dynamic Thresholding of Grey-Level Images"
        //    Proc. of the 8th Int. Conf. on Pattern Recognition, pp. 1251-1255
        //  2) Sezgin M. and Sankur B. (2004) "Survey over Image Thresholding
        //   Techniques and Quantitative Performance Evaluation" Journal of
        //   Electronic Imaging, 13(1): 146-165
        //   http://citeseer.ist.psu.edu/sezgin04survey.html
        // Ported to ImageJ plugin from E Celebi's fourier_0.8 routines
        // This version uses a circular local window, instead of a rectagular one
        ImagePlus Maximp, Minimp;
        ImageProcessor ipMax, ipMin;
        int local_contrast;
        int mid_gray;
        byte object;
        byte backg;
        int temp;

        if (doIwhite) {
            object = (byte) 0xff;
            backg = (byte) 0;
        } else {
            object = (byte) 0;
            backg = (byte) 0xff;
        }

        Maximp = duplicateImage(ip);
        ipMax = Maximp.getProcessor();
        RankFilters rf = new RankFilters();
        rf.rank(ipMax, radius, rf.MAX);// Maximum
        //Maximp.show();
        Minimp = duplicateImage(ip);
        ipMin = Minimp.getProcessor();
        rf.rank(ipMin, radius, rf.MIN); //Minimum
        //Minimp.show();
        byte[] pixels = (byte[]) ip.getPixels();
        byte[] max = (byte[]) ipMax.getPixels();
        byte[] min = (byte[]) ipMin.getPixels();

        for (int i = 0; i < pixels.length; i++) {
            local_contrast = (max[i] & 0xff) - (min[i] & 0xff);
            mid_gray = ((min[i] & 0xff) + (max[i] & 0xff)) / 2;
            temp = pixels[i] & 0x0000ff;
            if (local_contrast < contrast_threshold)
                pixels[i] = (mid_gray >= 128) ? object : backg;  //Low contrast region
            else
                pixels[i] = (temp >= mid_gray) ? object : backg;
        }
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
            Bernsen(processor, radius, contrastThreshold, true);
        }, progressInfo);
        iterationStep.addOutputData(getFirstOutputSlot(), new ImagePlusGreyscaleMaskData(img), progressInfo);
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
