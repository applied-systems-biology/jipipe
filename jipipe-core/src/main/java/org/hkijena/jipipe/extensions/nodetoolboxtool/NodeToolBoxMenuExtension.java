package org.hkijena.jipipe.extensions.nodetoolboxtool;

import org.hkijena.jipipe.ui.extension.GraphEditorToolBarButtonExtension;
import org.hkijena.jipipe.ui.grapheditor.JIPipeGraphCompartmentUI;
import org.hkijena.jipipe.ui.grapheditor.JIPipeGraphEditorUI;
import org.hkijena.jipipe.utils.UIUtils;

public class NodeToolBoxMenuExtension extends GraphEditorToolBarButtonExtension {

    public NodeToolBoxMenuExtension(JIPipeGraphEditorUI graphEditorUI) {
        super(graphEditorUI);
        setToolTipText("Open a list of all available nodes that can be dragged into the graph");
        setIcon(UIUtils.getIconFromResources("actions/cm_thumbnailsview.png"));
        addActionListener(e -> NodeToolBox.openNewToolBoxWindow());
    }

    @Override
    public boolean isVisibleInGraph() {
        return getGraphEditorUI() instanceof JIPipeGraphCompartmentUI;
    }
}
