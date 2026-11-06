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

package org.hkijena.jipipe.plugins.plots;

import org.junit.platform.launcher.LauncherSession;
import org.junit.platform.launcher.LauncherSessionListener;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.time.Instant;
import java.util.Comparator;

/**
 * Isolates this test JVM's JIPipe profile from the developer's real profile.
 *
 * <p>Every test class that boots the real JIPipe service
 * ({@code JIPipe.ensureInstance()}) would otherwise resolve the developer's
 * profile directory and read and write its {@code settings.json} and
 * {@code runtime-settings.json}: a test that changes settings persists them
 * into the user's real profile, and a stale in-memory snapshot of a test JVM
 * overwrites blocks the user changed in a concurrently running JIPipe instance
 * (work item #1356).
 *
 * <p>This listener runs at launcher-session start, i.e. before any test class,
 * and points {@link org.hkijena.jipipe.utils.PathUtils#getJIPipeUserDirBase()}
 * at a fresh temporary directory via the {@code jipipe.override.user.dir.base}
 * system property. The directory is deleted (best effort) when the session
 * closes. The environment variable {@code JIPIPE_OVERRIDE_USER_DIR_BASE} and
 * an initialization-settings override still take precedence, so a test that
 * redirects the profile itself keeps working.
 *
 * <p>Registered via {@code META-INF/services/org.junit.platform.launcher.LauncherSessionListener}
 * alongside the ImageJ patcher listener. The class is deliberately free of
 * ImageJ and JIPipe classes, so its ordering relative to
 * {@link ImageJLegacyPatcherLauncherSessionListener} cannot matter.
 */
public class TestProfileIsolationLauncherSessionListener implements LauncherSessionListener {

    /**
     * The system property that redirects the JIPipe profile base for the
     * current process (see {@link org.hkijena.jipipe.utils.PathUtils#getJIPipeUserDirBase()}).
     */
    public static final String OVERRIDE_PROPERTY = "jipipe.override.user.dir.base";

    private Path tempBase;

    @Override
    public void launcherSessionOpened(LauncherSession session) {
        cleanUpStaleTempBases();
        if (System.getProperty(OVERRIDE_PROPERTY) != null) {
            return; // already isolated (e.g. by an earlier session in this JVM)
        }
        try {
            tempBase = Files.createTempDirectory("jipipe-test-profile-");
        } catch (IOException e) {
            throw new IllegalStateException("Could not create the isolated test profile directory", e);
        }
        System.setProperty(OVERRIDE_PROPERTY, tempBase.toAbsolutePath().toString());
    }

    @Override
    public void launcherSessionClosed(LauncherSession session) {
        String override = System.getProperty(OVERRIDE_PROPERTY);
        if (override == null || tempBase == null || !override.equals(tempBase.toAbsolutePath().toString())) {
            return; // not ours (or an external mechanism replaced it)
        }
        System.clearProperty(OVERRIDE_PROPERTY);
        deleteRecursivelyQuietly(tempBase);
        tempBase = null;
    }

    /**
     * Best-effort recursive delete of the temporary profile base.
     */
    private static void deleteRecursivelyQuietly(Path root) {
        try (var stream = Files.walk(root)) {
            stream.sorted(Comparator.reverseOrder()).forEach(p -> {
                try {
                    Files.deleteIfExists(p);
                } catch (IOException ignored) {
                    // best-effort temp cleanup
                }
            });
        } catch (IOException ignored) {
            // the temp directory may already be gone
        }
    }

    /**
     * Deletes leftover profile bases of test JVMs that exited without running
     * {@link #launcherSessionClosed} (a full-app boot path can leave the JVM
     * via {@code Runtime#halt}, which skips both session close and shutdown
     * hooks). Only directories older than 24 hours are removed, so a session
     * that is running concurrently in another JVM is never touched.
     */
    private static void cleanUpStaleTempBases() {
        try (var stream = Files.newDirectoryStream(Path.of(System.getProperty("java.io.tmpdir")), "jipipe-test-profile-*")) {
            Instant cutoff = Instant.now().minus(Duration.ofHours(24));
            for (Path entry : stream) {
                try {
                    if (Files.getLastModifiedTime(entry).toInstant().isBefore(cutoff)) {
                        deleteRecursivelyQuietly(entry);
                    }
                } catch (IOException ignored) {
                    // best-effort temp cleanup
                }
            }
        } catch (IOException ignored) {
            // the temp directory may not exist yet
        }
    }
}
