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

package org.hkijena.pipelinej.ui.grapheditor.contextmenu.clipboard;

import org.hkijena.pipelinej.api.algorithm.ACAQGraph;
import org.hkijena.pipelinej.api.algorithm.ACAQGraphNode;
import org.hkijena.pipelinej.api.compartments.algorithms.ACAQCompartmentOutput;
import org.hkijena.pipelinej.api.history.PasteNodeGraphHistorySnapshot;
import org.hkijena.pipelinej.api.registries.ACAQAlgorithmRegistry;
import org.hkijena.pipelinej.ui.grapheditor.ACAQGraphCanvasUI;
import org.hkijena.pipelinej.ui.grapheditor.ACAQNodeUI;
import org.hkijena.pipelinej.ui.grapheditor.contextmenu.AlgorithmUIAction;
import org.hkijena.pipelinej.utils.JsonUtils;
import org.hkijena.pipelinej.utils.StringUtils;
import org.hkijena.pipelinej.utils.UIUtils;

import javax.swing.*;
import java.awt.*;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class AlgorithmGraphPasteAlgorithmUIAction implements AlgorithmUIAction {
    @Override
    public boolean matches(Set<ACAQNodeUI> selection) {
        return !StringUtils.isNullOrEmpty(UIUtils.getStringFromClipboard());
    }

    @Override
    public void run(ACAQGraphCanvasUI canvasUI, Set<ACAQNodeUI> selection) {
        try {
            String json = UIUtils.getStringFromClipboard();
            if (json != null) {
                ACAQGraph graph = JsonUtils.getObjectMapper().readValue(json, ACAQGraph.class);

                // Replace project compartment with IOInterface
                for (ACAQGraphNode node : graph.getAlgorithmNodes().values()) {
                    if (node instanceof ACAQCompartmentOutput) {
                        node.setDeclaration(ACAQAlgorithmRegistry.getInstance().getDeclarationById("io-interface"));
                    }
                }

                // Save the original locations (if available)
                int minX = Integer.MAX_VALUE;
                int minY = Integer.MAX_VALUE;
                Map<ACAQGraphNode, Point> originalLocations = new HashMap<>();
                for (ACAQGraphNode algorithm : graph.getAlgorithmNodes().values()) {
                    Point point = algorithm.getLocationWithin(algorithm.getCompartment(), canvasUI.getCurrentViewMode().name());
                    if (point != null) {
                        originalLocations.put(algorithm, point);
                        minX = Math.min(minX, point.x);
                        minY = Math.min(minY, point.y);
                    }
                }

                // Change the compartment
                String compartment = canvasUI.getCompartment();
                for (ACAQGraphNode algorithm : graph.getAlgorithmNodes().values()) {
                    algorithm.setCompartment(compartment);
                }

                // Update the location relative to the mouse
                Point cursor = canvasUI.getGraphEditorCursor();
                for (ACAQGraphNode algorithm : graph.getAlgorithmNodes().values()) {
                    Point original = originalLocations.getOrDefault(algorithm, null);
                    if (original != null) {
                        original.x = original.x - minX + cursor.x;
                        original.y = original.y - minY + cursor.y;
                        algorithm.setLocationWithin(compartment, original, canvasUI.getCurrentViewMode().name());
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
