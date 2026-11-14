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
import org.hkijena.jipipe.desktop.app.JIPipeDesktopProjectWindow;
import org.hkijena.jipipe.desktop.api.dataviewer.JIPipeDesktopDataViewerWindow;
import org.hkijena.jipipe.desktop.commons.components.tabs.JIPipeDesktopTabPane;
import org.hkijena.jipipe.desktop.app.commandbar.JIPipeDesktopCommandBarModel.Entry;

import javax.swing.Icon;
import java.awt.Component;
import java.awt.Container;
import java.awt.Frame;
import java.awt.Window;
import java.util.ArrayList;
import java.util.List;

/**
 * The window and tab switching command source of the per-window command bar
 * (design Section 4, the ephemeral entries).
 * <p>
 * All entries are built through the single {@link Entry#ephemeral} factory of
 * {@link JIPipeDesktopCommandBarModel}: they are generated at open time,
 * always available, never bindable, and carry no action id.
 * <p>
 * <strong>Window entries</strong> cover every open
 * {@link JIPipeDesktopProjectWindow} and {@link JIPipeDesktopDataViewerWindow}
 * except the focused one (identity comparison). A window is switchable
 * exactly when {@link Window#isDisplayable()} is true: a disposed window has
 * no native peer and is skipped, while a hidden-but-open window (e.g. a
 * minimized frame) is still displayable and therefore still listed (the
 * switch brings it to the front and un-hides it). Membership in the open
 * window sets should already exclude disposed windows; the
 * {@code isDisplayable()} check additionally guards the race in which the
 * command bar is built while a dispose is in progress.
 * <p>
 * <strong>Tab entries</strong> are a shallow recursive walk over the
 * focused window's descendant components: every {@link JIPipeDesktopTabPane}
 * found contributes one entry per tab of its {@code getTabs()} set, in
 * component-tree (pre-order) order. Nested panes (a tab whose content is
 * itself a tab pane) are included, which is the documented behavior of a
 * plain tree walk.
 * <p>
 * The entry construction itself is value-based (title/icon/command in,
 * {@link Entry} out) in the package-private {@code windowEntry}/{@code tabEntry}
 * helpers, so the pure contract (name prefix, category, ephemeral shape) is
 * unit-testable headless; the Swing-reading overloads are thin one-liners
 * that lift values off a live component and delegate.
 */
public final class JIPipeDesktopCommandBarSwitching {
    private JIPipeDesktopCommandBarSwitching() {
    }

    /**
     * @return one entry per open project and data viewer window except
     * {@code focused} (identity) and except non-displayable (disposed)
     * windows, in open-project-windows-then-open-data-viewer-windows order
     * (set iteration order inside each group)
     * @param focused the window the command bar is shown in, may be
     * {@code null} (then no window is excluded)
     */
    public static List<Entry> windowEntries(Window focused) {
        List<Entry> entries = new ArrayList<>();
        for (JIPipeDesktopProjectWindow window : JIPipeDesktopProjectWindow.getOpenWindows()) {
            addWindowEntry(entries, window, focused);
        }
        for (JIPipeDesktopDataViewerWindow window : JIPipeDesktopDataViewerWindow.getOpenWindows()) {
            addWindowEntry(entries, window, focused);
        }
        return List.copyOf(entries);
    }

    /**
     * @return one entry per tab of every {@link JIPipeDesktopTabPane} found
     * in the focused window's component tree, in tree walk (pre-order) order
     * @param focused the window whose component tree is walked, may be
     * {@code null} (then no tabs are found)
     */
    public static List<Entry> tabEntries(Window focused) {
        List<Entry> entries = new ArrayList<>();
        if (focused instanceof Container) {
            collectTabPanes((Container) focused, entries);
        }
        return List.copyOf(entries);
    }

    /**
     * @return the window entries followed by the tab entries for the given
     * window, i.e. {@code combine(windowEntries(focused),
     * tabEntries(focused))}
     * @param focused the window the command bar is shown in, may be {@code
     * null} (see {@link #windowEntries} and {@link #tabEntries})
     */
    public static List<Entry> all(Window focused) {
        return combine(windowEntries(focused), tabEntries(focused));
    }

    /**
     * The single construction site for window rows: name prefix, category,
     * and the ephemeral shape (always available, not configurable, null
     * action id) are pinned here and nowhere else.
     *
     * @param title   the window title (from {@link Frame#getTitle()})
     * @param icon    the row icon, may be {@code null}
     * @param command the runnable that fires the switch
     * @return the row
     */
    static Entry windowEntry(String title, Icon icon, Runnable command) {
        return Entry.ephemeral("Switch to window: " + title, "Windows", icon, command);
    }

    /**
     * Lifts the title off a live frame and delegates to
     * {@link #windowEntry(String, Icon, Runnable)}; the icon is the stable
     * JIPipe app icon and the command brings the frame to the front and
     * gives it focus.
     *
     * @param window the target frame
     * @return the row
     */
    static Entry windowEntry(Frame window) {
        return windowEntry(window.getTitle(),
                JIPipe.RESOURCES.getIcon16("apps/jipipe.png"),
                () -> {
                    window.toFront();
                    window.requestFocus();
                });
    }

    /**
     * The single construction site for tab rows: name prefix, category, and
     * the ephemeral shape are pinned here and nowhere else.
     *
     * @param title   the tab title (from {@link JIPipeDesktopTabPane.DocumentTab#getTitle()})
     * @param icon    the row icon (the tab icon), may be {@code null}
     * @param command the runnable that fires the switch
     * @return the row
     */
    static Entry tabEntry(String title, Icon icon, Runnable command) {
        return Entry.ephemeral("Switch to tab: " + title, "Tabs", icon, command);
    }

    /**
     * Lifts title and icon off a live tab and delegates to
     * {@link #tabEntry(String, Icon, Runnable)}; the command switches the
     * tab's pane to that tab.
     *
     * @param pane the tab's pane
     * @param tab  the target tab
     * @return the row
     */
    static Entry tabEntry(JIPipeDesktopTabPane pane, JIPipeDesktopTabPane.DocumentTab tab) {
        return tabEntry(tab.getTitle(), tab.getIcon(), () -> pane.switchToTab(tab));
    }

    /**
     * Concatenates the window entries and the tab entries: every window row
     * before every tab row, relative order inside each list preserved. This
     * is the pure ordering step that {@link #all(Window)} applies.
     *
     * @param windows the window entries, in their order
     * @param tabs    the tab entries, in their order
     * @return the concatenation
     */
    static List<Entry> combine(List<Entry> windows, List<Entry> tabs) {
        List<Entry> result = new ArrayList<>(windows.size() + tabs.size());
        result.addAll(windows);
        result.addAll(tabs);
        return List.copyOf(result);
    }

    private static void addWindowEntry(List<Entry> entries, Frame window, Window focused) {
        if (window == focused) {
            return;
        }
        if (!window.isDisplayable()) {
            // A disposed window lost its native peer. Normally it is already
            // out of the open window set at that point; this guards the
            // dispose-in-progress race. Hidden-but-open windows (e.g.
            // minimized) are still displayable and intentionally kept.
            return;
        }
        entries.add(windowEntry(window));
    }

    private static void collectTabPanes(Container container, List<Entry> entries) {
        if (container instanceof JIPipeDesktopTabPane tabPane) {
            for (JIPipeDesktopTabPane.DocumentTab tab : tabPane.getTabs()) {
                entries.add(tabEntry(tabPane, tab));
            }
        }
        for (Component child : container.getComponents()) {
            if (child instanceof Container) {
                collectTabPanes((Container) child, entries);
            }
        }
    }
}
