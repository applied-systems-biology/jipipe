package org.hkijena.jipipe.desktop.app.grapheditor.commons.edgeui;

import org.hkijena.jipipe.api.data.JIPipeDataSlot;
import org.hkijena.jipipe.api.nodes.JIPipeGraphEdge;
import org.hkijena.jipipe.api.nodes.JIPipeGraphEdgeControlPoint;
import org.hkijena.jipipe.api.nodes.JIPipeGraphNode;
import org.hkijena.jipipe.desktop.app.grapheditor.commons.JIPipeDesktopGraphCanvasUI;
import org.hkijena.jipipe.desktop.app.grapheditor.commons.JIPipeDesktopGraphInteractiveObjectUI;
import org.hkijena.jipipe.desktop.app.grapheditor.commons.JIPipeDesktopGraphInteractiveObjectUIUpdateViewCommand;
import org.hkijena.jipipe.desktop.app.grapheditor.commons.canvas.JIPipeDesktopGraphCanvasGrid;
import org.hkijena.jipipe.desktop.app.grapheditor.commons.canvas.JIPipeDesktopGraphCanvasResources;
import org.hkijena.jipipe.utils.ThemeUtils;

import java.awt.*;
import java.util.Set;

public class JIPipeDesktopGraphEdgeControlPointUI implements JIPipeDesktopGraphInteractiveObjectUI {
    private final JIPipeDesktopGraphCanvasUI canvasUI;
    private final JIPipeDataSlot source;
    private final JIPipeDataSlot target;
    private final JIPipeGraphEdge edge;
    private final JIPipeGraphEdgeControlPoint controlPoint;

    public JIPipeDesktopGraphEdgeControlPointUI(JIPipeDesktopGraphCanvasUI canvasUI, JIPipeDataSlot source, JIPipeDataSlot target, JIPipeGraphEdge edge, JIPipeGraphEdgeControlPoint controlPoint) {
        this.canvasUI = canvasUI;
        this.source = source;
        this.target = target;
        this.edge = edge;
        this.controlPoint = controlPoint;
    }

    @Override
    public Set<JIPipeGraphNode> getNodes() {
        return Set.of(source.getNode(), target.getNode());
    }

    @Override
    public void updateView(JIPipeDesktopGraphInteractiveObjectUIUpdateViewCommand command) {

    }

    public int getRenderedSize() {
        return (int) (JIPipeDesktopGraphCanvasResources.CONTROL_POINT_SIZE * canvasUI.getZoom());
    }

    public void paint(Graphics2D graphics2D) {
        graphics2D.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        int size = getRenderedSize();
        Point point = JIPipeDesktopGraphCanvasGrid.gridToRealLocation(controlPoint.toPoint(), canvasUI.getZoom());

        graphics2D.setPaint(canvasUI.getResources().getImprovedStrokeBackgroundColor());
        graphics2D.fillOval(point.x - size / 2, point.y - size / 2, size, size);

        if(canvasUI.getSelectionManager().getSelection().contains(this)) {
            graphics2D.setStroke(JIPipeDesktopGraphCanvasResources.STROKE_THICK);
            graphics2D.setPaint(ThemeUtils.getCurrentStyle().getNodeHighlightBorder());
            graphics2D.drawOval(point.x - size / 2, point.y - size / 2, size, size);
        }
        else {
            graphics2D.setStroke(JIPipeDesktopGraphCanvasResources.STROKE_UNIT);
            graphics2D.setColor(canvasUI.getResources().getEdgeColor(source, target, false, 0, 0));
            graphics2D.drawOval(point.x - size / 2, point.y - size / 2, size, size);
        }

        graphics2D.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_OFF);
    }

    public boolean doesContainPoint(int mouseX, int mouseY) {
        Point point = JIPipeDesktopGraphCanvasGrid.gridToRealLocation(controlPoint.toPoint(), canvasUI.getZoom());
        int size = getRenderedSize();
        int centerX = point.x - size / 2;
        int centerY = point.y - size / 2;
        return (Math.pow(mouseX -  centerX, 2) + Math.pow(mouseY - centerY, 2))  <= Math.pow(size, 2);
    }

    public Point getGridLocation() {
        return controlPoint.toPoint();
    }

    public void setGridLocation(Point newGridLocation) {
        controlPoint.set(newGridLocation);
    }

    public boolean doesEdgeIntersectRectangle(Rectangle selectionRectangle) {
        Point point = JIPipeDesktopGraphCanvasGrid.gridToRealLocation(controlPoint.toPoint(), canvasUI.getZoom());
        int size = getRenderedSize();
        Rectangle copy = new  Rectangle(selectionRectangle);
        copy.grow(size / 2, size / 2);
        int centerX = point.x - size / 2;
        int centerY = point.y - size / 2;
        Rectangle rectangle = new Rectangle(centerX - size / 2, centerY - size / 2, size, size);
        return copy.intersects(rectangle);
    }
}
