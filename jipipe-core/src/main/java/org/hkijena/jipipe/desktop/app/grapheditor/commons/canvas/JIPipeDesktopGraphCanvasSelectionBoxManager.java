package org.hkijena.jipipe.desktop.app.grapheditor.commons.canvas;

import org.hkijena.jipipe.api.nodes.annotation.JIPipeAnnotationGraphNode;
import org.hkijena.jipipe.desktop.app.grapheditor.commons.JIPipeDesktopGraphCanvasUI;
import org.hkijena.jipipe.desktop.app.grapheditor.commons.JIPipeDesktopGraphInteractiveObjectUI;
import org.hkijena.jipipe.desktop.app.grapheditor.commons.nodeui.JIPipeDesktopGraphNodeUI;

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
        if (selectionFirst != null) {
            selectionSecond = mouseEvent.getPoint();
            canvasUI.repaintLowLag();
            return true;
        }
        return false;
    }

    public void paint(Graphics2D graphics2D) {
        // Draw marquee rectangle
        if (selectionFirst != null && selectionSecond != null) {
            graphics2D.setStroke(JIPipeDesktopGraphCanvasUIConstants.STROKE_MARQUEE);
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
                if (canvasUI.isGraphAnnotationsLocked() && ui.getNode() instanceof JIPipeAnnotationGraphNode) {
                    continue;
                }
                if (selectionRectangle.intersects(ui.getBounds())) {
                    newSelection.add(ui);
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
        selectionFirst = mouseEvent.getPoint();
        return false;
    }
}
