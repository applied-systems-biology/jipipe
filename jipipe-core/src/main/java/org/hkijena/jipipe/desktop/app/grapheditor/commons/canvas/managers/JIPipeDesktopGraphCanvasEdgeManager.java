package org.hkijena.jipipe.desktop.app.grapheditor.commons.canvas.managers;

import gnu.trove.list.array.TIntArrayList;
import org.hkijena.jipipe.api.nodes.JIPipeGraphEdge;
import org.hkijena.jipipe.api.nodes.JIPipeGraphEdgeControlPoint;
import org.hkijena.jipipe.desktop.app.grapheditor.commons.JIPipeDesktopGraphCanvasUI;
import org.hkijena.jipipe.desktop.app.grapheditor.commons.canvas.JIPipeDesktopGraphCanvasGrid;
import org.hkijena.jipipe.desktop.app.grapheditor.commons.edgeui.JIPipeDesktopGraphEdgeUI;
import org.hkijena.jipipe.desktop.app.grapheditor.commons.nodeui.JIPipeDesktopGraphNodeUI;
import org.hkijena.jipipe.utils.PointRange;
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

        JIPipeDesktopGraphNodeUI sourceNodeUI = edgeUI.getSourceNodeUI();
        JIPipeDesktopGraphNodeUI targetNodeUI = edgeUI.getTargetNodeUI();

        // Skip edge if nodes don't exist
        if (sourceNodeUI == null || targetNodeUI == null) {
            return false;
        }

        PointRange sourcePointRange = edgeUI.getSourcePointRange();
        PointRange targetPointRange = edgeUI.getTargetPointRange();

        if (sourcePointRange == null || targetPointRange == null) {
            return false;
        }

        // Tighten the point ranges: Bringing the centers together
        sourcePointRange.add(sourceNodeUI.getLocation());
        targetPointRange.add(targetNodeUI.getLocation());
        PointRange.tighten(sourcePointRange, targetPointRange);

        // Check bounding box first for fast rejection
        Rectangle edgeBounds = new Rectangle(
                Math.min(sourcePointRange.center.x, targetPointRange.center.x),
                Math.min(sourcePointRange.center.y, targetPointRange.center.y),
                Math.abs(targetPointRange.center.x - sourcePointRange.center.x),
                Math.abs(targetPointRange.center.y - sourcePointRange.center.y)
        );

        // Expand bounding box by hit threshold with some margin
        edgeBounds.grow(hitThreshold, hitThreshold);

        if (!edgeBounds.contains(mouseX, mouseY)) {
            return false;
        }
//
//        System.out.println("Test: " + edgeUI.getSource() + " -> " + edgeUI.getTarget());

        // Perform precise hit detection based on edge shape
        JIPipeGraphEdge.Shape edgeShape = edgeUI.getEdge().getUiShape();

        return switch (edgeShape) {
            case Line ->
                    isMouseOverLineEdge(mouseX, mouseY, sourcePointRange.center, targetPointRange.center, hitThreshold);
            case Elbow ->
                    isMouseOverElbowEdge(mouseX, mouseY, sourcePointRange.center, sourceNodeUI.getBounds(), targetPointRange.center, hitThreshold, edgeUI);
            default -> false;
        };
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
     * Checks if mouse is over an elbow edge (L-shaped edge).
     * Uses the shared createElbowEdgeCoordinates method for consistent rendering and hit detection.
     *
     * @param mouseX       the mouse x coordinate
     * @param mouseY       the mouse y coordinate
     * @param sourcePoint  the source point of the edge
     * @param targetBounds the bounds of the target node
     * @param targetPoint  the target point of the edge
     * @param hitThreshold the distance threshold for hit detection
     * @return true if mouse is over the elbow edge
     */
    private boolean isMouseOverElbowEdge(int mouseX, int mouseY, Point sourcePoint, Rectangle targetBounds, Point targetPoint, int hitThreshold, JIPipeDesktopGraphEdgeUI edgeUI) {
        // Get control points from the edge UI
        List<Point> controlPoints = new ArrayList<>();
        if (edgeUI != null) {
            String compartmentUUID = StringUtils.nullToEmpty(canvasUI.getCompartmentUUID());
            for (JIPipeGraphEdgeControlPoint controlPoint : edgeUI.getEdge().getControlPoints()) {
                Point gridLocation = controlPoint.getLocationWithin(compartmentUUID);
                if (gridLocation != null) {
                    Point realLocation = JIPipeDesktopGraphCanvasGrid.gridToRealLocation(gridLocation, canvasUI.getZoom());
                    controlPoints.add(realLocation);
                }
            }
        }

        // If there are control points, process each segment
        if (!controlPoints.isEmpty()) {
            Point currentSource = sourcePoint;
            for (Point controlPoint : controlPoints) {
                if (isMouseOverLineEdge(mouseX, mouseY, currentSource, controlPoint, hitThreshold)) {
                    return true;
                }
                currentSource = controlPoint;
            }
            // Check the final segment to the target
            if (isMouseOverLineEdge(mouseX, mouseY, currentSource, targetPoint, hitThreshold)) {
                return true;
            }
        } else {
            // No control points, use the original elbow edge logic
            TIntArrayList xCoords = new TIntArrayList(8);
            TIntArrayList yCoords = new TIntArrayList(8);

            // Generate coordinates using the same method as rendering
            canvasUI.getPaintManager().createElbowEdgeCoordinates(sourcePoint, targetBounds, targetPoint, canvasUI.getZoom(), 0, 0, JIPipeDesktopGraphCanvasPaintManager.ArrowHeadMode.None, xCoords, yCoords);

            // Process the coordinate arrays returned by createElbowEdgeCoordinates
            for (int i = 0; i < xCoords.size() - 1; i++) {
                Point segmentStart = new Point(xCoords.get(i), yCoords.get(i));
                Point segmentEnd = new Point(xCoords.get(i + 1), yCoords.get(i + 1));

                if (isMouseOverLineEdge(mouseX, mouseY, segmentStart, segmentEnd, hitThreshold)) {
                    return true;
                }
            }
        }

        return false;
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

        // Skip edge if nodes don't exist
        JIPipeDesktopGraphNodeUI sourceNodeUI = edgeUI.getSourceNodeUI();
        JIPipeDesktopGraphNodeUI targetNodeUI = edgeUI.getTargetNodeUI();
        if (sourceNodeUI == null || targetNodeUI == null) {
            return false;
        }

        PointRange sourcePointRange = edgeUI.getSourcePointRange();
        PointRange targetPointRange = edgeUI.getTargetPointRange();
        if (sourcePointRange == null || targetPointRange == null) {
            return false;
        }

        // Tighten the point ranges: Bringing the centers together
        sourcePointRange.add(sourceNodeUI.getLocation());
        targetPointRange.add(targetNodeUI.getLocation());
        PointRange.tighten(sourcePointRange, targetPointRange);

        // Perform precise intersection testing based on edge shape
        JIPipeGraphEdge.Shape edgeShape = edgeUI.getEdge().getUiShape();
        return switch (edgeShape) {
            case Line ->
                    isLineIntersectingRectangle(sourcePointRange.center, targetPointRange.center, rectangle, hitThreshold);
            case Elbow -> isElbowIntersectingRectangle(sourcePointRange.center, sourceNodeUI.getBounds(),
                    targetPointRange.center, rectangle, hitThreshold, edgeUI);
            default -> false;
        };
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

    /**
     * Tests if an elbow (L-shaped) edge intersects with a rectangle.
     * Generates elbow coordinates and tests each segment for intersection.
     *
     * @param sourcePoint  the source point of the elbow edge
     * @param sourceBounds the bounds of the source node
     * @param targetPoint  the target point of the elbow edge
     * @param rectangle    the rectangle to test against
     * @param hitThreshold the width/height of the rectangle
     * @return true if the elbow edge intersects with the rectangle, false otherwise
     */
    private boolean isElbowIntersectingRectangle(Point sourcePoint, Rectangle sourceBounds,
                                                 Point targetPoint, Rectangle2D rectangle, int hitThreshold, JIPipeDesktopGraphEdgeUI edgeUI) {
        // Get control points from the edge UI
        List<Point> controlPoints = new ArrayList<>();
        if (edgeUI != null) {
            String compartmentUUID = StringUtils.nullToEmpty(canvasUI.getCompartmentUUID());
            for (JIPipeGraphEdgeControlPoint controlPoint : edgeUI.getEdge().getControlPoints()) {
                Point gridLocation = controlPoint.getLocationWithin(compartmentUUID);
                if (gridLocation != null) {
                    Point realLocation = JIPipeDesktopGraphCanvasGrid.gridToRealLocation(gridLocation, canvasUI.getZoom());
                    controlPoints.add(realLocation);
                }
            }
        }

        // If there are control points, process each segment
        if (!controlPoints.isEmpty()) {
            Point currentSource = sourcePoint;
            for (Point controlPoint : controlPoints) {
                if (isLineIntersectingRectangle(currentSource, controlPoint, rectangle, hitThreshold)) {
                    return true;
                }
                currentSource = controlPoint;
            }
            // Check the final segment to the target
            if (isLineIntersectingRectangle(currentSource, targetPoint, rectangle, hitThreshold)) {
                return true;
            }
        } else {
            // No control points, use the original elbow edge logic
            TIntArrayList xCoords = new TIntArrayList(8);
            TIntArrayList yCoords = new TIntArrayList(8);

            canvasUI.getPaintManager().createElbowEdgeCoordinates(sourcePoint, sourceBounds, targetPoint,
                    canvasUI.getZoom(), 0, 0, JIPipeDesktopGraphCanvasPaintManager.ArrowHeadMode.None, xCoords, yCoords);

            // Test each segment of the elbow against the rectangle
            for (int i = 0; i < xCoords.size() - 1; i++) {
                Point segmentStart = new Point(xCoords.get(i), yCoords.get(i));
                Point segmentEnd = new Point(xCoords.get(i + 1), yCoords.get(i + 1));

                if (isLineIntersectingRectangle(segmentStart, segmentEnd, rectangle, hitThreshold)) {
                    return true;
                }
            }
        }

        return false;
    }

    public boolean addControlPointToEdge(JIPipeDesktopGraphEdgeUI edgeUI, Point realLocation) {
        JIPipeGraphEdge edge = edgeUI.getEdge();

        // Convert cursor position from real coordinates to grid coordinates
        Point gridCursor = JIPipeDesktopGraphCanvasGrid.realLocationToGrid(realLocation, canvasUI.getZoom());

        // Get source and target node positions
        PointRange sourcePoint = edgeUI.getSourcePointRange();
        PointRange targetPoint = edgeUI.getTargetPointRange();

        if (sourcePoint == null || targetPoint == null) {
            return false;
        }

        // Convert to grid coordinates
        Point sourceGrid = JIPipeDesktopGraphCanvasGrid.realLocationToGrid(sourcePoint.center, canvasUI.getZoom());
        Point targetGrid = JIPipeDesktopGraphCanvasGrid.realLocationToGrid(targetPoint.center, canvasUI.getZoom());

        // Find the best segment to add the control point
        int bestSegmentIndex = findBestSegmentForControlPoint(edge, sourceGrid, targetGrid, gridCursor, canvasUI);

        if (bestSegmentIndex >= 0) {
            // Create a new control point
            JIPipeGraphEdgeControlPoint newControlPoint = new JIPipeGraphEdgeControlPoint();
            newControlPoint.setLocationWithin(canvasUI.getCompartmentUUID(), gridCursor);

            // Insert the control point at the appropriate position
            edge.getControlPoints().add(bestSegmentIndex, newControlPoint);
            canvasUI.repaintLowLag();
            return true;
        }

        return false;
    }

    /**
     * Finds the best segment index to add a control point
     *
     * @param edge       The edge to analyze
     * @param sourceGrid Source position in grid coordinates
     * @param targetGrid Target position in grid coordinates
     * @param cursorGrid Cursor position in grid coordinates
     * @param canvasUI   The canvas UI
     * @return The segment index where the control point should be added, or -1 if no suitable segment found
     */
    private int findBestSegmentForControlPoint(JIPipeGraphEdge edge, Point sourceGrid, Point targetGrid, Point cursorGrid, JIPipeDesktopGraphCanvasUI canvasUI) {
        List<JIPipeGraphEdgeControlPoint> controlPoints = edge.getControlPoints();

        // If no control points exist, add at the beginning (between source and target)
        if (controlPoints.isEmpty()) {
            return 0;
        }

        // Create a list of all segment endpoints (source, control points, target)
        List<Point> segmentPoints = new ArrayList<>();
        segmentPoints.add(sourceGrid);

        String compartmentUUID = StringUtils.nullToEmpty(canvasUI.getCompartmentUUID());
        for (JIPipeGraphEdgeControlPoint controlPoint : controlPoints) {
            Point location = controlPoint.getLocationWithin(compartmentUUID);
            if (location != null) {
                segmentPoints.add(location);
            }
        }
        segmentPoints.add(targetGrid);

        // Find the segment closest to the cursor
        int bestSegmentIndex = -1;

        for (int i = 0; i < segmentPoints.size() - 1; i++) {
            Point p1 = segmentPoints.get(i);
            Point p2 = segmentPoints.get(i + 1);

            // Calculate distance from cursor to the line segment
            // TODO: find the actual segment that the cursor hovers
        }

        return bestSegmentIndex + 1; // Convert to insertion index
    }
}
