package org.hkijena.jipipe.desktop.app.grapheditor.commons.canvas.managers;

import org.hkijena.jipipe.desktop.app.grapheditor.commons.JIPipeDesktopGraphCanvasUI;
import org.hkijena.jipipe.desktop.app.grapheditor.commons.JIPipeDesktopGraphInteractiveObjectUI;
import org.hkijena.jipipe.desktop.app.grapheditor.commons.canvas.JIPipeDesktopGraphCanvasResources;
import org.hkijena.jipipe.desktop.app.grapheditor.commons.edgeui.JIPipeDesktopGraphEdgeControlPointUI;
import org.hkijena.jipipe.desktop.app.grapheditor.commons.edgeui.JIPipeDesktopGraphEdgeUI;
import org.hkijena.jipipe.desktop.app.grapheditor.commons.nodeui.JIPipeDesktopGraphNodeUI;

import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseEvent;
import java.util.HashSet;
import java.util.Set;

public class JIPipeDesktopGraphCanvasSelectionBoxManager {

    private final JIPipeDesktopGraphCanvasUI canvasUI;

    private Point selectionFirst;
    private Point selectionSecond;

    public JIPipeDesktopGraphCanvasSelectionBoxManager(JIPipeDesktopGraphCanvasUI canvasUI) {
        this.canvasUI = canvasUI;
    }

    public boolean mouseDragged(MouseEvent mouseEvent) {
        if (selectionFirst != null && SwingUtilities.isLeftMouseButton(mouseEvent)) {
            selectionSecond = mouseEvent.getPoint();
            canvasUI.repaintLowLag();
            return true;
        } else {
            selectionFirst = null;
            selectionSecond = null;
        }
        return false;
    }

    public void paint(Graphics2D graphics2D) {
        // Draw marquee rectangle
        if (selectionFirst != null && selectionSecond != null) {
            graphics2D.setStroke(JIPipeDesktopGraphCanvasResources.STROKE_MARQUEE);
            graphics2D.setColor(Color.GRAY);
            int x0 = selectionFirst.x;
            int y0 = selectionFirst.y;
            int x1 = selectionSecond.x;
            int y1 = selectionSecond.y;
            int x = Math.min(x0, x1);
            int y = Math.min(y0, y1);
            int w = Math.abs(x0 - x1);
            int h = Math.abs(y0 - y1);
            graphics2D.drawRect(x, y, w, h);
        }
    }

    public void clear() {
        selectionFirst = null;
        selectionSecond = null;
    }

    public boolean mouseReleased(MouseEvent mouseEvent) {
        if (selectionFirst != null && selectionSecond != null) {
            int x0 = selectionFirst.x;
            int y0 = selectionFirst.y;
            int x1 = selectionSecond.x;
            int y1 = selectionSecond.y;
            int x = Math.min(x0, x1);
            int y = Math.min(y0, y1);
            int w = Math.abs(x0 - x1);
            int h = Math.abs(y0 - y1);
            Rectangle selectionRectangle = new Rectangle(x, y, w, h);
            Set<JIPipeDesktopGraphInteractiveObjectUI> newSelection = new HashSet<>();
            for (JIPipeDesktopGraphNodeUI ui : canvasUI.getNodeUIs().values()) {
                if (canvasUI.getToolLayerMask().test(ui) && selectionRectangle.intersects(ui.getBounds())) {
                    newSelection.add(ui);
                }
            }
            for (JIPipeDesktopGraphEdgeUI ui : canvasUI.getEdgeUIs().values()) {
                if (canvasUI.getToolLayerMask().test(ui) && canvasUI.getEdgeManager().doesEdgeIntersectRectangle(ui, selectionRectangle)) {
                    newSelection.add(ui);
                }
                for (JIPipeDesktopGraphEdgeControlPointUI controlPoint : ui.getControlPoints()) {
                    if (canvasUI.getToolLayerMask().test(controlPoint) && controlPoint.doesEdgeIntersectRectangle(selectionRectangle)) {
                        newSelection.add(controlPoint);
                    }
                }
            }

            if (!newSelection.isEmpty()) {
                if (!mouseEvent.isShiftDown()) {
                    canvasUI.getSelectionManager().clearSelection(false);
                }
                canvasUI.getSelectionManager().addAllToSelection(newSelection, true);
            }

            selectionFirst = null;
            selectionSecond = null;
            canvasUI.repaintLowLag();
            return true;
        }
        return false;
    }

    public boolean mousePressed(MouseEvent mouseEvent) {
        if (SwingUtilities.isLeftMouseButton(mouseEvent)) {
            selectionFirst = mouseEvent.getPoint();
            selectionSecond = null;
        } else {
            selectionFirst = null;
            selectionSecond = null;
        }
        return false;
    }
}
