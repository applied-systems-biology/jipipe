package org.hkijena.jipipe.desktop.app.grapheditor.commons.canvas.overlays;

import org.hkijena.jipipe.desktop.app.grapheditor.commons.JIPipeDesktopGraphCanvasUI;
import org.hkijena.jipipe.desktop.app.grapheditor.commons.canvas.managers.JIPipeDesktopGraphCanvasPaintManager;
import org.hkijena.jipipe.desktop.app.grapheditor.commons.nodeui.JIPipeDesktopGraphNodeUI;
import org.hkijena.jipipe.utils.ThemeUtils;

import java.awt.*;
import java.util.Set;
import java.util.UUID;

public class JIPipeDesktopGraphCanvasOutsideEdgesOverlay implements  JIPipeDesktopGraphCanvasOverlay {

    private final JIPipeDesktopGraphCanvasUI canvasUI;

    public JIPipeDesktopGraphCanvasOutsideEdgesOverlay(JIPipeDesktopGraphCanvasUI canvasUI) {
        this.canvasUI = canvasUI;
    }

    @Override
    public void paint(Graphics2D g) {

    }

    @Override
    public void paintComponent(Graphics2D g) {

        Stroke strokeInside = canvasUI.getResources().getEdgeStrokeInside();
        Stroke strokeBorder = canvasUI.getResources().getEdgeStrokeBorder();
        Stroke strokeBorderSelected = canvasUI.getResources().getSelectedEdgeStrokeBorder();

        for (JIPipeDesktopGraphNodeUI ui : canvasUI.getNodeUIs().values()) {
            Set<UUID> visibleCompartments = canvasUI.getGraph().getVisibleCompartmentUUIDsOf(ui.getNode());
            if (!visibleCompartments.isEmpty()) {
                boolean uiIsSelected = canvasUI.getSelectionManager().getSelection().contains(ui);

                Point sourcePoint = new Point();
                Point targetPoint = new Point();
                boolean uiIsOutput = canvasUI.getCompartmentUUID() == null || canvasUI.getCompartmentUUID().equals(ui.getNode().getCompartmentUUIDInParentGraph());

                if (uiIsOutput) {
                    // This is an output -> line goes outside
                    targetPoint.x = ui.getX() + ui.getWidth() / 2;
                    targetPoint.y = ui.getY() + ui.getHeight();
                    sourcePoint.x = targetPoint.x;
                    sourcePoint.y = canvasUI.getHeight();
                } else {
                    // This is an input
                    targetPoint.x = ui.getX() + ui.getWidth() / 2;
                    targetPoint.y = ui.getY();
                    sourcePoint.x = targetPoint.x;
                    sourcePoint.y = 0;
                }
                g.setStroke(uiIsSelected ? strokeBorderSelected : strokeBorder);
                g.setColor(ThemeUtils.getCurrentStyle().getNodeHighlightBorder());
                paintOutsideEdge(g, sourcePoint, targetPoint, JIPipeDesktopGraphCanvasPaintManager.ArrowHeadMode.Filled);
                g.setStroke(strokeInside);
                g.setColor(canvasUI.getResources().getImprovedStrokeBackgroundColor());
                paintOutsideEdge(g, sourcePoint, targetPoint, JIPipeDesktopGraphCanvasPaintManager.ArrowHeadMode.Filled);
            }
        }
    }

    private void paintOutsideEdge(Graphics2D g, Point sourcePoint, Point targetPoint, JIPipeDesktopGraphCanvasPaintManager.ArrowHeadMode arrowHeadMode) {
        int arrowHeadShift = canvasUI.getResources().getArrowHeadShift();
        int dx;
        int dy;
        if (arrowHeadMode == JIPipeDesktopGraphCanvasPaintManager.ArrowHeadMode.Filled) {
            dx = 0;
            dy = arrowHeadShift;
        } else {
            dx = 0;
            dy = 0;
        }
        g.drawLine(sourcePoint.x, sourcePoint.y, targetPoint.x + dx, targetPoint.y + dy);
        if (arrowHeadMode != JIPipeDesktopGraphCanvasPaintManager.ArrowHeadMode.None) {
            canvasUI.getPaintManager().paintArrowHead(g, targetPoint.x, targetPoint.y, arrowHeadMode);
        }
    }
}
