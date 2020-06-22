package org.hkijena.acaq5.ui.compartments;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.google.common.collect.ImmutableSet;
import org.hkijena.acaq5.api.ACAQProject;
import org.hkijena.acaq5.api.algorithm.ACAQAlgorithmGraph;
import org.hkijena.acaq5.api.algorithm.ACAQGraphNode;
import org.hkijena.acaq5.api.compartments.algorithms.ACAQCompartmentOutput;
import org.hkijena.acaq5.api.compartments.algorithms.ACAQProjectCompartment;
import org.hkijena.acaq5.api.data.ACAQDataSlot;
import org.hkijena.acaq5.ui.ACAQProjectWorkbench;
import org.hkijena.acaq5.ui.grapheditor.ACAQAlgorithmGraphCopyPasteBehavior;
import org.hkijena.acaq5.utils.JsonUtils;

import java.awt.*;
import java.awt.datatransfer.Clipboard;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.StringSelection;
import java.awt.datatransfer.Transferable;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * Copy & Paste behavior for {@link ACAQCompartmentGraphUI}
 */
public class ACAQCompartmentGraphCopyPasteBehavior implements ACAQAlgorithmGraphCopyPasteBehavior {

    private ACAQCompartmentGraphUI editorUI;

    /**
     * @param editorUI an editor for compartment graphs
     */
    public ACAQCompartmentGraphCopyPasteBehavior(ACAQCompartmentGraphUI editorUI) {
        this.editorUI = editorUI;
    }

    @Override
    public void copy(Set<ACAQGraphNode> compartmentNodes) {
        // This copy action converts the selection of ACAQProjectCompartment into one large graph
        Set<String> compartments = new HashSet<>();
        for (ACAQGraphNode compartmentNode : compartmentNodes) {
            compartments.add(((ACAQProjectCompartment) compartmentNode).getProjectCompartmentId());
        }

        // Create isolated graph
        Set<ACAQGraphNode> algorithms = new HashSet<>();
        ACAQAlgorithmGraph sourceGraph = null;
        for (ACAQGraphNode algorithm : ((ACAQProjectWorkbench) editorUI.getWorkbench()).getProject().getGraph().getAlgorithmNodes().values()) {
            if (compartments.contains(algorithm.getCompartment())) {
                algorithms.add(algorithm);
                sourceGraph = algorithm.getGraph();
            }
        }

        if (algorithms.isEmpty())
            return;

        ACAQAlgorithmGraph graph = sourceGraph.extract(algorithms, false);
        try {
            String json = JsonUtils.getObjectMapper().writerWithDefaultPrettyPrinter().writeValueAsString(graph);
            StringSelection selection = new StringSelection(json);
            Clipboard clipboard = Toolkit.getDefaultToolkit().getSystemClipboard();
            clipboard.setContents(selection, selection);
        } catch (JsonProcessingException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void cut(Set<ACAQGraphNode> algorithms) {
        copy(algorithms);
        for (ACAQGraphNode algorithm : ImmutableSet.copyOf(algorithms)) {
            editorUI.getAlgorithmGraph().removeNode(algorithm);
        }
    }

    @Override
    public void paste() {
        try {
            String json = getStringFromClipboard();
            if (json != null) {
                ACAQProject project = ((ACAQProjectWorkbench) editorUI.getWorkbench()).getProject();
                ACAQAlgorithmGraph sourceGraph = JsonUtils.getObjectMapper().readValue(json, ACAQAlgorithmGraph.class);

                // Collect all compartments
                Set<String> compartments = new HashSet<>();
                for (ACAQGraphNode algorithm : sourceGraph.getAlgorithmNodes().values()) {
                    compartments.add(algorithm.getCompartment());
                }

                // Create compartments and assign their compartments
                Map<String, ACAQProjectCompartment> compartmentNodeMap = new HashMap<>();
                for (String compartment : compartments) {
                    ACAQProjectCompartment compartmentNode = project.addCompartment(compartment);
                    compartmentNodeMap.put(compartment, compartmentNode);
                }

                // Add nodes
                ACAQAlgorithmGraph targetGraph = project.getGraph();
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
                    if (edge.getValue().getAlgorithm() instanceof ACAQCompartmentOutput) {
                        String sourceCompartment = edge.getValue().getAlgorithm().getCompartment();
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

    private static String getStringFromClipboard() {
        String ret = "";
        Clipboard sysClip = Toolkit.getDefaultToolkit().getSystemClipboard();

        Transferable clipTf = sysClip.getContents(null);

        if (clipTf != null) {

            if (clipTf.isDataFlavorSupported(DataFlavor.stringFlavor)) {
                try {
                    ret = (String) clipTf
                            .getTransferData(DataFlavor.stringFlavor);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }

        return ret;
    }
}
