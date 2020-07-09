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

import org.hkijena.pipelinej.api.ACAQProject;
import org.hkijena.pipelinej.api.algorithm.ACAQGraph;
import org.hkijena.pipelinej.api.algorithm.ACAQGraphNode;
import org.hkijena.pipelinej.api.compartments.algorithms.ACAQCompartmentOutput;
import org.hkijena.pipelinej.api.compartments.algorithms.ACAQProjectCompartment;
import org.hkijena.pipelinej.api.data.ACAQDataSlot;
import org.hkijena.pipelinej.api.history.PasteCompartmentGraphHistorySnapshot;
import org.hkijena.pipelinej.ui.ACAQProjectWorkbench;
import org.hkijena.pipelinej.ui.grapheditor.ACAQGraphCanvasUI;
import org.hkijena.pipelinej.ui.grapheditor.ACAQNodeUI;
import org.hkijena.pipelinej.ui.grapheditor.contextmenu.AlgorithmUIAction;
import org.hkijena.pipelinej.utils.JsonUtils;
import org.hkijena.pipelinej.utils.StringUtils;
import org.hkijena.pipelinej.utils.UIUtils;

import javax.swing.*;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import static org.hkijena.pipelinej.utils.UIUtils.getStringFromClipboard;

public class GraphCompartmentPasteAlgorithmUIAction implements AlgorithmUIAction {
    @Override
    public boolean matches(Set<ACAQNodeUI> selection) {
        return !StringUtils.isNullOrEmpty(UIUtils.getStringFromClipboard());
    }

    @Override
    public void run(ACAQGraphCanvasUI canvasUI, Set<ACAQNodeUI> selection) {
        try {
            String json = getStringFromClipboard();
            if (json != null) {
                ACAQProject project = ((ACAQProjectWorkbench) canvasUI.getWorkbench()).getProject();
                ACAQGraph sourceGraph = JsonUtils.getObjectMapper().readValue(json, ACAQGraph.class);

                // Collect all compartments
                Set<String> compartments = new HashSet<>();
                for (ACAQGraphNode algorithm : sourceGraph.getAlgorithmNodes().values()) {
                    compartments.add(algorithm.getCompartment());
                }

                // Create compartments and assign their compartments
                Map<String, ACAQProjectCompartment> compartmentNodeMap = new HashMap<>();
                for (String compartment : compartments) {
                    ACAQProjectCompartment compartmentNode = project.addCompartment(compartment);
                    canvasUI.getGraphHistory().addSnapshotBefore(new PasteCompartmentGraphHistorySnapshot(project, compartmentNode));
                    compartmentNodeMap.put(compartment, compartmentNode);
                }

                // Add nodes
                ACAQGraph targetGraph = project.getGraph();
                for (ACAQGraphNode algorithm : sourceGraph.getAlgorithmNodes().values()) {
                    String sourceCompartment = algorithm.getCompartment();
                    String targetCompartment = compartmentNodeMap.get(sourceCompartment).getProjectCompartmentId();

                    if (algorithm instanceof ACAQCompartmentOutput) {
                        // Copy slot configuration
                        ACAQCompartmentOutput outputNode = compartmentNodeMap.get(sourceCompartment).getOutputNode();
                        outputNode.getSlotConfiguration().setTo(algorithm.getSlotConfiguration());
                    } else {
                        targetGraph.insertNode(algorithm, targetCompartment);
                    }
                }

                // Add edges
                for (Map.Entry<ACAQDataSlot, ACAQDataSlot> edge : sourceGraph.getSlotEdges()) {
                    if (edge.getValue().getNode() instanceof ACAQCompartmentOutput) {
                        String sourceCompartment = edge.getValue().getNode().getCompartment();
                        ACAQCompartmentOutput outputNode = compartmentNodeMap.get(sourceCompartment).getOutputNode();
                        ACAQDataSlot outputNodeSlot = outputNode.getInputSlotMap().get(edge.getValue().getName());

                        targetGraph.connect(edge.getKey(), outputNodeSlot);
                    } else {
                        // We can directly connect it
                        targetGraph.connect(edge.getKey(), edge.getValue());
                    }
                }
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
        return "Copies compartments from clipboard into the current graph";
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
