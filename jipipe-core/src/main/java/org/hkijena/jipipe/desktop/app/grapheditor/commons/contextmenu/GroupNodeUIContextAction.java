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

package org.hkijena.jipipe.desktop.app.grapheditor.commons.contextmenu;

import org.hkijena.jipipe.JIPipe;
import org.hkijena.jipipe.api.data.JIPipeInputDataSlot;
import org.hkijena.jipipe.api.data.JIPipeMutableSlotConfiguration;
import org.hkijena.jipipe.api.data.JIPipeOutputDataSlot;
import org.hkijena.jipipe.api.grouping.GraphWrapperAlgorithmInput;
import org.hkijena.jipipe.api.grouping.GraphWrapperAlgorithmOutput;
import org.hkijena.jipipe.api.grouping.JIPipeNodeGroup;
import org.hkijena.jipipe.api.nodes.JIPipeGraph;
import org.hkijena.jipipe.api.nodes.JIPipeGraphNode;
import org.hkijena.jipipe.desktop.app.JIPipeDesktopProjectWorkbench;
import org.hkijena.jipipe.desktop.app.grapheditor.commons.JIPipeDesktopGraphCanvasUI;
import org.hkijena.jipipe.desktop.app.grapheditor.commons.nodeui.JIPipeDesktopGraphNodeUI;
import org.hkijena.jipipe.utils.StringUtils;
import org.hkijena.jipipe.utils.UIUtils;

import javax.swing.*;
import java.util.*;
import java.util.stream.Collectors;

public class GroupNodeUIContextAction implements NodeUIContextAction {
    @Override
    public boolean matches(Set<JIPipeDesktopGraphNodeUI> selection) {
        return JIPipe.getNodes().hasNodeInfoWithId("node-group") && !selection.isEmpty();
    }

    @Override
    public void run(JIPipeDesktopGraphCanvasUI canvasUI, Set<JIPipeDesktopGraphNodeUI> selection) {
        if (!JIPipeDesktopProjectWorkbench.canAddOrDeleteNodes(canvasUI.getDesktopWorkbench())) {
            return;
        }
        if (canvasUI.getHistoryJournal() != null) {
            Set<JIPipeGraphNode> nodes = selection.stream().map(JIPipeDesktopGraphNodeUI::getNode).collect(Collectors.toSet());
            UUID compartment = nodes.stream().map(JIPipeGraphNode::getUUIDInParentGraph).findFirst().orElse(null);
            canvasUI.getHistoryJournal().snapshot("Group", "Grouped nodes", compartment, UIUtils.getIconFromResources("actions/object-group.png"));
        }
        Set<JIPipeGraphNode> algorithms = selection.stream().map(JIPipeDesktopGraphNodeUI::getNode).collect(Collectors.toSet());
        JIPipeGraph originalGraph = canvasUI.getGraph();
        JIPipeGraph subGraph = originalGraph.extract(algorithms, false);
        JIPipeNodeGroup group = new JIPipeNodeGroup(subGraph, false, false, true);
        GraphWrapperAlgorithmInput groupInput = group.getGroupInput();
        GraphWrapperAlgorithmOutput groupOutput = group.getGroupOutput();

        Map<String, String> mappedInputs = new HashMap<>();
        Map<String, String> mappedOutputs = new HashMap<>();

        // Auto-create I/O slots and connect them
        for (JIPipeGraphNode originalNode : algorithms) {
            JIPipeGraphNode copyNode = group.getWrappedGraph().getEquivalentNode(originalNode);
            if (copyNode != null) {
                for (JIPipeInputDataSlot copyInputSlot : copyNode.getInputSlots()) {
                    // Only create input if there is no internal connection
                    if(group.getWrappedGraph().getInputIncomingSourceSlots(copyInputSlot).isEmpty()) {
                        boolean create = false;
                        if (!copyInputSlot.getInfo().isOptional()) {
                            // Not optional, so create it
                            create = true;
                        }
                        else {
                            JIPipeInputDataSlot originalInputSlot = originalNode.getInputSlot(copyInputSlot.getName());

                            // Optional, so only create if the original slot has a source
                            if(originalInputSlot != null && !originalGraph.getInputIncomingSourceSlots(originalInputSlot).isEmpty()) {
                                create = true;
                            }
                        }

                        if(create) {
                            // Check if we already created the mapped slot
                            String uniqueId = originalNode.getUUIDInParentGraph() + "/" + copyInputSlot.getName();
                            String name = mappedInputs.getOrDefault(uniqueId, null);

                            if(name == null) {
                                String baseName = StringUtils.makeFilesystemCompatible(originalNode.getName());
                                name = StringUtils.makeUniqueString(baseName + " " + copyInputSlot.getName(), " ", s -> groupInput.getOutputSlotMap().containsKey(s));
                                JIPipeMutableSlotConfiguration slotConfiguration = (JIPipeMutableSlotConfiguration) groupInput.getSlotConfiguration();
                                slotConfiguration.addSlot(name, copyInputSlot.getInfo(), true);
                                mappedInputs.put(uniqueId, name);
                            }

                            // Connect the mapped slot to the input
                            JIPipeOutputDataSlot outputSlot = groupInput.getOutputSlot(name);
                            group.getWrappedGraph().connect(outputSlot, copyInputSlot);
                        }
                    }
                }

                List<JIPipeOutputDataSlot> outputSlotsToCreate = new ArrayList<>();

                // First output pass -> only outside connections
                for (JIPipeOutputDataSlot copyOutputSlot : copyNode.getOutputSlots()) {
                    // Only create output if there is no internal connection
                    if(group.getWrappedGraph().getOutputOutgoingTargetSlots(copyOutputSlot).isEmpty()) {
                        JIPipeOutputDataSlot originalOutputSlot = originalNode.getOutputSlot(copyOutputSlot.getName());
                        // Check if this one goes outside
                        if(originalOutputSlot != null && !originalGraph.getOutputOutgoingTargetSlots(originalOutputSlot).isEmpty()) {
                            outputSlotsToCreate.add(copyOutputSlot);
                        }
                    }
                }

                // Second outside pass (only if none are found) -> rect to all unconnected outputs
                if(outputSlotsToCreate.isEmpty()) {
                    for (JIPipeOutputDataSlot copyOutputSlot : copyNode.getOutputSlots()) {
                        // Only create output if there is no internal connection
                        if(group.getWrappedGraph().getOutputOutgoingTargetSlots(copyOutputSlot).isEmpty()) {
                            outputSlotsToCreate.add(copyOutputSlot);
                        }
                    }
                }

                for (JIPipeOutputDataSlot copyOutputSlot : outputSlotsToCreate) {
                    // Check if we already created the mapped slot
                    String uniqueId = originalNode.getUUIDInParentGraph() + "/" + copyOutputSlot.getName();
                    String name = mappedOutputs.getOrDefault(uniqueId, null);

                    if(name == null) {
                        String baseName = StringUtils.makeFilesystemCompatible(originalNode.getName());
                        name = StringUtils.makeUniqueString(baseName + " " + copyOutputSlot.getName(), " ", s -> groupOutput.getOutputSlotMap().containsKey(s));
                        JIPipeMutableSlotConfiguration slotConfiguration = (JIPipeMutableSlotConfiguration) groupOutput.getSlotConfiguration();
                        slotConfiguration.addSlot(name, copyOutputSlot.getInfo(), true);
                        mappedOutputs.put(uniqueId, name);
                    }

                    // Connect the mapped slot to the input
                    JIPipeInputDataSlot inputSlot = groupOutput.getInputSlot(name);
                    group.getWrappedGraph().connect(copyOutputSlot, inputSlot);
                }

            }
        }


        if (JOptionPane.showConfirmDialog(canvasUI.getDesktopWorkbench().getWindow(), "Do you want to keep the selected nodes?", "Group", JOptionPane.YES_NO_OPTION) == JOptionPane.NO_OPTION) {
            for (JIPipeGraphNode algorithm : algorithms) {
                originalGraph.removeNode(algorithm, true);
            }
        }
        originalGraph.insertNode(group, canvasUI.getCompartmentUUID());
    }

    @Override
    public String getName() {
        return "Group";
    }

    @Override
    public String getDescription() {
        return "Moves the selected nodes into a group node.";
    }

    @Override
    public Icon getIcon() {
        return UIUtils.getIconFromResources("actions/object-group.png");
    }

}
