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

package org.hkijena.jipipe.desktop.app.grapheditor.contextpanel;

import org.hkijena.jipipe.desktop.app.JIPipeDesktopWorkbenchPanel;
import org.hkijena.jipipe.desktop.app.grapheditor.commons.JIPipeDesktopGraphEditorUI;
import org.hkijena.jipipe.utils.ThemeUtils;
import org.hkijena.jipipe.utils.UIUtils;

import javax.swing.*;
import java.awt.*;
import java.util.ArrayList;
import java.util.List;

public abstract class JIPipeDesktopGraphEditorContextPanelIsland extends JIPipeDesktopWorkbenchPanel {
    private final JIPipeDesktopGraphEditorUI graphEditorUI;
    private final JPanel contentPanel = new JPanel(new BorderLayout());

    public JIPipeDesktopGraphEditorContextPanelIsland(JIPipeDesktopGraphEditorUI graphEditorUI) {
        super(graphEditorUI.getDesktopWorkbench());
        this.graphEditorUI = graphEditorUI;
    }

    public void initializeContent() {
        setLayout(new BorderLayout(8, 8));
        setOpaque(false);

        JLabel titleLabel = new JLabel(getTitle());
        titleLabel.setFont(new Font(Font.DIALOG, Font.BOLD, ThemeUtils.getCurrentStyle().getFontSizeLarge()));
        titleLabel.setIcon(getTitleIcon());
        titleLabel.setBorder(BorderFactory.createEmptyBorder(8, 8, 12, 8));
        add(titleLabel, BorderLayout.NORTH);

        add(contentPanel, BorderLayout.CENTER);
    }

    public void postInitializeContent() {
        UIUtils.makeNonOpaque(getContentPanel(), true);
    }

    protected abstract Icon getTitleIcon();

    protected abstract String getTitle();

    public JPanel getContentPanel() {
        return contentPanel;
    }

    public JIPipeDesktopGraphEditorUI getGraphEditorUI() {
        return graphEditorUI;
    }

    /**
     * Releases any resources the island holds (e.g. settings bindings opened by
     * a form field of the island). Called by
     * {@link JIPipeDesktopGraphEditorContextPanel} when the island is torn down
     * — on every context-panel rebuild the previous pass's islands are
     * disposed exactly once, because a rebuild replaces every island.
     * <p>
     * Must be idempotent: subclasses that hold a resource close it and null the
     * reference (closing is idempotent), so a repeated call is a no-op.
     */
    public void dispose() {
    }

    /**
     * Disposes the islands of the previous rebuild pass and clears the tracking
     * list in one step, so a rebuild can never dispose an island twice or forget
     * to dispose one: the previous entries are taken, disposed exactly once, and
     * only then is the tracking list cleared.
     *
     * @param tracked the context panel's island list
     * @param <T>     the island type
     */
    public static <T extends JIPipeDesktopGraphEditorContextPanelIsland> void disposePreviousIslands(List<T> tracked) {
        List<T> previous = new ArrayList<>(tracked);
        tracked.clear();
        disposeAll(previous);
    }

    /**
     * Disposes the given islands (see {@link #dispose()}).
     *
     * @param islands the islands to dispose, in order
     */
    public static void disposeAll(List<? extends JIPipeDesktopGraphEditorContextPanelIsland> islands) {
        for (JIPipeDesktopGraphEditorContextPanelIsland island : islands) {
            island.dispose();
        }
    }
}
