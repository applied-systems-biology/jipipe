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

import org.hkijena.jipipe.desktop.app.commandbar.JIPipeDesktopCommandBarModel.Entry;
import org.junit.jupiter.api.Test;

import javax.swing.Icon;
import java.awt.Component;
import java.awt.Graphics;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Tests for the pure entry-construction and ordering contract of
 * {@link JIPipeDesktopCommandBarSwitching}.
 * <p>
 * These tests exercise the <em>value-based</em> builders
 * ({@code windowEntry(String, Icon, Runnable)} and
 * {@code tabEntry(String, Icon, Runnable)}) and the pure
 * {@code combine(List, List)} that {@code all(Window)} delegates to. They are
 * fully headless: no {@link java.awt.Window} is constructed (top-level windows
 * throw {@code HeadlessException} under CI, which is exactly why the tab-pane
 * dispose test guards on {@code GraphicsEnvironment.isHeadless()}); the Swing
 * walks themselves are thin one-liners that read a title/icon/command off a
 * live component and hand it to these builders, and are user-verified in the
 * app.
 * <p>
 * Pinned contract: window rows are named {@code "Switch to window: <title>"}
 * in category {@code "Windows"}; tab rows are named {@code "Switch to tab:
 * <title>"} in category {@code "Tabs"}; both are ephemeral (never
 * configurable, no action id, no binding, always available, not a header);
 * and {@code all()} orders every window row before every tab row.
 */
class JIPipeDesktopCommandBarSwitchingTest {

    /**
     * A trivial non-null icon so the icon-propagation path is covered without
     * pulling in {@code JIPipe.RESOURCES} (whose class init we keep out of the
     * pure builders so the tests stay decoupled from resource loading).
     */
    private static final Icon TEST_ICON = new Icon() {
        @Override
        public void paintIcon(Component c, Graphics g, int x, int y) {
            // no-op
        }

        @Override
        public int getIconWidth() {
            return 16;
        }

        @Override
        public int getIconHeight() {
            return 16;
        }
    };

    // ---------- window entry shape ----------

    @Test
    void windowEntryPinsNameCategoryAndEphemeralShape() {
        Entry entry = JIPipeDesktopCommandBarSwitching.windowEntry("Project A", TEST_ICON, () -> {
        });

        assertEquals("Switch to window: Project A", entry.name());
        assertEquals("Windows", entry.category());
        assertSame(TEST_ICON, entry.icon());
        // ephemeral contract (see Entry.ephemeral)
        assertFalse(entry.isConfigurable());
        assertNull(entry.actionId());
        assertNull(entry.bindingText());
        assertTrue(entry.variants().isEmpty());
        assertTrue(entry.isAvailable());
        assertFalse(entry.isCategoryHeader());
        assertNotNull(entry.command());
    }

    @Test
    void windowEntryAcceptsNullIcon() {
        Entry entry = JIPipeDesktopCommandBarSwitching.windowEntry("Project B", null, () -> {
        });

        assertEquals("Switch to window: Project B", entry.name());
        assertEquals("Windows", entry.category());
        assertNull(entry.icon());
    }

    // ---------- tab entry shape ----------

    @Test
    void tabEntryPinsNameCategoryAndEphemeralShape() {
        Entry entry = JIPipeDesktopCommandBarSwitching.tabEntry("Graph 1", TEST_ICON, () -> {
        });

        assertEquals("Switch to tab: Graph 1", entry.name());
        assertEquals("Tabs", entry.category());
        assertSame(TEST_ICON, entry.icon());
        assertFalse(entry.isConfigurable());
        assertNull(entry.actionId());
        assertNull(entry.bindingText());
        assertTrue(entry.variants().isEmpty());
        assertTrue(entry.isAvailable());
        assertFalse(entry.isCategoryHeader());
        assertNotNull(entry.command());
    }

    @Test
    void tabEntryAcceptsNullIcon() {
        // Tab icons are routinely null in the app (see the addTab overloads),
        // so the builder must not reject them.
        Entry entry = JIPipeDesktopCommandBarSwitching.tabEntry("Settings", null, () -> {
        });

        assertEquals("Switch to tab: Settings", entry.name());
        assertEquals("Tabs", entry.category());
        assertNull(entry.icon());
    }

    // ---------- all() ordering via the pure combine ----------

    @Test
    void combinePutsEveryWindowBeforeEveryTab() {
        Entry w1 = JIPipeDesktopCommandBarSwitching.windowEntry("W1", null, () -> {
        });
        Entry w2 = JIPipeDesktopCommandBarSwitching.windowEntry("W2", null, () -> {
        });
        Entry t1 = JIPipeDesktopCommandBarSwitching.tabEntry("T1", null, () -> {
        });
        Entry t2 = JIPipeDesktopCommandBarSwitching.tabEntry("T2", null, () -> {
        });

        // all(focused) == combine(windowEntries(focused), tabEntries(focused)),
        // so this pins the windows-then-tabs ordering that all() produces.
        List<Entry> all = JIPipeDesktopCommandBarSwitching.combine(List.of(w1, w2), List.of(t1, t2));

        assertEquals(4, all.size());
        assertSame(w1, all.get(0));
        assertSame(w2, all.get(1));
        assertSame(t1, all.get(2));
        assertSame(t2, all.get(3));
    }

    @Test
    void combineKeepsRelativeOrderWithinEachList() {
        // The relative order inside the window list and inside the tab list
        // must be preserved; combine only fixes the two blocks' relative
        // placement.
        Entry wa = JIPipeDesktopCommandBarSwitching.windowEntry("WA", null, () -> {
        });
        Entry wb = JIPipeDesktopCommandBarSwitching.windowEntry("WB", null, () -> {
        });
        Entry ta = JIPipeDesktopCommandBarSwitching.tabEntry("TA", null, () -> {
        });
        Entry tb = JIPipeDesktopCommandBarSwitching.tabEntry("TB", null, () -> {
        });

        List<Entry> all = JIPipeDesktopCommandBarSwitching.combine(List.of(wb, wa), List.of(tb, ta));

        assertSame(wb, all.get(0));
        assertSame(wa, all.get(1));
        assertSame(tb, all.get(2));
        assertSame(ta, all.get(3));
    }

    @Test
    void combineOfTwoEmptyListsIsEmpty() {
        assertTrue(JIPipeDesktopCommandBarSwitching.combine(List.of(), List.of()).isEmpty());
    }
}
