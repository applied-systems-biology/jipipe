package org.hkijena.jipipe.desktop.app.grapheditor.commons.canvas.overlays;

import com.google.common.collect.HashMultimap;
import com.google.common.collect.Multimap;
import org.hkijena.jipipe.api.data.JIPipeDataSlot;
import org.hkijena.jipipe.desktop.app.grapheditor.commons.JIPipeDesktopGraphCanvasUI;
import org.hkijena.jipipe.desktop.app.grapheditor.commons.JIPipeDesktopGraphInteractiveObjectUI;
import org.hkijena.jipipe.desktop.app.grapheditor.commons.edgeui.JIPipeDesktopGraphEdgeUI;
import org.hkijena.jipipe.desktop.app.grapheditor.commons.nodeui.JIPipeDesktopGraphNodeUI;
import org.hkijena.jipipe.desktop.app.grapheditor.commons.nodeui.triggers.JIPipeDesktopGraphNodeUISlotActiveArea;
import org.hkijena.jipipe.plugins.settings.application.JIPipeGraphEditorUIApplicationSettings;
import org.hkijena.jipipe.plugins.tunnels.JIPipeDataFlowTunnelUtils;
import org.hkijena.jipipe.plugins.tunnels.nodes.JIPipeDataFlowTunnel;
import org.hkijena.jipipe.plugins.tunnels.nodes.JIPipeDataFlowTunnelEntrance;
import org.hkijena.jipipe.plugins.tunnels.nodes.JIPipeDataFlowTunnelExit;
import org.hkijena.jipipe.utils.ThemeUtils;

import java.awt.*;
import java.util.HashSet;
import java.util.IdentityHashMap;
import java.util.Map;
import java.util.Set;

public class JIPipeDesktopGraphCanvasTunnelIOOverlay implements JIPipeDesktopGraphCanvasOverlay {

    private final JIPipeDesktopGraphCanvasUI canvasUI;
    private final JIPipeGraphEditorUIApplicationSettings settings;

    public JIPipeDesktopGraphCanvasTunnelIOOverlay(JIPipeDesktopGraphCanvasUI canvasUI) {
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

        Set<JIPipeDataFlowTunnel> hoveredTunnelNodes = new HashSet<>();
        Set<JIPipeDataFlowTunnel> fullyLinkedTunnelNodes = new HashSet<>();

        // Find tunnel nodes that are hovered/selected
        if (settings.isDrawLabelsOnHover() && !canvasUI.getDragManagerMove().isCurrentlyDraggingNode() && !canvasUI.getDragManagerConnect().isCurrentlyDraggingConnection() && canvasUI.getLastMousePosition() != null) {
            if (canvasUI.getCurrentlyMouseEnteredNode() != null && canvasUI.getCurrentlyMouseEnteredNode().getNode() instanceof JIPipeDataFlowTunnel tunnel) {
               hoveredTunnelNodes.add(tunnel);
            }

            // Add also selected tunnels
            for (JIPipeDesktopGraphInteractiveObjectUI ui : canvasUI.getSelectionManager().getSelection()) {
                if(ui instanceof JIPipeDesktopGraphNodeUI nodeUI && nodeUI.getNode() instanceof JIPipeDataFlowTunnel tunnel) {
                    hoveredTunnelNodes.add(tunnel);
                }
            }
        }

        // Find all linked tunnel entrances/exits and only cover those
        for (JIPipeDataFlowTunnel tunnelNode : hoveredTunnelNodes) {
            if(tunnelNode.hasValidTunnelKey()) {
                if(tunnelNode instanceof JIPipeDataFlowTunnelEntrance entrance) {
                    fullyLinkedTunnelNodes.addAll(JIPipeDataFlowTunnelUtils.findTunnelExits(canvasUI.getGraph(), canvasUI.getCompartmentUUID(), entrance.getTunnelKeyGroup(), entrance.getTunnelKey()));
                }
                else if(tunnelNode instanceof JIPipeDataFlowTunnelExit exit) {
                    fullyLinkedTunnelNodes.addAll(JIPipeDataFlowTunnelUtils.findTunnelEntrances(canvasUI.getGraph(), canvasUI.getCompartmentUUID(), exit.getTunnelKeyGroup(), exit.getTunnelKey()));
                }
            }
        }

        // Subtract from the selection set
        fullyLinkedTunnelNodes.removeAll(hoveredTunnelNodes);

        if (hoveredTunnelNodes.isEmpty()) {
            return;
        }

        final int thickness = 6;
        g.setStroke(new BasicStroke((int) Math.round(thickness * canvasUI.getZoom()), BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));

        for (JIPipeDataFlowTunnel tunnelNode : fullyLinkedTunnelNodes) {
            float colorHue = JIPipeDataFlowTunnelUtils.getTunnelColorHue(canvasUI, tunnelNode.getTunnelKeyGroup(), tunnelNode.getTunnelKey());
            g.setPaint(Color.getHSBColor(colorHue, ThemeUtils.getCurrentStyle().getNodeBorderSaturation(), ThemeUtils.getCurrentStyle().getNodeBorderBrightness()));
            JIPipeDesktopGraphNodeUI nodeUI = canvasUI.getNodeUI(tunnelNode);
            if(nodeUI != null) {
                int nodeX = nodeUI.getX();
                int nodeY = nodeUI.getY();
                int nodeWidth = nodeUI.getWidth();
                int nodeHeight = nodeUI.getHeight();
                int x = (int) Math.round(nodeX - ((double) thickness / 2) * canvasUI.getZoom());
                int y = (int) Math.round(nodeY - ((double) thickness / 2) * canvasUI.getZoom());
                int width = nodeWidth + (int) Math.round(thickness * canvasUI.getZoom());
                int height = (int) Math.round(nodeHeight + thickness * canvasUI.getZoom());
                g.drawRect(x + 1, y + 1, width - 2, height - 2);
            }
        }
    }
}
