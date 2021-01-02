package org.hkijena.jipipe.ui.extension;

import org.hkijena.jipipe.ui.JIPipeWorkbench;
import org.hkijena.jipipe.ui.grapheditor.JIPipeGraphEditorUI;

import javax.swing.*;

public abstract class GraphEditorToolBarButtonExtension extends JButton {
    private final JIPipeGraphEditorUI graphEditorUI;

    /**
     * Creates a new instance
     *
     * @param graphEditorUI the graph editor
     */
    public GraphEditorToolBarButtonExtension(JIPipeGraphEditorUI graphEditorUI) {
        this.graphEditorUI = graphEditorUI;
    }

    /**
     * Override this method to determine if this button should be available for a graph
     * @return if the button is displayed
     */
    public boolean isVisibleInGraph() {
        return true;
    }

    public JIPipeWorkbench getWorkbench() {
        return graphEditorUI.getWorkbench();
    }

    public JIPipeGraphEditorUI getGraphEditorUI() {
        return graphEditorUI;
    }
}
