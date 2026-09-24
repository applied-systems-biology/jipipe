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

package org.hkijena.jipipe.plugins.expressions.functions.statistics;

import org.hkijena.jipipe.plugins.expressions.JIPipeExpressionVariablesMap;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class HistogramThresholdFunctionTest {

    private List<Number> histogram(int... values) {
        List<Number> list = new ArrayList<>();
        for (int value : values) list.add(value);
        return list;
    }

    private List<Number> zeroHistogram(int size) {
        List<Number> list = new ArrayList<>();
        for (int i = 0; i < size; i++) list.add(0);
        return list;
    }

    /**
     * The 8-bit variant must reject histograms with more than 256 bins instead of
     * silently truncating them.
     */
    @Test
    void test8BitVariantRejectsOversizedHistogram() {
        HistogramThreshold8BitOtsu function = new HistogramThreshold8BitOtsu();
        List<Number> oversized = zeroHistogram(300);
        assertThrows(IllegalArgumentException.class,
                () -> function.evaluate(List.of(oversized), new JIPipeExpressionVariablesMap()));
    }

    /**
     * The 16-bit variant must reject histograms with more than 65536 bins instead of
     * silently truncating them.
     */
    @Test
    void test16BitVariantRejectsOversizedHistogram() {
        HistogramThreshold16BitOtsu function = new HistogramThreshold16BitOtsu();
        List<Number> oversized = zeroHistogram(65537);
        assertThrows(IllegalArgumentException.class,
                () -> function.evaluate(List.of(oversized), new JIPipeExpressionVariablesMap()));
    }

    /**
     * The generic function must validate nbins: values below 2 are rejected.
     */
    @Test
    void testNbinsValidation() {
        HistogramThresholdOtsu function = new HistogramThresholdOtsu();
        List<Number> histo = histogram(0, 0, 100, 200);
        assertThrows(IllegalArgumentException.class,
                () -> function.evaluate(List.of(histo, 1), new JIPipeExpressionVariablesMap()));
    }

    /**
     * Without nbins the function must behave exactly as before: truncate to 256 bins.
     */
    @Test
    void testLegacyBehaviorTruncatesTo256() {
        HistogramThresholdOtsu function = new HistogramThresholdOtsu();
        List<Number> large = histogram(new int[300]); // 300 zeros, would overflow 256
        large.set(50, 100);
        large.set(200, 100);
        Object result = function.evaluate(List.of(large), new JIPipeExpressionVariablesMap());
        assertEquals(Integer.class, result.getClass());
        int legacy = (int) result;
        // Same computation on a 256-truncated array
        List<Number> truncated = histogram(new int[256]);
        truncated.set(50, 100);
        truncated.set(200, 100);
        assertEquals((int) function.evaluate(List.of(truncated), new JIPipeExpressionVariablesMap()), legacy);
    }

    /**
     * With nbins=16 a 16-bin histogram is evaluated directly.
     */
    @Test
    void testNbinsParameter() {
        HistogramThresholdOtsu function = new HistogramThresholdOtsu();
        List<Number> histo = histogram(0, 0, 100, 200, 0, 0, 0, 0, 0, 0, 200, 100, 0, 0, 0, 0);
        Object result = function.evaluate(List.of(histo, 16), new JIPipeExpressionVariablesMap());
        int threshold = ((Number) result).intValue();
        assertEquals(true, threshold >= 3 && threshold <= 10, "Threshold between modes, got " + threshold);
    }

    /**
     * With nbins=16 a 300-bin histogram is truncated to the first 16 bins.
     */
    @Test
    void testNbinsParameterTruncates() {
        HistogramThresholdOtsu function = new HistogramThresholdOtsu();
        List<Number> large = zeroHistogram(300);
        large.set(2, 100);
        large.set(3, 200);
        large.set(12, 200);
        large.set(13, 100);
        int truncated = ((Number) function.evaluate(List.of(large, 16), new JIPipeExpressionVariablesMap())).intValue();
        List<Number> first16 = new ArrayList<>(large.subList(0, 16));
        int expected = ((Number) function.evaluate(List.of(first16, 16), new JIPipeExpressionVariablesMap())).intValue();
        assertEquals(expected, truncated, "300-bin input with nbins=16 must truncate to the first 16 bins");
    }
}
