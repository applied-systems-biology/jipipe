package org.hkijena.jipipe.desktop.app.grapheditor.commons.canvas.managers;

import gnu.trove.list.TIntList;
import gnu.trove.list.array.TIntArrayList;
import org.hkijena.jipipe.api.nodes.JIPipeGraphEdge;
import org.hkijena.jipipe.desktop.app.grapheditor.commons.JIPipeDesktopGraphCanvasUI;
import org.hkijena.jipipe.desktop.app.grapheditor.commons.canvas.JIPipeDesktopGraphCanvasGrid;
import org.hkijena.jipipe.desktop.app.grapheditor.commons.canvas.JIPipeDesktopGraphCanvasResources;
import org.hkijena.jipipe.desktop.app.grapheditor.commons.edgeui.JIPipeDesktopGraphEdgeUI;
import org.hkijena.jipipe.desktop.app.grapheditor.commons.nodeui.JIPipeDesktopGraphNodeUI;

import java.awt.*;
import java.util.List;

public class JIPipeDesktopGraphCanvasPaintManager {
    private final JIPipeDesktopGraphCanvasUI canvasUI;

    public JIPipeDesktopGraphCanvasPaintManager(JIPipeDesktopGraphCanvasUI canvasUI) {
        this.canvasUI = canvasUI;
    }

    public void paintArrowHead(Graphics2D g, int x, int y, ArrowHeadMode arrowHeadMode) {
        if (arrowHeadMode == ArrowHeadMode.Filled) {
            int sz = 1;
            int dy = -2 * sz - 4;
            g.drawPolygon(new int[]{x - sz, x + sz, x}, new int[]{y - sz + dy, y - sz + dy, y + dy}, 3);
        } else {
            g.drawLine(x - 6, y - 6, x, y);
            g.drawLine(x + 6, y - 6, x, y);
        }
    }


    private void paintCustomElbowEdge(Graphics2D g, Point sourcePoint, Rectangle sourceBounds, Point targetPoint, double scale, int viewX, int viewY, ArrowHeadMode arrowHeadMode) {

        TIntArrayList xCoords = new TIntArrayList(8);
        TIntArrayList yCoords = new TIntArrayList(8);

        createElbowEdgeCoordinates(sourcePoint, sourceBounds, targetPoint, scale, viewX, viewY, arrowHeadMode, xCoords, yCoords);

        // Draw the polygon
        g.drawPolyline(xCoords.toArray(), yCoords.toArray(), xCoords.size());

    }

    public void createElbowEdgeCoordinates(Point sourcePoint, Rectangle sourceBounds, Point targetPoint, double scale, int viewX, int viewY, ArrowHeadMode arrowHeadMode, TIntArrayList xCoords, TIntArrayList yCoords) {
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
        if (arrowHeadMode == ArrowHeadMode.Filled) {
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
     * @param g             the graphics
     * @param sourcePoint   the source point
     * @param sourceBounds  bounds of the source
     * @param targetPoint   the target point
     * @param shape         the line shape
     * @param scale         the scale
     * @param viewX         the view x
     * @param viewY         the view y
     * @param arrowHeadMode How arrow heads should be displayed
     */
    public void paintCustomEdge(Graphics2D g, Point sourcePoint, Rectangle sourceBounds, Point targetPoint, List<Point> controlPoints, JIPipeGraphEdge.Shape shape, double scale, int viewX, int viewY, ArrowHeadMode arrowHeadMode) {
        Point nextSource = sourcePoint;
        Point nextTarget;
        Rectangle nextSourceBounds = sourceBounds;

        if (controlPoints.isEmpty()) {
            nextTarget = targetPoint;
        } else {
            for (Point gridLocation : controlPoints) {
                nextTarget = JIPipeDesktopGraphCanvasGrid.gridToRealLocation(gridLocation, 1);
                paintCustomEdge(g, nextSource, nextSourceBounds, nextTarget, shape, scale, viewX, viewY, ArrowHeadMode.None);

                nextSource = nextTarget;
                nextSourceBounds = new Rectangle(nextSource.x, nextSource.y, 1, 1);
            }

            nextTarget = targetPoint;
        }

        paintCustomEdge(g, nextSource, nextSourceBounds, nextTarget, shape, scale, viewX, viewY, arrowHeadMode);

        g.setPaint(Color.RED);
        for (Point gridLocation : controlPoints) {
            Point point = JIPipeDesktopGraphCanvasGrid.gridToRealLocation(gridLocation, 1);
            g.fillOval(point.x, point.y, 5, 5);
        }
    }

    /**
     * Draws an edge between source point and the target point
     *
     * @param g             the graphics
     * @param sourcePoint   the source point
     * @param sourceBounds  bounds of the source
     * @param targetPoint   the target point
     * @param shape         the line shape
     * @param scale         the scale
     * @param viewX         the view x
     * @param viewY         the view y
     * @param arrowHeadMode How arrow heads should be displayed
     */
    public void paintCustomEdge(Graphics2D g, Point sourcePoint, Rectangle sourceBounds, Point targetPoint, JIPipeGraphEdge.Shape shape, double scale, int viewX, int viewY, ArrowHeadMode arrowHeadMode) {
        switch (shape) {
            case Elbow:
                paintCustomElbowEdge(g, sourcePoint, sourceBounds, targetPoint, scale, viewX, viewY, arrowHeadMode);
                break;
            case Line: {
                int arrowHeadShift = arrowHeadMode != ArrowHeadMode.None ? canvasUI.getResources().getArrowHeadShift() : 0;
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
        if (arrowHeadMode != ArrowHeadMode.None) {
            paintArrowHead(g, targetPoint.x, targetPoint.y, arrowHeadMode);
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
                    false, false, 0, 0);
        }
    }

    public void paintEdge(Graphics2D g, JIPipeDesktopGraphEdgeUI.SegmentedLines segmentedLines, ArrowHeadMode arrowHeadMode) {
        if(segmentedLines.size() < 2) {
            return;
        }
        if(segmentedLines.size() == 2) {
            // A simple line
            int x1 = segmentedLines.getX(0);
            int y1 = segmentedLines.getY(0);
            int x2 = segmentedLines.getX(1);
            int y2 = segmentedLines.getY(1);
            return;
        }
        for (int i = 1; i < segmentedLines.size(); i++) {
            int x1 = segmentedLines.getX(i -1);
            int y1 = segmentedLines.getY(i-1);
            int x2 = segmentedLines.getX(i);
            int y2 = segmentedLines.getY(i);
            if(x1 == x2 && y1 == y2) {
                continue;
            }
        }
    }

    public enum ArrowHeadMode {
        None,
        Filled,
        Thin
    }
}
