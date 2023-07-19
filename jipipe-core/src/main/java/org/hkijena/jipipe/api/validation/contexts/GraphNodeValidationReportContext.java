package org.hkijena.jipipe.api.validation.contexts;

import org.hkijena.jipipe.api.nodes.JIPipeGraphNode;
import org.hkijena.jipipe.api.validation.JIPipeValidationReportContext;
import org.hkijena.jipipe.ui.JIPipeWorkbench;
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
        return false;
    }

    @Override
    public void navigate(JIPipeWorkbench workbench) {

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
