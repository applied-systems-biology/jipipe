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

package org.hkijena.jipipe.extensions.imagejalgorithms.ij1.threshold.local;

import ij.ImagePlus;
import ij.gui.NewImage;
import ij.plugin.ContrastEnhancer;
import ij.plugin.filter.RankFilters;
import ij.process.Blitter;
import ij.process.ImageConverter;
import ij.process.ImageProcessor;
import org.hkijena.jipipe.api.JIPipeCitation;
import org.hkijena.jipipe.api.JIPipeDocumentation;
import org.hkijena.jipipe.api.JIPipeOrganization;
import org.hkijena.jipipe.api.JIPipeProgressInfo;
import org.hkijena.jipipe.api.data.JIPipeDefaultMutableSlotConfiguration;
import org.hkijena.jipipe.api.nodes.*;
import org.hkijena.jipipe.api.nodes.categories.ImagesNodeTypeCategory;
import org.hkijena.jipipe.api.parameters.JIPipeParameter;
import org.hkijena.jipipe.extensions.imagejdatatypes.datatypes.ImagePlusData;
import org.hkijena.jipipe.extensions.imagejdatatypes.datatypes.greyscale.ImagePlusGreyscale8UData;
import org.hkijena.jipipe.extensions.imagejdatatypes.datatypes.greyscale.ImagePlusGreyscaleMaskData;

import static org.hkijena.jipipe.extensions.imagejalgorithms.ImageJAlgorithmsExtension.ADD_MASK_QUALIFIER;

/**
 * Segmenter node that thresholds via an auto threshold
 * Based on code from {@link fiji.threshold.Auto_Local_Threshold}
 */
@JIPipeDocumentation(name = "Local auto threshold 2D (Phansalkar)", description = "Applies a local auto-thresholding algorithm. " +
        "The threshold is calculated as <code>t = mean * (1 + p * exp(-q * mean) + k * ((stdev / r) - 1))</code>.\n\n" +
        "If higher-dimensional data is provided, the filter is applied to each 2D slice.")
@JIPipeOrganization(menuPath = "Threshold\nLocal", nodeTypeCategory = ImagesNodeTypeCategory.class)
@JIPipeInputSlot(value = ImagePlusGreyscale8UData.class, slotName = "Input")
@JIPipeOutputSlot(value = ImagePlusGreyscaleMaskData.class, slotName = "Output")
@JIPipeCitation("Phansalskar N. et al. Adaptive local thresholding for detection of nuclei in diversity stained cytology images. " +
        "International Conference on Communications and Signal Processing (ICCSP), 2011, 218 - 220.")
public class PhansalkarLocalAutoThreshold2DAlgorithm extends JIPipeSimpleIteratingAlgorithm {
    private boolean darkBackground = true;
    private double k = 0.25;
    private double r = 0.5;
    private double p = 2;
    private double q = 10;
    private int radius = 15;

    /**
     * @param info the info
     */
    public PhansalkarLocalAutoThreshold2DAlgorithm(JIPipeNodeInfo info) {
        super(info, JIPipeDefaultMutableSlotConfiguration.builder().addInputSlot("Input", ImagePlusGreyscale8UData.class)
                .addOutputSlot("Output", ImagePlusGreyscaleMaskData.class, "Input", ADD_MASK_QUALIFIER)
                .allowOutputSlotInheritance(true)
                .seal()
                .build());
    }

    public PhansalkarLocalAutoThreshold2DAlgorithm(PhansalkarLocalAutoThreshold2DAlgorithm other) {
        super(other);
        this.darkBackground = other.darkBackground;
        this.k = other.k;
        this.r = other.r;
        this.p = other.p;
        this.q = other.q;
        this.radius = other.radius;
    }

    @JIPipeDocumentation(name = "K", description = "Value of the parameter 'k' in the threshold formula (see Phansalskar et al., 2011). A recommended value is 0.25.")
    @JIPipeParameter("k")
    public double getK() {
        return k;
    }

    @JIPipeParameter("k")
    public void setK(double k) {
        this.k = k;
    }

    @JIPipeDocumentation(name = "R", description = "Value of the parameter 'r' in the threshold formula (see Phansalskar et al., 2011). A recommended value is 0.5.")
    @JIPipeParameter("r")
    public double getR() {
        return r;
    }

    @JIPipeParameter("r")
    public void setR(double r) {
        this.r = r;
    }

    @JIPipeDocumentation(name = "P", description = "Value of the parameter 'p' in the threshold formula (see Phansalskar et al., 2011). A recommended value is 2.")
    @JIPipeParameter("p")
    public double getP() {
        return p;
    }

    @JIPipeParameter("p")
    public void setP(double p) {
        this.p = p;
    }

    @JIPipeDocumentation(name = "Q", description = "Value of the parameter 'q' in the threshold formula (see Phansalskar et al., 2011). A recommended value is 10.")
    @JIPipeParameter("q")
    public double getQ() {
        return q;
    }

    @JIPipeParameter("q")
    public void setQ(double q) {
        this.q = q;
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
    protected void runIteration(JIPipeDataBatch dataBatch, JIPipeProgressInfo progressInfo) {
        ImagePlusData inputData = dataBatch.getInputData(getFirstInputSlot(), ImagePlusGreyscale8UData.class, progressInfo);
        ImagePlus img = inputData.getDuplicateImage();
        if (!darkBackground) {
            img.getProcessor().invert();
        }
        Phansalkar(img, radius, k, r, p, q, true);
        dataBatch.addOutputData(getFirstOutputSlot(), new ImagePlusGreyscaleMaskData(img), progressInfo);
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

    public static void Phansalkar(ImagePlus imp, int radius, double k_value, double r_value, double p_value, double q_value, boolean doIwhite) {
        // This is a modification of Sauvola's thresholding method to deal with low contrast images.
        // Phansalskar N. et al. Adaptive local thresholding for detection of nuclei in diversity stained
        // cytology images.International Conference on Communications and Signal Processing (ICCSP), 2011,
        // 218 - 220.
        // In this method, the threshold t = mean*(1+p*exp(-q*mean)+k*((stdev/r)-1))
        // Phansalkar recommends k = 0.25, r = 0.5, p = 2 and q = 10. In this plugin, k and r are the
        // parameters 1 and 2 respectively, but the values of p and q are fixed.
        //
        // Implemented from Phansalkar's paper description by G. Landini
        // This version uses a circular local window, instead of a rectagular one

        ImagePlus Meanimp, Varimp, Orimp;
        ImageProcessor ip = imp.getProcessor(), ipMean, ipVar, ipOri;
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
        ContrastEnhancer ce = new ContrastEnhancer();
        ce.setNormalize(true); // Needs to be true for correct normalization
        ce.stretchHistogram(Meanimp, 0.0);
        ImageConverter ic = new ImageConverter(Meanimp);
        ic.convertToGray32();
        ipMean = Meanimp.getProcessor();
        ipMean.multiply(1.0 / 255);

        Orimp = duplicateImage(ip);
        ce.stretchHistogram(Orimp, 0.0);
        ic = new ImageConverter(Orimp);
        ic.convertToGray32();
        ipOri = Orimp.getProcessor();
        ipOri.multiply(1.0 / 255); //original to compare
        //Orimp.show();

        RankFilters rf = new RankFilters();
        rf.rank(ipMean, radius, rf.MEAN);// Mean

        //Meanimp.show();
        Varimp = duplicateImage(ip);
        ce.stretchHistogram(Varimp, 0.0);
        ic = new ImageConverter(Varimp);
        ic.convertToGray32();
        ipVar = Varimp.getProcessor();
        ipVar.multiply(1.0 / 255);

        rf.rank(ipVar, radius, rf.VARIANCE); //Variance
        ipVar.sqrt(); //SD

        //Varimp.show();
        byte[] pixels = (byte[]) ip.getPixels();
        float[] ori = (float[]) ipOri.getPixels();
        float[] mean = (float[]) ipMean.getPixels();
        float[] sd = (float[]) ipVar.getPixels();

        for (int i = 0; i < pixels.length; i++)
            pixels[i] = ((ori[i]) > (mean[i] * (1.0 + p_value * Math.exp(-q_value * mean[i]) + k_value * ((sd[i] / r_value) - 1.0)))) ? object : backg;
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
}
