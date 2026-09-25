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

package org.hkijena.jipipe.plugins.imagejalgorithms;

import net.imagej.patcher.LegacyInjector;
import org.junit.platform.launcher.LauncherSession;
import org.junit.platform.launcher.LauncherSessionListener;

/**
 * Forces the ImageJ2 legacy patcher to run before any test class of this module
 * is loaded; mirrors {@code org.hkijena.jipipe.ImageJLegacyPatcherLauncherSessionListener}
 * in jipipe-core (a module's test classpath does not include other modules'
 * test classes, so the service registration must be repeated here).
 *
 * <p>Without this, test classes that initialize ImageJ1 classes (e.g. loading
 * an algorithm class that references {@code ij.ImagePlus}) before
 * {@code JIPipe.ensureInstance()} creates the SciJava context cause the
 * legacy patcher to fail ({@code LinkageError: duplicate class definition} /
 * {@code No _hooks field found in ij.IJ}, depending on ordering), corrupting
 * the context so that no node infos are registered.
 *
 * <p>Registered via {@code META-INF/services/org.junit.platform.launcher.LauncherSessionListener}.
 */
public class ImageJLegacyPatcherLauncherSessionListener implements LauncherSessionListener {

    @Override
    public void launcherSessionOpened(LauncherSession session) {
        LegacyInjector.preinit();
    }
}
