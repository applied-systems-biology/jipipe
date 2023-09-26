/*
 * Copyright by Zoltán Cseresnyés, Ruman Gerst
 *
 * Research Group Applied Systems Biology - Head: Prof. Dr. Marc Thilo Figge
 * https://www.leibniz-hki.de/en/applied-systems-biology.html
 * HKI-Center for Systems Biology of Infection
 * Leibniz Institute for Natural Product Research and Infection Biology - Hans Knöll Institute (HKI)
 * Adolf-Reichwein-Straße 23, 07745 Jena, Germany
 *
 * The project code is licensed under BSD 2-Clause.
 * See the LICENSE file provided with the code for the full license.
 */

package org.hkijena.jipipe.ui.grapheditor.compartments.contextmenu.clipboard.clipboard;

import com.google.common.collect.ImmutableList;
import org.hkijena.jipipe.JIPipe;
import org.hkijena.jipipe.api.compartments.algorithms.IOInterfaceAlgorithm;
import org.hkijena.jipipe.api.nodes.JIPipeGraph;
import org.hkijena.jipipe.api.nodes.JIPipeGraphNode;
import org.hkijena.jipipe.api.nodes.categories.InternalNodeTypeCategory;
import org.hkijena.jipipe.ui.JIPipeProjectWorkbench;
import org.hkijena.jipipe.ui.grapheditor.general.JIPipeGraphCanvasUI;
import org.hkijena.jipipe.ui.grapheditor.general.contextmenu.NodeUIContextAction;
import org.hkijena.jipipe.ui.grapheditor.general.nodeui.JIPipeGraphNodeUI;
import org.hkijena.jipipe.utils.UIUtils;
import org.hkijena.jipipe.utils.json.JsonUtils;

import javax.swing.*;
import java.awt.*;
import java.awt.event.InputEvent;
import java.awt.event.KeyEvent;
import java.util.*;

public class AlgorithmGraphPasteNodeUIContextAction implements NodeUIContextAction {
    public static Map<UUID, JIPipeGraphNode> pasteNodes(JIPipeGraphCanvasUI canvasUI, String json) throws com.fasterxml.jackson.core.JsonProcessingException {
        if (!JIPipeProjectWorkbench.canAddOrDeleteNodes(canvasUI.getWorkbench()))
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
            }
            else if(node.getCategory() instanceof InternalNodeTypeCategory) {
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
            Point point = algorithm.getLocationWithin(compartmentUUIDInGraphAsString, canvasUI.getViewMode().name());
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
        UUID compartment = canvasUI.getCompartment();
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
                algorithm.setLocationWithin(compartment, original, canvasUI.getViewMode().name());
            }
        }

        // Add to graph
        if (canvasUI.getHistoryJournal() != null) {
            canvasUI.getHistoryJournal().snapshotBeforePasteNodes(graph.getGraphNodes(), canvasUI.getCompartment());
        }
        return canvasUI.getGraph().mergeWith(graph);
    }

    @Override
    public boolean matches(Set<JIPipeGraphNodeUI> selection) {
        return true;
    }

    @Override
    public void run(JIPipeGraphCanvasUI canvasUI, Set<JIPipeGraphNodeUI> selection) {
        try {
            String json = UIUtils.getStringFromClipboard();
            if (json != null) {
                pasteNodes(canvasUI, json);
            }
        } catch (Exception e) {
            JOptionPane.showMessageDialog(canvasUI.getWorkbench().getWindow(), "The current clipboard contents are no valid nodes/graph.", "Paste nodes", JOptionPane.ERROR_MESSAGE);
            e.printStackTrace();
        }
    }

    @Override
    public String getName() {
        return "Paste";
    }

    @Override
    public String getDescription() {
        return "Copies nodes from clipboard into the current graph";
    }

    @Override
    public Icon getIcon() {
        return UIUtils.getIconFromResources("actions/edit-paste.png");
    }

    @Override
    public KeyStroke getKeyboardShortcut() {
        return KeyStroke.getKeyStroke(KeyEvent.VK_V, InputEvent.CTRL_MASK, true);
    }

    @Override
    public boolean showInMultiSelectionPanel() {
        return false;
    }
}
