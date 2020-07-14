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

package org.hkijena.jipipe.ui.grapheditor.contextmenu.clipboard;

import org.hkijena.jipipe.api.algorithm.JIPipeGraph;
import org.hkijena.jipipe.api.algorithm.JIPipeGraphNode;
import org.hkijena.jipipe.api.compartments.algorithms.JIPipeCompartmentOutput;
import org.hkijena.jipipe.api.history.PasteNodeGraphHistorySnapshot;
import org.hkijena.jipipe.api.registries.JIPipeAlgorithmRegistry;
import org.hkijena.jipipe.ui.grapheditor.JIPipeGraphCanvasUI;
import org.hkijena.jipipe.ui.grapheditor.JIPipeNodeUI;
import org.hkijena.jipipe.ui.grapheditor.contextmenu.AlgorithmUIAction;
import org.hkijena.jipipe.utils.JsonUtils;
import org.hkijena.jipipe.utils.UIUtils;

import javax.swing.*;
import java.awt.*;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class AlgorithmGraphPasteAlgorithmUIAction implements AlgorithmUIAction {
    @Override
    public boolean matches(Set<JIPipeNodeUI> selection) {
        return true;
    }

    @Override
    public void run(JIPipeGraphCanvasUI canvasUI, Set<JIPipeNodeUI> selection) {
        try {
            String json = UIUtils.getStringFromClipboard();
            if (json != null) {
                JIPipeGraph graph = JsonUtils.getObjectMapper().readValue(json, JIPipeGraph.class);

                // Replace project compartment with IOInterface
                for (JIPipeGraphNode node : graph.getAlgorithmNodes().values()) {
                    if (node instanceof JIPipeCompartmentOutput) {
                        node.setInfo(JIPipeAlgorithmRegistry.getInstance().getInfoById("io-interface"));
                    }
                }

                // Save the original locations (if available)
                int minX = Integer.MAX_VALUE;
                int minY = Integer.MAX_VALUE;
                Map<JIPipeGraphNode, Point> originalLocations = new HashMap<>();
                for (JIPipeGraphNode algorithm : graph.getAlgorithmNodes().values()) {
                    Point point = algorithm.getLocationWithin(algorithm.getCompartment(), canvasUI.getViewMode().name());
                    if (point != null) {
                        originalLocations.put(algorithm, point);
                        minX = Math.min(minX, point.x);
                        minY = Math.min(minY, point.y);
                    }
                }

                // Change the compartment
                String compartment = canvasUI.getCompartment();
                for (JIPipeGraphNode algorithm : graph.getAlgorithmNodes().values()) {
                    algorithm.setCompartment(compartment);
                }

                // Update the location relative to the mouse
                Point cursor = canvasUI.getGraphEditorCursor();
                for (JIPipeGraphNode algorithm : graph.getAlgorithmNodes().values()) {
                    Point original = originalLocations.getOrDefault(algorithm, null);
                    if (original != null) {
                        original.x = original.x - minX + cursor.x;
                        original.y = original.y - minY + cursor.y;
                        algorithm.setLocationWithin(compartment, original, canvasUI.getViewMode().name());
                    }
                }

                // Add to graph
                canvasUI.getGraphHistory().addSnapshotBefore(new PasteNodeGraphHistorySnapshot(canvasUI.getGraph(),
                        new HashSet<>(graph.getAlgorithmNodes().values())));
                canvasUI.getGraph().mergeWith(graph);
            }
        } catch (Exception e) {
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
        return UIUtils.getIconFromResources("paste.png");
    }

    @Override
    public boolean isShowingInOverhang() {
        return false;
    }
}
