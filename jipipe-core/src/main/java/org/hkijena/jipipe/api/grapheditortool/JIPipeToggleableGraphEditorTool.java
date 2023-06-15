package org.hkijena.jipipe.api.grapheditortool;

import org.hkijena.jipipe.api.data.JIPipeDataSlot;
import org.hkijena.jipipe.api.nodes.JIPipeGraphEdge;
import org.hkijena.jipipe.ui.grapheditor.general.JIPipeGraphCanvasUI;

import java.awt.*;

public interface JIPipeToggleableGraphEditorTool extends JIPipeGraphEditorTool {

    void deactivate();

    default boolean allowsDragNodes() {
        return true;
    }

    default boolean allowsDragConnections() {
        return true;
    }

    default boolean canRenderEdge(JIPipeDataSlot source, JIPipeDataSlot target, JIPipeGraphEdge edge) {
        return true;
    }

    default void paintBelowNodesAfterEdges(Graphics2D g) {

    }

    default void paintBelowNodesAndEdges(Graphics2D graphics2D) {

    }

    default void paintAfterNodesAndEdges(Graphics2D graphics2D) {

    }

    default void paintMouse(JIPipeGraphCanvasUI canvasUI, Point lastMousePosition, Graphics2D graphics2D) {
        int x = lastMousePosition.x + 16;
        int y = lastMousePosition.y + 16;

        graphics2D.setFont(JIPipeGraphCanvasUI.GRAPH_TOOL_CURSOR_FONT);
        FontMetrics fontMetrics = graphics2D.getFontMetrics();

        int nameWidth = fontMetrics.stringWidth(getName());

        graphics2D.setStroke(JIPipeGraphCanvasUI.STROKE_UNIT);
        graphics2D.setColor(canvasUI.getSmartEdgeSlotBackground());
        graphics2D.fillRoundRect(x,y, nameWidth + 22 + 3, 22, 5, 5);
        graphics2D.setColor(canvasUI.getSmartEdgeSlotForeground());
        graphics2D.drawRoundRect(x,y, nameWidth + 22 + 3, 22, 5, 5);

        getIcon().paintIcon(canvasUI, graphics2D, x + 3, y + 3);
        graphics2D.drawString(getName(), x + 22, y + (fontMetrics.getAscent() - fontMetrics.getLeading()) + 22 / 2 - fontMetrics.getHeight() / 2);

    }
}
