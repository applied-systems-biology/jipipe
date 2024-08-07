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

package org.hkijena.jipipe.desktop.app.grapheditor.commons.properties;

import org.hkijena.jipipe.api.validation.JIPipeValidationReport;
import org.hkijena.jipipe.desktop.app.JIPipeDesktopWorkbench;
import org.hkijena.jipipe.desktop.app.JIPipeDesktopWorkbenchPanel;
import org.hkijena.jipipe.desktop.app.grapheditor.commons.AbstractJIPipeDesktopGraphEditorUI;
import org.hkijena.jipipe.desktop.commons.components.JIPipeDesktopFormPanel;
import org.hkijena.jipipe.desktop.commons.components.JIPipeDesktopUserFriendlyErrorUI;
import org.hkijena.jipipe.utils.UIUtils;

import javax.swing.*;
import java.awt.*;

public class JIPipeDesktopGraphEditorErrorPanel extends JIPipeDesktopWorkbenchPanel {

    private final AbstractJIPipeDesktopGraphEditorUI graphEditorUI;
    private JIPipeDesktopUserFriendlyErrorUI errorUI;

    public JIPipeDesktopGraphEditorErrorPanel(JIPipeDesktopWorkbench desktopWorkbench, AbstractJIPipeDesktopGraphEditorUI graphEditorUI) {
        super(desktopWorkbench);
        this.graphEditorUI = graphEditorUI;
        initialize();
    }

    private void initialize() {
        setLayout(new BorderLayout());

        errorUI = new JIPipeDesktopUserFriendlyErrorUI(getDesktopWorkbench(), null, JIPipeDesktopFormPanel.WITH_SCROLLING);
        errorUI.getToolBar().add(UIUtils.createButton("Clear", UIUtils.getIconFromResources("actions/clear-brush.png"), this::clearItems));
        add(errorUI, BorderLayout.CENTER);
    }

    public void clearItems() {
        errorUI.clear();
    }

    public AbstractJIPipeDesktopGraphEditorUI getGraphEditorUI() {
        return graphEditorUI;
    }

    public void setItems(JIPipeValidationReport report) {
        errorUI.clear();
        errorUI.displayErrors(report);
        errorUI.addVerticalGlue();
    }
}
