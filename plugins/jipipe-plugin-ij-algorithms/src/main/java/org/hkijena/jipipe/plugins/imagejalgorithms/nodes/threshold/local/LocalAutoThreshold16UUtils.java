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
import ij.gui.NewImage;
import ij.gui.OvalRoi;
import ij.gui.Roi;
import ij.plugin.filter.RankFilters;
import ij.process.Blitter;
import ij.process.ByteProcessor;
import ij.process.ImageConverter;
import ij.process.ImageProcessor;
import ij.process.ShortProcessor;
import org.hkijena.jipipe.utils.threshold.AutoThresholdMethod;
import org.hkijena.jipipe.utils.threshold.NBinsAutoThresholder;

/**
 * 16-bit local auto-threshold kernels, ported from the 8-bit variants
 * (originally from fiji.threshold.Auto_Local_Threshold).
 * All kernels write into an 8-bit output processor instead of mutating the input.
 */
public final class LocalAutoThreshold16UUtils {

    private LocalAutoThreshold16UUtils() {
    }

    /**
     * Duplicates a 16-bit processor into a new ImagePlus.
     */
    public static ImagePlus duplicateImage16U(ImageProcessor iProcessor) {
        int w = iProcessor.getWidth();
        int h = iProcessor.getHeight();
        ImagePlus iPlus = NewImage.createShortImage("Image", w, h, 1, NewImage.FILL_BLACK);
        ImageProcessor imageProcessor = iPlus.getProcessor();
        imageProcessor.copyBits(iProcessor, 0, 0, Blitter.COPY);
        return iPlus;
    }

    /**
     * Inverts a 16-bit slice in the full range (v -> 65535 - v).
     * <p>
     * {@link ShortProcessor#invert()} cannot be used for this: unless the (off by default)
     * {@code Prefs.fullRange16bitInversions} preference is set, it inverts around the
     * current min/max display range (v -> max + min - v), which would rescale each
     * slice to its own data range.
     *
     * @param ip the processor to invert in-place
     */
    public static void invertFullRange(ImageProcessor ip) {
        short[] pixels = (short[]) ip.getPixels();
        for (int i = 0; i < pixels.length; i++) {
            pixels[i] = (short) (65535 - (pixels[i] & 0xffff));
        }
    }

    /**
     * Port of the 8-bit Mean kernel.
     * See: Image Processing Learning Resources HIPR2
     * http://homepages.inf.ed.ac.uk/rbf/HIPR2/adpthrsh.htm
     */
    public static void Mean(ImageProcessor ip, int radius, int c_value, boolean doIwhite, ByteProcessor out) {
        ImagePlus Meanimp;
        ImageProcessor ipMean;
        byte object;
        byte backg;

        if (doIwhite) {
            object = (byte) 0xff;
            backg = (byte) 0;
        } else {
            object = (byte) 0;
            backg = (byte) 0xff;
        }

        Meanimp = duplicateImage16U(ip);
        ImageConverter ic = new ImageConverter(Meanimp);
        ic.convertToGray32();

        ipMean = Meanimp.getProcessor();
        RankFilters rf = new RankFilters();
        rf.rank(ipMean, radius, rf.MEAN);// Mean
        short[] pixels = (short[]) ip.getPixels();
        float[] mean = (float[]) ipMean.getPixels();
        byte[] outPixels = (byte[]) out.getPixels();

        for (int i = 0; i < pixels.length; i++)
            outPixels[i] = ((pixels[i] & 0xFFFF) > (int) (mean[i] - c_value)) ? object : backg;
    }

    /**
     * Port of the 8-bit Median kernel.
     * See: Image Processing Learning Resources HIPR2
     * http://homepages.inf.ed.ac.uk/rbf/HIPR2/adpthrsh.htm
     */
    public static void Median(ImageProcessor ip, int radius, int c_value, boolean doIwhite, ByteProcessor out) {
        ImagePlus Medianimp;
        ImageProcessor ipMedian;
        byte object;
        byte backg;

        if (doIwhite) {
            object = (byte) 0xff;
            backg = (byte) 0;
        } else {
            object = (byte) 0;
            backg = (byte) 0xff;
        }

        Medianimp = duplicateImage16U(ip);
        ipMedian = Medianimp.getProcessor();
        RankFilters rf = new RankFilters();
        rf.rank(ipMedian, radius, rf.MEDIAN);
        short[] pixels = (short[]) ip.getPixels();
        short[] median = (short[]) ipMedian.getPixels();
        byte[] outPixels = (byte[]) out.getPixels();

        for (int i = 0; i < pixels.length; i++)
            outPixels[i] = ((pixels[i] & 0xFFFF) > ((median[i] & 0xFFFF) - c_value)) ? object : backg;
    }

    /**
     * Port of the 8-bit MidGrey kernel.
     * See: Image Processing Learning Resources HIPR2
     * http://homepages.inf.ed.ac.uk/rbf/HIPR2/adpthrsh.htm
     */
    public static void MidGrey(ImageProcessor ip, int radius, int c_value, boolean doIwhite, ByteProcessor out) {
        ImagePlus Maximp, Minimp;
        ImageProcessor ipMax, ipMin;
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

        Maximp = duplicateImage16U(ip);
        ipMax = Maximp.getProcessor();
        RankFilters rf = new RankFilters();
        rf.rank(ipMax, radius, rf.MAX);// Maximum
        Minimp = duplicateImage16U(ip);
        ipMin = Minimp.getProcessor();
        rf.rank(ipMin, radius, rf.MIN); //Minimum
        short[] pixels = (short[]) ip.getPixels();
        short[] max = (short[]) ipMax.getPixels();
        short[] min = (short[]) ipMin.getPixels();
        byte[] outPixels = (byte[]) out.getPixels();

        for (int i = 0; i < pixels.length; i++) {
            mid_gray = ((max[i] & 0xFFFF) + (min[i] & 0xFFFF)) / 2;
            outPixels[i] = ((pixels[i] & 0xFFFF) > mid_gray - c_value) ? object : backg;
        }
    }

    /**
     * Port of the 8-bit Bernsen kernel.
     * Bernsen recommends WIN_SIZE = 31 and CONTRAST_THRESHOLD = 15.
     */
    public static void Bernsen(ImageProcessor ip, int radius, double contrast_threshold, boolean doIwhite, ByteProcessor out) {
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

        Maximp = duplicateImage16U(ip);
        ipMax = Maximp.getProcessor();
        RankFilters rf = new RankFilters();
        rf.rank(ipMax, radius, rf.MAX);// Maximum
        Minimp = duplicateImage16U(ip);
        ipMin = Minimp.getProcessor();
        rf.rank(ipMin, radius, rf.MIN); //Minimum
        short[] pixels = (short[]) ip.getPixels();
        short[] max = (short[]) ipMax.getPixels();
        short[] min = (short[]) ipMin.getPixels();
        byte[] outPixels = (byte[]) out.getPixels();

        for (int i = 0; i < pixels.length; i++) {
            local_contrast = (max[i] & 0xFFFF) - (min[i] & 0xFFFF);
            mid_gray = ((min[i] & 0xFFFF) + (max[i] & 0xFFFF)) / 2;
            temp = pixels[i] & 0xFFFF;
            if (local_contrast < contrast_threshold)
                outPixels[i] = (mid_gray >= 32768) ? object : backg;  //Low contrast region
            else
                outPixels[i] = (temp >= mid_gray) ? object : backg;
        }
    }

    /**
     * Port of the 8-bit Contrast kernel (G. Landini, 2013).
     */
    public static void Contrast(ImageProcessor ip, int radius, boolean doIwhite, ByteProcessor out) {
        ImagePlus Maximp, Minimp;
        ImageProcessor ipMax, ipMin;
        byte object;
        byte backg;

        if (doIwhite) {
            object = (byte) 0xff;
            backg = (byte) 0;
        } else {
            object = (byte) 0;
            backg = (byte) 0xff;
        }

        Maximp = duplicateImage16U(ip);
        ipMax = Maximp.getProcessor();
        RankFilters rf = new RankFilters();
        rf.rank(ipMax, radius, rf.MAX);// Maximum
        Minimp = duplicateImage16U(ip);
        ipMin = Minimp.getProcessor();
        rf.rank(ipMin, radius, rf.MIN); //Minimum
        short[] pixels = (short[]) ip.getPixels();
        short[] max = (short[]) ipMax.getPixels();
        short[] min = (short[]) ipMin.getPixels();
        byte[] outPixels = (byte[]) out.getPixels();
        for (int i = 0; i < pixels.length; i++) {
            outPixels[i] = ((Math.abs((max[i] & 0xFFFF) - (pixels[i] & 0xFFFF)) <= Math.abs((pixels[i] & 0xFFFF) - (min[i] & 0xFFFF))) && ((pixels[i] & 0xFFFF) != 0)) ? object : backg;
        }
    }

    /**
     * Port of the 8-bit Niblack kernel.
     * Niblack recommends K_VALUE = -0.2 for images with black foreground
     * objects, and K_VALUE = +0.2 for images with white foreground objects.
     */
    public static void Niblack(ImageProcessor ip, int radius, double k_value, int c_value, boolean doIwhite, ByteProcessor out) {
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

        Meanimp = duplicateImage16U(ip);
        ImageConverter ic = new ImageConverter(Meanimp);
        ic.convertToGray32();

        ipMean = Meanimp.getProcessor();
        RankFilters rf = new RankFilters();
        rf.rank(ipMean, radius, rf.MEAN);// Mean
        Varimp = duplicateImage16U(ip);
        ic = new ImageConverter(Varimp);
        ic.convertToGray32();
        ipVar = Varimp.getProcessor();
        rf.rank(ipVar, radius, rf.VARIANCE); //Variance
        short[] pixels = (short[]) ip.getPixels();
        float[] mean = (float[]) ipMean.getPixels();
        float[] var = (float[]) ipVar.getPixels();
        byte[] outPixels = (byte[]) out.getPixels();

        for (int i = 0; i < pixels.length; i++)
            outPixels[i] = ((pixels[i] & 0xFFFF) > (int) (mean[i] + k_value * Math.sqrt(var[i]) - c_value)) ? object : backg;
    }

    /**
     * Port of the 8-bit Sauvola kernel.
     * Sauvola recommends K_VALUE = 0.5; for 16-bit images R_VALUE should be
     * half the dynamic range (i.e. 32768).
     */
    public static void Sauvola(ImageProcessor ip, int radius, double k_value, double r_value, boolean doIwhite, ByteProcessor out) {
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

        Meanimp = duplicateImage16U(ip);
        ImageConverter ic = new ImageConverter(Meanimp);
        ic.convertToGray32();

        ipMean = Meanimp.getProcessor();
        RankFilters rf = new RankFilters();
        rf.rank(ipMean, radius, rf.MEAN);// Mean
        Varimp = duplicateImage16U(ip);
        ic = new ImageConverter(Varimp);
        ic.convertToGray32();
        ipVar = Varimp.getProcessor();
        rf.rank(ipVar, radius, rf.VARIANCE); //Variance
        short[] pixels = (short[]) ip.getPixels();
        float[] mean = (float[]) ipMean.getPixels();
        float[] var = (float[]) ipVar.getPixels();
        byte[] outPixels = (byte[]) out.getPixels();

        for (int i = 0; i < pixels.length; i++)
            outPixels[i] = ((pixels[i] & 0xFFFF) > (int) (mean[i] * (1.0 + k_value * ((Math.sqrt(var[i]) / r_value) - 1.0)))) ? object : backg;
    }

    /**
     * Port of the 8-bit Phansalkar kernel.
     * The threshold is calculated as t = mean * (1 + p * exp(-q * mean) + k * ((stdev / r) - 1)).
     * Intensities are normalized to [0, 1] (divided by 65535) before the local mean and
     * standard deviation are computed.
     */
    public static void Phansalkar(ImageProcessor ip, int radius, double k_value, double r_value, double p_value, double q_value, boolean doIwhite, ByteProcessor out) {
        ImagePlus Meanimp, Varimp, Orimp;
        ImageProcessor ipMean, ipVar, ipOri;
        byte object;
        byte backg;

        if (doIwhite) {
            object = (byte) 0xff;
            backg = (byte) 0;
        } else {
            object = (byte) 0;
            backg = (byte) 0xff;
        }

        Meanimp = duplicateImage16U(ip);
        ImageConverter ic = new ImageConverter(Meanimp);
        ic.convertToGray32();
        ipMean = Meanimp.getProcessor();
        ipMean.multiply(1.0 / 65535);

        Orimp = duplicateImage16U(ip);
        ic = new ImageConverter(Orimp);
        ic.convertToGray32();
        ipOri = Orimp.getProcessor();
        ipOri.multiply(1.0 / 65535); //original to compare

        RankFilters rf = new RankFilters();
        rf.rank(ipMean, radius, rf.MEAN);// Mean

        Varimp = duplicateImage16U(ip);
        ic = new ImageConverter(Varimp);
        ic.convertToGray32();
        ipVar = Varimp.getProcessor();
        ipVar.multiply(1.0 / 65535);

        rf.rank(ipVar, radius, rf.VARIANCE); //Variance
        ipVar.sqrt(); //SD

        float[] ori = (float[]) ipOri.getPixels();
        float[] mean = (float[]) ipMean.getPixels();
        float[] sd = (float[]) ipVar.getPixels();
        byte[] outPixels = (byte[]) out.getPixels();

        for (int i = 0; i < outPixels.length; i++)
            outPixels[i] = ((ori[i]) > (mean[i] * (1.0 + p_value * Math.exp(-q_value * mean[i]) + k_value * ((sd[i] / r_value) - 1.0)))) ? object : backg;
    }

    /**
     * Port of the 8-bit local Otsu kernel.
     * Per circular window, the histogram is built over the window's actual value
     * range (compact histogram) and thresholded via {@link NBinsAutoThresholder};
     * the result is offset back by the local minimum.
     */
    public static void Otsu(ImageProcessor ip, int radius, int c_value, boolean doIwhite, ByteProcessor out) {
        // Otsu's threshold algorithm
        // M. Emre Celebi 6.15.2007, Fourier Library https://sourceforge.net/projects/fourier-ipal/
        // ported to ImageJ plugin by G.Landini. Same algorithm as in Auto_Threshold, this time for local circular regions

        int w = ip.getWidth();
        int h = ip.getHeight();
        int position;
        int radiusx2 = radius * 2;
        short[] pixels = (short[]) ip.getPixels();
        byte[] outPixels = (byte[]) out.getPixels();
        byte object;
        byte backg;

        if (doIwhite) {
            object = (byte) 0xff;
            backg = (byte) 0;
        } else {
            object = (byte) 0;
            backg = (byte) 0xff;
        }

        Roi roi = new OvalRoi(0, 0, radiusx2, radiusx2);
        for (int y = 0; y < h; y++) {
            IJ.showProgress((double) (y) / (h - 1)); // this method is slow, so let's show the progress bar
            int roiy = y - radius;
            for (int x = 0; x < w; x++) {
                roi.setLocation(x - radius, roiy);
                ip.setRoi(roi);
                position = x + y * w;
                int[] data = ip.getHistogram();

                // Find the window's local value range and build a compact histogram over it
                int localMin = 0;
                while (localMin < data.length && data[localMin] == 0)
                    localMin++;
                int threshold;
                if (localMin == data.length) {
                    // Empty window (possible with ROIs completely outside the image)
                    threshold = 0;
                } else {
                    int localMax = data.length - 1;
                    while (data[localMax] == 0)
                        localMax--;
                    int range = localMax - localMin + 1;
                    int[] compact = new int[range];
                    System.arraycopy(data, localMin, compact, 0, range);
                    threshold = NBinsAutoThresholder.getThreshold(AutoThresholdMethod.Otsu, compact);
                    threshold += localMin;
                }
                threshold = threshold - c_value;
                if (threshold < 0)
                    threshold = 0;
                if (threshold > 65535)
                    threshold = 65535;
                outPixels[position] = ((pixels[position] & 0xFFFF) > threshold || (pixels[position] & 0xFFFF) == 65535) ? object : backg;
            }
        }
    }
}
