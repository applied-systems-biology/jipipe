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

import org.hkijena.jipipe.desktop.app.grapheditor.commons.JIPipeDesktopGraphEditorUI;
import org.hkijena.jipipe.desktop.commons.components.JIPipeDesktopFormPanel;

import java.awt.*;

public class JIPipeDesktopFormGraphEditorToolPanel<T extends JIPipeDesktopToggleableGraphEditorTool> extends JIPipeDesktopGraphEditorToolPanel<T> {

    private final JIPipeDesktopFormPanel formPanel;

    public JIPipeDesktopFormGraphEditorToolPanel(JIPipeDesktopGraphEditorUI graphEditorUI, T tool) {
        super(graphEditorUI, tool);
        this.formPanel = new JIPipeDesktopFormPanel(JIPipeDesktopFormPanel.TRANSPARENT_BACKGROUND);
    }

    @Override
    public void initializeContent() {
        super.initializeContent();
        getContentPanel().add(formPanel, BorderLayout.CENTER);
    }

    public JIPipeDesktopFormPanel getFormPanel() {
        return formPanel;
    }
}
