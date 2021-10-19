package org.hkijena.jipipe.extensions.tools;

import com.google.common.collect.ImmutableList;
import org.hkijena.jipipe.JIPipe;
import org.hkijena.jipipe.api.compartments.algorithms.IOInterfaceAlgorithm;
import org.hkijena.jipipe.api.compartments.algorithms.JIPipeCompartmentOutput;
import org.hkijena.jipipe.api.compartments.algorithms.JIPipeProjectCompartment;
import org.hkijena.jipipe.api.nodes.JIPipeGraph;
import org.hkijena.jipipe.api.nodes.JIPipeGraphNode;
import org.hkijena.jipipe.ui.JIPipeProjectWorkbench;
import org.hkijena.jipipe.ui.JIPipeWorkbench;
import org.hkijena.jipipe.ui.extension.JIPipeMenuExtension;
import org.hkijena.jipipe.ui.extension.JIPipeMenuExtensionTarget;
import org.hkijena.jipipe.utils.UIUtils;
import org.hkijena.jipipe.utils.ui.BusyCursor;

import javax.swing.*;
import java.util.UUID;

public class DissolveCompartmentsTool extends JIPipeMenuExtension {
    /**
     * Creates a new instance
     *
     * @param workbench workbench the extension is attached to
     */
    public DissolveCompartmentsTool(JIPipeWorkbench workbench) {
        super(workbench);
        setText("Dissolve compartments");
        setToolTipText("Merges all compartments into one large compartment. Compartment interfaces are converted into IO interface nodes.");
        setIcon(UIUtils.getIconFromResources("actions/rabbitvcs-merge.png"));
        addActionListener(e -> dissolveCompartments());
    }

    private void dissolveCompartments() {
        JIPipeProjectWorkbench workbench = (JIPipeProjectWorkbench) getWorkbench();
        if (JOptionPane.showConfirmDialog(workbench.getWindow(), "You will lose all manual node positions and all nodes will be put into one compartment. This operation cannot be undone. Do you want to continue?", "Dissolve compartments", JOptionPane.YES_NO_OPTION) == JOptionPane.NO_OPTION) {
            return;
        }
        workbench.getDocumentTabPane().closeAllTabs();
        try (BusyCursor cursor = new BusyCursor(workbench.getWindow())) {
            JIPipeGraph graph = new JIPipeGraph(workbench.getProject().getGraph());
            // Replace project compartment with IOInterface
            for (JIPipeGraphNode node : ImmutableList.copyOf(graph.getGraphNodes())) {
                if (node instanceof JIPipeCompartmentOutput) {
                    IOInterfaceAlgorithm replacement = new IOInterfaceAlgorithm((IOInterfaceAlgorithm) node);
                    replacement.setInfo(JIPipe.getNodes().getInfoById("io-interface"));
                    graph.replaceNode(node, replacement);
                }
            }
            // Delete everything
            for (JIPipeGraphNode node : ImmutableList.copyOf(workbench.getProject().getCompartmentGraph().getGraphNodes())) {
                workbench.getProject().removeCompartment((JIPipeProjectCompartment) node);
            }
            // Add new compartment and add the graph
            JIPipeProjectCompartment compartment = workbench.getProject().addCompartment("Merged");
            UUID compartmentUUID = compartment.getProjectCompartmentUUID();
            for (JIPipeGraphNode node : graph.getGraphNodes()) {
                graph.setCompartment(node.getUUIDInGraph(), compartmentUUID);
            }
            workbench.getProject().getGraph().mergeWith(graph);
        }
        workbench.getDocumentTabPane().selectSingletonTab(JIPipeProjectWorkbench.TAB_COMPARTMENT_EDITOR);
    }

    @Override
    public JIPipeMenuExtensionTarget getMenuTarget() {
        return JIPipeMenuExtensionTarget.ProjectToolsMenu;
    }

    @Override
    public String getMenuPath() {
        return "Project";
    }
}
