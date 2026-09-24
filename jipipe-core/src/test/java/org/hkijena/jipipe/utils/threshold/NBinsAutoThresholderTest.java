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
