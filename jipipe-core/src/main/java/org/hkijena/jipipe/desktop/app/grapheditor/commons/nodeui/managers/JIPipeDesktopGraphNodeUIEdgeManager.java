package org.hkijena.jipipe.desktop.app.grapheditor.commons.nodeui.managers;

import org.hkijena.jipipe.JIPipe;
import org.hkijena.jipipe.api.data.JIPipeDataSlot;
import org.hkijena.jipipe.api.data.JIPipeOutputDataSlot;
import org.hkijena.jipipe.api.nodes.JIPipeGraphEdge;
import org.hkijena.jipipe.desktop.app.grapheditor.commons.nodeui.JIPipeDesktopGraphNodeUI;

public class JIPipeDesktopGraphNodeUIEdgeManager {
    private final JIPipeDesktopGraphNodeUI nodeUI;

    public JIPipeDesktopGraphNodeUIEdgeManager(JIPipeDesktopGraphNodeUI nodeUI) {
        this.nodeUI = nodeUI;
    }

    public void setOutputEdgesShape(JIPipeGraphEdge.Shape shape) {
        if (nodeUI.getGraphCanvasUI().getHistoryJournal() != null) {
            nodeUI.getGraphCanvasUI().getHistoryJournal().snapshot("Set edge shape",
                    "Set the shape of all output edges of " + nodeUI.getNode().getDisplayName(),
                    nodeUI.getNode().getCompartmentUUIDInParentGraph(),
                    JIPipe.RESOURCES.getIcon16("actions/eye.png"));
        }
        for (JIPipeOutputDataSlot outputSlot : nodeUI.getNode().getOutputSlots()) {
            for (JIPipeGraphEdge graphEdge : nodeUI.getNode().getParentGraph().getGraph().outgoingEdgesOf(outputSlot)) {
                graphEdge.setUiShape(shape);
            }
        }
        nodeUI.invalidateAndRepaint(false, true);
    }

    public void setInputEdgesShape(JIPipeGraphEdge.Shape shape) {
        if (nodeUI.getGraphCanvasUI().getHistoryJournal() != null) {
            nodeUI.getGraphCanvasUI().getHistoryJournal().snapshot("Set edge edge",
                    "Set the shape of all input edges of " + nodeUI.getNode().getDisplayName(),
                    nodeUI.getNode().getCompartmentUUIDInParentGraph(),
                    JIPipe.RESOURCES.getIcon16("actions/eye.png"));
        }
        for (JIPipeDataSlot inputSlot : nodeUI.getNode().getInputSlots()) {
            for (JIPipeGraphEdge graphEdge : nodeUI.getNode().getParentGraph().getGraph().incomingEdgesOf(inputSlot)) {
                graphEdge.setUiShape(shape);
            }
        }
        nodeUI.invalidateAndRepaint(false, true);
    }


}
