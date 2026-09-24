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

package org.hkijena.jipipe.utils.threshold;

import ij.process.AutoThresholder;

import java.util.Arrays;

/**
 * Dispatches auto-thresholding for histograms with an arbitrary number of bins.
 * <p>
 * For histograms with at most 256 bins the call is delegated to ImageJ's
 * {@link AutoThresholder} (including its bilevel short-circuit), so results are
 * bit-identical to classic 8-bit thresholding. For larger histograms the
 * populated sub-range (first to last non-zero bin) is extracted once, the
 * matching n-bin implementation from {@link NBinsThresholdMethods} runs on the
 * compact array, and the result is offset back.
 */
public final class NBinsAutoThresholder {

    private static final AutoThresholder IMAGEJ_THRESHOLDER = new AutoThresholder();
    private static final int MAX_FAST_PATH_BINS = 256;

    private NBinsAutoThresholder() {
    }

    /**
     * Calculates a threshold for a histogram of arbitrary bin count.
     *
     * @param method    the method
     * @param histogram the histogram; its length is the bin count
     * @return the threshold bin index
     */
    public static int getThreshold(AutoThresholdMethod method, int[] histogram) {
        if (histogram == null)
            throw new IllegalArgumentException("Histogram is null");
        if (histogram.length <= MAX_FAST_PATH_BINS)
            return IMAGEJ_THRESHOLDER.getThreshold(AutoThresholder.Method.valueOf(method.name()), histogram);

        // Extract the populated sub-range
        int minbin = -1;
        int maxbin = -1;
        for (int i = 0; i < histogram.length; i++) {
            if (histogram[i] > 0) {
                if (minbin < 0) minbin = i;
                maxbin = i;
            }
        }
        if (minbin < 0)
            return 0; // empty histogram
        int[] compact = Arrays.copyOfRange(histogram, minbin, maxbin + 1);

        int threshold = invokePorted(method, compact);
        if (threshold < 0)
            threshold = 0;
        return threshold + minbin;
    }

    /**
     * 8-bit fast path with exact ImageJ semantics (histogram of at most 256 bins).
     *
     * @param method    the method
     * @param histogram the histogram (at most 256 bins)
     * @return the threshold bin index
     */
    public static int getThreshold8U(AutoThresholdMethod method, int[] histogram) {
        if (histogram == null)
            throw new IllegalArgumentException("Histogram is null");
        if (histogram.length > 256)
            throw new IllegalArgumentException("8-bit thresholding requires a histogram with at most 256 bins, got " + histogram.length);
        return IMAGEJ_THRESHOLDER.getThreshold(AutoThresholder.Method.valueOf(method.name()), histogram);
    }

    /**
     * 16-bit thresholding (histogram of at most 65536 bins).
     *
     * @param method    the method
     * @param histogram the histogram (at most 65536 bins)
     * @return the threshold bin index (= pixel value for full-range 16-bit histograms)
     */
    public static int getThreshold16U(AutoThresholdMethod method, int[] histogram) {
        if (histogram == null)
            throw new IllegalArgumentException("Histogram is null");
        if (histogram.length > 65536)
            throw new IllegalArgumentException("16-bit thresholding requires a histogram with at most 65536 bins, got " + histogram.length);
        return getThreshold(method, histogram);
    }

    private static int invokePorted(AutoThresholdMethod method, int[] histogram) {
        switch (method) {
            case Default: return NBinsThresholdMethods.Default(histogram);
            case Huang: return NBinsThresholdMethods.Huang(histogram);
            case Intermodes: return NBinsThresholdMethods.Intermodes(histogram);
            case IsoData: return NBinsThresholdMethods.IsoData(histogram);
            case IJ_IsoData: return NBinsThresholdMethods.IJ_IsoData(histogram);
            case Li: return NBinsThresholdMethods.Li(histogram);
            case MaxEntropy: return NBinsThresholdMethods.MaxEntropy(histogram);
            case Mean: return NBinsThresholdMethods.Mean(histogram);
            case MinError: return NBinsThresholdMethods.MinError(histogram);
            case Minimum: return NBinsThresholdMethods.Minimum(histogram);
            case Moments: return NBinsThresholdMethods.Moments(histogram);
            case Otsu: return NBinsThresholdMethods.Otsu(histogram);
            case Percentile: return NBinsThresholdMethods.Percentile(histogram);
            case RenyiEntropy: return NBinsThresholdMethods.RenyiEntropy(histogram);
            case Shanbhag: return NBinsThresholdMethods.Shanbhag(histogram);
            case Triangle: return NBinsThresholdMethods.Triangle(histogram);
            case Yen: return NBinsThresholdMethods.Yen(histogram);
            default: throw new UnsupportedOperationException("Unknown method: " + method);
        }
    }
}
