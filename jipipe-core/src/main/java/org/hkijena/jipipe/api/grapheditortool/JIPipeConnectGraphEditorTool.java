package org.hkijena.jipipe.api.grapheditortool;

import org.hkijena.jipipe.ui.grapheditor.general.JIPipeGraphEditorUI;
import org.hkijena.jipipe.utils.UIUtils;

import javax.swing.*;

public class JIPipeConnectGraphEditorTool implements JIPipeToggleableGraphEditorTool {

    private JIPipeGraphEditorUI graphEditorUI;
    @Override
    public String getName() {
        return "Connect slots";
    }

    @Override
    public String getTooltip() {
        return "Only allows the connection of slots without moving nodes around";
    }

    @Override
    public Icon getIcon() {
        return UIUtils.getIconFromResources("actions/lines-connector.png");
    }

    @Override
    public int getPriority() {
        return -9800;
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
        return false;
    }

    @Override
    public boolean allowsDragConnections() {
        return true;
    }
}
