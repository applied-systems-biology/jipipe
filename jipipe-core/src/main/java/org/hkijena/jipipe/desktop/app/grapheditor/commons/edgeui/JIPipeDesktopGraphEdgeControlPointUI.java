package org.hkijena.jipipe.desktop.app.grapheditor.commons.edgeui;

import org.hkijena.jipipe.api.data.JIPipeDataSlot;
import org.hkijena.jipipe.api.nodes.JIPipeGraphEdge;
import org.hkijena.jipipe.api.nodes.JIPipeGraphEdgeControlPoint;
import org.hkijena.jipipe.api.nodes.JIPipeGraphNode;
import org.hkijena.jipipe.desktop.app.grapheditor.commons.JIPipeDesktopGraphCanvasUI;
import org.hkijena.jipipe.desktop.app.grapheditor.commons.JIPipeDesktopGraphInteractiveObjectUI;
import org.hkijena.jipipe.desktop.app.grapheditor.commons.JIPipeDesktopGraphInteractiveObjectUIUpdateViewCommand;
import org.hkijena.jipipe.desktop.app.grapheditor.commons.canvas.JIPipeDesktopGraphCanvasGrid;

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

    public void paint(Graphics2D graphics2D) {
        graphics2D.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        Point point = JIPipeDesktopGraphCanvasGrid.gridToRealLocation(controlPoint.toPoint(), canvasUI.getZoom());
        graphics2D.setPaint(Color.RED);
        graphics2D.fillOval(point.x - 5, point.y - 5, 10, 10);
        graphics2D.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_OFF);
    }
}
