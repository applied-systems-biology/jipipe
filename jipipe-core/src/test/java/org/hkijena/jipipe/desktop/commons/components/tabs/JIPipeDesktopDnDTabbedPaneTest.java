/* Copyright by Zoltán Cseresnyés, Ruman Gerst
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

package org.hkijena.jipipe.desktop.commons.components.tabs;

import org.junit.jupiter.api.Test;

import javax.swing.*;
import java.awt.Point;
import java.awt.datatransfer.StringSelection;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Tests for the drag and drop guards of the DnD tabbed pane.
 * <p>
 * Background: a ghost glass pane that stays visible after a tab drag hijacks
 * every subsequent drag and drop operation in the window. During such a
 * hijack, an empty pane threw IndexOutOfBoundsException ("Index: -1, Tab
 * count: 0") for every drag motion event and unrelated drops never reached
 * their targets (folder imports, node template drags).
 */
public class JIPipeDesktopDnDTabbedPaneTest {

    /**
     * The DnD wiring installed by the production constructor (the glass pane
     * drop target and the drag gesture recognizer) requires a display: the
     * AWT classes throw HeadlessException in a headless environment (CI has
     * no X11). The logic under test here (target index computation, drop
     * handling, glass pane lifecycle) is display-independent, so the test
     * pane skips only the DnD wiring.
     */
    private static final class TestTabbedPane extends JIPipeDesktopDnDTabbedPane {

        TestTabbedPane() {
            super(JTabbedPane.TOP);
        }

        @Override
        protected void installDragAndDrop() {
            // AWT DnD requires a display; not under test here
        }
    }

    @Test
    public void targetTabIndexOnEmptyPaneReturnsNegativeOneWithoutThrowing() {
        JIPipeDesktopDnDTabbedPane tabbedPane = new TestTabbedPane();
        assertDoesNotThrow(() -> assertEquals(-1, tabbedPane.getTargetTabIndex(new Point(10, 10))));
    }

    @Test
    public void targetTabIndexOutsideTabsReturnsNegativeOne() {
        JIPipeDesktopDnDTabbedPane tabbedPane = new TestTabbedPane();
        tabbedPane.addTab("Tab", null, new JPanel(), "Tip");
        assertEquals(-1, tabbedPane.getTargetTabIndex(new Point(10000, 10000)));
    }

    @Test
    public void foreignDropOnLeakedGlassPaneIsRejectedAndHidesIt() {
        JRootPane rootPane = new JRootPane();
        JIPipeDesktopDnDTabbedPane tabbedPane = new TestTabbedPane();
        rootPane.setContentPane(tabbedPane);
        // Simulate the leak: a tab drag started and ended without a drop on
        // the glass pane, leaving it visible over the whole window
        tabbedPane.initGlassPane(new Point(5, 5));
        assertTrue(rootPane.getGlassPane().isVisible());
        boolean completed = tabbedPane.performDrop(new StringSelection("file.txt"), new Point(10, 10));
        assertFalse(completed);
        assertFalse(rootPane.getGlassPane().isVisible());
        assertEquals(-1, tabbedPane.dragTabIndex);
    }

    @Test
    public void tabDropStillConvertsAndHidesTheGlassPane() {
        JRootPane rootPane = new JRootPane();
        JIPipeDesktopDnDTabbedPane tabbedPane = new TestTabbedPane();
        rootPane.setContentPane(tabbedPane);
        tabbedPane.addTab("Tab", null, new JPanel(), "Tip");
        tabbedPane.initGlassPane(new Point(5, 5));
        tabbedPane.dragTabIndex = 0;
        boolean completed = tabbedPane.performDrop(
                new JIPipeDesktopDnDTabbedPaneTabTransferable(tabbedPane), new Point(10000, 10000));
        assertTrue(completed);
        assertFalse(rootPane.getGlassPane().isVisible());
        assertEquals(-1, tabbedPane.dragTabIndex);
        assertEquals(1, tabbedPane.getTabCount());
    }
}
