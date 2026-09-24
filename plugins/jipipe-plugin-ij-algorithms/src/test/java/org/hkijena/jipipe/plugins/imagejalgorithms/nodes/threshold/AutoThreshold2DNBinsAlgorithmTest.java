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

package org.hkijena.jipipe.plugins.imagejalgorithms.nodes.threshold;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class AutoThreshold2DNBinsAlgorithmTest {

    @Test
    void testBinIndexMapping() {
        // 8-bit range 0..255 into 256 bins: bin index = value
        assertEquals(0, AutoThreshold2DNBinsAlgorithm.binIndex(0, 0, 255, 256));
        assertEquals(255, AutoThreshold2DNBinsAlgorithm.binIndex(255, 0, 255, 256));
        assertEquals(127, AutoThreshold2DNBinsAlgorithm.binIndex(127, 0, 255, 256));
        // 8-bit range into 16 bins: each bin spans 16 values
        assertEquals(0, AutoThreshold2DNBinsAlgorithm.binIndex(15, 0, 255, 16));
        assertEquals(1, AutoThreshold2DNBinsAlgorithm.binIndex(16, 0, 255, 16));
        assertEquals(15, AutoThreshold2DNBinsAlgorithm.binIndex(255, 0, 255, 16));
        // Float range 10.0..20.0 into 10 bins
        assertEquals(0, AutoThreshold2DNBinsAlgorithm.binIndex(10.0, 10.0, 20.0, 10));
        assertEquals(5, AutoThreshold2DNBinsAlgorithm.binIndex(15.0, 10.0, 20.0, 10));
        assertEquals(9, AutoThreshold2DNBinsAlgorithm.binIndex(19.99, 10.0, 20.0, 10));
        assertEquals(9, AutoThreshold2DNBinsAlgorithm.binIndex(20.0, 10.0, 20.0, 10)); // clamped
    }
}
