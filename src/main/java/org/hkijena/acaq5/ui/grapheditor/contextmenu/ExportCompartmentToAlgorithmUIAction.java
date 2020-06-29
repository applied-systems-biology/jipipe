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

package org.hkijena.acaq5.ui.grapheditor.contextmenu;

import org.hkijena.acaq5.api.ACAQProject;
import org.hkijena.acaq5.api.ACAQValidityReport;
import org.hkijena.acaq5.api.algorithm.ACAQAlgorithmGraph;
import org.hkijena.acaq5.api.algorithm.ACAQGraphNode;
import org.hkijena.acaq5.api.compartments.algorithms.ACAQProjectCompartment;
import org.hkijena.acaq5.api.grouping.NodeGroup;
import org.hkijena.acaq5.ui.ACAQProjectWorkbench;
import org.hkijena.acaq5.ui.extensionbuilder.ACAQJsonAlgorithmExporter;
import org.hkijena.acaq5.ui.grapheditor.ACAQAlgorithmGraphCanvasUI;
import org.hkijena.acaq5.ui.grapheditor.ACAQAlgorithmUI;
import org.hkijena.acaq5.utils.UIUtils;

import javax.swing.*;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

public class ExportCompartmentToAlgorithmUIAction implements AlgorithmUIAction {
    @Override
    public boolean matches(Set<ACAQAlgorithmUI> selection) {
        return selection.size() == 1;
    }

    @Override
    public void run(ACAQAlgorithmGraphCanvasUI canvasUI, Set<ACAQAlgorithmUI> selection) {
        ACAQProjectCompartment compartment = (ACAQProjectCompartment) selection.iterator().next().getAlgorithm();
        ACAQProjectWorkbench projectWorkbench = (ACAQProjectWorkbench) canvasUI.getWorkbench();
        ACAQProject project = projectWorkbench.getProject();
        final String compartmentId = compartment.getProjectCompartmentId();
        ACAQValidityReport report = new ACAQValidityReport();
        for (Map.Entry<String, ACAQGraphNode> entry : project.getGraph().getAlgorithmNodes().entrySet()) {
            if (Objects.equals(entry.getValue().getCompartment(), compartmentId)) {
                report.forCategory(entry.getKey()).report(entry.getValue());
            }
        }
        if (!report.isValid()) {
            UIUtils.openValidityReportDialog(canvasUI, report, false);
            return;
        }

        ACAQAlgorithmGraph extractedGraph = project.getGraph().extract(project.getGraph().getAlgorithmsWithCompartment(compartmentId), true);
        NodeGroup nodeGroup = new NodeGroup(extractedGraph, true);
        ACAQJsonAlgorithmExporter.createExporter(projectWorkbench, nodeGroup, compartment.getName(), compartment.getCustomDescription());
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
