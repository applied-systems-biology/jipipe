/*
 * Copyright by Zoltán Cseresnyés, Ruman Gerst
 *
 * Research Group Applied Systems Biology - Head: Prof. Dr. Marc Thilo Figge
 * https://www.leibniz-hki.de/en/applied-systems-biology.html
 * HKI-Center for Systems Biology of Infection
 * Leibniz Institute for Natural Product Research and Infection Biology - Hans Knöll Institute (HKI)
 * Adolf-Reichwein-Straße 23, 07745 Jena, Germany
 *
 * The project code is licensed under MIT.
 * See the LICENSE file provided with the code for the full license.
 */

package org.hkijena.jipipe.desktop.app.grapheditor.commons.contextmenu.actions;

import com.fasterxml.jackson.core.JsonProcessingException;
import org.hkijena.jipipe.JIPipe;
import org.hkijena.jipipe.api.data.JIPipeDataSlot;
import org.hkijena.jipipe.api.nodes.JIPipeGraphEdge;
import org.hkijena.jipipe.api.nodes.JIPipeGraphEdgeControlPoint;
import org.hkijena.jipipe.desktop.app.grapheditor.commons.JIPipeDesktopGraphCanvasUI;
import org.hkijena.jipipe.desktop.app.grapheditor.commons.canvas.JIPipeDesktopGraphCanvasGrid;
import org.hkijena.jipipe.desktop.app.grapheditor.commons.canvas.managers.JIPipeDesktopGraphCanvasEdgeManager;
import org.hkijena.jipipe.desktop.app.grapheditor.commons.canvas.managers.JIPipeDesktopGraphCanvasNotificationsManager;
import org.hkijena.jipipe.desktop.app.grapheditor.commons.contextmenu.EdgesOnlyUIContextAction;
import org.hkijena.jipipe.desktop.app.grapheditor.commons.edgeui.JIPipeDesktopGraphEdgeUI;
import org.hkijena.jipipe.utils.PointRange;
import org.hkijena.jipipe.utils.StringUtils;
import org.hkijena.jipipe.utils.UIUtils;
import org.hkijena.jipipe.utils.json.JsonUtils;

import javax.swing.*;
import java.awt.*;
import java.awt.event.KeyEvent;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

public class AddEdgeControlPointUIContextAction implements EdgesOnlyUIContextAction {

    @Override
    public String getName() {
        return "Split edge";
    }

    @Override
    public String getDescription() {
        return "Adds a control point ";
    }

    @Override
    public Icon getIcon() {
        return JIPipe.RESOURCES.getIcon16("actions/node-add.png");
    }

    @Override
    public boolean showInMultiSelectionPanel() {
        return false;
    }

    @Override
    public boolean matchesEdges(Set<JIPipeDesktopGraphEdgeUI> selection) {
        return !selection.isEmpty();
    }

    @Override
    public void runEdges(JIPipeDesktopGraphCanvasUI canvasUI, Set<JIPipeDesktopGraphEdgeUI> selection) {
        Point graphEditorCursor = canvasUI.getGraphEditorCursor();
        if (graphEditorCursor == null) {
            return;
        }

        // Use the edge manager for hit detection and finding the best edge
        JIPipeDesktopGraphCanvasEdgeManager edgeManager = canvasUI.getEdgeManager();
        
        // Find the best edge to add a control point to based on cursor position
        JIPipeDesktopGraphEdgeUI bestEdge = null;
        double minDistance = Double.MAX_VALUE;
        
        for (JIPipeDesktopGraphEdgeUI edgeUI : selection) {
            if (edgeManager.isMouseOverEdge(graphEditorCursor.x, graphEditorCursor.y, edgeUI)) {
                // Calculate distance from cursor to edge center for selection
                PointRange sourcePoint = edgeUI.getSourcePointRange();
                PointRange targetPoint = edgeUI.getTargetPointRange();
                
                if (sourcePoint != null && targetPoint != null) {
                    PointRange.tighten(sourcePoint, targetPoint);
                    Point edgeCenter = new Point(
                        (sourcePoint.center.x + targetPoint.center.x) / 2,
                        (sourcePoint.center.y + targetPoint.center.y) / 2
                    );
                    
                    double distance = graphEditorCursor.distance(edgeCenter);
                    if (distance < minDistance) {
                        minDistance = distance;
                        bestEdge = edgeUI;
                    }
                }
            }
        }
        
        // Add control point to the best edge
        if (bestEdge != null) {
            addControlPointToEdge(bestEdge, graphEditorCursor, canvasUI);
        }
    }

    /**
     * Adds a control point to the specified edge at the given cursor position
     * @param edgeUI The edge UI to modify
     * @param cursorPosition The cursor position in real coordinates
     * @param canvasUI The canvas UI
     */
    private void addControlPointToEdge(JIPipeDesktopGraphEdgeUI edgeUI, Point cursorPosition, JIPipeDesktopGraphCanvasUI canvasUI) {
        JIPipeGraphEdge edge = edgeUI.getEdge();
        
        // Convert cursor position from real coordinates to grid coordinates
        Point gridCursor = JIPipeDesktopGraphCanvasGrid.realLocationToGrid(cursorPosition, canvasUI.getZoom());
        
        // Get source and target node positions
        PointRange sourcePoint = edgeUI.getSourcePointRange();
        PointRange targetPoint = edgeUI.getTargetPointRange();
        
        if (sourcePoint == null || targetPoint == null) {
            return;
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
        }
    }

    /**
     * Finds the best segment index to add a control point
     * @param edge The edge to analyze
     * @param sourceGrid Source position in grid coordinates
     * @param targetGrid Target position in grid coordinates
     * @param cursorGrid Cursor position in grid coordinates
     * @param canvasUI The canvas UI
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
        double minDistance = Double.MAX_VALUE;
        int bestSegmentIndex = -1;

        for (int i = 0; i < segmentPoints.size() - 1; i++) {
            Point p1 = segmentPoints.get(i);
            Point p2 = segmentPoints.get(i + 1);
            
            // Calculate distance from cursor to the line segment
            double distance = distanceToLineSegment(cursorGrid, p1, p2);
            
            if (distance < minDistance) {
                minDistance = distance;
                bestSegmentIndex = i; // Insert after this point
            }
        }

        return bestSegmentIndex + 1; // Convert to insertion index
    }

    /**
     * Calculates the minimum distance from a point to a line segment
     * @param point The point to measure distance from
     * @param lineStart Start of the line segment
     * @param lineEnd End of the line segment
     * @return The minimum distance
     */
    private double distanceToLineSegment(Point point, Point lineStart, Point lineEnd) {
        // Vector from lineStart to lineEnd
        double dx = lineEnd.x - lineStart.x;
        double dy = lineEnd.y - lineStart.y;
        
        // Vector from lineStart to point
        double px = point.x - lineStart.x;
        double py = point.y - lineStart.y;
        
        // Calculate the dot product
        double dot = px * dx + py * dy;
        double lenSq = dx * dx + dy * dy;
        
        // Handle case where line segment has zero length
        if (lenSq == 0) {
            return Math.sqrt(px * px + py * py);
        }
        
        // Calculate parameter t for the closest point on the line
        double t = Math.max(0, Math.min(1, dot / lenSq));
        
        // Calculate the closest point on the line segment
        double closestX = lineStart.x + t * dx;
        double closestY = lineStart.y + t * dy;
        
        // Calculate distance to the closest point
        double distanceX = point.x - closestX;
        double distanceY = point.y - closestY;
        
        return Math.sqrt(distanceX * distanceX + distanceY * distanceY);
    }
}
