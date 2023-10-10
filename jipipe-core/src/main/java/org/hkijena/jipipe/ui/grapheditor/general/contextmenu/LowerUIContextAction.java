package org.hkijena.jipipe.ui.grapheditor.general.contextmenu;

import org.hkijena.jipipe.api.nodes.annotation.JIPipeAnnotationGraphNode;
import org.hkijena.jipipe.ui.grapheditor.general.JIPipeGraphCanvasUI;
import org.hkijena.jipipe.ui.grapheditor.general.nodeui.JIPipeGraphNodeUI;
import org.hkijena.jipipe.utils.UIUtils;

import javax.swing.*;
import java.awt.event.InputEvent;
import java.awt.event.KeyEvent;
import java.util.Set;

public class LowerUIContextAction implements NodeUIContextAction {
    @Override
    public boolean matches(Set<JIPipeGraphNodeUI> selection) {
        return selection.stream().anyMatch(ui -> !ui.getNode().isUiLocked() && ui.getNode() instanceof JIPipeAnnotationGraphNode);
    }

    @Override
    public void run(JIPipeGraphCanvasUI canvasUI, Set<JIPipeGraphNodeUI> selection) {
        canvasUI.lowerSelection(selection);
        canvasUI.repaintLowLag();
    }

    @Override
    public String getName() {
        return "Lower";
    }

    @Override
    public String getDescription() {
        return "Lowers the Z of all selected graph annotations (functional and locked nodes are not affected)";
    }

    @Override
    public Icon getIcon() {
        return UIUtils.getIconFromResources("actions/object-order-lower.png");
    }

    @Override
    public KeyStroke getKeyboardShortcut() {
        return KeyStroke.getKeyStroke(KeyEvent.VK_PAGE_DOWN, InputEvent.SHIFT_MASK);
    }
}
