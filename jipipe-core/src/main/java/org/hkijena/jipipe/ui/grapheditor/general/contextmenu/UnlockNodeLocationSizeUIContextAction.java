package org.hkijena.jipipe.ui.grapheditor.general.contextmenu;

import org.hkijena.jipipe.api.nodes.JIPipeGraphNode;
import org.hkijena.jipipe.ui.grapheditor.NodeHotKeyStorage;
import org.hkijena.jipipe.ui.grapheditor.general.JIPipeGraphCanvasUI;
import org.hkijena.jipipe.ui.grapheditor.general.nodeui.JIPipeGraphNodeUI;
import org.hkijena.jipipe.utils.TooltipUtils;
import org.hkijena.jipipe.utils.UIUtils;

import javax.swing.*;
import java.awt.*;
import java.awt.event.KeyEvent;
import java.util.Set;
import java.util.UUID;

public class UnlockNodeLocationSizeUIContextAction implements NodeUIContextAction {
    @Override
    public boolean matches(Set<JIPipeGraphNodeUI> selection) {
        return selection.stream().anyMatch(ui -> ui.getNode().isUiLocked());
    }

    @Override
    public void run(JIPipeGraphCanvasUI canvasUI, Set<JIPipeGraphNodeUI> selection) {
        for (JIPipeGraphNodeUI nodeUI : selection) {
            nodeUI.getNode().setUiLocked(false);
        }
        canvasUI.repaintLowLag();
    }

    @Override
    public String getName() {
        return "Unlock location/size";
    }

    @Override
    public String getDescription() {
        return "Unlocks the location and size of all selected nodes";
    }

    @Override
    public Icon getIcon() {
        return UIUtils.getIconFromResources("actions/unlock.png");
    }

    @Override
    public KeyStroke getKeyboardShortcut() {
        return KeyStroke.getKeyStroke(KeyEvent.VK_L, KeyEvent.CTRL_MASK);
    }

}
