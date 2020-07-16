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

package org.hkijena.jipipe.ui.grapheditor.contextmenu;

import org.hkijena.jipipe.api.JIPipeProject;
import org.hkijena.jipipe.api.JIPipeValidityReport;
import org.hkijena.jipipe.api.algorithm.JIPipeGraph;
import org.hkijena.jipipe.api.algorithm.JIPipeGraphNode;
import org.hkijena.jipipe.api.compartments.algorithms.JIPipeProjectCompartment;
import org.hkijena.jipipe.api.grouping.NodeGroup;
import org.hkijena.jipipe.ui.JIPipeProjectWorkbench;
import org.hkijena.jipipe.ui.extensionbuilder.JIPipeJsonExporter;
import org.hkijena.jipipe.ui.grapheditor.JIPipeGraphCanvasUI;
import org.hkijena.jipipe.ui.grapheditor.JIPipeNodeUI;
import org.hkijena.jipipe.utils.UIUtils;

import javax.swing.*;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

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
        final String compartmentId = compartment.getProjectCompartmentId();
        JIPipeValidityReport report = new JIPipeValidityReport();
        for (Map.Entry<String, JIPipeGraphNode> entry : project.getGraph().getNodes().entrySet()) {
            if (Objects.equals(entry.getValue().getCompartment(), compartmentId)) {
                report.forCategory(entry.getKey()).report(entry.getValue());
            }
        }
        if (!report.isValid()) {
            UIUtils.openValidityReportDialog(canvasUI, report, false);
            return;
        }

        JIPipeGraph extractedGraph = project.getGraph().extract(project.getGraph().getAlgorithmsWithCompartment(compartmentId), true);
        NodeGroup nodeGroup = new NodeGroup(extractedGraph, true);
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
        return UIUtils.getIconFromResources("export.png");
    }

    @Override
    public boolean isShowingInOverhang() {
        return true;
    }

    @Override
    public boolean disableOnNonMatch() {
        return false;
    }
}
