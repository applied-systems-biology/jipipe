package org.hkijena.jipipe.plugins.canvasnotes.tools;

import org.hkijena.jipipe.JIPipe;
import org.hkijena.jipipe.api.grapheditortool.JIPipeDesktopToggleableGraphEditorTool;
import org.hkijena.jipipe.api.grapheditortool.JIPipeDesktopToggleableGraphEditorToolNodeLayerMask;
import org.hkijena.jipipe.desktop.app.grapheditor.commons.JIPipeDesktopGraphEditorUI;
import org.hkijena.jipipe.desktop.app.grapheditor.contextpanel.JIPipeDesktopGraphEditorContextPanelIsland;

import javax.swing.*;
import java.awt.event.MouseEvent;

public class EditNotesGraphNodeTool implements JIPipeDesktopToggleableGraphEditorTool {

    /**
     * The shortcut action id that selects this tool. The id is a stable
     * string literal (persisted in saved keymaps and the LEGACY preset) and
     * the action itself is registered by the {@link org.hkijena.jipipe.plugins.canvasnotes.CanvasNotesPlugin}.
     */
    public static final String SHORTCUT_ACTION_ID = "org.hkijena.jipipe.canvasnotes.tool.notes-edit";

    private JIPipeDesktopGraphEditorUI graphEditor;

    @Override
    public void deactivate() {

    }

    @Override
    public boolean isDeactivateOnRightClick() {
        return false;
    }

    @Override
    public JIPipeDesktopGraphEditorContextPanelIsland createPropertiesPanel(JIPipeDesktopGraphEditorUI graphEditorUI) {
        return new EditNotesGraphNodeToolProperties(graphEditorUI, this);
    }

    @Override
    public JIPipeDesktopToggleableGraphEditorToolNodeLayerMask getNodeLayerMask() {
        return JIPipeDesktopToggleableGraphEditorToolNodeLayerMask.AnnotationsOnly;
    }

    @Override
    public String getName() {
        return "Edit canvas notes";
    }

    @Override
    public String getTooltip() {
        return "Allows to move, edit, and delete canvas notes while active. "
                + "Tip: hold Ctrl and drag a note to edit it without activating this tool";
    }

    @Override
    public Icon getIcon() {
        return JIPipe.RESOURCES.getIcon16("actions/open-for-editing.png");
    }

    @Override
    public JIPipeDesktopGraphEditorUI getGraphEditor() {
        return graphEditor;
    }

    @Override
    public void setGraphEditor(JIPipeDesktopGraphEditorUI graphEditorUI) {
        this.graphEditor = graphEditorUI;
    }

    @Override
    public void activate() {

    }

    @Override
    public int getCategory() {
        return 64;
    }

    @Override
    public int getPriority() {
        return -5100;
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
