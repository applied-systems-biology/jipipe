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

public abstract class JIPipeDesktopGraphEditorContextPanelIsland extends JIPipeDesktopWorkbenchPanel {
    private final JIPipeDesktopGraphEditorUI graphEditorUI;
    private final JPanel contentPanel = new JPanel(new BorderLayout());

    public JIPipeDesktopGraphEditorContextPanelIsland(JIPipeDesktopGraphEditorUI graphEditorUI) {
        super(graphEditorUI.getDesktopWorkbench());
        this.graphEditorUI = graphEditorUI;
    }

    public void initializeContent() {
        setLayout(new BorderLayout(8,8));
        setOpaque(false);

        JLabel titleLabel = new JLabel(getTitle());
        titleLabel.setFont(new Font(Font.DIALOG, Font.BOLD, ThemeUtils.getCurrentStyle().getFontSizeLarge()));
        titleLabel.setIcon(getTitleIcon());
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
}
