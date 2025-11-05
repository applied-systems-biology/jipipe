package org.hkijena.jipipe.desktop.app.grapheditor.commons.canvas.managers;

import org.hkijena.jipipe.JIPipe;
import org.hkijena.jipipe.desktop.app.grapheditor.commons.JIPipeDesktopGraphCanvasUI;
import org.hkijena.jipipe.desktop.app.grapheditor.commons.canvas.JIPipeDesktopGraphCanvasGrid;
import org.hkijena.jipipe.desktop.app.grapheditor.commons.edgeui.JIPipeDesktopGraphEdgeControlPointUI;
import org.hkijena.jipipe.desktop.app.grapheditor.commons.edgeui.JIPipeDesktopGraphEdgeUI;
import org.hkijena.jipipe.utils.StringUtils;

import java.awt.*;
import java.awt.geom.Rectangle2D;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

public class JIPipeDesktopGraphCanvasEdgeManager {
    private final JIPipeDesktopGraphCanvasUI canvasUI;

    public JIPipeDesktopGraphCanvasEdgeManager(JIPipeDesktopGraphCanvasUI canvasUI) {
        this.canvasUI = canvasUI;
    }

    /**
     * Checks if the mouse coordinates are over a specific edge UI.
     * This method implements both bounding box and precise line detection.
     *
     * @param mouseX the mouse x coordinate
     * @param mouseY the mouse y coordinate
     * @param edgeUI the edge UI to test
     * @return true if mouse is over the edge, false otherwise
     */
    public boolean isMouseOverEdge(int mouseX, int mouseY, JIPipeDesktopGraphEdgeUI edgeUI) {
        int hitThreshold = (int) Math.max(1, canvasUI.getZoom() * 4) + 4;

        // Get the rendered line segments from the edge UI
        JIPipeDesktopGraphEdgeUI.SegmentedLines lineSegments = edgeUI.getRenderedLineSegments(1, canvasUI.getZoom(), 0, 0);

        if (lineSegments.isEmpty()) {
            return false;
        }

        // Check each segment for mouse intersection
        for (int i = 0; i < lineSegments.size() - 1; i++) {
            int x1 = lineSegments.getX(i);
            int y1 = lineSegments.getY(i);
            int x2 = lineSegments.getX(i + 1);
            int y2 = lineSegments.getY(i + 1);

            if (isMouseOverLineEdge(mouseX, mouseY, new Point(x1, y1), new Point(x2, y2), hitThreshold)) {
                return true;
            }
        }

        return false;
    }

    /**
     * Checks if mouse is over a straight line edge.
     * For straight lines (horizontal or vertical), uses rectangle-based hit testing.
     * For diagonal lines, uses precise point-to-line distance calculation.
     *
     * @param mouseX       the mouse x coordinate
     * @param mouseY       the mouse y coordinate
     * @param sourcePoint  the source point of the edge
     * @param targetPoint  the target point of the edge
     * @param hitThreshold the distance threshold for hit detection
     * @return true if mouse is over the line edge
     */
    private boolean isMouseOverLineEdge(int mouseX, int mouseY, Point sourcePoint, Point targetPoint, int hitThreshold) {
        // For straight lines (horizontal or vertical), use rectangle-based hit testing
        // For diagonal lines, use precise point-to-line distance calculation
        if (isStraightLine(sourcePoint, targetPoint)) {
            return isMouseOverLineRectangle(mouseX, mouseY, sourcePoint, targetPoint, hitThreshold);
        } else {
            return isMouseOverLineDistance(mouseX, mouseY, sourcePoint, targetPoint, hitThreshold);
        }
    }


    /**
     * Checks if mouse is over a line using rectangle-based hit testing.
     * This method creates a rectangle around the line and tests if the mouse point is inside it.
     * This is more user-friendly for straight line segments (horizontal or vertical).
     *
     * @param mouseX       the mouse x coordinate
     * @param mouseY       the mouse y coordinate
     * @param sourcePoint  the source point of the edge
     * @param targetPoint  the target point of the edge
     * @param hitThreshold the distance threshold for hit detection
     * @return true if mouse is over the line rectangle
     */
    private boolean isMouseOverLineRectangle(int mouseX, int mouseY, Point sourcePoint, Point targetPoint, int hitThreshold) {
        // Calculate the bounding rectangle of the line with hit threshold padding
        Rectangle lineRectangle = new Rectangle(
                Math.min(sourcePoint.x, targetPoint.x),
                Math.min(sourcePoint.y, targetPoint.y),
                Math.abs(targetPoint.x - sourcePoint.x),
                Math.abs(targetPoint.y - sourcePoint.y)
        );

        // Expand the rectangle by the hit threshold on all sides
        lineRectangle.grow(hitThreshold, hitThreshold);
//        System.out.println(lineRectangle);

        // Check if the mouse point is inside the expanded rectangle
        return lineRectangle.contains(mouseX, mouseY);
    }

    /**
     * Checks if mouse is over a line using point-to-line distance calculation.
     * This method calculates the perpendicular distance from the mouse to the line segment.
     * This is used for diagonal lines for precise hit detection.
     *
     * @param mouseX       the mouse x coordinate
     * @param mouseY       the mouse y coordinate
     * @param sourcePoint  the source point of the edge
     * @param targetPoint  the target point of the edge
     * @param hitThreshold the distance threshold for hit detection
     * @return true if mouse is over the line based on distance calculation
     */
    private boolean isMouseOverLineDistance(int mouseX, int mouseY, Point sourcePoint, Point targetPoint, int hitThreshold) {
        // Calculate distance from point to line segment
        // This implements the algorithm for finding the perpendicular distance from a point to a line segment
        double lineLengthSq = (targetPoint.x - sourcePoint.x) * (targetPoint.x - sourcePoint.x) +
                (targetPoint.y - sourcePoint.y) * (targetPoint.y - sourcePoint.y);

        if (lineLengthSq == 0) {
            // Source and target are the same point, just check distance to source
            int distanceToSource = (mouseX - sourcePoint.x) * (mouseX - sourcePoint.x) +
                    (mouseY - sourcePoint.y) * (mouseY - sourcePoint.y);
            return distanceToSource <= hitThreshold * hitThreshold;
        }

        // Calculate parameter t for projection onto line segment
        double t = ((mouseX - sourcePoint.x) * (targetPoint.x - sourcePoint.x) +
                (mouseY - sourcePoint.y) * (targetPoint.y - sourcePoint.y)) / lineLengthSq;

        // Clamp t to [0, 1] to ensure projection is on the line segment
        t = Math.max(0, Math.min(1, t));

        // Calculate closest point on line segment
        int closestX = sourcePoint.x + (int) (t * (targetPoint.x - sourcePoint.x));
        int closestY = sourcePoint.y + (int) (t * (targetPoint.y - sourcePoint.y));

        // Calculate squared distance from mouse to closest point
        int distanceSq = (mouseX - closestX) * (mouseX - closestX) +
                (mouseY - closestY) * (mouseY - closestY);

        // Add a small buffer to the hit threshold for better user experience
        int effectiveHitThreshold = hitThreshold + 2;
        return distanceSq <= effectiveHitThreshold * effectiveHitThreshold;
    }


    /**
     * Returns all edges that intersect with the specified rectangle.
     * This method efficiently finds edges by using bounding box culling and precise geometric testing.
     * It supports both straight line and elbow edge shapes, working with zoom-aware coordinates.
     *
     * @param rectangle the rectangle to test against (in screen coordinates)
     * @return collection of edges that intersect with the rectangle
     */
    public Collection<JIPipeDesktopGraphEdgeUI> getEdgesIntersectingRectangle(Rectangle rectangle) {
        return getEdgesIntersectingRectangle((int) rectangle.getX(), (int) rectangle.getY(),
                (int) rectangle.getWidth(), (int) rectangle.getHeight());
    }

    /**
     * Returns all edges that intersect with the specified rectangle.
     * This method efficiently finds edges by using bounding box culling and precise geometric testing.
     * It supports both straight line and elbow edge shapes, working with zoom-aware coordinates.
     *
     * @param x      the x coordinate of the rectangle (in screen coordinates)
     * @param y      the y coordinate of the rectangle (in screen coordinates)
     * @param width  the width of the rectangle (in screen coordinates)
     * @param height the height of the rectangle (in screen coordinates)
     * @return collection of edges that intersect with the rectangle
     */
    public List<JIPipeDesktopGraphEdgeUI> getEdgesIntersectingRectangle(int x, int y, int width, int height) {
        Rectangle queryRect = new Rectangle(x, y, width, height);
        List<JIPipeDesktopGraphEdgeUI> result = new ArrayList<>();

        // Get all edge UIs from the canvas
        for (JIPipeDesktopGraphEdgeUI edgeUI : canvasUI.getEdgeUIs().values()) {
            if (doesEdgeIntersectRectangle(edgeUI, queryRect)) {
                result.add(edgeUI);
            }
        }

        return result;
    }

    /**
     * Checks if a specific edge intersects with the given rectangle.
     * Uses bounding box culling for efficiency, then performs precise geometric testing.
     *
     * @param edgeUI    the edge to test
     * @param rectangle the rectangle to test against (in screen coordinates)
     * @return true if the edge intersects with the rectangle, false otherwise
     */
    public boolean doesEdgeIntersectRectangle(JIPipeDesktopGraphEdgeUI edgeUI, Rectangle rectangle) {
        int hitThreshold = (int) Math.max(1, canvasUI.getZoom() * 4) + 4;

        // Get the rendered line segments from the edge UI
        JIPipeDesktopGraphEdgeUI.SegmentedLines lineSegments = edgeUI.getRenderedLineSegments(canvasUI.getZoom(), canvasUI.getZoom(), 0, 0);

        if (lineSegments.isEmpty()) {
            return false;
        }

        // Check each segment for rectangle intersection
        for (int i = 0; i < lineSegments.size() - 1; i++) {
            int x1 = lineSegments.getX(i);
            int y1 = lineSegments.getY(i);
            int x2 = lineSegments.getX(i + 1);
            int y2 = lineSegments.getY(i + 1);

            if (isLineIntersectingRectangle(new Point(x1, y1), new Point(x2, y2), rectangle, hitThreshold)) {
                return true;
            }
        }

        return false;
    }

    /**
     * Tests if a straight line intersects with a rectangle.
     * Uses line-rectangle intersection algorithm.
     *
     * @param sourcePoint  the source point of the line
     * @param targetPoint  the target point of the line
     * @param rectangle    the rectangle to test against
     * @param hitThreshold the line width
     * @return true if the line intersects with the rectangle, false otherwise
     */
    private boolean isLineIntersectingRectangle(Point sourcePoint, Point targetPoint, Rectangle2D rectangle, int hitThreshold) {
        // Check if either endpoint is inside the rectangle
        if (rectangle.contains(sourcePoint.x, sourcePoint.y) || rectangle.contains(targetPoint.x, targetPoint.y)) {
            return true;
        }

//        System.out.println("ri " + sourcePoint + " <> " + targetPoint + " on " + rectangle);

        // For straight lines (horizontal or vertical), use rectangle-based hit testing
        if (isStraightLine(sourcePoint, targetPoint)) {
            return isLineRectangleIntersecting(sourcePoint, targetPoint, rectangle, hitThreshold);
        } else {
            return isLineSegmentIntersectingRectangle(sourcePoint, targetPoint, rectangle, hitThreshold);
        }
    }

    /**
     * Determines if a line is straight (horizontal or vertical).
     *
     * @param sourcePoint the source point of the edge
     * @param targetPoint the target point of the edge
     * @return true if the line is horizontal or vertical, false for diagonal lines
     */
    private boolean isStraightLine(Point sourcePoint, Point targetPoint) {
        return sourcePoint.x == targetPoint.x || sourcePoint.y == targetPoint.y;
    }

    /**
     * Tests if a straight line (horizontal or vertical) intersects with a rectangle.
     * More efficient for axis-aligned lines.
     *
     * @param sourcePoint  the source point of the line
     * @param targetPoint  the target point of the line
     * @param rectangle    the rectangle to test against
     * @param hitThreshold the line width
     * @return true if the line intersects with the rectangle, false otherwise
     */
    private boolean isLineRectangleIntersecting(Point sourcePoint, Point targetPoint, Rectangle2D rectangle, int hitThreshold) {
        // Calculate the bounding rectangle of the line with hit threshold expansion
        Rectangle lineRectangle = new Rectangle(
                Math.min(sourcePoint.x, targetPoint.x),
                Math.min(sourcePoint.y, targetPoint.y),
                Math.abs(targetPoint.x - sourcePoint.x),
                Math.abs(targetPoint.y - sourcePoint.y)
        );

        // Expand the rectangle by the hit threshold on all sides to handle zero-area rectangles
        if (hitThreshold > 0) {
            lineRectangle.grow(hitThreshold, hitThreshold);
        }

        // Test for rectangle intersection
        return lineRectangle.intersects(rectangle);
    }

    /**
     * Tests if a line segment intersects with a rectangle using proper line-rectangle intersection.
     * Uses the Liang-Barsky algorithm for clipping line segments against a rectangle.
     *
     * @param sourcePoint  the source point of the line segment
     * @param targetPoint  the target point of the line segment
     * @param rectangle    the rectangle to test against
     * @param hitThreshold the line width
     * @return true if the line segment intersects with the rectangle, false otherwise
     */
    private boolean isLineSegmentIntersectingRectangle(Point sourcePoint, Point targetPoint, Rectangle2D rectangle, int hitThreshold) {
        // Use Liang-Barsky line clipping algorithm
        double x1 = sourcePoint.x;
        double y1 = sourcePoint.y;
        double x2 = targetPoint.x;
        double y2 = targetPoint.y;

        double xmin = rectangle.getX();
        double ymin = rectangle.getY();
        double xmax = rectangle.getX() + rectangle.getWidth();
        double ymax = rectangle.getY() + rectangle.getHeight();

        double dx = x2 - x1;
        double dy = y2 - y1;

        double[] p = {-dx, dx, -dy, dy};
        double[] q = {x1 - xmin, xmax - x1, y1 - ymin, ymax - y1};

        double u1 = 0.0;
        double u2 = 1.0;

        for (int i = 0; i < 4; i++) {
            if (p[i] == 0) {
                // Line is parallel to the clipping boundary
                if (q[i] < 0) {
                    return false; // Line is outside the boundary
                }
            } else {
                double r = q[i] / p[i];
                if (p[i] < 0) {
                    if (r > u2) return false;
                    if (r > u1) u1 = r;
                } else {
                    if (r < u1) return false;
                    if (r < u2) u2 = r;
                }
            }
        }

        return true; // Line intersects the rectangle
    }


    public boolean addControlPointToEdge(JIPipeDesktopGraphEdgeUI edgeUI, Point realLocation) {
        Point toGrid = JIPipeDesktopGraphCanvasGrid.realLocationToGrid(realLocation, canvasUI.getZoom());
        for (Point existingControlPoint : edgeUI.getControlPointsInGridCoordinates()) {
            if (existingControlPoint.equals(toGrid)) {
                canvasUI.getNotificationsManager().addNotification("Edge control point already exists at this location",
                        JIPipe.RESOURCES.getIcon16("actions/node-add.png"),
                        JIPipeDesktopGraphCanvasNotificationsManager.NotificationType.Error);
                return false;
            }
        }

        int hitThreshold = (int) Math.max(1, canvasUI.getZoom() * 4) + 4;

        // Get the rendered line segments from the edge UI
        JIPipeDesktopGraphEdgeUI.SegmentedLines lineSegments = edgeUI.getRenderedLineSegments(canvasUI.getZoom(), canvasUI.getZoom(), 0, 0);

        if (lineSegments.isEmpty()) {
            return false;
        }

        // Check each segment for mouse intersection
        for (int i = 0; i < lineSegments.size() - 1; i++) {
            int x1 = lineSegments.getX(i);
            int y1 = lineSegments.getY(i);
            int x2 = lineSegments.getX(i + 1);
            int y2 = lineSegments.getY(i + 1);
            int index = lineSegments.getSegmentIndex(i);

            if (isMouseOverLineEdge(realLocation.x, realLocation.y, new Point(x1, y1), new Point(x2, y2), hitThreshold)) {
                edgeUI.addControlPoint(index, StringUtils.nullToEmpty(canvasUI.getCompartmentUUID()), toGrid.x, toGrid.y);
                canvasUI.repaintLowLag();
                return true;
            }
        }

        return false;
    }

    public boolean isMouseOverEdgeControlPoint(int mouseX, int mouseY, JIPipeDesktopGraphEdgeUI edgeUI, JIPipeDesktopGraphEdgeControlPointUI controlPoint) {
        return controlPoint.doesContainPoint(mouseX, mouseY);
    }

    public void clearControlPoints(JIPipeDesktopGraphEdgeUI edgeUI) {
        edgeUI.clearControlPoints();
    }
}
