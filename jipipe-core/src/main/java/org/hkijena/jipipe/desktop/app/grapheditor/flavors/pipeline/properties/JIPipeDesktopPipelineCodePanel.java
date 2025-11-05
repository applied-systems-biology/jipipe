package org.hkijena.jipipe.desktop.app.grapheditor.flavors.pipeline.properties;

import org.hkijena.jipipe.JIPipe;
import org.hkijena.jipipe.api.nodes.JIPipeScriptAlgorithm;
import org.hkijena.jipipe.desktop.app.codeeditor.JIPipeDesktopCodeEditorUI;
import org.hkijena.jipipe.desktop.app.codeeditor.JIPipeDesktopParameterCodeEditorDocument;
import org.hkijena.jipipe.desktop.app.grapheditor.commons.JIPipeDesktopGraphCanvasUI;
import org.hkijena.jipipe.desktop.app.grapheditor.commons.JIPipeDesktopGraphEditorUI;
import org.hkijena.jipipe.desktop.app.grapheditor.commons.JIPipeDesktopGraphInteractiveObjectUI;
import org.hkijena.jipipe.desktop.app.grapheditor.commons.canvas.events.JIPipeDesktopGraphCanvasUINodeSelectionChangedEvent;
import org.hkijena.jipipe.desktop.app.grapheditor.commons.canvas.events.JIPipeDesktopGraphCanvasUINodeSelectionChangedEventListener;
import org.hkijena.jipipe.desktop.app.grapheditor.commons.nodeui.JIPipeDesktopGraphNodeUI;
import org.hkijena.jipipe.plugins.parameters.ui.api.JIPipeDesktopScriptParameterEditorUI;
import org.hkijena.jipipe.utils.UIUtils;
import org.hkijena.jipipe.utils.ui.JIPipeDesktopDockPanel;

import javax.swing.*;

/**
 * Variant of {@link JIPipeDesktopCodeEditorUI} that automatically loads documents from the currently selected node
 */
public class JIPipeDesktopPipelineCodePanel extends JIPipeDesktopCodeEditorUI implements JIPipeDesktopGraphCanvasUINodeSelectionChangedEventListener {
    private final JIPipeDesktopGraphEditorUI graphEditorUI;
    private final JToggleButton pinDocumentButton = new JToggleButton(JIPipe.RESOURCES.getIcon16("actions/window-pin.png"));

    public JIPipeDesktopPipelineCodePanel(JIPipeDesktopGraphEditorUI graphEditorUI) {
        super(graphEditorUI.getDesktopWorkbench());
        this.graphEditorUI = graphEditorUI;
        graphEditorUI.getSelectionManager().getNodeSelectionChangedEventEmitter().subscribe(this);
        initialize();
    }

    private void initialize() {
        pinDocumentButton.setToolTipText("Keep the current script open even if the node is deselected");
        UIUtils.makeButtonFlat25x25(pinDocumentButton);

        getToolBar().add(Box.createHorizontalGlue());
        getToolBar().add(pinDocumentButton);
    }

    public JIPipeDesktopGraphEditorUI getGraphEditorUI() {
        return graphEditorUI;
    }

    @Override
    public void onGraphCanvasNodeSelectionChanged(JIPipeDesktopGraphCanvasUINodeSelectionChangedEvent event) {
        JIPipeDesktopGraphCanvasUI canvasUI = event.getCanvasUI();
        JIPipeDesktopDockPanel dockPanel = graphEditorUI.getDockPanel();
        if (canvasUI.getSelectionManager().getSelection().size() == 1) {
            JIPipeDesktopGraphInteractiveObjectUI selectedObject = canvasUI.getSelectionManager().getSelection().iterator().next();
            if (selectedObject instanceof JIPipeDesktopGraphNodeUI nodeUI && nodeUI.getNode() instanceof JIPipeScriptAlgorithm scriptAlgorithm) {
                setDocument(new JIPipeDesktopParameterCodeEditorDocument(scriptAlgorithm.getScriptParameterAccess()));
                dockPanel.activatePanel(JIPipeDesktopScriptParameterEditorUI.DOCK_CODE, false);
                return;
            }
        }
        if (getDocument() != null && !pinDocumentButton.isSelected()) {
            setDocument(null);

            // Hide the code panel if required
            if (!dockPanel.getSavedState().getVisibilities().getOrDefault(JIPipeDesktopScriptParameterEditorUI.DOCK_CODE, false)) {
                dockPanel.deactivatePanel(JIPipeDesktopScriptParameterEditorUI.DOCK_CODE, false);
            }
        }
    }
}
