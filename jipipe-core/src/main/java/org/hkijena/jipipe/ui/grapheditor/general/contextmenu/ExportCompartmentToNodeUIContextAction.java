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

package org.hkijena.jipipe.ui.grapheditor.general.contextmenu;

import org.hkijena.jipipe.api.JIPipeIssueReport;
import org.hkijena.jipipe.api.JIPipeProject;
import org.hkijena.jipipe.api.compartments.algorithms.JIPipeProjectCompartment;
import org.hkijena.jipipe.api.grouping.NodeGroup;
import org.hkijena.jipipe.api.nodes.JIPipeGraph;
import org.hkijena.jipipe.api.nodes.JIPipeGraphNode;
import org.hkijena.jipipe.ui.JIPipeProjectWorkbench;
import org.hkijena.jipipe.ui.extensionbuilder.JIPipeJsonExporter;
import org.hkijena.jipipe.ui.grapheditor.general.JIPipeGraphCanvasUI;
import org.hkijena.jipipe.ui.grapheditor.general.nodeui.JIPipeNodeUI;
import org.hkijena.jipipe.utils.UIUtils;

import javax.swing.*;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;

public class ExportCompartmentToNodeUIContextAction implements NodeUIContextAction {
    @Override
    public boolean matches(Set<JIPipeNodeUI> selection) {
        return selection.size() == 1;
    }

    @Override
    public void run(JIPipeGraphCanvasUI canvasUI, Set<JIPipeNodeUI> selection) {
        JIPipeProjectCompartment compartment = (JIPipeProjectCompartment) selection.iterator().next().getNode();
        JIPipeProjectWorkbench projectWorkbench = (JIPipeProjectWorkbench) canvasUI.getWorkbench();
        JIPipeProject project = projectWorkbench.getProject();
        final UUID compartmentId = compartment.getProjectCompartmentUUID();
        JIPipeIssueReport report = new JIPipeIssueReport();
        for (JIPipeGraphNode node : project.getGraph().getGraphNodes()) {
            if (Objects.equals(node.getCompartmentUUIDInParentGraph(), compartmentId)) {
                report.resolve(node.getDisplayName()).report(node);
            }
        }
        if (!report.isValid()) {
            UIUtils.openValidityReportDialog(canvasUI, report, "Issues with nodes", "The following issues were found with the contained nodes. Retry after resolving the problems.", false);
            return;
        }

        JIPipeGraph extractedGraph = project.getGraph().extract(project.getGraph().getNodesWithinCompartment(compartmentId), true);
        NodeGroup nodeGroup = new NodeGroup(extractedGraph, true, false, true);
        JIPipeJsonExporter.createExporter(projectWorkbench, nodeGroup, compartment.getName(), compartment.getCustomDescription());
    }

    @Override
    public String getName() {
        return "Export as algorithm";
    }

    @Override
    public String getDescription() {
        return "Converts the selected compartment into a custom algorithm";
    }

    @Override
    public Icon getIcon() {
        return UIUtils.getIconFromResources("actions/document-export.png");
    }

}
