package org.hkijena.jipipe.desktop.app.grapheditor.commons.canvas.overlays;

import org.hkijena.jipipe.api.data.JIPipeDataSlot;
import org.hkijena.jipipe.desktop.app.grapheditor.commons.JIPipeDesktopGraphCanvasUI;
import org.hkijena.jipipe.desktop.app.grapheditor.commons.JIPipeDesktopGraphInteractiveObjectUI;
import org.hkijena.jipipe.desktop.app.grapheditor.commons.canvas.JIPipeDesktopGraphCanvasResources;
import org.hkijena.jipipe.desktop.app.grapheditor.commons.edgeui.JIPipeDesktopGraphEdgeUI;
import org.hkijena.jipipe.desktop.app.grapheditor.commons.nodeui.JIPipeDesktopGraphNodeUI;
import org.hkijena.jipipe.utils.PointRange;

import javax.swing.border.Border;
import java.awt.*;
import java.util.Set;

public class JIPipeDesktopGraphCanvasEdgesOverlay implements JIPipeDesktopGraphCanvasOverlay{

    private final JIPipeDesktopGraphCanvasUI canvasUI;

    public JIPipeDesktopGraphCanvasEdgesOverlay(JIPipeDesktopGraphCanvasUI canvasUI) {
        this.canvasUI = canvasUI;
    }

    @Override
    public void paint(Graphics2D g) {

    }

    @Override
    public void paintComponent(Graphics2D g) {

        Set<JIPipeDesktopGraphInteractiveObjectUI> selection = canvasUI.getSelectionManager().getSelection();
        Stroke strokeBorder = canvasUI.getResources().getEdgeStrokeBorder();
        Stroke strokeBorderSelected = canvasUI.getResources().getSelectedEdgeStrokeBorder();
        Stroke strokeBorderAdjacent = canvasUI.getResources().getAdjacentEdgeStrokeBorder();
        Stroke strokeInside = canvasUI.getResources().getEdgeStrokeInside();

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

            Stroke stroke = strokeBorder;
            if(selection.contains(edgeUI)) {
                stroke = strokeBorderSelected;
            }
            else if(  selection.contains(sourceUI) ||
                    selection.contains(targetUI)) {
                stroke = strokeBorderAdjacent;
            }

            // Hidden edges
            if (edgeUI.isCommentEdge()) {
                edgeUI.paint(g,
                        JIPipeDesktopGraphCanvasResources.STROKE_COMMENT,
                        null,
                        1,
                        0,
                        0,
                        true
                );
            } else {
                edgeUI.paint(g,
                        strokeInside,
                        stroke,
                        1,
                        0,
                        0,
                        true
                );
            }
        }
    }
}
