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

package org.hkijena.jipipe.ui.grapheditor.general.contextmenu;

import org.hkijena.jipipe.api.JIPipeProject;
import org.hkijena.jipipe.api.grouping.NodeGroup;
import org.hkijena.jipipe.api.nodes.JIPipeGraph;
import org.hkijena.jipipe.api.nodes.JIPipeGraphNode;
import org.hkijena.jipipe.api.validation.JIPipeValidationReport;
import org.hkijena.jipipe.api.validation.contexts.UnspecifiedValidationReportContext;
import org.hkijena.jipipe.ui.JIPipeProjectWorkbench;
import org.hkijena.jipipe.ui.components.tabs.DocumentTabPane;
import org.hkijena.jipipe.ui.extensionbuilder.JIPipeJsonExporter;
import org.hkijena.jipipe.ui.grapheditor.general.JIPipeGraphCanvasUI;
import org.hkijena.jipipe.ui.grapheditor.general.nodeui.JIPipeGraphNodeUI;
import org.hkijena.jipipe.utils.UIUtils;

import javax.swing.*;
import java.util.Set;
import java.util.stream.Collectors;

public class ExportNodeUIContextAction implements NodeUIContextAction {
    @Override
    public boolean matches(Set<JIPipeGraphNodeUI> selection) {
        return !selection.isEmpty();
    }

    @Override
    public void run(JIPipeGraphCanvasUI canvasUI, Set<JIPipeGraphNodeUI> selection) {
        Set<JIPipeGraphNode> algorithms = selection.stream().map(JIPipeGraphNodeUI::getNode).collect(Collectors.toSet());
        JIPipeValidationReport report = new JIPipeValidationReport();
        for (JIPipeGraphNode algorithm : algorithms) {
            algorithm.reportValidity(new UnspecifiedValidationReportContext(), report);
        }
        if (!report.isEmpty()) {
            UIUtils.openValidityReportDialog(canvasUI.getWorkbench(),
                    canvasUI,
                    report,
                    "Issues detected",
                    "There are issues with the configuration of the selected node. Try again after resolving them.",
                    false);
            return;
        }

        JIPipeProjectWorkbench projectWorkbench = (JIPipeProjectWorkbench) canvasUI.getWorkbench();
        JIPipeProject project = projectWorkbench.getProject();
        NodeGroup group;
        if (algorithms.size() > 1 || !(algorithms.iterator().next() instanceof NodeGroup)) {
            JIPipeGraph graph = project.getGraph().extract(algorithms, true);
            group = new NodeGroup(graph, true, false, true);
        } else {
            group = new NodeGroup((NodeGroup) algorithms.iterator().next());
        }
        JIPipeJsonExporter exporter = new JIPipeJsonExporter(projectWorkbench, group);
        projectWorkbench.getDocumentTabPane().addTab("Export custom algorithm",
                UIUtils.getIconFromResources("actions/document-export.png"),
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
        return UIUtils.getIconFromResources("actions/document-export.png");
    }

}
