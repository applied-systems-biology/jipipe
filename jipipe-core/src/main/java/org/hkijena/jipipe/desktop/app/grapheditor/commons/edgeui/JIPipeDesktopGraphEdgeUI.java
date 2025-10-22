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

package org.hkijena.jipipe.desktop.app.grapheditor.commons.edgeui;

import gnu.trove.list.array.TIntArrayList;
import org.hkijena.jipipe.api.data.JIPipeDataSlot;
import org.hkijena.jipipe.api.nodes.JIPipeGraphEdge;
import org.hkijena.jipipe.api.nodes.JIPipeGraphEdgeControlPoint;
import org.hkijena.jipipe.api.nodes.JIPipeGraphNode;
import org.hkijena.jipipe.api.nodes.JIPipeSerializedGraphConnection;
import org.hkijena.jipipe.desktop.app.grapheditor.commons.JIPipeDesktopGraphCanvasUI;
import org.hkijena.jipipe.desktop.app.grapheditor.commons.JIPipeDesktopGraphInteractiveObjectUI;
import org.hkijena.jipipe.desktop.app.grapheditor.commons.JIPipeDesktopGraphInteractiveObjectUIUpdateViewCommand;
import org.hkijena.jipipe.desktop.app.grapheditor.commons.canvas.JIPipeDesktopGraphCanvasGrid;
import org.hkijena.jipipe.desktop.app.grapheditor.commons.canvas.managers.JIPipeDesktopGraphCanvasPaintManager;
import org.hkijena.jipipe.desktop.app.grapheditor.commons.nodeui.JIPipeDesktopGraphNodeUI;
import org.hkijena.jipipe.plugins.core.nodes.JIPipeCommentNode;
import org.hkijena.jipipe.utils.PointRange;
import org.hkijena.jipipe.utils.StringUtils;
import org.hkijena.jipipe.utils.ThemeUtils;
import org.jetbrains.annotations.NotNull;

import java.awt.*;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;

public class JIPipeDesktopGraphEdgeUI implements JIPipeDesktopGraphInteractiveObjectUI, Comparable<JIPipeDesktopGraphEdgeUI> {
    private final JIPipeDesktopGraphCanvasUI canvasUI;
    private final JIPipeDataSlot source;
    private final JIPipeDataSlot target;
    private final JIPipeGraphEdge edge;

    public JIPipeDesktopGraphEdgeUI(JIPipeDesktopGraphCanvasUI canvasUI, JIPipeDataSlot source, JIPipeDataSlot target, JIPipeGraphEdge edge) {
        this.canvasUI = canvasUI;
        this.source = source;
        this.target = target;
        this.edge = edge;
    }

    public JIPipeSerializedGraphConnection toConnection() {
        return new JIPipeSerializedGraphConnection(source, target, edge);
    }

    public JIPipeGraphEdge getEdge() {
        return edge;
    }

    public JIPipeDataSlot getSource() {
        return source;
    }

    public JIPipeDataSlot getTarget() {
        return target;
    }

    public boolean isCommentEdge() {
        return source.getNode() instanceof JIPipeCommentNode || target.getNode() instanceof JIPipeCommentNode;
    }

    public JIPipeDesktopGraphNodeUI getSourceNodeUI() {
        return canvasUI.getNodeUIs().getOrDefault(source.getNode(), null);
    }

    public JIPipeDesktopGraphNodeUI getTargetNodeUI() {
        return canvasUI.getNodeUIs().getOrDefault(target.getNode(), null);
    }

    public PointRange getSourcePointRange() {
        JIPipeDesktopGraphNodeUI sourceNodeUI = getSourceNodeUI();
        if (sourceNodeUI != null) {
            return sourceNodeUI.getSlotLocation(source);
        }
        return null;
    }

    public PointRange getTargetPointRange() {
        JIPipeDesktopGraphNodeUI targetNodeUI = getTargetNodeUI();
        if (targetNodeUI != null) {
            return targetNodeUI.getSlotLocation(target);
        }
        return null;
    }

    public int getUIManhattanDistance() {
        PointRange sourcePoint = getSourcePointRange();
        PointRange targetPoint = getTargetPointRange();

        if (sourcePoint != null && targetPoint != null) {

            // Tighten the point ranges: Bringing the centers together
            PointRange.tighten(sourcePoint, targetPoint);

            return Math.abs(sourcePoint.center.x - targetPoint.center.x) + Math.abs(sourcePoint.center.y - targetPoint.center.y);
        } else {
            return -1;
        }
    }

    @Override
    public int compareTo(@NotNull JIPipeDesktopGraphEdgeUI o) {
        return Integer.compare(getUIManhattanDistance(), o.getUIManhattanDistance());
    }

    @Override
    public Set<JIPipeGraphNode> getNodes() {
        return Set.of(source.getNode(), target.getNode());
    }

    @Override
    public void updateView(JIPipeDesktopGraphInteractiveObjectUIUpdateViewCommand command) {

    }

    public void paint(Graphics2D g,
                      Stroke stroke,
                      Stroke strokeBorder,
                      double scale,
                      int viewX,
                      int viewY,
                      boolean enableArrows,
                      boolean multiColor,
                      int multiColorIndex,
                      int multiColorMax) {

        JIPipeDesktopGraphNodeUI sourceNodeUI = getSourceNodeUI();
        JIPipeDesktopGraphNodeUI targetNodeUI = getTargetNodeUI();

        if (sourceNodeUI == null || targetNodeUI == null) {
            return;
        }

        PointRange sourcePoint = getSourcePointRange();
        PointRange targetPoint = getTargetPointRange();
        sourcePoint.add(sourceNodeUI.getLocation());
        targetPoint.add(targetNodeUI.getLocation());
        JIPipeGraphEdge.Shape uiShape = edge.getUiShape();

        // Tighten the point ranges: Bringing the centers together
        PointRange.tighten(sourcePoint, targetPoint);

        if (strokeBorder != null) {
            // Fully outlined stroke
            JIPipeDesktopGraphCanvasPaintManager.ArrowHeadMode arrowHeadMode = enableArrows ? JIPipeDesktopGraphCanvasPaintManager.ArrowHeadMode.Filled : JIPipeDesktopGraphCanvasPaintManager.ArrowHeadMode.None;

            if (multiColor) {
                paintMultiColor(g, stroke, strokeBorder, scale, viewX, viewY, multiColor, multiColorIndex, multiColorMax, arrowHeadMode);
            } else {
                paintRegular(g, stroke, strokeBorder, scale, viewX, viewY, sourcePoint, targetPoint, arrowHeadMode);
            }
        } else {
            // Just a single stroke
            paintThin(g, stroke, scale, viewX, viewY, enableArrows, multiColor, multiColorIndex, multiColorMax);
        }
    }

    /**
     * Gets the control points in grid location for the current compartment
     *
     * @return the control points in grid location
     */
    public List<Point> getControlPointsInGridCoordinates() {
        if (edge.getControlPoints().isEmpty()) {
            return Collections.emptyList();
        } else {
            String compartmentUUID = StringUtils.nullToEmpty(canvasUI.getCompartmentUUID());
            List<Point> controlPoints = new ArrayList<>(edge.getControlPoints().size());
            for (JIPipeGraphEdgeControlPoint controlPoint : edge.getControlPoints(compartmentUUID)) {
                controlPoints.add(controlPoint.toPoint());
            }
            return controlPoints;
        }
    }

    /**
     * Gets the rendered line segments in real coordinates
     *
     * @param scale the zoom
     * @param viewX the view x shift
     * @param viewY the view y shift
     * @return the line segments
     */
    public SegmentedLines getRenderedLineSegments(double scale, int viewX, int viewY) {

        JIPipeDesktopGraphNodeUI sourceNodeUI = getSourceNodeUI();
        JIPipeDesktopGraphNodeUI targetNodeUI = getTargetNodeUI();
        SegmentedLines result = new SegmentedLines();

        if (sourceNodeUI == null || targetNodeUI == null) {
            return result;
        }

        PointRange sourcePoint = getSourcePointRange();
        PointRange targetPoint = getTargetPointRange();
        sourcePoint.add(sourceNodeUI.getLocation());
        targetPoint.add(targetNodeUI.getLocation());
        final JIPipeGraphEdge.Shape shape = edge.getUiShape();

        // Tighten the point ranges: Bringing the centers together
        // TODO: Not working for control point edges correctly
        // TODO: we need to tighten to the first control point if we have control points
        PointRange.tighten(sourcePoint, targetPoint);

        Point nextSource = sourcePoint.center;
        Point nextTarget;
        Rectangle nextSourceBounds = sourceNodeUI.getBounds();

        if (edge.getControlPoints().isEmpty()) {
            nextTarget = targetPoint.center;
        } else {
            List<Point> controlPoints = getControlPointsInGridCoordinates();
            for (Point gridLocation : controlPoints) {
                nextTarget = JIPipeDesktopGraphCanvasGrid.gridToRealLocation(gridLocation, 1);
                addEdgeCoordinates(nextSource, nextSourceBounds, nextTarget, shape, scale, viewX, viewY, result);

                nextSource = nextTarget;
                nextSourceBounds = new Rectangle(nextSource.x, nextSource.y, 1, 1);
                result.nextSegment();
            }

            nextTarget = targetPoint.center;
        }

        addEdgeCoordinates(nextSource, nextSourceBounds, nextTarget, shape, scale, viewX, viewY, result);
        return result;
    }

    private void addEdgeCoordinates(Point nextSource, Rectangle nextSourceBounds, Point nextTarget, JIPipeGraphEdge.Shape shape, double scale, int viewX, int viewY, SegmentedLines result) {
        switch (shape) {
            case Line -> addLineEdgeCoordinates(nextSource, nextTarget, scale, viewX, viewY, result);
            case Elbow ->
                    addElbowEdgeCoordinates(nextSource, nextSourceBounds, nextTarget, scale, viewX, viewY, result);
            default -> throw new IllegalArgumentException("Unsupported shape " + shape);
        }
    }

    private void addLineEdgeCoordinates(Point sourcePoint, Point targetPoint, double scale, int viewX, int viewY, SegmentedLines result) {
        int dx;
        int dy;
        dx = 0;
        dy = 0;
        result.add((int) (scale * sourcePoint.x) + viewX, (int) (scale * sourcePoint.y) + viewY);
        result.add((int) (scale * targetPoint.x) + viewX + dx, (int) (scale * targetPoint.y) + viewY + dy);
    }

    public void addElbowEdgeCoordinates(Point sourcePoint, Rectangle sourceBounds, Point targetPoint, double scale, int viewX, int viewY, SegmentedLines result) {
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
        sourceB = sourcePoint.x;
        targetB = targetPoint.x;
        componentStartB = sourceBounds.x;
        componentEndB = sourceBounds.x + sourceBounds.width;

        int a0 = sourceA;
        int b0 = sourceB;
        int a1 = sourceA;
        int b1 = sourceB;

        addElbowPolygonCoordinate(a0, b0, scale, viewX, viewY, result);

        // Target point is above the source. We have to navigate around it
        if (sourceA > targetA) {
            // Add some space in major direction
            a1 += buffer;
            addElbowPolygonCoordinate(a1, b1, scale, viewX, viewY, result);

            // Go left or right
            if (targetB <= b1) {
                b1 = Math.max(0, componentStartB - buffer);
            } else {
                b1 = componentEndB + buffer;
            }
            addElbowPolygonCoordinate(a1, b1, scale, viewX, viewY, result);

            // Go to target height
            a1 = Math.max(0, targetA - buffer);
            addElbowPolygonCoordinate(a1, b1, scale, viewX, viewY, result);
        } else if (sourceB != targetB) {
            // Add some space in major direction
            int dA = targetA - sourceA;
            a1 = Math.min(sourceA + buffer, sourceA + dA / 2);
            addElbowPolygonCoordinate(a1, b1, scale, viewX, viewY, result);
        }

        // Target point X is shifted
        if (b1 != targetB) {
            b1 = targetB;
            addElbowPolygonCoordinate(a1, b1, scale, viewX, viewY, result);
        }

        // Go to end point
        a1 = targetA;
        addElbowPolygonCoordinate(a1, b1, scale, viewX, viewY, result);
    }

    private void addElbowPolygonCoordinate(int a1, int b1, double scale, int viewX, int viewY, SegmentedLines result) {
        int x2, y2;
        x2 = (int) (b1 * scale) + viewX;
        y2 = (int) (a1 * scale) + viewY;
        result.add(x2, y2);
    }

    private void paintThin(Graphics2D g, Stroke stroke, double scale, int viewX, int viewY, boolean enableArrows, boolean multiColor, int multiColorIndex, int multiColorMax) {
        if(edge.getUiShape() == JIPipeGraphEdge.Shape.Line) {
            enableArrows = false;
        }
        JIPipeDesktopGraphCanvasPaintManager.ArrowHeadMode arrowHeadMode = enableArrows ? JIPipeDesktopGraphCanvasPaintManager.ArrowHeadMode.Thin : JIPipeDesktopGraphCanvasPaintManager.ArrowHeadMode.None;

        g.setStroke(stroke);
        g.setColor(canvasUI.getResources().getEdgeColor(source, target, multiColor, multiColorIndex, multiColorMax));
        canvasUI.getPaintManager().paintEdge(g, getRenderedLineSegments(scale, viewX, viewY), arrowHeadMode);
    }

    private void paintRegular(Graphics2D g, Stroke stroke, Stroke strokeBorder, double scale, int viewX, int viewY, PointRange sourcePoint, PointRange targetPoint, JIPipeDesktopGraphCanvasPaintManager.ArrowHeadMode arrowHeadMode) {
        if(edge.getUiShape() == JIPipeGraphEdge.Shape.Line) {
            arrowHeadMode = JIPipeDesktopGraphCanvasPaintManager.ArrowHeadMode.None;
        }
        SegmentedLines renderedLineSegments = getRenderedLineSegments(scale, viewX, viewY);

        g.setStroke(strokeBorder);
        g.setColor(canvasUI.getResources().getEdgeColor(source, target, false, 0, 0));
        canvasUI.getPaintManager().paintEdge(g, renderedLineSegments, arrowHeadMode);
        g.setStroke(stroke);

        g.setPaint(canvasUI.getResources().getEdgeBackgroundPaint(source, target, sourcePoint, targetPoint, canvasUI.getResources().getImprovedStrokeBackgroundColor()));
        canvasUI.getPaintManager().paintEdge(g, renderedLineSegments, arrowHeadMode);
    }

    private void paintMultiColor(Graphics2D g, Stroke stroke, Stroke strokeBorder, double scale, int viewX, int viewY, boolean multiColor, int multiColorIndex, int multiColorMax, JIPipeDesktopGraphCanvasPaintManager.ArrowHeadMode arrowHeadMode) {
        if(edge.getUiShape() == JIPipeGraphEdge.Shape.Line) {
            arrowHeadMode = JIPipeDesktopGraphCanvasPaintManager.ArrowHeadMode.None;
        }
        SegmentedLines renderedLineSegments = getRenderedLineSegments(scale, viewX, viewY);

        g.setStroke(strokeBorder);
        Color edgeColor = canvasUI.getResources().getEdgeColor(source, target, multiColor, multiColorIndex, multiColorMax);
        Color edgeOutlineColor = ThemeUtils.isUsingDarkTheme() ? edgeColor.brighter() : edgeColor.darker();

        g.setColor(edgeOutlineColor);
        canvasUI.getPaintManager().paintEdge(g, renderedLineSegments, arrowHeadMode);
        g.setStroke(stroke);

        g.setPaint(edgeColor);
        canvasUI.getPaintManager().paintEdge(g, renderedLineSegments, arrowHeadMode);
    }

    public void moveControlPointsByGrid(int dx, int dy) {
        List<JIPipeGraphEdgeControlPoint> controlPoints = edge.getControlPoints(StringUtils.nullToEmpty(canvasUI.getCompartmentUUID()));
        for (JIPipeGraphEdgeControlPoint controlPoint : controlPoints) {
            controlPoint.setX(controlPoint.getX() + dx);
            controlPoint.setY(controlPoint.getY() + dy);
        }
    }

    public static class SegmentedLines {
        private final TIntArrayList segmentIndex = new TIntArrayList();
        private final TIntArrayList xCoords = new TIntArrayList();
        private final TIntArrayList yCoords = new TIntArrayList();
        private int segmentCounter = 0;

        public SegmentedLines() {
        }

        public void nextSegment() {
            ++segmentCounter;
        }

        public void add(int x, int y) {
            segmentIndex.add(segmentCounter);
            xCoords.add(x);
            yCoords.add(y);
        }

        public boolean isEmpty() {
            return segmentIndex.isEmpty() && xCoords.isEmpty() && yCoords.isEmpty();
        }

        public boolean isMultiSegmented() {
            return segmentCounter > 0;
        }

        public int getSegmentCount() {
            return segmentCounter;
        }

        public int size() {
            return segmentIndex.size();
        }

        public int getX(int index) {
            return xCoords.get(index);
        }

        public int getY(int index) {
            return yCoords.get(index);
        }

        public int getSegmentIndex(int index) {
            return this.segmentIndex.get(index);
        }

        public int[] getXPoints() {
            return xCoords.toArray();
        }

        public int[] getYPoints() {
            return yCoords.toArray();
        }
    }
}
