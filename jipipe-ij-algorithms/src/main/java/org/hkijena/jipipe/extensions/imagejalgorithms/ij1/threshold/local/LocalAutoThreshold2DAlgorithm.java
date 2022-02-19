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

import ij.IJ;
import ij.ImagePlus;
import ij.gui.NewImage;
import ij.gui.OvalRoi;
import ij.gui.Roi;
import ij.plugin.filter.RankFilters;
import ij.process.Blitter;
import ij.process.ImageConverter;
import ij.process.ImageProcessor;
import org.hkijena.jipipe.api.JIPipeDocumentation;
import org.hkijena.jipipe.api.JIPipeNode;
import org.hkijena.jipipe.api.JIPipeProgressInfo;
import org.hkijena.jipipe.api.data.JIPipeDefaultMutableSlotConfiguration;
import org.hkijena.jipipe.api.nodes.*;
import org.hkijena.jipipe.api.nodes.categories.ImagesNodeTypeCategory;
import org.hkijena.jipipe.api.parameters.JIPipeParameter;
import org.hkijena.jipipe.extensions.imagejdatatypes.datatypes.ImagePlusData;
import org.hkijena.jipipe.extensions.imagejdatatypes.datatypes.greyscale.ImagePlusGreyscale8UData;
import org.hkijena.jipipe.extensions.imagejdatatypes.datatypes.greyscale.ImagePlusGreyscaleMaskData;
import org.scijava.Priority;

import static org.hkijena.jipipe.extensions.imagejalgorithms.ImageJAlgorithmsExtension.ADD_MASK_QUALIFIER;

/**
 * Segmenter node that thresholds via an auto threshold
 * Based on code from {@link fiji.threshold.Auto_Local_Threshold}
 */
@JIPipeDocumentation(name = "Local auto threshold 2D", description = "Applies a local auto-thresholding algorithm.\n\n" +
        "This node supports various methods:\n\nMean (threshold is mean local pixel value)\n" +
        "Median (threshold is median local pixel value)\n" +
        "MidGray (threshold is average of min and max pixel values)\n" +
        "Otsu (threshold is the local otsu threshold)\n\n" +
        "If higher-dimensional data is provided, the filter is applied to each 2D slice.")
@JIPipeNode(menuPath = "Threshold\nLocal", nodeTypeCategory = ImagesNodeTypeCategory.class)
@JIPipeInputSlot(value = ImagePlusGreyscale8UData.class, slotName = "Input")
@JIPipeOutputSlot(value = ImagePlusGreyscaleMaskData.class, slotName = "Output")
public class LocalAutoThreshold2DAlgorithm extends JIPipeSimpleIteratingAlgorithm {

    private Method method = Method.Otsu;
    private boolean darkBackground = true;
    private int radius = 15;
    private int modifier = 0;

    /**
     * @param info the info
     */
    public LocalAutoThreshold2DAlgorithm(JIPipeNodeInfo info) {
        super(info, JIPipeDefaultMutableSlotConfiguration.builder().addInputSlot("Input", "", ImagePlusGreyscale8UData.class)
                .addOutputSlot("Output", "", ImagePlusGreyscaleMaskData.class, "Input", ADD_MASK_QUALIFIER)
                .allowOutputSlotInheritance(true)
                .seal()
                .build());
    }

    /**
     * Copies the algorithm
     *
     * @param other the original
     */
    public LocalAutoThreshold2DAlgorithm(LocalAutoThreshold2DAlgorithm other) {
        super(other);
        this.method = other.method;
        this.darkBackground = other.darkBackground;
        this.modifier = other.modifier;
        this.radius = other.radius;
    }

    public static void Mean(ImagePlus imp, int radius, int c_value, boolean doIwhite) {
        // See: Image Processing Learning Resourches HIPR2
        // http://homepages.inf.ed.ac.uk/rbf/HIPR2/adpthrsh.htm
        ImagePlus Meanimp;
        ImageProcessor ip = imp.getProcessor(), ipMean;
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
        byte[] pixels = (byte[]) ip.getPixels();
        float[] mean = (float[]) ipMean.getPixels();

        for (int i = 0; i < pixels.length; i++)
            pixels[i] = ((pixels[i] & 0xff) > (int) (mean[i] - c_value)) ? object : backg;
        //imp.updateAndDraw();
    }

    public static void Median(ImagePlus imp, int radius, int c_value, boolean doIwhite) {
        // See: Image Processing Learning Resourches HIPR2
        // http://homepages.inf.ed.ac.uk/rbf/HIPR2/adpthrsh.htm
        ImagePlus Medianimp;
        ImageProcessor ip = imp.getProcessor(), ipMedian;
        byte object;
        byte backg;

        if (doIwhite) {
            object = (byte) 0xff;
            backg = (byte) 0;
        } else {
            object = (byte) 0;
            backg = (byte) 0xff;
        }

        Medianimp = duplicateImage(ip);
        ipMedian = Medianimp.getProcessor();
        RankFilters rf = new RankFilters();
        rf.rank(ipMedian, radius, rf.MEDIAN);
        //Medianimp.show();
        byte[] pixels = (byte[]) ip.getPixels();
        byte[] median = (byte[]) ipMedian.getPixels();

        for (int i = 0; i < pixels.length; i++)
            pixels[i] = ((pixels[i] & 0xff) > ((median[i] & 0xff) - c_value)) ? object : backg;
        //imp.updateAndDraw();
    }

    public static void MidGrey(ImagePlus imp, int radius, int c_value, boolean doIwhite) {
        // See: Image Processing Learning Resourches HIPR2
        // http://homepages.inf.ed.ac.uk/rbf/HIPR2/adpthrsh.htm
        ImagePlus Maximp, Minimp;
        ImageProcessor ip = imp.getProcessor(), ipMax, ipMin;
        int mid_gray;
        byte object;
        byte backg;

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
            pixels[i] = ((pixels[i] & 0xff) > (((max[i] & 0xff) + (min[i] & 0xff)) / 2) - c_value) ? object : backg;
        }
        //imp.updateAndDraw();
    }

    public static void Otsu(ImagePlus imp, int radius, int c_value, boolean doIwhite) {
        // Otsu's threshold algorithm
        // M. Emre Celebi 6.15.2007, Fourier Library https://sourceforge.net/projects/fourier-ipal/
        // ported to ImageJ plugin by G.Landini. Same algorithm as in Auto_Threshold, this time for local circular regions

        int[] data;
        int w = imp.getWidth();
        int h = imp.getHeight();
        int position;
        int radiusx2 = radius * 2;
        ImageProcessor ip = imp.getProcessor();
        byte[] pixels = (byte[]) ip.getPixels();
        byte[] pixelsOut = new byte[pixels.length]; // need this to avoid changing the image data (and further histograms)
        byte object;
        byte backg;

        if (doIwhite) {
            object = (byte) 0xff;
            backg = (byte) 0;
        } else {
            object = (byte) 0;
            backg = (byte) 0xff;
        }

        int ih, roiy, L = 256; //L is for 8bit images.
        int threshold;
        int num_pixels;
        double total_mean;    /* mean gray-level for the whole image */
        double bcv, term;    /* between-class variance, scaling term */
        double max_bcv;        /* max BCV */
        double[] cnh = new double[L];    /* cumulative normalized histogram */
        double[] mean = new double[L];    /* mean gray-level */
        double[] histo = new double[L];    /* normalized histogram */


        Roi roi = new OvalRoi(0, 0, radiusx2, radiusx2);
        //ip.setRoi(roi);
        for (int y = 0; y < h; y++) {
            IJ.showProgress((double) (y) / (h - 1)); // this method is slow, so let's show the progress bar
            roiy = y - radius;
            for (int x = 0; x < w; x++) {
                roi.setLocation(x - radius, roiy);
                ip.setRoi(roi);
                //ip.setRoi(new OvalRoi(x-radius, roiy, radiusx2, radiusx2));
                position = x + y * w;
                data = ip.getHistogram();

                //----
                /* Calculate total numbre of pixels */
                num_pixels = 0;

                for (ih = 0; ih < L; ih++)
                    num_pixels = num_pixels + data[ih];

                term = 1.0 / (double) num_pixels;

                /* Calculate the normalized histogram */
                for (ih = 0; ih < L; ih++) {
                    histo[ih] = term * data[ih];
                }

                /* Calculate the cumulative normalized histogram */
                cnh[0] = histo[0];
                for (ih = 1; ih < L; ih++) {
                    cnh[ih] = cnh[ih - 1] + histo[ih];
                }

                mean[0] = 0.0;

                for (ih = 1; ih < L; ih++) {
                    mean[ih] = mean[ih - 1] + ih * histo[ih];
                }

                total_mean = mean[L - 1];

                //	Calculate the BCV at each gray-level and find the threshold that maximizes it
                threshold = 0; //Integer.MIN_VALUE;
                max_bcv = 0.0;

                for (ih = 0; ih < L; ih++) {
                    bcv = total_mean * cnh[ih] - mean[ih];
                    bcv *= bcv / (cnh[ih] * (1.0 - cnh[ih]));

                    if (max_bcv < bcv) {
                        max_bcv = bcv;
                        threshold = ih;
                    }
                }
                threshold = (threshold - c_value) & 0xff;
                pixelsOut[position] = ((pixels[position] & 0xff) > threshold || (pixels[position] & 0xff) == 255) ? object : backg;
            }
        }
        for (position = 0; position < w * h; position++)
            pixels[position] = pixelsOut[position]; //update with thresholded pixels
    }

    public static ImagePlus duplicateImage(ImageProcessor iProcessor) {
        int w = iProcessor.getWidth();
        int h = iProcessor.getHeight();
        ImagePlus iPlus = NewImage.createByteImage("Image", w, h, 1, NewImage.FILL_BLACK);
        ImageProcessor imageProcessor = iPlus.getProcessor();
        imageProcessor.copyBits(iProcessor, 0, 0, Blitter.COPY);
        return iPlus;
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
        switch (method) {
            case Mean:
                Mean(img, radius, modifier, true);
                break;
            case Otsu:
                Otsu(img, radius, modifier, true);
                break;
            case Median:
                Median(img, radius, modifier, true);
                break;
            case MidGrey:
                MidGrey(img, radius, modifier, true);
                break;
        }
        dataBatch.addOutputData(getFirstOutputSlot(), new ImagePlusGreyscaleMaskData(img), progressInfo);
    }

    @JIPipeParameter(value = "method", priority = Priority.HIGH)
    @JIPipeDocumentation(name = "Method", description = "Determines the thresholding method:\n\n" +
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

    public enum Method {
        Mean,
        Median,
        MidGrey,
        Otsu
    }
}
