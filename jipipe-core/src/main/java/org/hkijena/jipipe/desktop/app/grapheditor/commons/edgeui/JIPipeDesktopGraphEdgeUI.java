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

import org.hkijena.jipipe.api.data.JIPipeDataSlot;
import org.hkijena.jipipe.api.nodes.JIPipeGraphEdge;
import org.hkijena.jipipe.api.nodes.JIPipeGraphNode;
import org.hkijena.jipipe.desktop.app.grapheditor.commons.JIPipeDesktopGraphCanvasUI;
import org.hkijena.jipipe.desktop.app.grapheditor.commons.JIPipeDesktopGraphInteractiveObjectUI;
import org.hkijena.jipipe.desktop.app.grapheditor.commons.JIPipeDesktopGraphInteractiveObjectUIUpdateViewCommand;
import org.hkijena.jipipe.desktop.app.grapheditor.commons.canvas.managers.JIPipeDesktopGraphCanvasPaintManager;
import org.hkijena.jipipe.desktop.app.grapheditor.commons.nodeui.JIPipeDesktopGraphNodeUI;
import org.hkijena.jipipe.plugins.core.nodes.JIPipeCommentNode;
import org.hkijena.jipipe.utils.PointRange;
import org.hkijena.jipipe.utils.ThemeUtils;
import org.jetbrains.annotations.NotNull;

import java.awt.*;
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

        if(sourceNodeUI == null || targetNodeUI == null) {
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

            if(multiColor) {
                g.setStroke(strokeBorder);
                Color edgeColor = canvasUI.getResources().getEdgeColor(source, target, multiColor, multiColorIndex, multiColorMax);
                Color edgeOutlineColor = ThemeUtils.isUsingDarkTheme() ? edgeColor.brighter() : edgeColor.darker();

                g.setColor(edgeOutlineColor);
                canvasUI.getPaintManager().paintEdge(g, sourcePoint.center, sourceNodeUI.getBounds(), targetPoint.center, uiShape, scale, viewX, viewY, arrowHeadMode);
                g.setStroke(stroke);

                g.setPaint(edgeColor);
                canvasUI.getPaintManager().paintEdge(g, sourcePoint.center, sourceNodeUI.getBounds(), targetPoint.center, uiShape, scale, viewX, viewY, arrowHeadMode);
            }
            else {
                g.setStroke(strokeBorder);
                g.setColor(canvasUI.getResources().getEdgeColor(source, target, false, 0, 0));
                canvasUI.getPaintManager().paintEdge(g, sourcePoint.center, sourceNodeUI.getBounds(), targetPoint.center, uiShape, scale, viewX, viewY, arrowHeadMode);
                g.setStroke(stroke);

                g.setPaint(canvasUI.getResources().getEdgeBackgroundPaint(source, target, sourcePoint, targetPoint, canvasUI.getResources().getImprovedStrokeBackgroundColor()));
                canvasUI.getPaintManager().paintEdge(g, sourcePoint.center, sourceNodeUI.getBounds(), targetPoint.center, uiShape, scale, viewX, viewY, arrowHeadMode);
            }
        } else {
            // Just a single stroke
            JIPipeDesktopGraphCanvasPaintManager.ArrowHeadMode arrowHeadMode = enableArrows ? JIPipeDesktopGraphCanvasPaintManager.ArrowHeadMode.Thin : JIPipeDesktopGraphCanvasPaintManager.ArrowHeadMode.None;

            g.setStroke(stroke);
            g.setColor(canvasUI.getResources().getEdgeColor(source, target, multiColor, multiColorIndex, multiColorMax));
            canvasUI.getPaintManager().paintEdge(g, sourcePoint.center, sourceNodeUI.getBounds(), targetPoint.center, uiShape, scale, viewX, viewY, arrowHeadMode);
        }
    }
}
