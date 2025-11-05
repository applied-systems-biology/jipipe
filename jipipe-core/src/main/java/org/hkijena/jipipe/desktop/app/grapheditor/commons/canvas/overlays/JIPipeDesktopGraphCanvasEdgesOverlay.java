package org.hkijena.jipipe.desktop.app.grapheditor.commons.canvas.overlays;

import org.hkijena.jipipe.api.data.JIPipeDataSlot;
import org.hkijena.jipipe.desktop.app.grapheditor.commons.JIPipeDesktopGraphCanvasUI;
import org.hkijena.jipipe.desktop.app.grapheditor.commons.JIPipeDesktopGraphInteractiveObjectUI;
import org.hkijena.jipipe.desktop.app.grapheditor.commons.canvas.JIPipeDesktopGraphCanvasResources;
import org.hkijena.jipipe.desktop.app.grapheditor.commons.edgeui.JIPipeDesktopGraphEdgeControlPointUI;
import org.hkijena.jipipe.desktop.app.grapheditor.commons.edgeui.JIPipeDesktopGraphEdgeUI;
import org.hkijena.jipipe.desktop.app.grapheditor.commons.nodeui.JIPipeDesktopGraphNodeUI;

import java.awt.*;
import java.util.Set;

public class JIPipeDesktopGraphCanvasEdgesOverlay implements JIPipeDesktopGraphCanvasOverlay {

    private final JIPipeDesktopGraphCanvasUI canvasUI;

    public JIPipeDesktopGraphCanvasEdgesOverlay(JIPipeDesktopGraphCanvasUI canvasUI) {
        this.canvasUI = canvasUI;
    }

    private static void paintMutedEdge(Graphics2D g, JIPipeDesktopGraphEdgeUI edgeUI, Stroke strokeMuted) {
        edgeUI.paint(g,
                strokeMuted,
                null,
                1,
                0,
                0,
                true,
                false,
                0,
                0);
    }

    private static void paintRegularEdge(Graphics2D g, JIPipeDesktopGraphEdgeUI edgeUI, Stroke strokeInside, Stroke strokeBorder) {
        edgeUI.paint(g,
                strokeInside,
                strokeBorder,
                1,
                0,
                0,
                true,
                false, 0, 0);
    }

    private static void paintMultiColorEdge(Graphics2D g, JIPipeDesktopGraphEdgeUI edgeUI, Stroke strokeInside, Stroke strokeBorder, int multiColorIndex, int multiColorMax) {
        edgeUI.paint(g,
                strokeInside,
                strokeBorder,
                1,
                0,
                0,
                true,
                true, multiColorIndex, multiColorMax);
    }

    private static void paintCommentEdge(Graphics2D g, JIPipeDesktopGraphEdgeUI edgeUI, boolean edgeHasMultiColor, int multiColorIndex, int multiColorMax) {
        edgeUI.paint(g,
                JIPipeDesktopGraphCanvasResources.STROKE_COMMENT,
                null,
                1,
                0,
                0,
                true,
                edgeHasMultiColor, multiColorIndex, multiColorMax);
    }

    @Override
    public void paint(Graphics2D g) {

    }

    @Override
    public void paintComponent(Graphics2D g) {

        Set<JIPipeDesktopGraphInteractiveObjectUI> selection = canvasUI.getSelectionManager().getSelection();
        Stroke strokeBorderDefault = canvasUI.getResources().getEdgeStrokeBorder();
        Stroke strokeBorderSelected = canvasUI.getResources().getSelectedEdgeStrokeBorder();
        Stroke strokeBorderAdjacent = canvasUI.getResources().getAdjacentEdgeStrokeBorder();
        Stroke strokeInside = canvasUI.getResources().getEdgeStrokeInside();
        Stroke strokeMuted = JIPipeDesktopGraphCanvasResources.STROKE_SMART_EDGE;

        // We enable multicolor display depending on whether nodes are selected
        int multiColorIndex = 0;
        int multiColorMax = 0;
        boolean hasEdgeSelection = false;

        if (!selection.isEmpty()) {
            for (JIPipeDesktopGraphEdgeUI edgeUI : canvasUI.getEdgeUIs().values()) {
                JIPipeDesktopGraphNodeUI sourceNodeUI = edgeUI.getSourceNodeUI();
                JIPipeDesktopGraphNodeUI targetNodeUI = edgeUI.getTargetNodeUI();

                if (sourceNodeUI == null || targetNodeUI == null) {
                    continue;
                }

                if (selection.contains(sourceNodeUI) || selection.contains(targetNodeUI)) {
                    ++multiColorMax;
                }
                if (selection.contains(edgeUI)) {
                    hasEdgeSelection = true;
                    ++multiColorMax;
                }
            }
        }

        final boolean multiColor = multiColorMax > 0;

        // Paint edges
        for (JIPipeDesktopGraphEdgeUI edgeUI : canvasUI.getEdgeUIs().values()) {
            JIPipeDataSlot source = edgeUI.getSource();
            JIPipeDataSlot target = edgeUI.getTarget();

            if (!canvasUI.getToolManager().hasDefaultTool()) {
                if (!canvasUI.getToolManager().getCurrentTool().canRenderEdge(source, target, edgeUI.getEdge())) {
                    continue;
                }
            }

            // Check for only showing selected nodes
            JIPipeDesktopGraphNodeUI sourceUI = canvasUI.getNodeUIs().getOrDefault(source.getNode(), null);
            JIPipeDesktopGraphNodeUI targetUI = canvasUI.getNodeUIs().getOrDefault(target.getNode(), null);

            if (sourceUI == null || targetUI == null) {
                continue;
            }

            boolean edgeHasMultiColor = multiColorMax > 0 && (selection.contains(edgeUI) || selection.contains(sourceUI) || selection.contains(targetUI));

            Stroke strokeBorder = strokeBorderDefault;
            if (selection.contains(edgeUI)) {
                strokeBorder = strokeBorderSelected;
            } else if (selection.contains(sourceUI) ||
                    selection.contains(targetUI)) {
                strokeBorder = strokeBorderAdjacent;
            }

            if (edgeUI.isCommentEdge()) {
                paintCommentEdge(g, edgeUI, edgeHasMultiColor, multiColorIndex, multiColorMax);
                paintEdgeControlPoints(g, edgeUI, multiColor, multiColorIndex, multiColorMax);
            } else if (selection.contains(edgeUI)) {
                if (edgeHasMultiColor) {
                    paintMultiColorEdge(g, edgeUI, strokeInside, strokeBorder, multiColorIndex, multiColorMax);
                    ++multiColorIndex;
                    paintEdgeControlPoints(g, edgeUI, multiColor, multiColorIndex, multiColorMax);
                } else {
                    paintRegularEdge(g, edgeUI, strokeInside, strokeBorder);
                    paintEdgeControlPoints(g, edgeUI, multiColor, multiColorIndex, multiColorMax);
                }
            } else {
                if (multiColor) {
                    if (edgeHasMultiColor) {
                        paintMultiColorEdge(g, edgeUI, strokeInside, strokeBorder, multiColorIndex, multiColorMax);
                        ++multiColorIndex;
                        paintEdgeControlPoints(g, edgeUI, multiColor, multiColorIndex, multiColorMax);
                    } else {
                        // Mute the edge
                        paintMutedEdge(g, edgeUI, strokeMuted);
                    }
                } else {
                    paintRegularEdge(g, edgeUI, strokeInside, strokeBorder);
                    paintEdgeControlPoints(g, edgeUI, multiColor, multiColorIndex, multiColorMax);
                }
            }
        }
    }

    private void paintEdgeControlPoints(Graphics2D g, JIPipeDesktopGraphEdgeUI edgeUI, boolean multiColor, int multiColorIndex, int multiColorMax) {
        for (JIPipeDesktopGraphEdgeControlPointUI controlPoint : edgeUI.getControlPoints()) {
            controlPoint.paint(g, multiColor, multiColorIndex, multiColorMax);
        }
    }
}
