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

package org.hkijena.jipipe.desktop.app.grapheditor.templatemanage;

import org.hkijena.jipipe.JIPipe;
import org.hkijena.jipipe.api.JIPipeNodeTemplate;
import org.hkijena.jipipe.api.project.JIPipeProject;
import org.hkijena.jipipe.desktop.app.JIPipeDesktopWorkbench;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseEvent;
import java.awt.event.MouseWheelEvent;
import java.util.ArrayList;
import java.util.List;

/**
 * GUI interaction tests for the {@link TemplateManagementDialog} that model-level
 * tests cannot see: the right-click selection spec and mouse-wheel scrolling of the
 * outer page. Both are theme-independent (they assert on selection indices and the
 * scroll position, not on colors), so no theme switching is required. They are
 * skipped on headless environments.
 */
class TemplateManagementDialogInteractionTest {
    static JDialog ownerWindow;

    @BeforeAll
    static void boot() {
        JIPipe.ensureInstance();
    }

    static JIPipeDesktopWorkbench stubWorkbench(JIPipeProject project) {
        return new JIPipeDesktopWorkbench() {
            @Override public JIPipeProject getProject() { return project; }
            @Override public Window getWindow() { return ownerWindow; }
            @Override public org.scijava.Context getContext() { return JIPipe.getInstance().getContext(); }
            @Override public org.hkijena.jipipe.api.notifications.JIPipeNotificationInbox getNotificationInbox() { return null; }
            @Override public void sendStatusBarText(String text) { }
            @Override public void showMessageDialog(String message, String title) { }
            @Override public void showErrorDialog(String message, String title) { }
            @Override public boolean isProjectModified() { return false; }
            @Override public void setProjectModified(boolean modified) { }
            @Override public org.hkijena.jipipe.desktop.commons.components.tabs.JIPipeDesktopTabPane getDocumentTabPane() { return null; }
            @Override public org.hkijena.jipipe.plugins.cef.JIPipeCefClientService getCefClientService() { return null; }
        };
    }

    static void assumeAWT() {
        try {
            Assumptions.assumeTrue(!GraphicsEnvironment.isHeadless(), "headless environment");
        } catch (Throwable t) {
            Assumptions.assumeTrue(false, "AWT not available: " + t);
        }
    }

    static List<JList> collectLists(Container c, List<JList> out) {
        for (Component child : c.getComponents()) {
            if (child instanceof JList l) out.add(l);
            if (child instanceof Container cc) collectLists(cc, out);
        }
        return out;
    }

    /** A real right-click: press (not the popup trigger) then release (the popup trigger, Linux-style). */
    static void rightClick(JList<?> list, int index) {
        Rectangle b = list.getCellBounds(index, index);
        int x = b.x + b.width / 2, y = b.y + b.height / 2;
        long t = System.currentTimeMillis();
        list.dispatchEvent(new MouseEvent(list, MouseEvent.MOUSE_PRESSED, t, 0, x, y, 1, false, MouseEvent.BUTTON3));
        list.dispatchEvent(new MouseEvent(list, MouseEvent.MOUSE_RELEASED, t + 5, 0, x, y, 1, true, MouseEvent.BUTTON3));
    }

    /** Hide any popups the right-click opened so they do not interfere with the next case. */
    static void dismissPopups(Container root) {
        List<JPopupMenu> popups = new ArrayList<>();
        collectPopups(root, popups);
        for (JPopupMenu p : popups) {
            if (p.isVisible()) {
                p.setVisible(false);
            }
        }
    }
    static void collectPopups(Container c, List<JPopupMenu> out) {
        for (Component child : c.getComponents()) {
            if (child instanceof JPopupMenu p) out.add(p);
            if (child instanceof Container cc) collectPopups(cc, out);
        }
    }

    @Test
    @SuppressWarnings("unchecked")
    void rightClickSelectionSpec() throws Exception {
        assumeAWT();
        ownerWindow = new JDialog();
        ownerWindow.setSize(1, 1);
        TemplateManagementDialog dlg = new TemplateManagementDialog(stubWorkbench(null), null);
        dlg.setSize(800, 600);
        dlg.setVisible(true);
        dlg.validate();
        settle();

        JList<JIPipeNodeTemplate> list = (JList<JIPipeNodeTemplate>)
            collectLists(dlg.getContentPane(), new ArrayList<>()).stream()
                .filter(l -> l.getModel().getSize() > 10)
                .findFirst().orElse(null);
        Assumptions.assumeTrue(list != null, "no populated section list found");

        checkCase(dlg, list, "none selected -> right-click selects it", new int[]{}, 2, new int[]{2});
        checkCase(dlg, list, "selection not including item -> right-click selects only it", new int[]{0, 3}, 1, new int[]{1});
        checkCase(dlg, list, "item in multi-selection -> right-click keeps the selection", new int[]{0, 3}, 3, new int[]{0, 3});
        checkCase(dlg, list, "item is sole selection -> right-click keeps it", new int[]{1}, 1, new int[]{1});

        dlg.dispose();
    }

    static void checkCase(TemplateManagementDialog dlg, JList<?> list, String name, int[] preSel, int click, int[] expected) {
        list.clearSelection();
        for (int i : preSel) {
            list.addSelectionInterval(i, i);
        }
        rightClick(list, click);
        int[] actual = list.getSelectedIndices();
        dismissPopups(dlg.getRootPane());
        // A wrong selection is a genuine failure, not a skip.
        Assertions.assertArrayEquals(expected, actual, name + " (right-click " + click + ")");
    }

    @Test
    void wheelOverListScrollsOuterPage() throws Exception {
        assumeAWT();
        ownerWindow = new JDialog();
        ownerWindow.setSize(1, 1);
        TemplateManagementDialog dlg = new TemplateManagementDialog(stubWorkbench(null), null);
        dlg.setSize(800, 600);
        dlg.setVisible(true);
        dlg.validate();
        settle();

        JList<?> list = collectLists(dlg.getContentPane(), new ArrayList<>()).stream()
            .filter(l -> l.getModel().getSize() > 10)
            .findFirst().orElse(null);
        Assumptions.assumeTrue(list != null, "no populated section list found");

        // The nearest JScrollPane ancestor of the list is the form's single outer
        // page scroll (the section list is hosted in a plain panel, not its own
        // scroll pane — a scroll pane there would have swallowed the wheel).
        JScrollPane outer = (JScrollPane) SwingUtilities.getAncestorOfClass(JScrollPane.class, list);
        Assumptions.assumeTrue(outer != null, "no outer scroll pane found");

        int before = outer.getViewport().getViewPosition().y;
        // A downward wheel unit (rotation +1) over the list.
        list.dispatchEvent(new MouseWheelEvent(list, MouseWheelEvent.MOUSE_WHEEL,
            System.currentTimeMillis(), 0, 40, 40, 40, 40, 0, false,
            MouseWheelEvent.WHEEL_UNIT_SCROLL, 1, 1));
        settle();
        int after = outer.getViewport().getViewPosition().y;

        // The wheel MUST move the outer page scroll — a regression here is a
        // genuine failure, not a skip.
        Assertions.assertTrue(after > before,
            "wheel over the list did not scroll the outer page (before=" + before + ", after=" + after + ")");

        dlg.dispose();
    }

    static void settle() throws InterruptedException {
        for (int i = 0; i < 8; i++) {
            Toolkit.getDefaultToolkit().sync();
            Thread.sleep(40);
        }
    }
}
