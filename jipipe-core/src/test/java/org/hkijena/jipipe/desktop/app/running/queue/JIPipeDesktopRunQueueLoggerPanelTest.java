package org.hkijena.jipipe.desktop.app.running.queue;

import org.hkijena.jipipe.JIPipe;
import org.hkijena.jipipe.api.run.JIPipeQueuedRunnableExecutor;
import org.hkijena.jipipe.api.settings.SettingsBinding;
import org.hkijena.jipipe.plugins.settings.SettingsKeys;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.awt.GraphicsEnvironment;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;

/**
 * Pins the construction contract of the run-queue logger panel: the stored
 * show-log setting is applied to the panel's UI at construction time, so the
 * apply must happen AFTER the UI (the logger component) exists. Before the
 * order fix, constructing the panel with a running settings service threw an
 * NPE from {@code updateLoggerVisibility()} (the apply ran before
 * {@code initialize()} created the component) - which crashed project-window
 * startup on tab restore (the compartments/pipeline graph editor hosts a
 * logger panel, created the moment the project loads).
 */
class JIPipeDesktopRunQueueLoggerPanelTest {

    @BeforeAll
    static void boot() {
        try {
            if (GraphicsEnvironment.isHeadless()) {
                Assumptions.assumeTrue(false, "headless environment");
                return;
            }
        } catch (Throwable t) {
            Assumptions.assumeTrue(false, "AWT not available: " + t);
            return;
        }
        JIPipe.ensureInstance();
    }

    /**
     * The stored value is applied at construction time, which is exactly the
     * branch that crashed: with the logger component not built yet, the apply
     * touched it. The test profile's value is restored afterwards.
     */
    @Test
    void constructingWithStoredShowLogFalseDoesNotCrash() {
        SettingsBinding<Boolean> binding = JIPipe.getSettingsService().bind(SettingsKeys.RUN_QUEUE_SHOW_LOG);
        boolean original = binding.get();
        binding.set(false);
        try {
            assertDoesNotThrow(() -> {
                JIPipeDesktopRunQueueLoggerPanel panel =
                        new JIPipeDesktopRunQueueLoggerPanel(null, new JIPipeQueuedRunnableExecutor("test-queue"));
                panel.dispose();
            }, "the stored show-log value is applied to the panel UI at construction - it must not crash");
        } finally {
            binding.set(original);
            binding.close();
        }
    }

    @Test
    void constructingWithStoredShowLogTrueDoesNotCrash() {
        SettingsBinding<Boolean> binding = JIPipe.getSettingsService().bind(SettingsKeys.RUN_QUEUE_SHOW_LOG);
        boolean original = binding.get();
        binding.set(true);
        try {
            assertDoesNotThrow(() -> {
                JIPipeDesktopRunQueueLoggerPanel panel =
                        new JIPipeDesktopRunQueueLoggerPanel(null, new JIPipeQueuedRunnableExecutor("test-queue"));
                panel.dispose();
            }, "the stored show-log value is applied to the panel UI at construction - it must not crash");
        } finally {
            binding.set(original);
            binding.close();
        }
    }
}
