package org.hkijena.jipipe.desktop.app.grapheditor.contextpanel;

import org.hkijena.jipipe.api.grapheditortool.tools.DefaultGraphEditorTool;
import org.hkijena.jipipe.api.grapheditortool.JIPipeDesktopToggleableGraphEditorTool;
import org.hkijena.jipipe.desktop.app.JIPipeDesktopWorkbenchPanel;
import org.hkijena.jipipe.desktop.app.grapheditor.commons.JIPipeDesktopGraphEditorUI;
import org.hkijena.jipipe.desktop.commons.components.JIPipeDesktopFormPanel;
import org.hkijena.jipipe.utils.UIUtils;

import java.awt.*;

public class JIPipeDesktopGraphEditorContextPanel extends JIPipeDesktopWorkbenchPanel {
    private final JIPipeDesktopGraphEditorUI graphEditorUI;
    private final JIPipeDesktopFormPanel formPanel;

    public JIPipeDesktopGraphEditorContextPanel(JIPipeDesktopGraphEditorUI graphEditorUI) {
        super(graphEditorUI.getDesktopWorkbench());
        this.graphEditorUI = graphEditorUI;
        this.formPanel = new JIPipeDesktopFormPanel(JIPipeDesktopFormPanel.WITH_SCROLLING | JIPipeDesktopFormPanel.TRANSPARENT_BACKGROUND);

        initialize();
    }

    private void initialize() {
        setLayout(new BorderLayout(8,8));
        add(formPanel, BorderLayout.CENTER);
    }

    public JIPipeDesktopGraphEditorUI getGraphEditorUI() {
        return graphEditorUI;
    }

    public void rebuild() {
        formPanel.clear();

        // Add tool settings panel
        JIPipeDesktopToggleableGraphEditorTool currentTool = graphEditorUI.getCurrentTool();
        if(currentTool == null) {
            currentTool = new DefaultGraphEditorTool();
        }
        formPanel.addWideToForm(UIUtils.wrapInBackgroundIslandPanelIfNeeded(currentTool.createPropertiesPanel(graphEditorUI)));
    }
}
