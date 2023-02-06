package org.hkijena.jipipe.api.grapheditortool;

import org.hkijena.jipipe.ui.grapheditor.general.JIPipeGraphEditorUI;

import javax.swing.*;

public interface JIPipeGraphEditorTool {

    String getName();

    String getTooltip();
    Icon getIcon();

    default int getPriority() {
        return 0;
    }

    default boolean supports(JIPipeGraphEditorUI graphEditorUI) {
        return true;
    }

    JIPipeGraphEditorUI getGraphEditor();

    void setGraphEditor(JIPipeGraphEditorUI graphEditorUI);

    void activate();

    int getCategory();
}
