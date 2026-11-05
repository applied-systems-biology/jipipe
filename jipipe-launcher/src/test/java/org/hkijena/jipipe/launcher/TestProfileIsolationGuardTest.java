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

package org.hkijena.jipipe.launcher;

import org.hkijena.jipipe.JIPipe;
import org.junit.jupiter.api.Test;

import java.nio.file.Path;
import java.nio.file.Paths;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Fail-closed guard for the test-suite profile isolation (work item #1356):
 * if the {@link TestProfileIsolationLauncherSessionListener} registration is
 * ever removed (or the {@code jipipe.override.user.dir.base} property check in
 * {@code PathUtils} breaks), test JVMs would silently target the developer's
 * real profile again. This test must fail in that case. Lives in this module
 * (like {@code SettingsDelegateTest}) because its test classes boot the real
 * service against the profile.
 */
class TestProfileIsolationGuardTest {

    @Test
    void testSessionResolvesItsProfileInsideTheIsolatedBase() {
        String override = System.getProperty(TestProfileIsolationLauncherSessionListener.OVERRIDE_PROPERTY);
        assertNotNull(override,
                "the launcher session listener must set " + TestProfileIsolationLauncherSessionListener.OVERRIDE_PROPERTY
                        + "; without it this test JVM reads and writes the real user profile");
        Path userDir = JIPipe.getJIPipeUserDir(false).toAbsolutePath().normalize();
        Path base = Paths.get(override).toAbsolutePath().normalize();
        assertTrue(userDir.startsWith(base),
                "the test JVM must resolve its JIPipe user dir inside the isolated test profile, but got " + userDir);
    }
}
