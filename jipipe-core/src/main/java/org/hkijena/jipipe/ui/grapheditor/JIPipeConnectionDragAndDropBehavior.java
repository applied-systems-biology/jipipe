package org.hkijena.jipipe.ui.grapheditor;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.hkijena.jipipe.api.algorithm.JIPipeGraph;
import org.hkijena.jipipe.api.algorithm.JIPipeGraphNode;
import org.hkijena.jipipe.api.data.JIPipeDataSlot;
import org.hkijena.jipipe.api.data.JIPipeSlotType;
import org.hkijena.jipipe.utils.JsonUtils;

import javax.swing.*;
import java.awt.*;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.StringSelection;
import java.awt.datatransfer.Transferable;
import java.awt.dnd.*;

/**
 * Adds connection drag & drop behavior to a data slot
 */
public class JIPipeConnectionDragAndDropBehavior implements DropTargetListener, DragGestureListener {

    private final JIPipeDataSlotUI slotUI;
    private final JButton assignButton;

    /**
     * Adds connection drag & drop behavior to a data slot
     *
     * @param slotUI       the slot UI
     * @param assignButton the assign button
     */
    public JIPipeConnectionDragAndDropBehavior(JIPipeDataSlotUI slotUI, JButton assignButton) {
        this.slotUI = slotUI;
        this.assignButton = assignButton;
        DragSource dragSource = new DragSource();
        dragSource.createDefaultDragGestureRecognizer(
                assignButton,
                DnDConstants.ACTION_LINK,
                this
        );
        dragSource.createDefaultDragGestureRecognizer(
                slotUI,
                DnDConstants.ACTION_LINK,
                this
        );
        new DropTarget(slotUI, this);
    }

    @Override
    public void dragEnter(DropTargetDragEvent dtde) {

    }

    @Override
    public void dragOver(DropTargetDragEvent dtde) {

    }

    @Override
    public void dropActionChanged(DropTargetDragEvent dtde) {

    }

    @Override
    public void dragExit(DropTargetEvent dte) {

    }

    @Override
    public void drop(DropTargetDropEvent dtde) {
        try {
            Transferable tr = dtde.getTransferable();
            DataFlavor[] flavors = tr.getTransferDataFlavors();
            for (int i = 0; i < flavors.length; i++) {
                if (flavors[i].isFlavorTextType()) {
                    Object transferData = tr.getTransferData(flavors[i]);
                    if (!(transferData instanceof String))
                        continue;
                    dtde.acceptDrop(dtde.getDropAction());
                    JsonNode node = JsonUtils.getObjectMapper().readValue((String) transferData, JsonNode.class);
                    JsonNode graphNodeLink = node.path("node");
                    JsonNode graphSlotLink = node.path("slot");
                    JsonNode graphSlotTypeLink = node.path("slot-type");
                    if (!graphNodeLink.isMissingNode() && !graphSlotLink.isMissingNode() && !graphSlotTypeLink.isMissingNode()) {
                        JIPipeGraphNode graphNode = slotUI.getGraph().getAlgorithmNodes().getOrDefault(graphNodeLink.asText(), null);
                        if (graphNode != null) {
                            JIPipeSlotType slotType = JIPipeSlotType.valueOf(graphSlotTypeLink.asText());
                            JIPipeDataSlot secondSlot = null;
                            if (slotType == JIPipeSlotType.Input) {
                                secondSlot = graphNode.getInputSlot(graphSlotLink.asText());
                            } else if (slotType == JIPipeSlotType.Output) {
                                secondSlot = graphNode.getOutputSlot(graphSlotLink.asText());
                            }
                            if (secondSlot != null) {
                                connectSlots(slotUI.getSlot(), secondSlot);
                            }
                        }
                    }

                    dtde.dropComplete(true);
                }
            }
            return;
        } catch (Throwable t) {
            t.printStackTrace();
        }
        dtde.rejectDrop();
    }

    private void connectSlots(JIPipeDataSlot firstSlot, JIPipeDataSlot secondSlot) {
        JIPipeGraph graph = firstSlot.getNode().getGraph();
        if (graph != secondSlot.getNode().getGraph())
            return;
        if (firstSlot.isInput() != secondSlot.isInput()) {
            if (firstSlot.isInput()) {
                graph.connect(secondSlot, firstSlot);
            } else {
                graph.connect(firstSlot, secondSlot);
            }
        }
    }

    @Override
    public void dragGestureRecognized(DragGestureEvent dge) {
        ObjectNode node = JsonUtils.getObjectMapper().getNodeFactory().objectNode();
        node.put("node", slotUI.getSlot().getNode().getIdInGraph());
        node.put("slot", slotUI.getSlot().getName());
        node.put("slot-type", slotUI.getSlot().getSlotType().name());
        try {
            String string = JsonUtils.getObjectMapper().writeValueAsString(node);
            StringSelection selection = new StringSelection(string);
            dge.startDrag(Cursor.getPredefinedCursor(Cursor.CROSSHAIR_CURSOR),
                    selection);
        } catch (JsonProcessingException e) {
            e.printStackTrace();
        }
    }
}
