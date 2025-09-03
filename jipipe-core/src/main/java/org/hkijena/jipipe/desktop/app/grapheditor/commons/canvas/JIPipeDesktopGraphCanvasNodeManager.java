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

package org.hkijena.jipipe.desktop.app.grapheditor.commons.canvas;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.google.common.collect.ImmutableList;
import org.hkijena.jipipe.JIPipe;
import org.hkijena.jipipe.api.compartments.algorithms.IOInterfaceAlgorithm;
import org.hkijena.jipipe.api.nodes.JIPipeGraph;
import org.hkijena.jipipe.api.nodes.JIPipeGraphNode;
import org.hkijena.jipipe.api.nodes.categories.InternalNodeTypeCategory;
import org.hkijena.jipipe.desktop.app.JIPipeDesktopProjectWorkbench;
import org.hkijena.jipipe.desktop.app.grapheditor.commons.JIPipeDesktopGraphCanvasUI;
import org.hkijena.jipipe.utils.json.JsonUtils;
import org.hkijena.jipipe.utils.ui.events.MouseDraggedEvent;
import org.hkijena.jipipe.utils.ui.events.MouseDraggedEventListener;

import java.awt.*;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.MouseMotionListener;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class JIPipeDesktopGraphCanvasNodeManager {
    private final JIPipeDesktopGraphCanvasUI canvasUI;

    public JIPipeDesktopGraphCanvasNodeManager(JIPipeDesktopGraphCanvasUI canvasUI) {
        this.canvasUI = canvasUI;
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
            Point point = algorithm.getNodeUILocationWithin(compartmentUUIDInGraphAsString, canvasUI.getViewMode().name());
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
                original.x = (int) (original.x - minX + (cursor.x / canvasUI.getZoom()) / canvasUI.getViewMode().getGridWidth());
                original.y = (int) (original.y - minY + (cursor.y / canvasUI.getZoom()) / canvasUI.getViewMode().getGridHeight());
                algorithm.setNodeUILocationWithin(compartment, original, canvasUI.getViewMode().name());
            }
        }

        // Add to graph
        if (canvasUI.getHistoryJournal() != null) {
            canvasUI.getHistoryJournal().snapshotBeforePasteNodes(graph.getGraphNodes(), canvasUI.getCompartmentUUID());
        }
        return canvasUI.getGraph().mergeWith(graph);
    }
}
