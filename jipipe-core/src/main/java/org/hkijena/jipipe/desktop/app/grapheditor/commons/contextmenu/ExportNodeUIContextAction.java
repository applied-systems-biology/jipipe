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

import org.hkijena.jipipe.api.grouping.JIPipeNodeGroup;
import org.hkijena.jipipe.api.nodes.JIPipeGraph;
import org.hkijena.jipipe.api.nodes.JIPipeGraphNode;
import org.hkijena.jipipe.api.project.JIPipeProject;
import org.hkijena.jipipe.api.validation.JIPipeValidationReport;
import org.hkijena.jipipe.api.validation.contexts.UnspecifiedValidationReportContext;
import org.hkijena.jipipe.desktop.app.JIPipeDesktopProjectWorkbench;
import org.hkijena.jipipe.desktop.app.grapheditor.commons.JIPipeDesktopGraphCanvasUI;
import org.hkijena.jipipe.desktop.app.grapheditor.commons.nodeui.JIPipeDesktopGraphNodeUI;
import org.hkijena.jipipe.desktop.commons.components.tabs.JIPipeDesktopTabPane;
import org.hkijena.jipipe.desktop.jsonextensionbuilder.extensionbuilder.JIPipeDesktopJsonExporter;
import org.hkijena.jipipe.utils.UIUtils;

import javax.swing.*;
import java.util.Set;
import java.util.stream.Collectors;

public class ExportNodeUIContextAction implements NodeUIContextAction {
    @Override
    public boolean matches(Set<JIPipeDesktopGraphNodeUI> selection) {
        return !selection.isEmpty();
    }

    @Override
    public void run(JIPipeDesktopGraphCanvasUI canvasUI, Set<JIPipeDesktopGraphNodeUI> selection) {
        Set<JIPipeGraphNode> algorithms = selection.stream().map(JIPipeDesktopGraphNodeUI::getNode).collect(Collectors.toSet());
        JIPipeValidationReport report = new JIPipeValidationReport();
        for (JIPipeGraphNode algorithm : algorithms) {
            algorithm.reportValidity(new UnspecifiedValidationReportContext(), report);
        }
        if (!report.isEmpty()) {
            UIUtils.openValidityReportDialog(canvasUI.getDesktopWorkbench(),
                    canvasUI,
                    report,
                    "Issues detected",
                    "There are issues with the configuration of the selected node. Try again after resolving them.",
                    false);
            return;
        }

        JIPipeDesktopProjectWorkbench projectWorkbench = (JIPipeDesktopProjectWorkbench) canvasUI.getDesktopWorkbench();
        JIPipeProject project = projectWorkbench.getProject();
        JIPipeNodeGroup group;
        if (algorithms.size() > 1 || !(algorithms.iterator().next() instanceof JIPipeNodeGroup)) {
            JIPipeGraph graph = project.getGraph().extract(algorithms, true);
            group = new JIPipeNodeGroup(graph, true, false, true);
        } else {
            group = new JIPipeNodeGroup((JIPipeNodeGroup) algorithms.iterator().next());
        }
        JIPipeDesktopJsonExporter exporter = new JIPipeDesktopJsonExporter(projectWorkbench, group);
        projectWorkbench.getDocumentTabPane().addTab("Export custom algorithm",
                UIUtils.getIconFromResources("actions/document-export.png"),
                exporter,
                JIPipeDesktopTabPane.CloseMode.withAskOnCloseButton);
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
