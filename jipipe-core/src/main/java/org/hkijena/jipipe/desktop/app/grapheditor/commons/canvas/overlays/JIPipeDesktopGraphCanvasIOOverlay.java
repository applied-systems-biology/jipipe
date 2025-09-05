package org.hkijena.jipipe.desktop.app.grapheditor.commons.canvas.overlays;

import com.google.common.collect.HashMultimap;
import com.google.common.collect.Multimap;
import org.hkijena.jipipe.api.data.JIPipeDataSlot;
import org.hkijena.jipipe.desktop.app.grapheditor.commons.JIPipeDesktopGraphCanvasUI;
import org.hkijena.jipipe.desktop.app.grapheditor.commons.edgeui.JIPipeDesktopGraphEdgeUI;
import org.hkijena.jipipe.desktop.app.grapheditor.commons.nodeui.JIPipeDesktopGraphNodeUI;
import org.hkijena.jipipe.desktop.app.grapheditor.commons.nodeui.triggers.JIPipeDesktopGraphNodeUISlotActiveArea;
import org.hkijena.jipipe.plugins.settings.JIPipeGraphEditorUIApplicationSettings;
import org.hkijena.jipipe.utils.ThemeUtils;

import java.awt.*;
import java.util.IdentityHashMap;
import java.util.Map;

public class JIPipeDesktopGraphCanvasIOOverlay implements JIPipeDesktopGraphCanvasOverlay {

    private final JIPipeDesktopGraphCanvasUI canvasUI;
    private final JIPipeGraphEditorUIApplicationSettings settings;

    public JIPipeDesktopGraphCanvasIOOverlay(JIPipeDesktopGraphCanvasUI canvasUI) {
        this.canvasUI = canvasUI;
        this.settings = canvasUI.getSettings();
    }

    @Override
    public void paintComponent(Graphics2D g) {

    }

    @Override
    public void paint(Graphics2D g) {
        if (!settings.isDrawLabelsOnHover()) {
            return;
        }

        // Target to sources
        Map<JIPipeDesktopGraphEdgeUI, Integer> edgeIds = new IdentityHashMap<>();
        Multimap<JIPipeDataSlot, JIPipeDesktopGraphEdgeUI> highlightedEdges = HashMultimap.create();

        // Find edges of interest
        if (settings.isDrawLabelsOnHover() && !canvasUI.getDragManagerMove().isCurrentlyDraggingNode() && !canvasUI.getDragManagerConnect().isCurrentlyDraggingConnection() && canvasUI.getLastMousePosition() != null) {
            if (canvasUI.getCurrentlyMouseEnteredNode() != null && canvasUI.getCurrentlyMouseEnteredNodeActiveArea() instanceof JIPipeDesktopGraphNodeUISlotActiveArea slot) {
                for (JIPipeDesktopGraphEdgeUI displayedSlotEdge : canvasUI.getEdgeUIs().values()) {
                    if (slot.getSlot() == displayedSlotEdge.getTarget() || slot.getSlot() == displayedSlotEdge.getSource()) {

                        // Set ID
                        int id = edgeIds.getOrDefault(displayedSlotEdge, edgeIds.size() + 1);
                        edgeIds.put(displayedSlotEdge, id);

                        if (slot.getSlot() == displayedSlotEdge.getTarget()) {
                            highlightedEdges.put(displayedSlotEdge.getSource(), displayedSlotEdge);
                        } else if (slot.getSlot() == displayedSlotEdge.getSource()) {
                            highlightedEdges.put(displayedSlotEdge.getTarget(), displayedSlotEdge);
                        }

                    }
                }
            }
        }

        // Cancel if there is only a single edge and not far away
        if (edgeIds.isEmpty()) {
            return;
        }
        g.setPaint(ThemeUtils.getCurrentStyle().getPrimaryColor());
        final int thickness = 8;
        g.setStroke(new BasicStroke((int) Math.round(thickness * canvasUI.getZoom()), BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));

        for (JIPipeDataSlot dataSlot : highlightedEdges.keySet()) {
            JIPipeDesktopGraphNodeUI nodeUI = canvasUI.getNodeUIs().get(dataSlot.getNode());
            if (nodeUI != null) {
                JIPipeDesktopGraphNodeUISlotActiveArea slotActiveArea = nodeUI.getSlotActiveArea(dataSlot);
                int nodeX = nodeUI.getX();
                int nodeY = nodeUI.getY();
                if (slotActiveArea != null && slotActiveArea.getLastFillRect() != null) {
                    Rectangle lastFillRect = slotActiveArea.getLastFillRect();
                    int x = (int) Math.round(lastFillRect.x - ((double) thickness / 2) * canvasUI.getZoom());
                    int y = (int) Math.round(lastFillRect.y -  ((double) thickness / 2) * canvasUI.getZoom());
                    int width = lastFillRect.width + (int) Math.round( thickness * canvasUI.getZoom());
                    int height = (int) Math.round(lastFillRect.height + thickness * canvasUI.getZoom());

                    g.drawRect(nodeX + x + 1, nodeY + y + 1, width - 2, height - 2);
                }
            }
        }
    }
}
