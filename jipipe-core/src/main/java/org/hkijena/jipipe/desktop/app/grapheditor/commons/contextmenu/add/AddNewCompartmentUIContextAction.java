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

package org.hkijena.jipipe.desktop.app.grapheditor.commons.contextmenu.add;

import org.hkijena.jipipe.JIPipe;
import org.hkijena.jipipe.api.JIPipeWorkbench;
import org.hkijena.jipipe.api.shortcuts.JIPipeShortcutActions;
import org.hkijena.jipipe.desktop.app.JIPipeDesktopProjectWorkbench;
import org.hkijena.jipipe.desktop.app.grapheditor.addnodepanel.JIPipeDesktopNodeBrowser;
import org.hkijena.jipipe.desktop.app.grapheditor.commons.JIPipeDesktopGraphCanvasUI;
import org.hkijena.jipipe.desktop.app.grapheditor.commons.JIPipeDesktopGraphInteractiveObjectUI;
import org.hkijena.jipipe.desktop.app.grapheditor.commons.contextmenu.GraphInteractiveObjectUIContextAction;
import org.hkijena.jipipe.desktop.app.grapheditor.flavors.compartments.JIPipeDesktopCompartmentsGraphEditorUI;
import org.hkijena.jipipe.utils.ui.JIPipeDesktopDockPanel;

import javax.swing.*;
import java.util.Set;

public class AddNewCompartmentUIContextAction implements GraphInteractiveObjectUIContextAction {

    /**
     * The id of the shortcut action that triggers this context action
     * (see {@link org.hkijena.jipipe.api.shortcuts.JIPipeShortcutActions}).
     */
    public static final String SHORTCUT_ACTION_ID = JIPipeShortcutActions.COMPARTMENT_ADD.id();

    @Override
    public boolean matches(Set<JIPipeDesktopGraphInteractiveObjectUI> selection) {
        return true;
    }

    @Override
    public void run(JIPipeDesktopGraphCanvasUI canvasUI, Set<JIPipeDesktopGraphInteractiveObjectUI> selection) {
        JIPipeWorkbench workbench = canvasUI.getDesktopWorkbench();
        if (workbench instanceof JIPipeDesktopProjectWorkbench) {
            JIPipeDesktopDockPanel dockPanel = canvasUI.getGraphEditorUI().getDockPanel();
            dockPanel.activatePanel(JIPipeDesktopCompartmentsGraphEditorUI.DOCK_ADD_NODES, true);
            SwingUtilities.invokeLater(() -> {
                dockPanel.getPanelComponent(JIPipeDesktopCompartmentsGraphEditorUI.DOCK_ADD_NODES, JIPipeDesktopNodeBrowser.class).focusSearchBar();
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
        return JIPipe.RESOURCES.getIcon16("actions/add.png");
    }

    @Override
    public String getShortcutActionId() {
        return SHORTCUT_ACTION_ID;
    }
}
