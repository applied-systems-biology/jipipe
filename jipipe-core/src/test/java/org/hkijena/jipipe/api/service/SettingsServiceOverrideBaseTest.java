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

package org.hkijena.jipipe.api.service;

import org.hkijena.jipipe.JIPipe;
import org.hkijena.jipipe.api.service.components.JIPipeSettingsServiceComponent;
import org.hkijena.jipipe.api.settings.SettingsScope;
import org.hkijena.jipipe.utils.VersionUtils;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;

/**
 * Pins that a production {@link JIPipeSettingsServiceComponent} resolves its
 * profile directory <em>when it is used</em>, not when it is constructed.
 * <p>
 * <b>The defect this guards (user-data safety).</b> The component is created
 * together with the service — i.e. before the CLI has installed
 * {@code --override-profile}
 * ({@link JIPipeServiceInitializationSettings#setOverrideUserDirBase(Path)}) and
 * before the startup migration runs. Resolving the directory in the
 * constructor caches whatever {@link JIPipe#getJIPipeUserDir(boolean)} returns
 * at that instant (the developer's real profile). The startup then migrates and
 * prepares the <em>override</em> profile (the initializer resolves the
 * directory at initialization time) while the settings documents load and save
 * the <em>real</em> one: one process writing settings into a profile the user
 * did not ask for. Resolving lazily makes both halves see the override.
 * <p>
 * <b>Why this is an honest pin.</b> A bare {@code new JIPipeService()} builds
 * every component without initializing the service (no directory is created and
 * no document is loaded), which is exactly the CLI's construction phase. The
 * test then installs the override on a published instance and only afterwards
 * asks the component for its files: a constructor-time cache can only return
 * the pre-override directory, a lazy resolution returns the override's. This
 * also works in the shared full-suite JVM (no dependence on whether another
 * test booted the service first).
 */
class SettingsServiceOverrideBaseTest {

    private JIPipeService previousInstance;
    private Path tempBase;

    @BeforeEach
    void isolateSingleton() throws IOException {
        // Publish a throwaway instance so the override can be installed, and
        // restore whatever the JVM had before afterwards.
        previousInstance = JIPipe.getInstance();
        tempBase = Files.createTempDirectory("jipipe-settings-override");
    }

    @AfterEach
    void restore() {
        deleteQuietly(tempBase);
        tempBase = null;
        // Leave the JVM's singleton exactly as this test found it.
        restoreSingleton(previousInstance);
    }

    @Test
    void productionComponentResolvesTheProfileDirLazily() {
        // CLI construction phase: the service (and this component) exist,
        // nothing is initialized, the override is not installed yet.
        JIPipeService service = new JIPipeService();
        JIPipeService previous = JIPipe.getInstance();
        publishSingleton(service);
        try {
            JIPipeSettingsServiceComponent component = service.getSettings();
            Path preOverrideDir = JIPipe.getJIPipeUserDir(false);

            // CLI: --override-profile, then the startup migration/load.
            Path expectedProfileDir = tempBase.resolve(VersionUtils.getJIPipeVersion());
            JIPipe.getInstance().getInitializationSettings().setOverrideUserDirBase(tempBase);
            assertEquals(expectedProfileDir, JIPipe.getJIPipeUserDir(false));

            assertEquals(expectedProfileDir.resolve("settings.json"),
                    component.fileFor(SettingsScope.APPLICATION),
                    "the component must resolve the profile directory when it is used, so the override set "
                            + "after construction is honored (otherwise the settings are written to the real profile)");
            assertEquals(expectedProfileDir.resolve("runtime-settings.json"),
                    component.fileFor(SettingsScope.RUNTIME));
            assertNotEquals(preOverrideDir, expectedProfileDir,
                    "sanity: the override must actually change the profile directory");

            // The resolution is cached: the later reset of the override does
            // not move the component's directory mid-session.
            JIPipe.getInstance().getInitializationSettings().setOverrideUserDirBase(null);
            assertEquals(expectedProfileDir.resolve("settings.json"),
                    component.fileFor(SettingsScope.APPLICATION),
                    "the first resolution is cached for the session");
            component.dispose();
        } finally {
            JIPipe.getInstance().getInitializationSettings().setOverrideUserDirBase(null);
            publishSingleton(previous);
        }
    }

    /**
     * The hermetic path is unchanged: an explicit directory is used as-is and
     * the JIPipe user dir is never consulted (the component may exist without a
     * service and without a JIPipe instance).
     */
    @Test
    void forTestingUsesItsExplicitDirectoryAsIs() {
        Path explicit = tempBase.resolve("explicit-test-profile");
        JIPipeSettingsServiceComponent component = JIPipeSettingsServiceComponent.forTesting(explicit);
        try {
            assertEquals(explicit.resolve("settings.json"), component.fileFor(SettingsScope.APPLICATION));
            assertEquals(explicit.resolve("runtime-settings.json"), component.fileFor(SettingsScope.RUNTIME));
        } finally {
            component.dispose();
        }
    }

    /**
     * Publishes the given service as the JIPipe singleton (it may be
     * {@code null}) and returns the one that was published before.
     * <p>
     * The singleton has no public setter (production code publishes it through
     * {@code JIPipe.createInstance}), and this test needs a bare
     * {@link JIPipeService} that is constructed but not initialized. The field
     * is accessed reflectively and always restored, so no other test can see a
     * different instance.
     */
    private static void publishSingleton(JIPipeService service) {
        try {
            java.lang.reflect.Field field = JIPipe.class.getDeclaredField("instance");
            field.setAccessible(true);
            field.set(null, service);
        } catch (ReflectiveOperationException e) {
            throw new IllegalStateException("Could not publish the JIPipe test instance", e);
        }
    }

    private static void restoreSingleton(JIPipeService previous) {
        publishSingleton(previous);
    }

    private static void deleteQuietly(Path root) {
        if (root == null) {
            return;
        }
        try (Stream<Path> stream = Files.walk(root)) {
            stream.sorted(Comparator.reverseOrder()).forEach(p -> {
                try {
                    Files.deleteIfExists(p);
                } catch (IOException ignored) {
                    // best effort
                }
            });
        } catch (IOException ignored) {
            // already gone
        }
    }
}