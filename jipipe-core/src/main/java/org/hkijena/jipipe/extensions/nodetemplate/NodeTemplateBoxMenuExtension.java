package org.hkijena.jipipe.extensions.nodetemplate;

import org.hkijena.jipipe.ui.extension.GraphEditorToolBarButtonExtension;
import org.hkijena.jipipe.ui.grapheditor.JIPipeGraphEditorUI;
import org.hkijena.jipipe.ui.grapheditor.JIPipePipelineGraphEditorUI;
import org.hkijena.jipipe.utils.UIUtils;

public class NodeTemplateBoxMenuExtension extends GraphEditorToolBarButtonExtension {

    public NodeTemplateBoxMenuExtension(JIPipeGraphEditorUI graphEditorUI) {
        super(graphEditorUI);
        setToolTipText("Open a list of all available nodes that can be dragged into the graph");
        setIcon(UIUtils.getIconFromResources("actions/favorites.png"));
        addActionListener(e -> NodeTemplateBox.openNewToolBoxWindow(graphEditorUI.getWorkbench()));
    }

    @Override
    public boolean isVisibleInGraph() {
        return getGraphEditorUI() instanceof JIPipePipelineGraphEditorUI;
    }
}
