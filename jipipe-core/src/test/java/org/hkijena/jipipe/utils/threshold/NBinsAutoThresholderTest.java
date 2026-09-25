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
import org.junit.jupiter.api.Test;

import java.util.Random;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class NBinsAutoThresholderTest {

    /**
     * On 256-bin histograms every method must return exactly what ImageJ's
     * AutoThresholder returns.
     */
    @Test
    void testFidelityWithImageJOn256Bins() {
        AutoThresholder imageJ = new AutoThresholder();
        Random random = new Random(42);
        for (AutoThresholdMethod method : AutoThresholdMethod.values()) {
            for (int trial = 0; trial < 100; trial++) {
                int[] histogram = randomHistogram(random, 256, trial);
                int expected = imageJ.getThreshold(AutoThresholder.Method.valueOf(method.name()), histogram);
                int actual = NBinsAutoThresholder.getThreshold(method, histogram);
                assertEquals(expected, actual, method + " trial " + trial);
            }
        }
    }

    @Test
    void testNullHistogramRejected() {
        assertThrows(IllegalArgumentException.class,
                () -> NBinsAutoThresholder.getThreshold(AutoThresholdMethod.Otsu, null));
    }

    /**
     * Bilevel histograms (one or two populated bins) must return exactly what ImageJ's
     * {@code AutoThresholder.getThreshold} returns, which applies its bilevel short-circuit
     * for histograms of any length: the second populated bin index minus one (the ImageJ
     * default {@code bilevelSubractOne=true}). This must also hold for >256-bin histograms,
     * where the n-bin path runs instead of the ImageJ fast path.
     */
    @Test
    void testBilevelMatchesImageJOn65536Bins() {
        AutoThresholder imageJ = new AutoThresholder();
        // One populated bin (constant image)
        int[] constant = new int[65536];
        constant[1000] = 500;
        // Two populated, adjacent bins
        int[] adjacent = new int[65536];
        adjacent[1000] = 250;
        adjacent[1001] = 250;
        // Two populated, distant bins
        int[] distant = new int[65536];
        distant[1000] = 512;
        distant[20000] = 512;
        for (AutoThresholdMethod method : AutoThresholdMethod.values()) {
            assertEquals(imageJ.getThreshold(AutoThresholder.Method.valueOf(method.name()), constant.clone()),
                    NBinsAutoThresholder.getThreshold(method, constant.clone()),
                    method + " must match ImageJ on a constant (bilevel-1) 16-bit histogram");
            assertEquals(imageJ.getThreshold(AutoThresholder.Method.valueOf(method.name()), adjacent.clone()),
                    NBinsAutoThresholder.getThreshold(method, adjacent.clone()),
                    method + " must match ImageJ on an adjacent two-value 16-bit histogram");
            assertEquals(imageJ.getThreshold(AutoThresholder.Method.valueOf(method.name()), distant.clone()),
                    NBinsAutoThresholder.getThreshold(method, distant.clone()),
                    method + " must match ImageJ on a distant two-value 16-bit histogram");
        }
    }

    /**
     * Consistency of the n-bin path with the ImageJ fast path on identical data:
     * a fully populated 256-bin histogram goes through the ImageJ fast path when
     * passed directly, and through the n-bin path when embedded into a 65536-bin
     * histogram (the sub-range extraction yields exactly the original array).
     * Both paths must return the same threshold (plus the embedding offset),
     * so deactivating the fast-path switch cannot change results.
     */
    @Test
    void testNBinPathMatchesFastPathOnSameData() {
        Random random = new Random(42);
        for (AutoThresholdMethod method : AutoThresholdMethod.values()) {
            for (int trial = 0; trial < 20; trial++) {
                // Fully populated: sub-range extraction must yield exactly this array
                int[] histogram = new int[256];
                for (int i = 0; i < 256; i++)
                    histogram[i] = 1 + random.nextInt(100);
                if (trial % 2 == 0) { // make it bimodal
                    for (int i = 0; i < 256; i++)
                        histogram[i] += (int) (100 * Math.exp(-Math.pow(i - 60, 2) / 200.0))
                                + (int) (100 * Math.exp(-Math.pow(i - 190, 2) / 200.0));
                }
                int fastPath = NBinsAutoThresholder.getThreshold(method, histogram.clone());
                for (int offset : new int[]{0, 1000, 32000}) {
                    int[] embedded = new int[65536];
                    System.arraycopy(histogram, 0, embedded, offset, 256);
                    int nBinPath = NBinsAutoThresholder.getThreshold(method, embedded);
                    assertEquals(fastPath + offset, nBinPath,
                            method + " trial " + trial + " offset " + offset + ": n-bin path must match fast path");
                }
            }
        }
    }

    /**
     * The n-bin path must be bit-identical to ImageJ's own handling of histograms
     * with more than 256 bins (ImageJ also extracts the populated sub-range and
     * offsets the result back) for arbitrary, zero-containing histograms.
     */
    @Test
    void testNBinPathMatchesImageJOn65536Bins() {
        AutoThresholder imageJ = new AutoThresholder();
        Random random = new Random(7);
        for (AutoThresholdMethod method : AutoThresholdMethod.values()) {
            for (int trial = 0; trial < 5; trial++) {
                int[] histogram = new int[65536];
                int offset = 100 + trial * 977;
                switch (trial % 3) {
                    case 0: { // bimodal with zeros in between
                        for (int i = 0; i < 300; i++)
                            histogram[offset + i] = (int) (100 * Math.exp(-Math.pow(i - 80, 2) / 2000.0))
                                    + (int) (100 * Math.exp(-Math.pow(i - 220, 2) / 2000.0));
                        break;
                    }
                    case 1: { // uniform random with zeros
                        for (int i = 0; i < 800; i += 3)
                            histogram[offset + i] = 1 + random.nextInt(100);
                        break;
                    }
                    default: { // sparse
                        for (int k = 0; k < 20; k++)
                            histogram[offset + random.nextInt(500)] = 1 + random.nextInt(100);
                        break;
                    }
                }
                int expected = imageJ.getThreshold(AutoThresholder.Method.valueOf(method.name()), histogram.clone());
                int actual = NBinsAutoThresholder.getThreshold(method, histogram.clone());
                assertEquals(expected, actual, method + " trial " + trial + " must match ImageJ on a 65536-bin histogram");
            }
        }
    }

    /**
     * 65536-bin histogram with a sparse populated range: threshold must be
     * offset back into the original bin space and stay within the populated range.
     */
    @Test
    void test16BitSparseHistogram() {
        // Populated bins 1000..1200, bimodal: peaks at 1050 and 1150
        int[] histogram = new int[65536];
        for (int i = 1030; i <= 1070; i++) histogram[i] = 200 - Math.abs(i - 1050) * 4;
        for (int i = 1130; i <= 1170; i++) histogram[i] = 200 - Math.abs(i - 1150) * 4;
        int threshold = NBinsAutoThresholder.getThreshold(AutoThresholdMethod.Otsu, histogram);
        // Must stay within the populated range (bins 1030..1170). The valley 1071..1129 is
        // all-zero, so Otsu's BCV is tied across the valley and ImageJ's strict '<' scan
        // returns the first plateau index (1070) -- verified against ImageJ 1.54p.
        assertEquals(true, threshold >= 1030 && threshold <= 1170, "Otsu threshold within populated range, got " + threshold);
        assertEquals(new AutoThresholder().getThreshold(AutoThresholder.Method.Otsu, histogram), threshold,
                "Otsu must match ImageJ on the 16-bit sparse histogram");
        // Translation invariance: shifting the populated range by +1000 must shift the threshold by +1000
        int[] shifted = new int[65536];
        for (int i = 0; i < 65536; i++) shifted[i] = 0;
        for (int i = 1000; i <= 1200; i++) shifted[i + 1000] = histogram[i];
        int shiftedThreshold = NBinsAutoThresholder.getThreshold(AutoThresholdMethod.Otsu, shifted);
        assertEquals(threshold + 1000, shiftedThreshold, "Otsu translation invariance");
    }

    /**
     * Custom bin counts: a bimodal 16-bin histogram must yield a threshold between the modes.
     */
    @Test
    void testCustomBinCount() {
        int[] histogram = new int[16];
        histogram[2] = 100;
        histogram[3] = 200;
        histogram[12] = 200;
        histogram[13] = 100;
        int threshold = NBinsAutoThresholder.getThreshold(AutoThresholdMethod.Otsu, histogram);
        assertEquals(true, threshold >= 3 && threshold <= 12, "Threshold between modes, got " + threshold);
    }

    private int[] randomHistogram(Random random, int bins, int trial) {
        int[] histogram = new int[bins];
        switch (trial % 3) {
            case 0: // bimodal
                for (int i = 0; i < bins; i++)
                    histogram[i] = (int) (100 * Math.exp(-Math.pow(i - 60, 2) / 200.0))
                            + (int) (100 * Math.exp(-Math.pow(i - 190, 2) / 200.0));
                break;
            case 1: // uniform random
                for (int i = 0; i < bins; i++)
                    histogram[i] = random.nextInt(100);
                break;
            default: // sparse
                histogram[random.nextInt(bins)] = 10;
                histogram[random.nextInt(bins)] = 50;
                histogram[random.nextInt(bins)] = 25;
                break;
        }
        return histogram;
    }
}
