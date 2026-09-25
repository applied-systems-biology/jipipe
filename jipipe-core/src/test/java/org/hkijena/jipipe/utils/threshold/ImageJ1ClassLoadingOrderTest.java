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
import org.hkijena.jipipe.JIPipe;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertNotNull;

/**
 * Regression test for the ImageJ legacy patcher ordering issue:
 * this class initializes ImageJ1 classes ({@link AutoThresholder}). If a later
 * test (e.g. InstrumentationAPITest) creates the SciJava context, the ImageJ2
 * legacy patcher must already have run — enforced by the
 * {@code ImageJLegacyPatcherLauncherSessionListener}, which preinitializes the
 * patcher before any test class is loaded.
 *
 * <p>This test alone cannot reproduce the ordering; it is executed before
 * {@code org.hkijena.jipipe.api.instrumentation.InstrumentationAPITest} when
 * running the suite (alphabetical/dependency order) to guard the regression.
 */
class ImageJ1ClassLoadingOrderTest {

    @Test
    void imageJ1ClassesCanBeLoadedBeforeJIPipeInitialization() {
        // Load ImageJ1 classes first
        AutoThresholder autoThresholder = new AutoThresholder();
        int threshold = autoThresholder.getThreshold(AutoThresholder.Method.Otsu,
                new int[]{0, 5, 10, 5, 0});
        assertNotNull(threshold);

        // Then initialize JIPipe (creates the SciJava context with LegacyService)
        JIPipe.ensureInstance();
        assertNotNull(JIPipe.getInstance());
    }
}
