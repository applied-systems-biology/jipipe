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

package org.hkijena.jipipe.api.grapheditortool;

import org.hkijena.jipipe.desktop.app.JIPipeDesktopWorkbenchPanel;
import org.hkijena.jipipe.desktop.app.grapheditor.commons.JIPipeDesktopGraphEditorUI;
import org.hkijena.jipipe.utils.ThemeUtils;

import javax.swing.*;
import java.awt.*;

public class JIPipeDesktopToggleableGraphEditorToolPanel<T extends JIPipeDesktopToggleableGraphEditorTool> extends JIPipeDesktopWorkbenchPanel {
    private final JIPipeDesktopGraphEditorUI graphEditorUI;
    private final T tool;
    private final JPanel contentPanel = new JPanel(new BorderLayout());

    public JIPipeDesktopToggleableGraphEditorToolPanel(JIPipeDesktopGraphEditorUI graphEditorUI, T tool) {
        super(graphEditorUI.getDesktopWorkbench());
        this.graphEditorUI = graphEditorUI;
        this.tool = tool;
        initialize();
    }

    private void initialize() {
        setLayout(new BorderLayout(8,8));
        setOpaque(false);

        JLabel titleLabel = new JLabel(tool.getName());
        titleLabel.setFont(new Font(Font.DIALOG, Font.BOLD, ThemeUtils.getCurrentStyle().getFontSizeLarge()));
        titleLabel.setIcon(tool.getIcon());
        add(titleLabel, BorderLayout.NORTH);

        add(contentPanel, BorderLayout.CENTER);
    }

    public T getTool() {
        return tool;
    }

    public JPanel getContentPanel() {
        return contentPanel;
    }

    public JIPipeDesktopGraphEditorUI getGraphEditorUI() {
        return graphEditorUI;
    }
}
