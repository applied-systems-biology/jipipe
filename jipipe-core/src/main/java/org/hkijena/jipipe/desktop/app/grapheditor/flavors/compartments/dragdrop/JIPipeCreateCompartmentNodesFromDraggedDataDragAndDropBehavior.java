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

import org.hkijena.jipipe.api.compartments.algorithms.JIPipeProjectCompartment;
import org.hkijena.jipipe.api.nodes.JIPipeGraph;
import org.hkijena.jipipe.desktop.app.grapheditor.commons.JIPipeDesktopGraphCanvasUI;
import org.hkijena.jipipe.desktop.app.grapheditor.commons.JIPipeDesktopGraphDragAndDropBehavior;
import org.hkijena.jipipe.desktop.app.grapheditor.flavors.compartments.JIPipeDesktopCompartmentsGraphEditorUI;
import org.hkijena.jipipe.plugins.settings.JIPipeGraphEditorUIApplicationSettings;
import org.hkijena.jipipe.utils.json.JsonUtils;

import javax.swing.*;
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
        Point gridLocation = canvas.getViewMode().realLocationToGrid(mousePosition, canvas.getZoom());
        Point realLocation = canvas.getViewMode().gridToRealLocation(gridLocation, canvas.getZoom());
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
        if (canvas.getCurrentConnectionDragSource() != null || canvas.getCurrentConnectionDragTarget() != null) {
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
                if (transferData instanceof String) {
                    String text = (String) transferData;
                    processDrop(text);
                    dtde.dropComplete(true);
                }
            }
            return;
        } catch (Throwable t) {
            t.printStackTrace();
            showErrorMessage();
        }
        dtde.rejectDrop();
    }

    private void showErrorMessage() {
        if (canvas.getCurrentConnectionDragSource() == null && JIPipeGraphEditorUIApplicationSettings.getInstance().isNotifyInvalidDragAndDrop()) {
            String message = "<html>You probably wanted to drop some data into this graph.<br/>" +
                    "This is not possible, as the <strong>Compartment Graph</strong> only organizes your project into " +
                    "multiple sections.<br/><br/>Please double-click a node inside this graph to edit the pipeline.</html>";
            JOptionPane.showMessageDialog(canvas, new JLabel(message), "Drag & drop not supported", JOptionPane.ERROR_MESSAGE);
        }
    }

    /**
     * Processes the drop as serializable (nodes)
     *
     * @param text json
     */
    private void processDrop(String text) {
        try {
            if (text != null) {
                boolean droppedEmptyGraph = false;
                try {
                    canvas.pasteNodes(text);
                }
                catch (NullPointerException ignored) {
                    droppedEmptyGraph = true;
                }

                // Process the "create compartment" drop
                JIPipeGraph graph = JsonUtils.getObjectMapper().readValue(text, JIPipeGraph.class);
                if(graph.getGraphNodes().stream().anyMatch(node -> node instanceof JIPipeProjectCompartment)) {
                    ((JIPipeDesktopCompartmentsGraphEditorUI)canvas.getGraphEditorUI()).addCompartment();
                }
                else if(droppedEmptyGraph) {
                    throw new NullPointerException("Empty graph dropped");
                }
            }
        } catch (Exception e) {
            if (JIPipeGraphEditorUIApplicationSettings.getInstance().isNotifyInvalidDragAndDrop()) {
                JOptionPane.showMessageDialog(canvas.getDesktopWorkbench().getWindow(),
                        "The dropped string is no valid node/graph.",
                        "Drop nodes",
                        JOptionPane.ERROR_MESSAGE);
                e.printStackTrace();
            }
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
