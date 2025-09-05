package org.hkijena.jipipe.desktop.app.grapheditor.commons.canvas;

import org.hkijena.jipipe.JIPipe;
import org.hkijena.jipipe.desktop.app.grapheditor.commons.JIPipeDesktopGraphCanvasUI;
import org.hkijena.jipipe.desktop.app.grapheditor.commons.JIPipeDesktopGraphInteractiveObjectUI;
import org.hkijena.jipipe.desktop.app.grapheditor.commons.canvas.events.JIPipeDesktopGraphCanvasUIUpdatedEvent;
import org.hkijena.jipipe.desktop.app.grapheditor.commons.nodeui.JIPipeDesktopGraphNodeUI;
import org.hkijena.jipipe.desktop.app.grapheditor.commons.nodeui.triggers.JIPipeDesktopGraphNodeUISlotActiveArea;

import java.awt.*;
import java.awt.event.MouseEvent;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

public class JIPipeDesktopGraphCanvasDragManagerMove {
    private final JIPipeDesktopGraphCanvasUI canvasUI;
    private final Map<JIPipeDesktopGraphInteractiveObjectUI, Point> currentlyDraggedOffsets = new HashMap<>();
    private boolean hasDragSnapshot = false;
    private long lastTimeExpandedNegative = 0;

    public JIPipeDesktopGraphCanvasDragManagerMove(JIPipeDesktopGraphCanvasUI canvasUI) {
        this.canvasUI = canvasUI;
    }

    public void createMoveSnapshotIfNeeded() {
        if (!hasDragSnapshot) {
            if (canvasUI.getHistoryJournal() != null) {
                canvasUI.getHistoryJournal().snapshot("Move nodes",
                        "Nodes were dragged with the mouse",
                        canvasUI.getCompartmentUUID(),
                        JIPipe.RESOURCES.getIcon16("actions/transform-move.png"));
            }
            hasDragSnapshot = true;
        }
    }

    public boolean startDragCurrentNodeSelection(MouseEvent mouseEvent) {
        if (canvasUI.getToolManager().currentToolAllowsNodeDragging()) {
            this.hasDragSnapshot = false;
            canvasUI.getDragManagerConnect().cancelDragging();
            for (JIPipeDesktopGraphInteractiveObjectUI interactiveObjectUI : canvasUI.getSelectionManager().getSelection()) {
                if(interactiveObjectUI instanceof JIPipeDesktopGraphNodeUI nodeUI) {
                    if (nodeUI.getNode().isUiLocked())
                        continue;
                    Point offset = new Point();
                    offset.x = nodeUI.getX() - mouseEvent.getX();
                    offset.y = nodeUI.getY() - mouseEvent.getY();
                    currentlyDraggedOffsets.put(nodeUI, offset);
                }
            }
            return !currentlyDraggedOffsets.isEmpty();
        } else {
            canvasUI.cancelAllDraggingOperations();
        }
        return false;
    }

    public boolean mouseDragged(MouseEvent mouseEvent) {
        if(!getCurrentlyDraggedOffsets().isEmpty()) {
            // Calculate final movement for all nodes
            int gridDx = 0;
            int gridDy = 0;

            for (Map.Entry<JIPipeDesktopGraphInteractiveObjectUI, Point> entry : currentlyDraggedOffsets.entrySet()) {
                Point currentlyDraggedOffset = entry.getValue();
                if(entry.getKey() instanceof JIPipeDesktopGraphNodeUI nodeUI) {

                    int x = Math.max(0, currentlyDraggedOffset.x + mouseEvent.getX());
                    int y = Math.max(0, currentlyDraggedOffset.y + mouseEvent.getY());

                    Point targetGridPoint = JIPipeDesktopGraphCanvasGrid.realLocationToGrid(new Point(x, y), canvasUI.getZoom());
                    int dx = targetGridPoint.x - nodeUI.getStoredGridLocation().x;
                    int dy = targetGridPoint.y - nodeUI.getStoredGridLocation().y;

                    if (dx != 0 || dy != 0) {
                        gridDx = dx;
                        gridDy = dy;
                        break;
                    }
                }
            }

            int negativeDx = 0;
            int negativeDy = 0;
            long currentTimeMillis = System.currentTimeMillis();
            if (currentTimeMillis - lastTimeExpandedNegative > 100) {
                for (Map.Entry<JIPipeDesktopGraphInteractiveObjectUI, Point> entry : currentlyDraggedOffsets.entrySet()) {
                    if(entry.getKey() instanceof JIPipeDesktopGraphNodeUI currentlyDragged) {

                        Point newGridLocation = new Point(currentlyDragged.getStoredGridLocation().x + gridDx, currentlyDragged.getStoredGridLocation().y + gridDy);
                        if (newGridLocation.x <= 0) {
                            negativeDx = Math.min(negativeDx, newGridLocation.x - 1);
                            lastTimeExpandedNegative = currentTimeMillis;
                        }
                        if (newGridLocation.y <= 0) {
                            negativeDy = Math.min(negativeDy, newGridLocation.y - 1);
                            lastTimeExpandedNegative = currentTimeMillis;
                        }
                    }
                }
            }

            if (negativeDx < 0 || negativeDy < 0) {
                // Negative expansion
                for (JIPipeDesktopGraphNodeUI value : canvasUI.getNodeUIs().values()) {
                    if (!currentlyDraggedOffsets.containsKey(value)) {
                        Point storedGridLocation = value.getStoredGridLocation();
                        value.moveToGridLocation(new Point(storedGridLocation.x - negativeDx, storedGridLocation.y - negativeDy), true, true);
                    }
                }
            }

            for (Map.Entry<JIPipeDesktopGraphInteractiveObjectUI, Point> entry : currentlyDraggedOffsets.entrySet()) {
                if(entry.getKey() instanceof JIPipeDesktopGraphNodeUI currentlyDragged) {
                    Point newGridLocation = new Point(currentlyDragged.getStoredGridLocation().x + gridDx, currentlyDragged.getStoredGridLocation().y + gridDy);

                    if (!hasDragSnapshot) {
                        // Check if something would change
                        if (!Objects.equals(currentlyDragged.getStoredGridLocation(), newGridLocation)) {
                            createMoveSnapshotIfNeeded();
                        }
                    }

                    currentlyDragged.moveToGridLocation(newGridLocation, true, true);
                }

            }

            canvasUI.repaintLowLag();
            canvasUI.revalidateParent();
            canvasUI.getGraphCanvasUpdatedEventEmitter().emit(new JIPipeDesktopGraphCanvasUIUpdatedEvent(canvasUI));

            return true;
        }
        return false;
    }

    public Map<JIPipeDesktopGraphInteractiveObjectUI, Point> getCurrentlyDraggedOffsets() {
        return currentlyDraggedOffsets;
    }

    public boolean isCurrentlyDraggingNode() {
        return hasDragSnapshot;
    }

    public void cancelDragging() {
        // Node dragging
        currentlyDraggedOffsets.clear();
        hasDragSnapshot = false;
    }

    public boolean isBeingDragged(JIPipeDesktopGraphInteractiveObjectUI interactiveObjectUI) {
        return currentlyDraggedOffsets.containsKey(interactiveObjectUI);
    }

    public boolean mousePressed(MouseEvent mouseEvent) {
        if (currentlyDraggedOffsets.isEmpty()) {
            JIPipeDesktopGraphNodeUI ui = canvasUI.pickNodeUI(mouseEvent);
            if (ui != null) {
                if (mouseEvent.isShiftDown()) {
                    if (canvasUI.getSelectionManager().getSelection().contains(ui))
                        canvasUI.getSelectionManager().removeFromSelection(ui);
                    else
                        canvasUI.getSelectionManager().addToSelection(ui);
                } else {
                    if (canvasUI.getSelectionManager().getSelection().isEmpty() || canvasUI.getSelectionManager().getSelection().size() == 1) {
                        canvasUI.getSelectionManager().selectOnly(ui);
                    } else {
                        if (!canvasUI.getSelectionManager().getSelection().contains(ui)) {
                            canvasUI.getSelectionManager().selectOnly(ui);
                        }
                    }
                }
                this.hasDragSnapshot = false;
            } else {
                return false;
            }
        }
        if(!canvasUI.getSelectionManager().getSelection().isEmpty()) {
            return startDragCurrentNodeSelection(mouseEvent);
        }
        return false;
    }
}
