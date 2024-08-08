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

import org.hkijena.jipipe.desktop.app.grapheditor.addnodepanel.JIPipeDesktopAddNodesPanel;
import org.hkijena.jipipe.desktop.app.grapheditor.commons.JIPipeDesktopGraphCanvasUI;
import org.hkijena.jipipe.desktop.app.grapheditor.commons.nodeui.JIPipeDesktopGraphNodeUI;
import org.hkijena.jipipe.desktop.app.grapheditor.nodefinder.JIPipeDesktopNodeFinderDialogUI;
import org.hkijena.jipipe.desktop.app.grapheditor.flavors.pipeline.JIPipeDesktopPipelineGraphEditorUI;
import org.hkijena.jipipe.utils.UIUtils;
import org.hkijena.jipipe.utils.ui.JIPipeDesktopDockPanel;

import javax.swing.*;
import java.awt.event.KeyEvent;
import java.util.Set;

public class AddNewNodeUIContextAction implements NodeUIContextAction {
    @Override
    public boolean matches(Set<JIPipeDesktopGraphNodeUI> selection) {
        return true;
    }

    @Override
    public void run(JIPipeDesktopGraphCanvasUI canvasUI, Set<JIPipeDesktopGraphNodeUI> selection) {


        if(canvasUI.getGraphEditorUI() instanceof JIPipeDesktopPipelineGraphEditorUI) {
            JIPipeDesktopDockPanel dockPanel = canvasUI.getGraphEditorUI().getDockPanel();
            dockPanel.activatePanel(JIPipeDesktopPipelineGraphEditorUI.DOCK_ADD_NODES, true);
            SwingUtilities.invokeLater(() -> {
                dockPanel.getPanel(JIPipeDesktopPipelineGraphEditorUI.DOCK_ADD_NODES, JIPipeDesktopAddNodesPanel.class).focusSearchBar();
            });
        }
        else {
            JIPipeDesktopNodeFinderDialogUI dialogUI = new JIPipeDesktopNodeFinderDialogUI(canvasUI, null);
            dialogUI.setVisible(true);
        }

    }

    @Override
    public String getName() {
        return "Add new node here ...";
    }

    @Override
    public KeyStroke getKeyboardShortcut() {
        return KeyStroke.getKeyStroke(KeyEvent.VK_A, KeyEvent.SHIFT_MASK, true);
    }

    @Override
    public String getDescription() {
        return "Opens a dialog that allows to search and add a node at the specified location";
    }

    @Override
    public Icon getIcon() {
        return UIUtils.getIconFromResources("actions/add.png");
    }
}
