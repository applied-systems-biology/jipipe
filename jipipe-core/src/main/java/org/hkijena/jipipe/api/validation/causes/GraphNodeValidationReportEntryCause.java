package org.hkijena.jipipe.api.validation.causes;

import org.hkijena.jipipe.api.nodes.JIPipeGraphNode;
import org.hkijena.jipipe.api.validation.JIPipeValidationReportEntryCause;
import org.hkijena.jipipe.ui.JIPipeWorkbench;
import org.hkijena.jipipe.utils.StringUtils;
import org.hkijena.jipipe.utils.UIUtils;

import javax.swing.*;

public class GraphNodeValidationReportEntryCause extends JIPipeValidationReportEntryCause {

    private final JIPipeGraphNode graphNode;

    public GraphNodeValidationReportEntryCause(JIPipeGraphNode graphNode) {
        this.graphNode = graphNode;
    }

    public GraphNodeValidationReportEntryCause(JIPipeValidationReportEntryCause parent, JIPipeGraphNode graphNode) {
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
