package org.hkijena.jipipe.ui.grapheditor.general.contextmenu;

import org.hkijena.jipipe.ui.grapheditor.general.JIPipeGraphCanvasUI;
import org.hkijena.jipipe.ui.grapheditor.general.nodeui.JIPipeGraphNodeUI;
import org.hkijena.jipipe.ui.grapheditor.nodefinder.JIPipeNodeFinderDialogUI;
import org.hkijena.jipipe.utils.UIUtils;

import javax.swing.*;
import java.util.Set;

public class AddNewNodeUIContextAction implements NodeUIContextAction {
    @Override
    public boolean matches(Set<JIPipeGraphNodeUI> selection) {
        return true;
    }

    @Override
    public void run(JIPipeGraphCanvasUI canvasUI, Set<JIPipeGraphNodeUI> selection) {
        JIPipeNodeFinderDialogUI dialogUI = new JIPipeNodeFinderDialogUI(canvasUI, null);
        dialogUI.setVisible(true);
    }

    @Override
    public String getName() {
        return "Add new node here ...";
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
