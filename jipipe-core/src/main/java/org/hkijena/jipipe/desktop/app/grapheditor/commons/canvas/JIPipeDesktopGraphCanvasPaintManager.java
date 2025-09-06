package org.hkijena.jipipe.desktop.app.grapheditor.commons.canvas;

import gnu.trove.list.TIntList;
import gnu.trove.list.array.TIntArrayList;
import org.hkijena.jipipe.api.nodes.JIPipeGraphEdge;
import org.hkijena.jipipe.desktop.app.grapheditor.commons.JIPipeDesktopGraphCanvasUI;
import org.hkijena.jipipe.desktop.app.grapheditor.commons.edgeui.JIPipeDesktopGraphEdgeUI;
import org.hkijena.jipipe.desktop.app.grapheditor.commons.nodeui.JIPipeDesktopGraphNodeUI;

import java.awt.*;

public class JIPipeDesktopGraphCanvasPaintManager {
    private final JIPipeDesktopGraphCanvasUI canvasUI;

    public JIPipeDesktopGraphCanvasPaintManager(JIPipeDesktopGraphCanvasUI canvasUI) {
        this.canvasUI = canvasUI;
    }

    public void paintArrowHead(Graphics2D g, int x, int y) {
        int sz = 1;
        int dy = -2 * sz - 4;
        g.drawPolygon(new int[]{x - sz, x + sz, x}, new int[]{y - sz + dy, y - sz + dy, y + dy}, 3);
    }



    private void paintElbowEdge(Graphics2D g, Point sourcePoint, Rectangle sourceBounds, Point targetPoint, double scale, int viewX, int viewY, boolean enableArrows) {
        int buffer;
        int sourceA;
        int targetA;
        int sourceB;
        int targetB;
        int componentStartB;
        int componentEndB;

        buffer = JIPipeDesktopGraphCanvasGrid.GRID_HEIGHT / 2;
        sourceA = sourcePoint.y;
        targetA = targetPoint.y;
        if (enableArrows) {
            targetA += canvasUI.getResources().getArrowHeadShift();
        }
        sourceB = sourcePoint.x;
        targetB = targetPoint.x;
        componentStartB = sourceBounds.x;
        componentEndB = sourceBounds.x + sourceBounds.width;

        int a0 = sourceA;
        int b0 = sourceB;
        int a1 = sourceA;
        int b1 = sourceB;

        TIntArrayList xCoords = new TIntArrayList(8);
        TIntArrayList yCoords = new TIntArrayList(8);

        addElbowPolygonCoordinate(a0, b0, scale, viewX, viewY, xCoords, yCoords);

        // Target point is above the source. We have to navigate around it
        if (sourceA > targetA) {
            // Add some space in major direction
            a1 += buffer;
            addElbowPolygonCoordinate(a1, b1, scale, viewX, viewY, xCoords, yCoords);

            // Go left or right
            if (targetB <= b1) {
                b1 = Math.max(0, componentStartB - buffer);
            } else {
                b1 = componentEndB + buffer;
            }
            addElbowPolygonCoordinate(a1, b1, scale, viewX, viewY, xCoords, yCoords);

            // Go to target height
            a1 = Math.max(0, targetA - buffer);
            addElbowPolygonCoordinate(a1, b1, scale, viewX, viewY, xCoords, yCoords);
        } else if (sourceB != targetB) {
            // Add some space in major direction
            int dA = targetA - sourceA;
            a1 = Math.min(sourceA + buffer, sourceA + dA / 2);
            addElbowPolygonCoordinate(a1, b1, scale, viewX, viewY, xCoords, yCoords);
        }

        // Target point X is shifted
        if (b1 != targetB) {
            b1 = targetB;
            addElbowPolygonCoordinate(a1, b1, scale, viewX, viewY, xCoords, yCoords);
        }

        // Go to end point
        a1 = targetA;
        addElbowPolygonCoordinate(a1, b1, scale, viewX, viewY, xCoords, yCoords);

        // Draw the polygon
        g.drawPolyline(xCoords.toArray(), yCoords.toArray(), xCoords.size());

    }

    private void addElbowPolygonCoordinate(int a1, int b1, double scale, int viewX, int viewY, TIntList xCoords, TIntList yCoords) {
        int x2, y2;
        x2 = (int) (b1 * scale) + viewX;
        y2 = (int) (a1 * scale) + viewY;
        xCoords.add(x2);
        yCoords.add(y2);
    }

    /**
     * Draws an edge between source point and the target point
     *
     * @param g            the graphics
     * @param sourcePoint  the source point
     * @param sourceBounds bounds of the source
     * @param targetPoint  the target point
     * @param shape        the line shape
     * @param scale        the scale
     * @param viewX        the view x
     * @param viewY        the view y
     * @param enableArrows enable arrows
     */
    public void paintEdge(Graphics2D g, Point sourcePoint, Rectangle sourceBounds, Point targetPoint, JIPipeGraphEdge.Shape shape, double scale, int viewX, int viewY, boolean enableArrows) {
        switch (shape) {
            case Elbow:
                paintElbowEdge(g, sourcePoint, sourceBounds, targetPoint, scale, viewX, viewY, enableArrows);
                break;
            case Line: {
                int arrowHeadShift = enableArrows ? canvasUI.getResources().getArrowHeadShift() : 0;
                int dx;
                int dy;
                dx = 0;
                dy = arrowHeadShift;
                g.drawLine((int) (scale * sourcePoint.x) + viewX,
                        (int) (scale * sourcePoint.y) + viewY,
                        (int) (scale * targetPoint.x) + viewX + dx,
                        (int) (scale * targetPoint.y) + viewY + dy);
            }
            break;
        }
        if (enableArrows) {
            paintArrowHead(g, targetPoint.x, targetPoint.y);
        }
    }

    /**
     * Draws a minimap of this canvas.
     *
     * @param graphics2D the graphics
     * @param scale      the scale
     * @param viewX      move the locations by this value
     * @param viewY      move the locations by this value
     */
    public void paintMiniMap(Graphics2D graphics2D, double scale, int viewX, int viewY) {

        paintMinimapEdges(graphics2D, scale, viewX, viewY);

        Stroke defaultStroke = JIPipeDesktopGraphCanvasResources.STROKE_UNIT;
        Stroke selectedStroke = JIPipeDesktopGraphCanvasResources.STROKE_SELECTION;

        for (JIPipeDesktopGraphNodeUI nodeUI : canvasUI.getNodeUIs().values()) {
            int x = (int) (nodeUI.getX() * scale) + viewX;
            int y = (int) (nodeUI.getY() * scale) + viewY;
            int width = (int) (nodeUI.getWidth() * scale);
            int height = (int) (nodeUI.getHeight() * scale);

            nodeUI.paintMinimap(graphics2D, x, y, width, height, (BasicStroke) defaultStroke, (BasicStroke) selectedStroke, canvasUI.getSelectionManager().getSelection());
        }
    }

    private void paintMinimapEdges(Graphics2D graphics2D, double scale, int viewX, int viewY) {
        graphics2D.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_OFF);
        graphics2D.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
        graphics2D.setColor(Color.LIGHT_GRAY);
        for (JIPipeDesktopGraphEdgeUI edgeUI : canvasUI.getEdgeUIs().values()) {
            edgeUI.paint(graphics2D,
                    JIPipeDesktopGraphCanvasResources.STROKE_UNIT,
                    null,
                    scale,
                    viewX,
                    viewY,
                    false);
        }
    }
}
