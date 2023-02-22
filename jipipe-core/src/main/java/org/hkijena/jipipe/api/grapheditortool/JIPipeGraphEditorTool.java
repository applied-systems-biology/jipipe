package org.hkijena.jipipe.api.grapheditortool;

import org.hkijena.jipipe.ui.grapheditor.general.JIPipeGraphEditorUI;

import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseListener;
import java.awt.event.MouseMotionListener;

public interface JIPipeGraphEditorTool extends MouseMotionListener, MouseListener {

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

    default Cursor getCursor() {
        return Cursor.getPredefinedCursor(Cursor.DEFAULT_CURSOR);
    }
}
