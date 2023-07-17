package org.hkijena.jipipe.api.validation.causes;

import org.hkijena.jipipe.api.nodes.JIPipeGraph;
import org.hkijena.jipipe.api.validation.JIPipeValidationReportEntryCause;
import org.hkijena.jipipe.ui.JIPipeWorkbench;
import org.hkijena.jipipe.utils.UIUtils;

import javax.swing.*;

public class GraphValidationReportEntryCause extends JIPipeValidationReportEntryCause {

    private final JIPipeGraph graph;

    public GraphValidationReportEntryCause(JIPipeGraph graph) {
        this.graph = graph;
    }

    public GraphValidationReportEntryCause(JIPipeValidationReportEntryCause parent, JIPipeGraph graph) {
        super(parent);
        this.graph = graph;
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
        return "Graph";
    }

    @Override
    public Icon renderIcon() {
        return UIUtils.getIconFromResources("actions/distribute-graph-directed.png");
    }
}
