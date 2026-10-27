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

import org.junit.platform.launcher.LauncherSession;
import org.junit.platform.launcher.LauncherSessionListener;

/**
 * Keeps ImageJ1's {@code IJ.log} from opening its GUI "Log" window during tests.
 *
 * <p>ImageJ1's {@code IJ.log(String)} lazily creates a visible
 * {@code ij.text.TextWindow} the first time it is called — it only skips window
 * creation when the static {@code logPanel} field is already non-null. Some
 * threshold algorithms log on degenerate input (e.g.
 * {@code ij.process.AutoThresholder.MinErrorI} logs
 * {@code "MinError(I): NaN, not converging"} for certain synthetic histograms),
 * so a test that exercises those paths — like
 * {@link org.hkijena.jipipe.utils.threshold.NBinsAutoThresholderTest} — pops up
 * the Log window over the user's desktop.
 *
 * <p>This listener runs at launcher-session start (before any test) and
 * pre-seeds {@code ij.IJ.logPanel} with a plain, never-shown
 * {@code ij.text.TextPanel} (a lightweight, headless-safe AWT panel that simply
 * accumulates text). ImageJ1 log lines then land in that panel instead of a
 * window.
 *
 * <p>{@link #installHeadlessLogSink()} is idempotent and is also invoked from
 * the test classes that can trigger ImageJ1 logging, so the suppression holds
 * even when the build forks one JVM per test class (where this session listener
 * does not run in every fork).
 *
 * <p>Registered via {@code META-INF/services/org.junit.platform.launcher.LauncherSessionListener}.
 */
public class ImageJHeadlessLogListener implements LauncherSessionListener {

    /**
     * Pre-seeds {@code ij.IJ.logPanel} with a headless {@code ij.text.TextPanel}
     * so {@code IJ.log} never creates the GUI Log window. Safe to call
     * repeatedly; a failure here must never break the test run (at worst the
     * known Log window reappears).
     */
    public static void installHeadlessLogSink() {
        try {
            Class<?> ijClass = Class.forName("ij.IJ");
            java.lang.reflect.Field logPanel = ijClass.getDeclaredField("logPanel");
            logPanel.setAccessible(true);
            if (logPanel.get(null) == null) {
                logPanel.set(null, new ij.text.TextPanel());
            }
        } catch (Throwable t) {
            System.err.println("[jipipe-tests] Could not install the headless ImageJ log sink: " + t);
        }
    }

    @Override
    public void launcherSessionOpened(LauncherSession session) {
        installHeadlessLogSink();
    }
}
