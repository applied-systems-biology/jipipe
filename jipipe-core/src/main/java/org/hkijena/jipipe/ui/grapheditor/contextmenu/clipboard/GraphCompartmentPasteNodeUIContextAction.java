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

import org.hkijena.jipipe.api.JIPipeProject;
import org.hkijena.jipipe.api.algorithm.JIPipeGraph;
import org.hkijena.jipipe.api.algorithm.JIPipeGraphNode;
import org.hkijena.jipipe.api.compartments.algorithms.JIPipeCompartmentOutput;
import org.hkijena.jipipe.api.compartments.algorithms.JIPipeProjectCompartment;
import org.hkijena.jipipe.api.data.JIPipeDataSlot;
import org.hkijena.jipipe.api.history.PasteCompartmentGraphHistorySnapshot;
import org.hkijena.jipipe.ui.JIPipeProjectWorkbench;
import org.hkijena.jipipe.ui.grapheditor.JIPipeGraphCanvasUI;
import org.hkijena.jipipe.ui.grapheditor.JIPipeNodeUI;
import org.hkijena.jipipe.ui.grapheditor.contextmenu.NodeUIContextAction;
import org.hkijena.jipipe.utils.JsonUtils;
import org.hkijena.jipipe.utils.StringUtils;
import org.hkijena.jipipe.utils.UIUtils;

import javax.swing.*;
import java.awt.event.InputEvent;
import java.awt.event.KeyEvent;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import static org.hkijena.jipipe.utils.UIUtils.getStringFromClipboard;

public class GraphCompartmentPasteNodeUIContextAction implements NodeUIContextAction {
    @Override
    public boolean matches(Set<JIPipeNodeUI> selection) {
        return !StringUtils.isNullOrEmpty(UIUtils.getStringFromClipboard());
    }

    @Override
    public void run(JIPipeGraphCanvasUI canvasUI, Set<JIPipeNodeUI> selection) {
        try {
            String json = getStringFromClipboard();
            if (json != null) {
                JIPipeProject project = ((JIPipeProjectWorkbench) canvasUI.getWorkbench()).getProject();
                JIPipeGraph sourceGraph = JsonUtils.getObjectMapper().readValue(json, JIPipeGraph.class);

                // Collect all compartments
                Set<String> compartments = new HashSet<>();
                for (JIPipeGraphNode algorithm : sourceGraph.getNodes().values()) {
                    compartments.add(algorithm.getCompartment());
                }

                // Create compartments and assign their compartments
                Map<String, JIPipeProjectCompartment> compartmentNodeMap = new HashMap<>();
                for (String compartment : compartments) {
                    JIPipeProjectCompartment compartmentNode = project.addCompartment(compartment);
                    canvasUI.getGraphHistory().addSnapshotBefore(new PasteCompartmentGraphHistorySnapshot(project, compartmentNode));
                    compartmentNodeMap.put(compartment, compartmentNode);
                }

                // Add nodes
                JIPipeGraph targetGraph = project.getGraph();
                for (JIPipeGraphNode algorithm : sourceGraph.getNodes().values()) {
                    String sourceCompartment = algorithm.getCompartment();
                    String targetCompartment = compartmentNodeMap.get(sourceCompartment).getProjectCompartmentId();

                    if (algorithm instanceof JIPipeCompartmentOutput) {
                        // Copy slot configuration
                        JIPipeCompartmentOutput outputNode = compartmentNodeMap.get(sourceCompartment).getOutputNode();
                        outputNode.getSlotConfiguration().setTo(algorithm.getSlotConfiguration());
                    } else {
                        targetGraph.insertNode(algorithm, targetCompartment);
                    }
                }

                // Add edges
                for (Map.Entry<JIPipeDataSlot, JIPipeDataSlot> edge : sourceGraph.getSlotEdges()) {
                    if (edge.getValue().getNode() instanceof JIPipeCompartmentOutput) {
                        String sourceCompartment = edge.getValue().getNode().getCompartment();
                        JIPipeCompartmentOutput outputNode = compartmentNodeMap.get(sourceCompartment).getOutputNode();
                        JIPipeDataSlot outputNodeSlot = outputNode.getInputSlotMap().get(edge.getValue().getName());

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
        return UIUtils.getIconFromResources("actions/edit-paste.png");
    }

    @Override
    public boolean isShowingInOverhang() {
        return false;
    }

    @Override
    public KeyStroke getKeyboardShortcut() {
        return KeyStroke.getKeyStroke(KeyEvent.VK_V, InputEvent.CTRL_MASK, true);
    }
}
