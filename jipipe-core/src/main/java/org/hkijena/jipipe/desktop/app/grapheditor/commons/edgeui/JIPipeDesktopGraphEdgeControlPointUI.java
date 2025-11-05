package org.hkijena.jipipe.desktop.app.grapheditor.commons.edgeui;

import org.hkijena.jipipe.JIPipe;
import org.hkijena.jipipe.api.data.JIPipeDataSlot;
import org.hkijena.jipipe.api.nodes.JIPipeGraphEdgeControlPoint;
import org.hkijena.jipipe.api.nodes.JIPipeGraphNode;
import org.hkijena.jipipe.desktop.app.grapheditor.commons.JIPipeDesktopGraphCanvasUI;
import org.hkijena.jipipe.desktop.app.grapheditor.commons.JIPipeDesktopGraphInteractiveObjectUI;
import org.hkijena.jipipe.desktop.app.grapheditor.commons.JIPipeDesktopGraphInteractiveObjectUIUpdateViewCommand;
import org.hkijena.jipipe.desktop.app.grapheditor.commons.canvas.JIPipeDesktopGraphCanvasGrid;
import org.hkijena.jipipe.desktop.app.grapheditor.commons.canvas.JIPipeDesktopGraphCanvasResources;
import org.hkijena.jipipe.utils.ThemeUtils;

import javax.swing.*;
import java.awt.*;
import java.util.Set;

public class JIPipeDesktopGraphEdgeControlPointUI implements JIPipeDesktopGraphInteractiveObjectUI {
    private final JIPipeDesktopGraphCanvasUI canvasUI;
    private final JIPipeDesktopGraphEdgeUI edgeUI;
    private final JIPipeDataSlot source;
    private final JIPipeDataSlot target;
    private final JIPipeGraphEdgeControlPoint controlPoint;

    public JIPipeDesktopGraphEdgeControlPointUI(JIPipeDesktopGraphCanvasUI canvasUI, JIPipeDesktopGraphEdgeUI edgeUI, JIPipeDataSlot source, JIPipeDataSlot target, JIPipeGraphEdgeControlPoint controlPoint) {
        this.canvasUI = canvasUI;
        this.edgeUI = edgeUI;
        this.source = source;
        this.target = target;
        this.controlPoint = controlPoint;
    }

    @Override
    public Set<JIPipeGraphNode> getNodes() {
        return Set.of(source.getNode(), target.getNode());
    }

    @Override
    public void updateView(JIPipeDesktopGraphInteractiveObjectUIUpdateViewCommand command) {

    }

    @Override
    public String getDisplayName() {
        return "Control point: " + edgeUI.getDisplayName();
    }

    @Override
    public String getDescription() {
        return "An edge control point";
    }

    @Override
    public Icon getIcon() {
        return JIPipe.RESOURCES.getIcon16("actions/snap-nodes-cusp.png");
    }

    public int getRenderedSize() {
        return (int) (JIPipeDesktopGraphCanvasResources.CONTROL_POINT_SIZE * canvasUI.getZoom());
    }

    public Point getRenderedLocation() {
        Point point = JIPipeDesktopGraphCanvasGrid.gridToRealLocation(controlPoint.toPoint(), canvasUI.getZoom());
        int size = getRenderedSize();
        point.x -= size / 2 + 1;
        point.y -= size / 2 + 1;
        return point;
    }

    public void paint(Graphics2D graphics2D, boolean multiColor, int multiColorIndex, int multiColorMax) {
        graphics2D.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        int size = getRenderedSize();
        Point location = getRenderedLocation();


        graphics2D.setPaint(canvasUI.getResources().getEdgeBackgroundPaint(source,
                target,
                null,
                null,
                canvasUI.getResources().getImprovedStrokeBackgroundColor()));
        graphics2D.fillOval(location.x, location.y, size, size);

        if (canvasUI.getSelectionManager().getSelection().contains(this)) {
            graphics2D.setStroke(JIPipeDesktopGraphCanvasResources.STROKE_THICK);
            graphics2D.setPaint(ThemeUtils.getCurrentStyle().getNodeHighlightBorder());
            graphics2D.drawOval(location.x, location.y, size, size);
        } else {
            graphics2D.setStroke(JIPipeDesktopGraphCanvasResources.STROKE_UNIT);
            graphics2D.setColor(canvasUI.getResources().getEdgeColor(source, target, multiColor, multiColorIndex, multiColorMax));
            graphics2D.drawOval(location.x, location.y, size, size);
        }

        graphics2D.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_OFF);
    }

    public boolean doesContainPoint(int mouseX, int mouseY) {
        Point location = getRenderedLocation();
        int size = getRenderedSize();
        return (Math.pow(mouseX - location.x, 2) + Math.pow(mouseY - location.y, 2)) <= Math.pow(size, 2);
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
        Rectangle copy = new Rectangle(selectionRectangle);
        copy.grow(size / 2, size / 2);
        int centerX = point.x - size / 2;
        int centerY = point.y - size / 2;
        Rectangle rectangle = new Rectangle(centerX - size / 2, centerY - size / 2, size, size);
        return copy.intersects(rectangle);
    }

    public JIPipeDesktopGraphEdgeUI getEdgeUI() {
        return edgeUI;
    }

    public JIPipeDesktopGraphCanvasUI getCanvasUI() {
        return canvasUI;
    }

    public JIPipeDataSlot getSource() {
        return source;
    }

    public JIPipeDataSlot getTarget() {
        return target;
    }

    public JIPipeGraphEdgeControlPoint getControlPoint() {
        return controlPoint;
    }
}
