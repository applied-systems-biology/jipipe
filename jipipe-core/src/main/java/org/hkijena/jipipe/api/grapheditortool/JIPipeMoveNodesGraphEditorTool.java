package org.hkijena.jipipe.api.grapheditortool;

import org.hkijena.jipipe.ui.grapheditor.general.JIPipeGraphEditorUI;
import org.hkijena.jipipe.utils.UIUtils;

import javax.swing.*;
import java.awt.event.MouseEvent;

public class JIPipeMoveNodesGraphEditorTool implements JIPipeToggleableGraphEditorTool {

    private JIPipeGraphEditorUI graphEditorUI;

    @Override
    public String getName() {
        return "Move nodes";
    }

    @Override
    public String getTooltip() {
        return "Only moves nodes without modifying existing connections";
    }

    @Override
    public Icon getIcon() {
        return UIUtils.getIconFromResources("actions/transform-move.png");
    }

    @Override
    public int getPriority() {
        return -9900;
    }

    @Override
    public JIPipeGraphEditorUI getGraphEditor() {
        return graphEditorUI;
    }

    @Override
    public void setGraphEditor(JIPipeGraphEditorUI graphEditorUI) {
        this.graphEditorUI = graphEditorUI;
    }

    @Override
    public void activate() {

    }

    @Override
    public int getCategory() {
        return -10000;
    }

    @Override
    public void deactivate() {

    }

    @Override
    public boolean allowsDragNodes() {
        return true;
    }

    @Override
    public boolean allowsDragConnections() {
        return false;
    }

    @Override
    public void mouseClicked(MouseEvent e) {

    }

    @Override
    public void mousePressed(MouseEvent e) {

    }

    @Override
    public void mouseReleased(MouseEvent e) {

    }

    @Override
    public void mouseEntered(MouseEvent e) {

    }

    @Override
    public void mouseExited(MouseEvent e) {

    }

    @Override
    public void mouseDragged(MouseEvent e) {

    }

    @Override
    public void mouseMoved(MouseEvent e) {

    }
}
