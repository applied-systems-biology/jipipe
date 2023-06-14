package org.hkijena.jipipe.extensions.graphannotation.tools;

import org.hkijena.jipipe.api.nodes.JIPipeAnnotationGraphNodeTool;
import org.hkijena.jipipe.extensions.graphannotation.nodes.ArrowAnnotationGraphNode;

import javax.swing.*;
import java.awt.*;
import java.awt.event.KeyEvent;

public class ArrowAnnotationGraphNodeTool extends JIPipeAnnotationGraphNodeTool<ArrowAnnotationGraphNode> {
    public ArrowAnnotationGraphNodeTool() {
        super(ArrowAnnotationGraphNode.class);
    }

    @Override
    protected ArrowAnnotationGraphNode createAndConfigureNode(Point firstPoint, Point secondPoint) {
        double angle = Math.atan2(secondPoint.y - firstPoint.y, secondPoint.x - firstPoint.x);
        ArrowAnnotationGraphNode node = super.createAndConfigureNode(firstPoint, secondPoint);
        node.setAngle((int) (angle / (Math.PI * 2) * 360));
        return node;
    }

    @Override
    protected void paintDragOverlay(Graphics2D graphics2D, int x0, int y0, int x1, int y1) {
        graphics2D.drawLine(x0, y0, x1, y1);

        // Draw arrowhead
        int arrowSize = 10;
        double angle = Math.atan2(y1 - y0, x1 - x0);
        int ax1 = (int) (x1 - arrowSize * Math.cos(angle - Math.PI / 6));
        int ay1 = (int) (y1 - arrowSize * Math.sin(angle - Math.PI / 6));
        int ax2 = (int) (x1 - arrowSize * Math.cos(angle + Math.PI / 6));
        int ay2 = (int) (y1 - arrowSize * Math.sin(angle + Math.PI / 6));
        graphics2D.drawLine(x1, y1, ax1, ay1);
        graphics2D.drawLine(x1, y1, ax2, ay2);
    }

    @Override
    public KeyStroke getKeyBinding() {
        return KeyStroke.getKeyStroke(KeyEvent.VK_F6, 0);
    }

    @Override
    public int getPriority() {
        return -4800;
    }
}
