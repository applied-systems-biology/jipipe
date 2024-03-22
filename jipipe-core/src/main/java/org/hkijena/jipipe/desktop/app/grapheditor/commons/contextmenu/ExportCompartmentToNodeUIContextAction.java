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

import org.hkijena.jipipe.api.compartments.algorithms.JIPipeProjectCompartment;
import org.hkijena.jipipe.api.grouping.JIPipeNodeGroup;
import org.hkijena.jipipe.api.nodes.JIPipeGraph;
import org.hkijena.jipipe.api.nodes.JIPipeGraphNode;
import org.hkijena.jipipe.api.project.JIPipeProject;
import org.hkijena.jipipe.api.validation.JIPipeValidationReport;
import org.hkijena.jipipe.api.validation.contexts.GraphNodeValidationReportContext;
import org.hkijena.jipipe.desktop.app.JIPipeDesktopProjectWorkbench;
import org.hkijena.jipipe.desktop.app.grapheditor.commons.JIPipeDesktopGraphCanvasUI;
import org.hkijena.jipipe.desktop.app.grapheditor.commons.nodeui.JIPipeDesktopGraphNodeUI;
import org.hkijena.jipipe.desktop.jsonextensionbuilder.extensionbuilder.JIPipeDesktopJsonExporter;
import org.hkijena.jipipe.utils.UIUtils;

import javax.swing.*;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;

public class ExportCompartmentToNodeUIContextAction implements NodeUIContextAction {
    @Override
    public boolean matches(Set<JIPipeDesktopGraphNodeUI> selection) {
        return selection.size() == 1;
    }

    @Override
    public void run(JIPipeDesktopGraphCanvasUI canvasUI, Set<JIPipeDesktopGraphNodeUI> selection) {
        JIPipeProjectCompartment compartment = (JIPipeProjectCompartment) selection.iterator().next().getNode();
        JIPipeDesktopProjectWorkbench projectWorkbench = (JIPipeDesktopProjectWorkbench) canvasUI.getDesktopWorkbench();
        JIPipeProject project = projectWorkbench.getProject();
        final UUID compartmentId = compartment.getProjectCompartmentUUID();
        JIPipeValidationReport report = new JIPipeValidationReport();
        for (JIPipeGraphNode node : project.getGraph().getGraphNodes()) {
            if (Objects.equals(node.getCompartmentUUIDInParentGraph(), compartmentId)) {
                report.report(new GraphNodeValidationReportContext(node), node);
            }
        }
        if (!report.isEmpty()) {
            UIUtils.openValidityReportDialog(canvasUI.getDesktopWorkbench(),
                    canvasUI,
                    report,
                    "Issues with nodes",
                    "The following issues were found with the contained nodes. Retry after resolving the problems.",
                    false);
            return;
        }

        JIPipeGraph extractedGraph = project.getGraph().extract(project.getGraph().getNodesWithinCompartment(compartmentId), true);
        JIPipeNodeGroup nodeGroup = new JIPipeNodeGroup(extractedGraph, true, false, true);
        JIPipeDesktopJsonExporter.createExporter(projectWorkbench, nodeGroup, compartment.getName(), compartment.getCustomDescription());
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
