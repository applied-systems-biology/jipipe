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

import org.hkijena.jipipe.JIPipe;
import org.hkijena.jipipe.desktop.app.grapheditor.commons.JIPipeDesktopGraphEditorUI;
import org.hkijena.jipipe.desktop.app.grapheditor.commons.canvas.JIPipeDesktopGraphCanvasResources;
import org.hkijena.jipipe.desktop.commons.components.JIPipeDesktopFormPanel;
import org.hkijena.jipipe.utils.UIUtils;

import javax.swing.*;
import java.awt.*;

public class SelectionPanel extends JIPipeDesktopGraphEditorContextPanelIsland {

    private final JIPipeDesktopFormPanel formPanel;

    public SelectionPanel(JIPipeDesktopGraphEditorUI graphEditorUI) {
        super(graphEditorUI);
        this.formPanel = new JIPipeDesktopFormPanel(JIPipeDesktopFormPanel.TRANSPARENT_BACKGROUND);
    }

    @Override
    public void initializeContent() {
        super.initializeContent();
        getContentPanel().add(formPanel, BorderLayout.CENTER);
        formPanel.addWideToForm(UIUtils.createLeftAlignedButton("Clear selection", JIPipe.RESOURCES.getIcon16("actions/message-close.png"), () -> {
            getGraphEditorUI().getSelectionManager().clearSelection();
        }));
    }

    @Override
    protected Icon getTitleIcon() {
        return JIPipe.RESOURCES.getIcon16("actions/gtk-select-all.png");
    }

    @Override
    protected String getTitle() {
        return "Selection (" + getGraphEditorUI().getSelectionManager().getSelection().size() + ")";
    }
}
