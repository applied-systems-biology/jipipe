package org.hkijena.jipipe.extensions.utils.contextmenu;

import org.hkijena.jipipe.ui.grapheditor.JIPipeGraphCanvasUI;
import org.hkijena.jipipe.ui.grapheditor.JIPipeNodeUI;
import org.hkijena.jipipe.ui.grapheditor.contextmenu.NodeUIContextAction;
import org.hkijena.jipipe.ui.parameters.ParameterExplorerWindow;
import org.hkijena.jipipe.utils.UIUtils;

import javax.swing.*;
import java.util.Set;

public class ParameterExplorerContextMenuAction implements NodeUIContextAction {
    @Override
    public boolean matches(Set<JIPipeNodeUI> selection) {
        return selection.size() == 1;
    }

    @Override
    public void run(JIPipeGraphCanvasUI canvasUI, Set<JIPipeNodeUI> selection) {
        ParameterExplorerWindow window = new ParameterExplorerWindow(canvasUI.getWorkbench(), selection.iterator().next().getNode());
        window.setSize(1024,768);
        window.setLocationRelativeTo(canvasUI.getWorkbench().getWindow());
        window.setVisible(true);
    }

    @Override
    public String getName() {
        return "Explore parameters";
    }

    @Override
    public String getDescription() {
        return "Allows to display technical information about the current parameters. This is useful for creating annotations that contain parameter info.";
    }

    @Override
    public Icon getIcon() {
        return UIUtils.getIconFromResources("actions/dialog-icon-preview.png");
    }

    @Override
    public boolean isShowingInOverhang() {
        return true;
    }
}
