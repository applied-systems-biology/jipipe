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

package org.hkijena.jipipe.desktop.app.grapheditor.commons.canvas.managers;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.google.common.collect.ImmutableList;
import org.hkijena.jipipe.JIPipe;
import org.hkijena.jipipe.api.compartments.algorithms.IOInterfaceAlgorithm;
import org.hkijena.jipipe.api.data.JIPipeDataSlot;
import org.hkijena.jipipe.api.nodes.JIPipeGraph;
import org.hkijena.jipipe.api.nodes.JIPipeGraphNode;
import org.hkijena.jipipe.api.nodes.categories.InternalNodeTypeCategory;
import org.hkijena.jipipe.desktop.app.JIPipeDesktopProjectWorkbench;
import org.hkijena.jipipe.desktop.app.grapheditor.commons.JIPipeDesktopGraphCanvasUI;
import org.hkijena.jipipe.desktop.app.grapheditor.commons.canvas.JIPipeDesktopGraphCanvasGrid;
import org.hkijena.jipipe.desktop.app.grapheditor.commons.nodeui.JIPipeDesktopGraphNodeUI;
import org.hkijena.jipipe.plugins.settings.application.JIPipeGraphEditorUIApplicationSettings;
import org.hkijena.jipipe.utils.json.JsonUtils;

import javax.swing.*;
import java.awt.*;
import java.util.*;

public class JIPipeDesktopGraphCanvasNodeManager {
    private final JIPipeDesktopGraphCanvasUI canvasUI;

    public JIPipeDesktopGraphCanvasNodeManager(JIPipeDesktopGraphCanvasUI canvasUI) {
        this.canvasUI = canvasUI;
    }

    public void autoPlaceCloseToCursor(JIPipeDesktopGraphNodeUI ui, boolean force) {
        int minX = 0;
        int minY = 0;
        Point cursor = canvasUI.getGraphEditorCursor();
        if (cursor != null) {
            minX = cursor.x;
            minY = cursor.y;
        }
        ui.moveToClosestGridPoint(new Point(minX, minY), force, true);
        if (canvasUI.getGraphEditorUI() != null) {
            canvasUI.getGraphEditorUI().scrollToAlgorithm(ui);
        }
    }

    public Set<JIPipeDesktopGraphNodeUI> getNodesAfter(int x, int y) {
        Set<JIPipeDesktopGraphNodeUI> result = new HashSet<>();
        for (JIPipeDesktopGraphNodeUI ui : canvasUI.getNodeUIs().values()) {
            if (ui.getY() >= y)
                result.add(ui);
        }
        return result;
    }

    public void autoPlaceTargetAdjacent(JIPipeDesktopGraphNodeUI sourceAlgorithmUI, JIPipeDataSlot source, JIPipeDesktopGraphNodeUI targetAlgorithmUI, JIPipeDataSlot target) {
        int sourceSlotIndex = source.getNode().getOutputSlots().indexOf(source);
        int targetSlotIndex = target.getNode().getInputSlots().indexOf(target);
        if (sourceSlotIndex < 0 || targetSlotIndex < 0) {
            autoPlaceCloseToCursor(targetAlgorithmUI, true);
            return;
        }

        Set<JIPipeDesktopGraphNodeUI> nodesAfter = getNodesAfter(sourceAlgorithmUI.getRightX(), sourceAlgorithmUI.getBottomY());
        int x = sourceAlgorithmUI.getSlotLocation(source).center.x + sourceAlgorithmUI.getX();
        x -= targetAlgorithmUI.getSlotLocation(target).center.x;
        int y = (int) Math.round(sourceAlgorithmUI.getBottomY() + JIPipeDesktopGraphCanvasGrid.GRID_HEIGHT * canvasUI.getZoom());
        Point targetPoint = new Point(x, y);
        if (JIPipeGraphEditorUIApplicationSettings.getInstance().isAutoLayoutMovesOtherNodes()) {
            if (!targetAlgorithmUI.moveToClosestGridPoint(targetPoint, false, true)) {
                if (nodesAfter.isEmpty())
                    return;
                // Move all other algorithms
                int minDistance = Integer.MAX_VALUE;
                for (JIPipeDesktopGraphNodeUI ui : nodesAfter) {
                    if (ui == targetAlgorithmUI || ui == sourceAlgorithmUI)
                        continue;
                    minDistance = Math.min(minDistance, ui.getY() - sourceAlgorithmUI.getBottomY());
                }
                int translateY = (int) Math.round(targetAlgorithmUI.getHeight() + JIPipeDesktopGraphCanvasGrid.GRID_HEIGHT * canvasUI.getZoom() * 2 - minDistance);
                for (JIPipeDesktopGraphNodeUI ui : nodesAfter) {
                    if (ui == targetAlgorithmUI || ui == sourceAlgorithmUI)
                        continue;
                    ui.moveToClosestGridPoint(new Point(ui.getX(), ui.getY() + translateY), true, true);
                }
                if (!targetAlgorithmUI.moveToClosestGridPoint(targetPoint, false, true)) {
                    autoPlaceCloseToCursor(targetAlgorithmUI, true);
                }
            }
        } else {
            autoPlaceCloseToLocation(targetAlgorithmUI, targetPoint);
        }
    }


    /**
     * Moves the node close to a real location
     *
     * @param ui       the node
     * @param location a real location
     */
    public void autoPlaceCloseToLocation(JIPipeDesktopGraphNodeUI ui, Point location) {

        int minX = location.x;
        int minY = location.y;

        Set<Rectangle> otherShapes = new HashSet<>();
        for (JIPipeDesktopGraphNodeUI otherUi : canvasUI.getNodeUIs().values()) {
            if (ui != otherUi) {
                otherShapes.add(otherUi.getBounds());
            }
        }

        Rectangle viewRectangle = null;
        JScrollPane scrollPane = canvasUI.getScrollPane();
        if (scrollPane != null) {
            int hValue = scrollPane.getHorizontalScrollBar().getValue();
            int vValue = scrollPane.getVerticalScrollBar().getValue();
            int hWidth = scrollPane.getHorizontalScrollBar().getVisibleAmount();
            int vHeight = scrollPane.getVerticalScrollBar().getVisibleAmount();
            viewRectangle = new Rectangle(hValue, vValue, hWidth, vHeight);

            viewRectangle.width -= JIPipeDesktopGraphCanvasGrid.GRID_WIDTH / 4;
            viewRectangle.height -= JIPipeDesktopGraphCanvasGrid.GRID_HEIGHT / 4;
        }

//        System.out.println("Loc: " + location);
//        System.out.println("View: " + viewRectangle);
        Rectangle currentShape = new Rectangle(minX, minY, ui.getWidth(), ui.getHeight());

        if (viewRectangle != null && !viewRectangle.contains(location)) {
            minX = viewRectangle.x + JIPipeDesktopGraphCanvasGrid.GRID_WIDTH;
            minY = viewRectangle.y + JIPipeDesktopGraphCanvasGrid.GRID_HEIGHT;
        }

        boolean found;
        do {
            found = true;
            for (Rectangle otherShape : otherShapes) {
                if (otherShape.intersects(currentShape)) {
                    found = false;
                    break;
                }
            }
            if (!found) {
                currentShape.x += JIPipeDesktopGraphCanvasGrid.GRID_WIDTH;
            }
            /*
             * Check if we are still within the visible rectangle.
             * Prevent nodes going to somewhere else
             */
            if (viewRectangle != null && !viewRectangle.intersects(currentShape)) {
                currentShape.x = minX;
                currentShape.y = minY;
                break;
            }
            /*
             * Check if we are too far away
             * The user expects the new node to be close to the cursor
             */
            double relativeDistanceToOriginalPoint = Math.abs(1.0 * minY - currentShape.y) / currentShape.height;
            if (relativeDistanceToOriginalPoint > 2) {
                currentShape.x = minX;
                currentShape.y = minY;
                break;
            }
        }
        while (!found);

//        System.out.println(currentShape);
        ui.moveToClosestGridPoint(new Point(currentShape.x, currentShape.y), true, true);
    }


    public Map<UUID, JIPipeGraphNode> pasteNodes(String json) throws JsonProcessingException {
        if (!JIPipeDesktopProjectWorkbench.canAddOrDeleteNodes(canvasUI.getDesktopWorkbench()))
            return Collections.emptyMap();
        JIPipeGraph graph = JsonUtils.getObjectMapper().readValue(json, JIPipeGraph.class);
        if (graph.isEmpty()) {
            throw new NullPointerException("Empty graph pasted.");
        }

        // Replace project compartment with IOInterface
        for (JIPipeGraphNode node : ImmutableList.copyOf(graph.getGraphNodes())) {
            if (node instanceof IOInterfaceAlgorithm && node.getCategory() instanceof InternalNodeTypeCategory) {
                IOInterfaceAlgorithm replacement = new IOInterfaceAlgorithm((IOInterfaceAlgorithm) node);
                replacement.setInfo(JIPipe.getNodes().getInfoById("io-interface"));
                graph.replaceNode(node, replacement);
            } else if (node.getCategory() instanceof InternalNodeTypeCategory) {
                // Don't paste internal nodes
                graph.removeNode(node, false);
            }
        }

        // Save the original locations (if available)
        int minX = Integer.MAX_VALUE;
        int minY = Integer.MAX_VALUE;
        Map<JIPipeGraphNode, Point> originalLocations = new HashMap<>();
        for (JIPipeGraphNode algorithm : graph.getGraphNodes()) {
            String compartmentUUIDInGraphAsString = algorithm.getCompartmentUUIDInGraphAsString();
            Point point = algorithm.getNodeUILocationWithin(compartmentUUIDInGraphAsString);
            if (point != null) {
                originalLocations.put(algorithm, point);
                minX = Math.min(minX, point.x);
                minY = Math.min(minY, point.y);
            }
        }
        if (minX == Integer.MAX_VALUE)
            minX = 0;
        if (minY == Integer.MAX_VALUE)
            minY = 0;

        // Change the compartment
        UUID compartment = canvasUI.getCompartmentUUID();
        for (JIPipeGraphNode node : graph.getGraphNodes()) {
            graph.setCompartment(node.getUUIDInParentGraph(), compartment);
        }

        // Update the location relative to the mouse
        Point cursor = canvasUI.getGraphEditorCursor();
        for (JIPipeGraphNode algorithm : graph.getGraphNodes()) {
            Point original = originalLocations.getOrDefault(algorithm, null);
            if (original != null) {
                original.x = (int) (original.x - minX + (cursor.x / canvasUI.getZoom()) / JIPipeDesktopGraphCanvasGrid.GRID_WIDTH);
                original.y = (int) (original.y - minY + (cursor.y / canvasUI.getZoom()) / JIPipeDesktopGraphCanvasGrid.GRID_HEIGHT);
                algorithm.setNodeUILocationWithin(compartment, original);
            }
        }

        // Add to graph
        if (canvasUI.getHistoryJournal() != null) {
            canvasUI.getHistoryJournal().snapshotBeforePasteNodes(graph.getGraphNodes(), canvasUI.getCompartmentUUID());
        }
        return canvasUI.getGraph().mergeWith(graph);
    }
}
