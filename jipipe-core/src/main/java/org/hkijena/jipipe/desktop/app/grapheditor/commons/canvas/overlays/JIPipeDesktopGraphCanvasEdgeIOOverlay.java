package org.hkijena.jipipe.desktop.app.grapheditor.commons.canvas.overlays;

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
import java.util.Set;

public class JIPipeDesktopGraphCanvasEdgeIOOverlay implements JIPipeDesktopGraphCanvasOverlay {

    private final JIPipeDesktopGraphCanvasUI canvasUI;
    private final JIPipeGraphEditorUIApplicationSettings settings;

    public JIPipeDesktopGraphCanvasEdgeIOOverlay(JIPipeDesktopGraphCanvasUI canvasUI) {
        this.canvasUI = canvasUI;
        this.settings = canvasUI.getSettings();
    }

    @Override
    public void paintComponent(Graphics2D g) {

    }

    @Override
    public void paint(Graphics2D g) {
        if (!settings.isHighlightIOOnEdgeSelect()) {
            return;
        }

        final int thickness = 6;
        g.setPaint(ThemeUtils.getCurrentStyle().getPrimaryColor());
        g.setStroke(new BasicStroke((int) Math.round(thickness * canvasUI.getZoom()), BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));

        for (JIPipeDesktopGraphInteractiveObjectUI ui : canvasUI.getSelectionManager().getSelection()) {
            if(ui instanceof JIPipeDesktopGraphEdgeUI edgeUI) {
                paintForSlot(g, edgeUI.getSourceNodeUI(), edgeUI.getSource());
                paintForSlot(g, edgeUI.getTargetNodeUI(), edgeUI.getTarget());
            }
        }
    }

    private void paintForSlot(Graphics2D g, JIPipeDesktopGraphNodeUI nodeUI, JIPipeDataSlot dataSlot) {
        if(nodeUI == null || dataSlot == null) {
            return;
        }
        final int thickness = 6;
        JIPipeDesktopGraphNodeUISlotActiveArea slotActiveArea = nodeUI.getSlotActiveArea(dataSlot);
        int nodeX = nodeUI.getX();
        int nodeY = nodeUI.getY();
        if (slotActiveArea != null && slotActiveArea.getLastFillRect() != null) {
            Rectangle lastFillRect = slotActiveArea.getLastFillRect();
            int x = (int) Math.round(lastFillRect.x - ((double) thickness / 2) * canvasUI.getZoom());
            int y = (int) Math.round(lastFillRect.y - ((double) thickness / 2) * canvasUI.getZoom());
            int width = lastFillRect.width + (int) Math.round(thickness * canvasUI.getZoom());
            int height = (int) Math.round(lastFillRect.height + thickness * canvasUI.getZoom());

            g.drawRect(nodeX + x + 1, nodeY + y + 1, width - 2, height - 2);
        }
    }
}
