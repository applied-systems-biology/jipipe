package org.hkijena.jipipe.desktop.app.grapheditor.commons.canvas;

import gnu.trove.list.array.TIntArrayList;
import org.hkijena.jipipe.api.nodes.JIPipeGraphEdge;
import org.hkijena.jipipe.desktop.app.grapheditor.commons.JIPipeDesktopGraphCanvasUI;
import org.hkijena.jipipe.desktop.app.grapheditor.commons.edgeui.JIPipeDesktopGraphEdgeUI;
import org.hkijena.jipipe.desktop.app.grapheditor.commons.nodeui.JIPipeDesktopGraphNodeUI;
import org.hkijena.jipipe.utils.PointRange;

import java.awt.*;

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
            case Line -> isMouseOverLineEdge(mouseX, mouseY, sourcePointRange.center, targetPointRange.center, hitThreshold);
            case Elbow -> isMouseOverElbowEdge(mouseX, mouseY, sourcePointRange.center, sourceNodeUI.getBounds(), targetPointRange.center, hitThreshold);
            default -> false;
        };
    }

    /**
     * Checks if mouse is over a straight line edge.
     * For straight lines (horizontal or vertical), uses rectangle-based hit testing.
     * For diagonal lines, uses precise point-to-line distance calculation.
     *
     * @param mouseX the mouse x coordinate
     * @param mouseY the mouse y coordinate
     * @param sourcePoint the source point of the edge
     * @param targetPoint the target point of the edge
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
     * Determines if a line is straight (horizontal or vertical).
     * This is used to decide between rectangle-based and distance-based hit testing.
     *
     * @param sourcePoint the source point of the edge
     * @param targetPoint the target point of the edge
     * @return true if the line is horizontal or vertical, false for diagonal lines
     */
    private boolean isStraightLine(Point sourcePoint, Point targetPoint) {
        return sourcePoint.x == targetPoint.x || sourcePoint.y == targetPoint.y;
    }

    /**
     * Checks if mouse is over a line using rectangle-based hit testing.
     * This method creates a rectangle around the line and tests if the mouse point is inside it.
     * This is more user-friendly for straight line segments (horizontal or vertical).
     *
     * @param mouseX the mouse x coordinate
     * @param mouseY the mouse y coordinate
     * @param sourcePoint the source point of the edge
     * @param targetPoint the target point of the edge
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
     * @param mouseX the mouse x coordinate
     * @param mouseY the mouse y coordinate
     * @param sourcePoint the source point of the edge
     * @param targetPoint the target point of the edge
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
     * @param mouseX the mouse x coordinate
     * @param mouseY the mouse y coordinate
     * @param sourcePoint the source point of the edge
     * @param targetBounds the bounds of the target node
     * @param targetPoint the target point of the edge
     * @param hitThreshold the distance threshold for hit detection
     * @return true if mouse is over the elbow edge
     */
    private boolean isMouseOverElbowEdge(int mouseX, int mouseY, Point sourcePoint, Rectangle targetBounds, Point targetPoint, int hitThreshold) {
        // Use the paint manager's coordinate generation logic for consistency
        TIntArrayList xCoords = new TIntArrayList(8);
        TIntArrayList yCoords = new TIntArrayList(8);

        // Generate coordinates using the same method as rendering
        canvasUI.getPaintManager().createElbowEdgeCoordinates(sourcePoint, targetBounds, targetPoint, 1, 0, 0, true, xCoords, yCoords);

        // Process the coordinate arrays returned by createElbowEdgeCoordinates
        for (int i = 0; i < xCoords.size() - 1; i++) {
            Point segmentStart = new Point(xCoords.get(i), yCoords.get(i));
            Point segmentEnd = new Point(xCoords.get(i + 1), yCoords.get(i + 1));

            if (isMouseOverLineEdge(mouseX, mouseY, segmentStart, segmentEnd, hitThreshold)) {
                return true;
            }
        }

        return false;
    }
}
