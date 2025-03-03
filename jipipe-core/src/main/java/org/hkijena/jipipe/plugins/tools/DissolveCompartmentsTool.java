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

package org.hkijena.jipipe.plugins.tools;

import com.google.common.collect.ImmutableList;
import org.hkijena.jipipe.JIPipe;
import org.hkijena.jipipe.api.compartments.algorithms.IOInterfaceAlgorithm;
import org.hkijena.jipipe.api.compartments.algorithms.JIPipeProjectCompartment;
import org.hkijena.jipipe.api.compartments.algorithms.JIPipeProjectCompartmentOutput;
import org.hkijena.jipipe.api.nodes.JIPipeGraph;
import org.hkijena.jipipe.api.nodes.JIPipeGraphNode;
import org.hkijena.jipipe.desktop.api.JIPipeDesktopMenuExtension;
import org.hkijena.jipipe.desktop.api.JIPipeMenuExtensionTarget;
import org.hkijena.jipipe.desktop.app.JIPipeDesktopProjectWorkbench;
import org.hkijena.jipipe.desktop.app.JIPipeDesktopWorkbench;
import org.hkijena.jipipe.utils.UIUtils;
import org.hkijena.jipipe.utils.ui.BusyCursor;

import javax.swing.*;
import java.util.UUID;

public class DissolveCompartmentsTool extends JIPipeDesktopMenuExtension {
    /**
     * Creates a new instance
     *
     * @param workbench workbench the extension is attached to
     */
    public DissolveCompartmentsTool(JIPipeDesktopWorkbench workbench) {
        super(workbench);
        setText("Dissolve compartments");
        setToolTipText("Merges all compartments into one large compartment. Compartment interfaces are converted into IO interface nodes.");
        setIcon(UIUtils.getIconFromResources("actions/rabbitvcs-merge.png"));
        addActionListener(e -> dissolveCompartments());
    }

    private void dissolveCompartments() {
        JIPipeDesktopProjectWorkbench workbench = (JIPipeDesktopProjectWorkbench) getDesktopWorkbench();
        if (JOptionPane.showConfirmDialog(workbench.getWindow(), "You will lose all manual node positions and all nodes will be put into one compartment. This operation cannot be undone. Do you want to continue?", "Dissolve compartments", JOptionPane.YES_NO_OPTION) == JOptionPane.NO_OPTION) {
            return;
        }
        workbench.getDocumentTabPane().closeAllTabs(true);
        try (BusyCursor cursor = new BusyCursor(workbench.getWindow())) {
            JIPipeGraph graph = new JIPipeGraph(workbench.getProject().getGraph());
            // Replace project compartment with IOInterface
            for (JIPipeGraphNode node : ImmutableList.copyOf(graph.getGraphNodes())) {
                if (node instanceof JIPipeProjectCompartmentOutput) {
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
                graph.setCompartment(node.getUUIDInParentGraph(), compartmentUUID);
            }
            workbench.getProject().getGraph().mergeWith(graph);
        }
        workbench.getDocumentTabPane().selectSingletonTab(JIPipeDesktopProjectWorkbench.TAB_COMPARTMENT_EDITOR);
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
