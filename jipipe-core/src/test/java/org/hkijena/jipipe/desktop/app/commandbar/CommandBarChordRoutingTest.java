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

package org.hkijena.jipipe.desktop.app.commandbar;

import org.hkijena.jipipe.JIPipe;
import org.hkijena.jipipe.desktop.app.shortcuts.JIPipeDesktopShortcutDispatcher;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import javax.swing.JFrame;
import javax.swing.KeyStroke;
import java.util.function.Predicate;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;

/**
 * Tests for the command bar's multi-window routing of the dispatcher's
 * single chord-prefix hook (issue #1346). The hook is a static slot
 * application-wide, so the bar routes a pending prefix through its
 * registered instances: the bar whose window owns the keyboard focus
 * accepts, every other bar declines, and the hook itself is installed by
 * the first bar and cleared only when the LAST bar uninstalls (not when
 * any one bar uninstalls while others remain).
 * <p>
 * {@link JIPipe#ensureInstance()} boots the full service stack headlessly
 * the same way {@code CommandBarSettingsTest} does: the module's
 * {@code LauncherSessionListener}s isolate the profile into a temp
 * directory and pre-patch ImageJ, so a frame with a root pane can be
 * constructed without a display.
 */
class CommandBarChordRoutingTest {

    private static final KeyStroke PREFIX = KeyStroke.getKeyStroke("pressed CTRL");

    @BeforeAll
    static void headless() {
        // The frames constructed below are never shown; the focus-manager
        // checks read null windows and decline, so headless is sufficient.
        System.setProperty("java.awt.headless", "true");
        JIPipe.ensureInstance();
    }

    @BeforeEach
    void cleanHook() {
        JIPipeDesktopShortcutDispatcher.setChordPrefixHandler(null);
    }

    @AfterEach
    void releaseHook() {
        JIPipeDesktopShortcutDispatcher.setChordPrefixHandler(null);
    }

    @Test
    void acceptChordPrefixDeclinesWhileUninstalled() {
        // The routing seam must not report acceptance for a bar that is not
        // installed (no host window) or uninstalled after having had one.
        JIPipeDesktopCommandBar bar = new JIPipeDesktopCommandBar();
        assertFalse(bar.acceptChordPrefix(PREFIX),
                "a bar without a host window must decline the prefix");

        JFrame window = new JFrame();
        window.setVisible(false);
        bar.install(window);
        assertFalse(bar.acceptChordPrefix(PREFIX),
                "an installed bar whose window does not own the keyboard focus must decline");
        bar.uninstall();
        assertFalse(bar.acceptChordPrefix(PREFIX),
                "an uninstalled bar must decline the prefix again");
        window.dispose();
    }

    @Test
    void hookRoutesThroughInstalledBarsAndSurvivesOtherUninstalls() {
        JFrame windowA = new JFrame();
        JFrame windowB = new JFrame();
        windowA.setVisible(false);
        windowB.setVisible(false);
        JIPipeDesktopCommandBar barA = new JIPipeDesktopCommandBar();
        JIPipeDesktopCommandBar barB = new JIPipeDesktopCommandBar();
        try {
            barA.install(windowA);
            barB.install(windowB);
            // The first installed bar owns the static hook; the second bar
            // must not replace it.
            assertSame(barA.chordPrefixHandler(), JIPipeDesktopShortcutDispatcher.currentChordPrefixHandler(),
                    "the hook must be owned by the first installed bar");

            // In this test environment no window owns the keyboard focus, so
            // every registered bar declines: the routing hook reports "none
            // accepted" instead of throwing.
            assertFalse(JIPipeDesktopShortcutDispatcher.currentChordPrefixHandler().test(PREFIX),
                    "the routing hook must decline when no installed bar can accept the prefix");

            // Uninstalling one of two bars must keep the SAME hook installed:
            // barB is still registered and must keep receiving prefixes
            // (the hook instance is the one the first bar installed; it now
            // routes through the remaining bar).
            Predicate<KeyStroke> hook = JIPipeDesktopShortcutDispatcher.currentChordPrefixHandler();
            barA.uninstall();
            assertSame(hook, JIPipeDesktopShortcutDispatcher.currentChordPrefixHandler(),
                    "uninstalling one of two bars must not clear the hook");

            // Only when the LAST bar uninstalls does the hook go away.
            barB.uninstall();
            assertNull(JIPipeDesktopShortcutDispatcher.currentChordPrefixHandler(),
                    "uninstalling the last bar must clear the hook");
        } finally {
            barA.uninstall();
            barB.uninstall();
            windowA.dispose();
            windowB.dispose();
        }
    }

    @Test
    void routeChordPrefixDeclinesWhenNoBarIsInstalled() {
        // The package-private seam the hook lambda routes through: with an
        // empty registry of bars it must report "none accepted" rather than
        // throwing.
        assertFalse(JIPipeDesktopCommandBar.routeChordPrefix(PREFIX));
    }

    @Test
    void uninstallingTheOwnerKeepsTheHookWhileOthersRemain() {
        // Regression for the reported failure: with two windows open, the
        // first-constructed bar owned the hook. Uninstalling it (window
        // closed) must NOT clear the hook while barB is still installed -
        // otherwise the remaining window loses chord mode entirely.
        JFrame windowA = new JFrame();
        JFrame windowB = new JFrame();
        windowA.setVisible(false);
        windowB.setVisible(false);
        JIPipeDesktopCommandBar barA = new JIPipeDesktopCommandBar();
        JIPipeDesktopCommandBar barB = new JIPipeDesktopCommandBar();
        try {
            barA.install(windowA);
            Predicate<KeyStroke> hook = JIPipeDesktopShortcutDispatcher.currentChordPrefixHandler();
            barB.install(windowB);
            assertSame(hook, JIPipeDesktopShortcutDispatcher.currentChordPrefixHandler(),
                    "the second install must not replace the hook");

            // The owner (barA, first installed) uninstalls first: the hook
            // must survive.
            barA.uninstall();
            assertSame(hook, JIPipeDesktopShortcutDispatcher.currentChordPrefixHandler(),
                    "uninstalling the hook-owning bar while another bar remains must not clear the hook");

            // The remaining bar still receives prefixes (it declines here
            // only because no window owns the keyboard focus).
            assertFalse(hook.test(PREFIX));

            // Only the last uninstall clears the hook.
            barB.uninstall();
            assertNull(JIPipeDesktopShortcutDispatcher.currentChordPrefixHandler(),
                    "uninstalling the last bar must clear the hook");
        } finally {
            barA.uninstall();
            barB.uninstall();
            windowA.dispose();
            windowB.dispose();
        }
    }
}
