package org.hkijena.jipipe.extensions.nodetoolboxtool;

import org.hkijena.jipipe.api.JIPipeOrganization;
import org.hkijena.jipipe.ui.JIPipeWorkbench;
import org.hkijena.jipipe.ui.extension.GraphEditorToolBarButtonExtension;
import org.hkijena.jipipe.ui.extension.MenuExtension;
import org.hkijena.jipipe.ui.extension.MenuTarget;
import org.hkijena.jipipe.ui.grapheditor.JIPipeGraphCompartmentUI;
import org.hkijena.jipipe.ui.grapheditor.JIPipeGraphEditorUI;
import org.hkijena.jipipe.utils.UIUtils;

import java.awt.event.ActionListener;

public class NodeToolBoxMenuExtension extends GraphEditorToolBarButtonExtension {

    public NodeToolBoxMenuExtension(JIPipeGraphEditorUI graphEditorUI) {
        super(graphEditorUI);
        setToolTipText("Open a list of all available nodes that can be dragged into the graph");
        setIcon(UIUtils.getIconFromResources("actions/cm_thumbnailsview.png"));
        addActionListener(e -> NodeToolBoxWindow.openNewToolBox());
    }

    @Override
    public boolean isVisibleInGraph() {
        return getGraphEditorUI() instanceof JIPipeGraphCompartmentUI;
    }
}
