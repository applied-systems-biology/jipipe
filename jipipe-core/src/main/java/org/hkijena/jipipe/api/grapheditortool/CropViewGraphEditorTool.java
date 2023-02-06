package org.hkijena.jipipe.api.grapheditortool;

import org.hkijena.jipipe.ui.grapheditor.general.JIPipeGraphEditorUI;
import org.hkijena.jipipe.utils.UIUtils;

import javax.swing.*;

public class CropViewGraphEditorTool implements JIPipeGraphEditorTool {

    private JIPipeGraphEditorUI graphEditor;

    @Override
    public String getName() {
        return "Center view to nodes";
    }

    @Override
    public String getTooltip() {
        return "Removes large empty areas around the graph.";
    }

    @Override
    public Icon getIcon() {
        return UIUtils.getIconFromResources("actions/view-restore.png");
    }

    @Override
    public JIPipeGraphEditorUI getGraphEditor() {
        return graphEditor;
    }

    @Override
    public void setGraphEditor(JIPipeGraphEditorUI graphEditorUI) {
        this.graphEditor = graphEditorUI;
    }

    @Override
    public void activate() {
        if (graphEditor.getHistoryJournal() != null) {
            graphEditor.getHistoryJournal().snapshot("Center view to nodes",
                    "Apply center view to nodes",
                    graphEditor.getCompartment(),
                    UIUtils.getIconFromResources("actions/view-restore.png"));
        }
        graphEditor.getCanvasUI().crop(true);
    }

    @Override
    public int getCategory() {
        return 10000;
    }
}
