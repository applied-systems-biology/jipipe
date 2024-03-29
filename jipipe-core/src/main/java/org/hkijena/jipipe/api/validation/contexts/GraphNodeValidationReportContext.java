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

package org.hkijena.jipipe.api.validation.contexts;

import org.hkijena.jipipe.api.nodes.JIPipeGraph;
import org.hkijena.jipipe.api.nodes.JIPipeGraphNode;
import org.hkijena.jipipe.api.validation.JIPipeValidationReportContext;
import org.hkijena.jipipe.api.validation.NavigableJIPipeValidationReportContext;
import org.hkijena.jipipe.ui.JIPipeProjectWorkbench;
import org.hkijena.jipipe.ui.JIPipeWorkbench;
import org.hkijena.jipipe.ui.components.tabs.DocumentTabPane;
import org.hkijena.jipipe.ui.grapheditor.algorithmpipeline.JIPipePipelineGraphEditorUI;
import org.hkijena.jipipe.utils.UIUtils;

import javax.swing.*;
import java.util.UUID;

public class GraphNodeValidationReportContext extends NavigableJIPipeValidationReportContext {

    private final JIPipeGraphNode graphNode;

    public GraphNodeValidationReportContext(JIPipeGraphNode graphNode) {
        this.graphNode = graphNode;
    }

    public GraphNodeValidationReportContext(JIPipeValidationReportContext parent, JIPipeGraphNode graphNode) {
        super(parent);
        this.graphNode = graphNode;
    }

    private JIPipeGraphNode findTargetNode(JIPipeWorkbench workbench) {
        if (workbench instanceof JIPipeProjectWorkbench) {
            JIPipeGraph parentGraph = graphNode.getParentGraph();
            if (parentGraph != null) {
                UUID uuid = graphNode.getUUIDInParentGraph();
                if (uuid != null) {
                    return ((JIPipeProjectWorkbench) workbench).getProject().getGraph().getNodeByUUID(uuid);
                }
            }
        }
        return null;
    }

    @Override
    public boolean canNavigate(JIPipeWorkbench workbench) {
        return findTargetNode(workbench) != null;
    }

    @Override
    public void navigate(JIPipeWorkbench workbench) {
        JIPipeGraphNode targetNode = findTargetNode(workbench);
        if (workbench instanceof JIPipeProjectWorkbench && targetNode != null) {
            SwingUtilities.invokeLater(() -> {
                DocumentTabPane.DocumentTab pipelineEditorTab = ((JIPipeProjectWorkbench) workbench).getOrOpenPipelineEditorTab(graphNode.getProjectCompartment(), true);
                SwingUtilities.invokeLater(() -> {
                    JIPipePipelineGraphEditorUI ui = (JIPipePipelineGraphEditorUI) pipelineEditorTab.getContent();
                    ui.selectOnly(ui.getCanvasUI().getNodeUIs().get(targetNode));
                });
            });
        }
    }

    @Override
    public String renderName() {
        return graphNode.getDisplayName();
    }

    @Override
    public Icon renderIcon() {
        return UIUtils.getIconFromResources("actions/graph-node.png");
    }
}
