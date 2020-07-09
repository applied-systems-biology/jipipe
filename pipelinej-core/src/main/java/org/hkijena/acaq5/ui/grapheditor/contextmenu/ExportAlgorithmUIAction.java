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
import org.hkijena.acaq5.api.algorithm.ACAQGraph;
import org.hkijena.acaq5.api.algorithm.ACAQGraphNode;
import org.hkijena.acaq5.api.grouping.NodeGroup;
import org.hkijena.acaq5.ui.ACAQProjectWorkbench;
import org.hkijena.acaq5.ui.components.DocumentTabPane;
import org.hkijena.acaq5.ui.extensionbuilder.ACAQJsonExporter;
import org.hkijena.acaq5.ui.grapheditor.ACAQGraphCanvasUI;
import org.hkijena.acaq5.ui.grapheditor.ACAQNodeUI;
import org.hkijena.acaq5.utils.UIUtils;

import javax.swing.*;
import java.util.Set;
import java.util.stream.Collectors;

public class ExportAlgorithmUIAction implements AlgorithmUIAction {
    @Override
    public boolean matches(Set<ACAQNodeUI> selection) {
        return !selection.isEmpty();
    }

    @Override
    public void run(ACAQGraphCanvasUI canvasUI, Set<ACAQNodeUI> selection) {
        Set<ACAQGraphNode> algorithms = selection.stream().map(ACAQNodeUI::getNode).collect(Collectors.toSet());
        ACAQValidityReport report = new ACAQValidityReport();
        for (ACAQGraphNode algorithm : algorithms) {
            algorithm.reportValidity(report.forCategory(algorithm.getName()));
        }
        if (!report.isValid()) {
            UIUtils.openValidityReportDialog(canvasUI, report, false);
            return;
        }

        ACAQProjectWorkbench projectWorkbench = (ACAQProjectWorkbench) canvasUI.getWorkbench();
        ACAQProject project = projectWorkbench.getProject();
        ACAQGraph graph = project.getGraph().extract(algorithms, true);
        NodeGroup group = new NodeGroup(graph, true);
        ACAQJsonExporter exporter = new ACAQJsonExporter(projectWorkbench, group);
        projectWorkbench.getDocumentTabPane().addTab("Export custom algorithm",
                UIUtils.getIconFromResources("export.png"),
                exporter,
                DocumentTabPane.CloseMode.withAskOnCloseButton);
        projectWorkbench.getDocumentTabPane().switchToLastTab();
    }

    @Override
    public String getName() {
        return "Export";
    }

    @Override
    public String getDescription() {
        return "Exports the algorithms as custom algorithm";
    }

    @Override
    public Icon getIcon() {
        return UIUtils.getIconFromResources("export.png");
    }

    @Override
    public boolean isShowingInOverhang() {
        return false;
    }

    @Override
    public boolean disableOnNonMatch() {
        return false;
    }
}
