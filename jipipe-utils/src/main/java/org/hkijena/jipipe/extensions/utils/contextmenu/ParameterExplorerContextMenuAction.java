package org.hkijena.jipipe.extensions.utils.contextmenu;

import org.hkijena.jipipe.ui.grapheditor.general.JIPipeGraphCanvasUI;
import org.hkijena.jipipe.ui.grapheditor.general.contextmenu.NodeUIContextAction;
import org.hkijena.jipipe.ui.grapheditor.general.nodeui.JIPipeGraphNodeUI;
import org.hkijena.jipipe.ui.parameters.ParameterExplorerWindow;
import org.hkijena.jipipe.utils.UIUtils;

import javax.swing.*;
import java.util.Set;

public class ParameterExplorerContextMenuAction implements NodeUIContextAction {
    @Override
    public boolean matches(Set<JIPipeGraphNodeUI> selection) {
        return selection.size() == 1;
    }

    @Override
    public void run(JIPipeGraphCanvasUI canvasUI, Set<JIPipeGraphNodeUI> selection) {
        ParameterExplorerWindow window = new ParameterExplorerWindow(canvasUI.getWorkbench(), selection.iterator().next().getNode());
        window.setSize(1024, 768);
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

}
