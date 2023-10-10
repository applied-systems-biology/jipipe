package org.hkijena.jipipe.ui.grapheditor.general.contextmenu;

import org.hkijena.jipipe.api.nodes.annotation.JIPipeAnnotationGraphNode;
import org.hkijena.jipipe.ui.grapheditor.general.JIPipeGraphCanvasUI;
import org.hkijena.jipipe.ui.grapheditor.general.nodeui.JIPipeGraphNodeUI;
import org.hkijena.jipipe.utils.UIUtils;

import javax.swing.*;
import java.awt.event.InputEvent;
import java.awt.event.KeyEvent;
import java.util.Set;

public class SendToForegroundUIContextAction implements NodeUIContextAction {
    @Override
    public boolean matches(Set<JIPipeGraphNodeUI> selection) {
        return selection.stream().anyMatch(ui -> !ui.getNode().isUiLocked() && ui.getNode() instanceof JIPipeAnnotationGraphNode);
    }

    @Override
    public void run(JIPipeGraphCanvasUI canvasUI, Set<JIPipeGraphNodeUI> selection) {
        canvasUI.sendSelectionToForeground(selection);
        canvasUI.repaintLowLag();
    }

    @Override
    public String getName() {
        return "Send to foreground";
    }

    @Override
    public String getDescription() {
        return "Sends all selected graph annotations to the foreground (functional and locked nodes are not affected)";
    }

    @Override
    public Icon getIcon() {
        return UIUtils.getIconFromResources("actions/object-order-front.png");
    }

    @Override
    public KeyStroke getKeyboardShortcut() {
        return KeyStroke.getKeyStroke(KeyEvent.VK_PAGE_UP, InputEvent.SHIFT_MASK | InputEvent.CTRL_MASK);
    }
}
