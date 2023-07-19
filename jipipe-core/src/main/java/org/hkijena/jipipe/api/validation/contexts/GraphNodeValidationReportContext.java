package org.hkijena.jipipe.api.validation.contexts;

import org.hkijena.jipipe.api.nodes.JIPipeGraph;
import org.hkijena.jipipe.api.nodes.JIPipeGraphNode;
import org.hkijena.jipipe.api.validation.JIPipeValidationReportContext;
import org.hkijena.jipipe.ui.JIPipeProjectWorkbench;
import org.hkijena.jipipe.ui.JIPipeWorkbench;
import org.hkijena.jipipe.ui.components.tabs.DocumentTabPane;
import org.hkijena.jipipe.ui.grapheditor.algorithmpipeline.JIPipePipelineGraphEditorUI;
import org.hkijena.jipipe.utils.UIUtils;

import javax.swing.*;

public class GraphNodeValidationReportContext extends JIPipeValidationReportContext {

    private final JIPipeGraphNode graphNode;

    public GraphNodeValidationReportContext(JIPipeGraphNode graphNode) {
        this.graphNode = graphNode;
    }

    public GraphNodeValidationReportContext(JIPipeValidationReportContext parent, JIPipeGraphNode graphNode) {
        super(parent);
        this.graphNode = graphNode;
    }

    @Override
    public boolean canNavigate(JIPipeWorkbench workbench) {
        if(workbench instanceof JIPipeProjectWorkbench) {
            JIPipeGraph parentGraph = graphNode.getParentGraph();
            if (parentGraph != null) {
                if (parentGraph.getProject() == ((JIPipeProjectWorkbench) workbench).getProject()) {
                    return parentGraph.containsNode(graphNode);
                }
            }
        }
        return false;
    }

    @Override
    public void navigate(JIPipeWorkbench workbench) {
        if(canNavigate(workbench)) {
            JIPipeProjectWorkbench projectWorkbench = (JIPipeProjectWorkbench) workbench;
            SwingUtilities.invokeLater(() -> {
                DocumentTabPane.DocumentTab pipelineEditorTab = projectWorkbench.getOrOpenPipelineEditorTab(graphNode.getProjectCompartment(), true);
                SwingUtilities.invokeLater(() -> {
                    JIPipePipelineGraphEditorUI ui = (JIPipePipelineGraphEditorUI) pipelineEditorTab.getContent();
                    ui.selectOnly(ui.getCanvasUI().getNodeUIs().get(graphNode));
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
