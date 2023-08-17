package org.hkijena.jipipe.ui.grapheditor.general.contextmenu;

import org.hkijena.jipipe.api.JIPipeProject;
import org.hkijena.jipipe.ui.JIPipeProjectWorkbench;
import org.hkijena.jipipe.ui.JIPipeWorkbench;
import org.hkijena.jipipe.ui.grapheditor.general.JIPipeGraphCanvasUI;
import org.hkijena.jipipe.ui.grapheditor.general.nodeui.JIPipeGraphNodeUI;
import org.hkijena.jipipe.ui.grapheditor.nodefinder.JIPipeNodeFinderDialogUI;
import org.hkijena.jipipe.utils.UIUtils;

import javax.swing.*;
import java.util.Set;

public class AddNewCompartmentUIContextAction implements NodeUIContextAction {
    @Override
    public boolean matches(Set<JIPipeGraphNodeUI> selection) {
        return true;
    }

    @Override
    public void run(JIPipeGraphCanvasUI canvasUI, Set<JIPipeGraphNodeUI> selection) {
        JIPipeWorkbench workbench = canvasUI.getWorkbench();
        if(workbench instanceof JIPipeProjectWorkbench) {
            if (!JIPipeProjectWorkbench.canAddOrDeleteNodes(workbench))
                return;
            JIPipeProject project = ((JIPipeProjectWorkbench) workbench).getProject();
            String compartmentName = JOptionPane.showInputDialog(SwingUtilities.getWindowAncestor(canvasUI),
                    "Please enter the name of the compartment",
                    "Compartment");
            if (compartmentName != null && !compartmentName.trim().isEmpty()) {
                if (canvasUI.getHistoryJournal() != null) {
                    canvasUI.getHistoryJournal().snapshotBeforeAddCompartment(compartmentName);
                }
                project.addCompartment(compartmentName);
            }
        }
    }

    @Override
    public String getName() {
        return "Add new compartment here ...";
    }

    @Override
    public String getDescription() {
        return "Adds a new compartment at the specified location";
    }

    @Override
    public Icon getIcon() {
        return UIUtils.getIconFromResources("actions/add.png");
    }
}
