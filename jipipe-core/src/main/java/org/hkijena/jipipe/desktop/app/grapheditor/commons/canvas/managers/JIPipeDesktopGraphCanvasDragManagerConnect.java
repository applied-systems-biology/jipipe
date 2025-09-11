package org.hkijena.jipipe.desktop.app.grapheditor.commons.canvas.managers;

import org.hkijena.jipipe.api.nodes.JIPipeGraphEdge;
import org.hkijena.jipipe.desktop.app.grapheditor.commons.JIPipeDesktopGraphCanvasUI;
import org.hkijena.jipipe.desktop.app.grapheditor.commons.canvas.JIPipeDesktopGraphCanvasResources;
import org.hkijena.jipipe.desktop.app.grapheditor.commons.nodeui.JIPipeDesktopGraphNodeUI;
import org.hkijena.jipipe.desktop.app.grapheditor.commons.nodeui.JIPipeDesktopGraphNodeUIActiveArea;
import org.hkijena.jipipe.desktop.app.grapheditor.commons.nodeui.triggers.JIPipeDesktopGraphNodeUIAddSlotButtonActiveArea;
import org.hkijena.jipipe.desktop.app.grapheditor.commons.nodeui.triggers.JIPipeDesktopGraphNodeUISlotActiveArea;
import org.hkijena.jipipe.utils.PointRange;

import java.awt.*;
import java.awt.event.MouseEvent;

public class JIPipeDesktopGraphCanvasDragManagerConnect {
    private final JIPipeDesktopGraphCanvasUI canvasUI;

    private JIPipeDesktopGraphNodeUIActiveArea currentConnectionDragSource;
    private boolean currentConnectionDragSourceDragged;
    private JIPipeDesktopGraphNodeUIActiveArea currentConnectionDragTarget;


    public JIPipeDesktopGraphCanvasDragManagerConnect(JIPipeDesktopGraphCanvasUI canvasUI) {
        this.canvasUI = canvasUI;
    }

    public void cancelDragging() {
        // Slot dragging
        this.currentConnectionDragSourceDragged = false;
        setCurrentConnectionDragSource(null);
        setCurrentConnectionDragTarget(null);
        canvasUI.resetCursor();
        canvasUI.repaintLowLag();
    }


    public boolean isCurrentlyDraggingConnection() {
        return currentConnectionDragSource != null && currentConnectionDragTarget != null;
    }

    public JIPipeDesktopGraphNodeUIActiveArea getCurrentConnectionDragSource() {
        return currentConnectionDragSource;
    }

    public void setCurrentConnectionDragSource(JIPipeDesktopGraphNodeUIActiveArea currentConnectionDragSource) {
        this.currentConnectionDragSource = currentConnectionDragSource;
    }

    public JIPipeDesktopGraphNodeUIActiveArea getCurrentConnectionDragTarget() {
        return currentConnectionDragTarget;
    }

    public void setCurrentConnectionDragTarget(JIPipeDesktopGraphNodeUIActiveArea currentConnectionDragTarget) {
        this.currentConnectionDragTarget = currentConnectionDragTarget;
    }

    public boolean mouseDragged(MouseEvent mouseEvent) {
        if (currentConnectionDragSource != null) {
            // Mark this as actual dragging
            this.currentConnectionDragSourceDragged = true;

            JIPipeDesktopGraphNodeUI nodeUI = canvasUI.pickNodeUI(mouseEvent);
            if (nodeUI != null && currentConnectionDragSource.getNodeUI() != nodeUI) {
                // Advanced dragging behavior
                boolean snapped = false;

                if (currentConnectionDragSource instanceof JIPipeDesktopGraphNodeUISlotActiveArea) {
                    JIPipeDesktopGraphNodeUISlotActiveArea currentConnectionDragSource_ = (JIPipeDesktopGraphNodeUISlotActiveArea) currentConnectionDragSource;
                 /*
                Auto snap to input/output if there is only one
                 */
                    if (currentConnectionDragSource_.getSlot().isInput()) {
                        if (nodeUI.getNode().getOutputSlots().size() == 1 && !nodeUI.isSlotsOutputsEditable()) {
                            if (!nodeUI.getOutputSlotMap().values().isEmpty()) {
                                // Auto snap to output
                                JIPipeDesktopGraphNodeUISlotActiveArea slotUI = nodeUI.getOutputSlotMap().values().iterator().next();
                                setCurrentConnectionDragTarget(slotUI);
                                snapped = true;
                            }
                        }
                    } else {
                        if (nodeUI.getNode().getInputSlots().size() == 1 && !nodeUI.isSlotsInputsEditable()) {
                            // Auto snap to input
                            if (!nodeUI.getInputSlotMap().values().isEmpty()) {
                                JIPipeDesktopGraphNodeUISlotActiveArea slotUI = nodeUI.getInputSlotMap().values().iterator().next();
                                setCurrentConnectionDragTarget(slotUI);
                                snapped = true;
                            }
                        }
                    }

                /*
                Sticky snap: Stay in last snapped position if we were in it before
                 */
                    if (currentConnectionDragTarget != null && currentConnectionDragTarget.getNodeUI() == nodeUI) {
                        JIPipeDesktopGraphNodeUIActiveArea addSlotState = nodeUI.pickAddSlotAtMousePosition(mouseEvent);
                        if (addSlotState == null) {
                            JIPipeDesktopGraphNodeUISlotActiveArea slotState = nodeUI.pickSlotAtMousePosition(mouseEvent);
                            if (slotState != null && slotState.getSlot().isInput() != currentConnectionDragSource_.getSlot().isInput()) {
                                setCurrentConnectionDragTarget(slotState);
                            }
                            snapped = true;
                        }
                    }

                /*
                Default: Snap exactly to input/output
                 */
                    if (!snapped) {
                        JIPipeDesktopGraphNodeUISlotActiveArea slotState = nodeUI.pickSlotAtMousePosition(mouseEvent);
                        if (slotState != null && slotState.getSlot().isInput() != currentConnectionDragSource_.getSlot().isInput()) {
                            setCurrentConnectionDragTarget(slotState);
                            snapped = true;
                        } else {
                            setCurrentConnectionDragTarget(null);
                        }
                    }

                /*
                Snap to "create input"
                 */
                    if (!snapped) {
                        JIPipeDesktopGraphNodeUIActiveArea slotState = nodeUI.pickAddSlotAtMousePosition(mouseEvent);
                        if (currentConnectionDragSource_.isOutput() && slotState != null && slotState == nodeUI.getAddInputSlotArea()) {
                            setCurrentConnectionDragTarget(slotState);
                            snapped = true;
                        } else if (currentConnectionDragSource_.isInput() && slotState != null && slotState == nodeUI.getAddOutputSlotArea()) {
                            setCurrentConnectionDragTarget(slotState);
                            snapped = true;
                        } else {
                            setCurrentConnectionDragTarget(null);
                        }
                    }
                } else {
                    // TODO?
                }

            } else {
                setCurrentConnectionDragTarget(null);
            }
            canvasUI.setCursor(Cursor.getPredefinedCursor(Cursor.CROSSHAIR_CURSOR));
            canvasUI.repaintLowLag();

            return true;
        }
        return false;
    }

    public void paintCurrentlyDraggedConnection(Graphics2D g) {
        if (currentConnectionDragSourceDragged && currentConnectionDragSource != null) {
            g.setStroke(canvasUI.getResources().getStrokeHighlight());
            PointRange sourcePoint;
            PointRange targetPoint = null;

            if (currentConnectionDragSource instanceof JIPipeDesktopGraphNodeUISlotActiveArea) {
                sourcePoint = currentConnectionDragSource.getNodeUI().getSlotLocation(((JIPipeDesktopGraphNodeUISlotActiveArea) currentConnectionDragSource).getSlot());
            } else {
                sourcePoint = currentConnectionDragSource.getZoomedHitAreaCenter();
            }

            sourcePoint.add(currentConnectionDragSource.getNodeUI().getLocation());

            if (currentConnectionDragTarget != null &&
                    currentConnectionDragTarget != currentConnectionDragSource &&
                    currentConnectionDragTarget.getNodeUI().getNode() != currentConnectionDragSource.getNodeUI().getNode()) {
                JIPipeDesktopGraphNodeUI nodeUI = currentConnectionDragTarget.getNodeUI();
                if (currentConnectionDragTarget instanceof JIPipeDesktopGraphNodeUISlotActiveArea) {
                    targetPoint = nodeUI.getSlotLocation(((JIPipeDesktopGraphNodeUISlotActiveArea) currentConnectionDragTarget).getSlot());
                } else {
                    targetPoint = currentConnectionDragTarget.getZoomedHitAreaCenter();
                }
                targetPoint.add(nodeUI.getLocation());
            }
            if (targetPoint != null) {
                if (currentConnectionDragSource instanceof JIPipeDesktopGraphNodeUISlotActiveArea && currentConnectionDragTarget instanceof JIPipeDesktopGraphNodeUISlotActiveArea) {
                    JIPipeDesktopGraphNodeUISlotActiveArea currentConnectionDragSource_ = (JIPipeDesktopGraphNodeUISlotActiveArea) currentConnectionDragSource;
                    JIPipeDesktopGraphNodeUISlotActiveArea currentConnectionDragTarget_ = (JIPipeDesktopGraphNodeUISlotActiveArea) currentConnectionDragTarget;
                    if (currentConnectionDragTarget == null || (!canvasUI.getGraph().getGraph().containsEdge(currentConnectionDragSource_.getSlot(), currentConnectionDragTarget_.getSlot())
                            && !canvasUI.getGraph().getGraph().containsEdge(currentConnectionDragTarget_.getSlot(), currentConnectionDragSource_.getSlot()))) {
                        g.setColor(JIPipeDesktopGraphCanvasResources.COLOR_HIGHLIGHT_GREEN);
                    } else {
                        g.setColor(Color.RED);
                    }
                } else {
                    g.setColor(JIPipeDesktopGraphCanvasResources.COLOR_HIGHLIGHT_GREEN);
                }
            } else {
                g.setColor(Color.DARK_GRAY);
            }
            if (targetPoint == null) {
                Point mousePosition = canvasUI.getLastMousePosition();
                if (mousePosition != null) {
                    targetPoint = new PointRange(mousePosition.x, mousePosition.y);
                }
            }

            if (targetPoint != null) {
                // Tighten the point ranges: Bringing the centers together
                PointRange.tighten(sourcePoint, targetPoint);

                // Draw arrow
                if (currentConnectionDragSource instanceof JIPipeDesktopGraphNodeUISlotActiveArea) {
                    if (((JIPipeDesktopGraphNodeUISlotActiveArea) currentConnectionDragSource).getSlot().isOutput()) {
                        canvasUI.getPaintManager().paintEdge(g,
                                sourcePoint.center,
                                currentConnectionDragSource.getNodeUI().getBounds(),
                                targetPoint.center,
                                JIPipeGraphEdge.Shape.Elbow,
                                1,
                                0,
                                0,
                                JIPipeDesktopGraphCanvasPaintManager.ArrowHeadMode.Filled);
                    } else {
                        canvasUI.getPaintManager().paintEdge(g,
                                targetPoint.center,
                                currentConnectionDragSource.getNodeUI().getBounds(),
                                sourcePoint.center,
                                JIPipeGraphEdge.Shape.Elbow,
                                1,
                                0,
                                0,
                                JIPipeDesktopGraphCanvasPaintManager.ArrowHeadMode.Filled);
                    }
                } else if (currentConnectionDragTarget instanceof JIPipeDesktopGraphNodeUISlotActiveArea) {
                    if (((JIPipeDesktopGraphNodeUISlotActiveArea) currentConnectionDragTarget).getSlot().isInput()) {
                        canvasUI.getPaintManager().paintEdge(g,
                                sourcePoint.center,
                                currentConnectionDragSource.getNodeUI().getBounds(),
                                targetPoint.center,
                                JIPipeGraphEdge.Shape.Elbow,
                                1,
                                0,
                                0,
                                JIPipeDesktopGraphCanvasPaintManager.ArrowHeadMode.Filled);
                    } else {
                        canvasUI.getPaintManager().paintEdge(g,
                                targetPoint.center,
                                currentConnectionDragSource.getNodeUI().getBounds(),
                                sourcePoint.center,
                                JIPipeGraphEdge.Shape.Elbow,
                                1,
                                0,
                                0,
                                JIPipeDesktopGraphCanvasPaintManager.ArrowHeadMode.Filled);
                    }
                } else {
                    canvasUI.getPaintManager().paintEdge(g,
                            targetPoint.center,
                            currentConnectionDragSource.getNodeUI().getBounds(),
                            sourcePoint.center,
                            JIPipeGraphEdge.Shape.Elbow,
                            1,
                            0,
                            0,
                            JIPipeDesktopGraphCanvasPaintManager.ArrowHeadMode.None);
                }
            }
        }
    }

    private void startDragSlot(JIPipeDesktopGraphNodeUISlotActiveArea startSlot) {
        if (canvasUI.getToolManager().currentToolAllowsConnectionDragging()) {
            this.currentConnectionDragSourceDragged = false;
            canvasUI.setCursor(Cursor.getPredefinedCursor(Cursor.CROSSHAIR_CURSOR));
            setCurrentConnectionDragSource(startSlot);
        } else {
            canvasUI.cancelAllDraggingOperations();
        }
    }

    public void mouseReleased(MouseEvent mouseEvent) {
        if (currentConnectionDragSource instanceof JIPipeDesktopGraphNodeUISlotActiveArea && currentConnectionDragTarget instanceof JIPipeDesktopGraphNodeUISlotActiveArea) {
            canvasUI.connectOrDisconnectSlots(((JIPipeDesktopGraphNodeUISlotActiveArea) currentConnectionDragSource).getSlot(), ((JIPipeDesktopGraphNodeUISlotActiveArea) currentConnectionDragTarget).getSlot());
        } else if (currentConnectionDragSource instanceof JIPipeDesktopGraphNodeUISlotActiveArea && currentConnectionDragTarget instanceof JIPipeDesktopGraphNodeUIAddSlotButtonActiveArea) {
            canvasUI.connectCreateNewSlot(((JIPipeDesktopGraphNodeUISlotActiveArea) currentConnectionDragSource).getSlot(), currentConnectionDragTarget.getNodeUI());
        }
    }

    public boolean mousePressed(MouseEvent mouseEvent) {
        if (canvasUI.getToolManager().currentToolAllowsConnectionDragging()) {
            // Attempt to drag a slot
            JIPipeDesktopGraphNodeUI ui = canvasUI.pickNodeUI(mouseEvent);
            if (ui != null) {
                JIPipeDesktopGraphNodeUISlotActiveArea slotState = ui.pickSlotAtMousePosition(mouseEvent);
                if (slotState != null) {
                    startDragSlot(slotState);
                    return true;
                } else {
                    return false;
                }
            }
        }
        return false;
    }
}
