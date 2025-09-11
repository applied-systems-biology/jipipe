/*
 * Copyright by Zoltán Cseresnyés, Ruman Gerst
 *
 * Research Group Applied Systems Biology - Head: Prof. Dr. Marc Thilo Figge
 * https://www.leibniz-hki.de/en/applied-systems-biology.html
 * HKI-Center for Systems Biology of Infection
 * Leibniz Institute for Natural Product Research and Infection Biology - Hans Knöll Institute (HKI)
 * Adolf-Reichwein-Straße 23, 07745 Jena, Germany
 *
 * The project code is licensed under MIT.
 * See the LICENSE file provided with the code for the full license.
 */

package org.hkijena.jipipe.desktop.app.grapheditor.flavors.compartments.dragdrop;

import org.hkijena.jipipe.JIPipe;
import org.hkijena.jipipe.api.compartments.algorithms.JIPipeProjectCompartment;
import org.hkijena.jipipe.api.nodes.JIPipeGraph;
import org.hkijena.jipipe.desktop.app.grapheditor.commons.JIPipeDesktopGraphCanvasUI;
import org.hkijena.jipipe.desktop.app.grapheditor.commons.JIPipeDesktopGraphDragAndDropBehavior;
import org.hkijena.jipipe.desktop.app.grapheditor.commons.canvas.JIPipeDesktopGraphCanvasGrid;
import org.hkijena.jipipe.desktop.app.grapheditor.commons.canvas.managers.JIPipeDesktopGraphCanvasNotificationsManager;
import org.hkijena.jipipe.desktop.app.grapheditor.flavors.compartments.JIPipeDesktopCompartmentsGraphEditorUI;
import org.hkijena.jipipe.utils.json.JsonUtils;

import java.awt.*;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.Transferable;
import java.awt.dnd.DropTargetDragEvent;
import java.awt.dnd.DropTargetDropEvent;
import java.awt.dnd.DropTargetEvent;

/**
 * Installs Drag and Drop features that create filesystem nodes
 */
public class JIPipeCreateCompartmentNodesFromDraggedDataDragAndDropBehavior implements JIPipeDesktopGraphDragAndDropBehavior {

    private JIPipeDesktopGraphCanvasUI canvas;

    @Override
    public void dragEnter(DropTargetDragEvent dtde) {

    }

    @Override
    public void dragOver(DropTargetDragEvent dtde) {
        Point mousePosition = dtde.getLocation();
        if (mousePosition == null)
            return;
        Point gridLocation = JIPipeDesktopGraphCanvasGrid.realLocationToGrid(mousePosition, canvas.getZoom());
        Point realLocation = JIPipeDesktopGraphCanvasGrid.gridToRealLocation(gridLocation, canvas.getZoom());
        canvas.setGraphEditCursor(realLocation);
        canvas.repaintLowLag();
    }

    @Override
    public void dropActionChanged(DropTargetDragEvent dtde) {

    }

    @Override
    public void dragExit(DropTargetEvent dte) {

    }

    @Override
    public synchronized void drop(DropTargetDropEvent dtde) {
        if (canvas.getDragManagerConnect().getCurrentConnectionDragSource() != null || canvas.getDragManagerConnect().getCurrentConnectionDragTarget() != null) {
            dtde.rejectDrop();
            return;
        }
        try {
            Transferable tr = dtde.getTransferable();
            DataFlavor[] flavors = tr.getTransferDataFlavors();
            boolean accept = false;
            DataFlavor acceptedFlavor = null;
            for (DataFlavor flavor : flavors) {
                if (flavor.isFlavorTextType()) {
                    accept = true;
                    if (acceptedFlavor == null)
                        acceptedFlavor = flavor;
                }
            }
            if (accept) {
                dtde.acceptDrop(dtde.getDropAction());
                Object transferData = tr.getTransferData(acceptedFlavor);
                if (transferData instanceof String text) {
                    processDrop(text);
                    dtde.dropComplete(true);
                } else {
                    accept = false;
                }
            }
            if (!accept) {
                showErrorMessage();
            }
            return;
        } catch (Throwable t) {
            t.printStackTrace();
            showErrorMessage();
        }
        dtde.rejectDrop();
    }

    private void showErrorMessage() {
        canvas.getNotificationsManager().addNotification(
                "Only compartments can be dropped into this graph",
                JIPipe.RESOURCES.getIcon16("actions/insert-object.png"),
                JIPipeDesktopGraphCanvasNotificationsManager.NotificationType.Error
        );
    }


    /**
     * Processes drop as serializable (nodes)
     *
     * @param text json
     */
    private void processDrop(String text) {
        try {
            if (text != null) {
                boolean droppedEmptyGraph = false;
                try {
                    canvas.getNodeManager().pasteNodes(text);
                } catch (NullPointerException ignored) {
                    droppedEmptyGraph = true;
                }

                // Process the "create compartment" drop
                JIPipeGraph graph = JsonUtils.getObjectMapper().readValue(text, JIPipeGraph.class);
                if (graph.getGraphNodes().stream().anyMatch(node -> node instanceof JIPipeProjectCompartment)) {
                    ((JIPipeDesktopCompartmentsGraphEditorUI) canvas.getGraphEditorUI()).addCompartment();
                } else if (droppedEmptyGraph) {
                    throw new NullPointerException("Empty graph dropped");
                }
            }
        } catch (Exception e) {
            canvas.getNotificationsManager().addNotification(
                    "The dropped item is no valid node/graph.",
                    JIPipe.RESOURCES.getIcon16("actions/insert-object.png"),
                    JIPipeDesktopGraphCanvasNotificationsManager.NotificationType.Error
            );
            e.printStackTrace();
        }
    }

    @Override
    public JIPipeDesktopGraphCanvasUI getCanvas() {
        return canvas;
    }

    @Override
    public void setCanvas(JIPipeDesktopGraphCanvasUI canvas) {
        this.canvas = canvas;
    }
}
