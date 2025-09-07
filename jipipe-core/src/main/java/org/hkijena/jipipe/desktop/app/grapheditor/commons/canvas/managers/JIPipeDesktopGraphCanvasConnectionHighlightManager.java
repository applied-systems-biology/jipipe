package org.hkijena.jipipe.desktop.app.grapheditor.commons.canvas.managers;

import org.hkijena.jipipe.api.data.JIPipeDataSlot;
import org.hkijena.jipipe.api.nodes.JIPipeGraphEdge;
import org.hkijena.jipipe.desktop.app.grapheditor.commons.JIPipeDesktopGraphCanvasUI;
import org.hkijena.jipipe.desktop.app.grapheditor.commons.canvas.JIPipeDesktopGraphCanvasResources;
import org.hkijena.jipipe.desktop.app.grapheditor.commons.canvas.JIPipeDesktopGraphCanvasUIConnectHighlight;
import org.hkijena.jipipe.desktop.app.grapheditor.commons.canvas.JIPipeDesktopGraphCanvasUIDisconnectHighlight;
import org.hkijena.jipipe.desktop.app.grapheditor.commons.nodeui.JIPipeDesktopGraphNodeUI;
import org.hkijena.jipipe.utils.PointRange;

import java.awt.*;
import java.util.Set;

public class JIPipeDesktopGraphCanvasConnectionHighlightManager {
    private final JIPipeDesktopGraphCanvasUI canvasUI;
    private JIPipeDesktopGraphCanvasUIDisconnectHighlight disconnectHighlight;
    private JIPipeDesktopGraphCanvasUIConnectHighlight connectHighlight;

    public JIPipeDesktopGraphCanvasConnectionHighlightManager(JIPipeDesktopGraphCanvasUI canvasUI) {
        this.canvasUI = canvasUI;
    }

    public JIPipeDesktopGraphCanvasUIDisconnectHighlight getDisconnectHighlight() {
        return disconnectHighlight;
    }

    public void setDisconnectHighlight(JIPipeDesktopGraphCanvasUIDisconnectHighlight disconnectHighlight) {
        this.disconnectHighlight = disconnectHighlight;
        canvasUI.repaintLowLag();
    }

    public JIPipeDesktopGraphCanvasUIConnectHighlight getConnectHighlight() {
        return connectHighlight;
    }

    public void setConnectHighlight(JIPipeDesktopGraphCanvasUIConnectHighlight connectHighlight) {
        this.connectHighlight = connectHighlight;
        canvasUI.repaintLowLag();
    }

    public void paintDisconnectHighlight(Graphics2D g) {
        if (disconnectHighlight != null) {
            g.setStroke(canvasUI.getResources().getStrokeHighlight());
            g.setColor(Color.RED);
            if (disconnectHighlight.getTarget().getSlot().isInput()) {
                Set<JIPipeDataSlot> sources = disconnectHighlight.getSources();
                for (JIPipeDataSlot source : sources) {
                    JIPipeDataSlot target = disconnectHighlight.getTarget().getSlot();
                    JIPipeDesktopGraphNodeUI sourceUI = canvasUI.getNodeUIs().getOrDefault(source.getNode(), null);
                    JIPipeDesktopGraphNodeUI targetUI = canvasUI.getNodeUIs().getOrDefault(target.getNode(), null);

                    if (sourceUI != null && targetUI != null) {
                        PointRange sourcePoint;
                        PointRange targetPoint;

                        sourcePoint = sourceUI.getSlotLocation(source);
                        sourcePoint.add(sourceUI.getLocation());
                        targetPoint = targetUI.getSlotLocation(target);
                        targetPoint.add(targetUI.getLocation());

                        // Tighten the point ranges: Bringing the centers together
                        PointRange.tighten(sourcePoint, targetPoint);

                        // Draw arrow
                        canvasUI.getPaintManager().paintEdge(g,
                                sourcePoint.center,
                                sourceUI.getBounds(),
                                targetPoint.center,
                                JIPipeGraphEdge.Shape.Elbow,
                                1,
                                0,
                                0,
                                true);
                    }
                }
            } else if (disconnectHighlight.getTarget().getSlot().isOutput()) {
                JIPipeDataSlot source = disconnectHighlight.getTarget().getSlot();
                for (JIPipeDataSlot target : canvasUI.getGraph().getOutputOutgoingTargetSlots(source)) {
                    JIPipeDesktopGraphNodeUI sourceUI = canvasUI.getNodeUIs().getOrDefault(source.getNode(), null);
                    JIPipeDesktopGraphNodeUI targetUI = canvasUI.getNodeUIs().getOrDefault(target.getNode(), null);

                    if (sourceUI != null && targetUI != null) {
                        PointRange sourcePoint;
                        PointRange targetPoint;

                        sourcePoint = sourceUI.getSlotLocation(source);
                        sourcePoint.add(sourceUI.getLocation());
                        targetPoint = targetUI.getSlotLocation(target);
                        targetPoint.add(targetUI.getLocation());

                        // Tighten the point ranges: Bringing the centers together
                        PointRange.tighten(sourcePoint, targetPoint);

                        // Draw arrow
                        canvasUI.getPaintManager().paintEdge(g,
                                sourcePoint.center,
                                sourceUI.getBounds(),
                                targetPoint.center,
                                JIPipeGraphEdge.Shape.Elbow,
                                1,
                                0,
                                0,
                                true);
                    }
                }
            }
        }
    }

    public void paintConnectHighlight(Graphics2D g) {
        if (connectHighlight != null) {
            g.setStroke(canvasUI.getResources().getStrokeHighlight());
            g.setColor(JIPipeDesktopGraphCanvasResources.COLOR_HIGHLIGHT_GREEN);
            if (connectHighlight.getTarget().getSlot().isInput()) {
                JIPipeDataSlot source = connectHighlight.getSource().getSlot();
                JIPipeDataSlot target = connectHighlight.getTarget().getSlot();
                JIPipeDesktopGraphNodeUI sourceUI = canvasUI.getNodeUIs().getOrDefault(source.getNode(), null);
                JIPipeDesktopGraphNodeUI targetUI = canvasUI.getNodeUIs().getOrDefault(target.getNode(), null);

                if (sourceUI != null && targetUI != null) {
                    PointRange sourcePoint;
                    PointRange targetPoint;

                    sourcePoint = sourceUI.getSlotLocation(source);
                    sourcePoint.add(sourceUI.getLocation());
                    targetPoint = targetUI.getSlotLocation(target);
                    targetPoint.add(targetUI.getLocation());

                    // Tighten the point ranges: Bringing the centers together
                    PointRange.tighten(sourcePoint, targetPoint);

                    // Draw arrow
                    canvasUI.getPaintManager().paintEdge(g,
                            sourcePoint.center,
                            sourceUI.getBounds(),
                            targetPoint.center,
                            JIPipeGraphEdge.Shape.Elbow,
                            1,
                            0,
                            0,
                            true);
                }
            } else if (disconnectHighlight.getTarget().getSlot().isOutput()) {
                JIPipeDataSlot target = connectHighlight.getSource().getSlot();
                JIPipeDataSlot source = connectHighlight.getTarget().getSlot();
                JIPipeDesktopGraphNodeUI sourceUI = canvasUI.getNodeUIs().getOrDefault(source.getNode(), null);
                JIPipeDesktopGraphNodeUI targetUI = canvasUI.getNodeUIs().getOrDefault(target.getNode(), null);
                if (sourceUI != null && targetUI != null) {
                    PointRange sourcePoint;
                    PointRange targetPoint;

                    sourcePoint = sourceUI.getSlotLocation(source);
                    sourcePoint.add(sourceUI.getLocation());
                    targetPoint = targetUI.getSlotLocation(target);
                    targetPoint.add(targetUI.getLocation());

                    // Tighten the point ranges: Bringing the centers together
                    PointRange.tighten(sourcePoint, targetPoint);

                    // Draw arrow
                    canvasUI.getPaintManager().paintEdge(g,
                            sourcePoint.center,
                            sourceUI.getBounds(),
                            targetPoint.center,
                            JIPipeGraphEdge.Shape.Elbow,
                            1,
                            0,
                            0,
                            true);
                }
            }
        }
    }
}
