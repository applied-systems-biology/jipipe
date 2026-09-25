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

package org.hkijena.jipipe;

import net.imagej.patcher.LegacyInjector;
import org.junit.platform.launcher.LauncherSession;
import org.junit.platform.launcher.LauncherSessionListener;

/**
 * Forces the ImageJ2 legacy patcher to run before any test class is loaded.
 *
 * <p>The legacy patcher ({@link LegacyInjector}) bytecode-patches ImageJ1
 * classes for cross-compatibility with ImageJ2. Patching is only possible
 * while the affected classes are not yet initialized. If a test initializes
 * ImageJ1 classes (e.g. by instantiating {@code ij.process.AutoThresholder})
 * before the SciJava context (which loads {@code LegacyService}) is created,
 * context creation fails with {@code No _hooks field found in ij.IJ}.
 *
 * <p>This listener runs at launcher-session start, i.e. before test discovery
 * and class loading, and preinitializes the patcher — making the test suite
 * independent of test-class ordering.
 *
 * <p>Registered via {@code META-INF/services/org.junit.platform.launcher.LauncherSessionListener}.
 */
public class ImageJLegacyPatcherLauncherSessionListener implements LauncherSessionListener {

    @Override
    public void launcherSessionOpened(LauncherSession session) {
        LegacyInjector.preinit();
    }
}
