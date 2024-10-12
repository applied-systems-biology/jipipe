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

package org.hkijena.jipipe.desktop.app.grapheditor.commons.contextmenu;

import org.hkijena.jipipe.api.JIPipeWorkbench;
import org.hkijena.jipipe.desktop.app.JIPipeDesktopProjectWorkbench;
import org.hkijena.jipipe.desktop.app.grapheditor.addnodepanel.JIPipeDesktopAddNodesPanel;
import org.hkijena.jipipe.desktop.app.grapheditor.commons.JIPipeDesktopGraphCanvasUI;
import org.hkijena.jipipe.desktop.app.grapheditor.commons.nodeui.JIPipeDesktopGraphNodeUI;
import org.hkijena.jipipe.desktop.app.grapheditor.flavors.compartments.JIPipeDesktopCompartmentsGraphEditorUI;
import org.hkijena.jipipe.utils.UIUtils;
import org.hkijena.jipipe.utils.ui.JIPipeDesktopDockPanel;

import javax.swing.*;
import java.awt.event.KeyEvent;
import java.util.Set;

public class AddNewCompartmentUIContextAction implements NodeUIContextAction {
    @Override
    public boolean matches(Set<JIPipeDesktopGraphNodeUI> selection) {
        return true;
    }

    @Override
    public void run(JIPipeDesktopGraphCanvasUI canvasUI, Set<JIPipeDesktopGraphNodeUI> selection) {
        JIPipeWorkbench workbench = canvasUI.getDesktopWorkbench();
        if (workbench instanceof JIPipeDesktopProjectWorkbench) {
            JIPipeDesktopDockPanel dockPanel = canvasUI.getGraphEditorUI().getDockPanel();
            dockPanel.activatePanel(JIPipeDesktopCompartmentsGraphEditorUI.DOCK_ADD_NODES, true);
            SwingUtilities.invokeLater(() -> {
                dockPanel.getPanelComponent(JIPipeDesktopCompartmentsGraphEditorUI.DOCK_ADD_NODES, JIPipeDesktopAddNodesPanel.class).focusSearchBar();
            });
        }
    }

    @Override
    public String getName() {
        return "Add new node here ...";
    }

    @Override
    public String getDescription() {
        return "Adds a new node at the specified location";
    }

    @Override
    public Icon getIcon() {
        return UIUtils.getIconFromResources("actions/add.png");
    }

    @Override
    public KeyStroke getKeyboardShortcut() {
        return KeyStroke.getKeyStroke(KeyEvent.VK_A, KeyEvent.SHIFT_MASK, true);
    }
}
