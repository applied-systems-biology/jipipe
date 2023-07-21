package org.hkijena.jipipe.api.validation.contexts;

import org.hkijena.jipipe.api.nodes.JIPipeGraph;
import org.hkijena.jipipe.api.validation.JIPipeValidationReportContext;
import org.hkijena.jipipe.utils.UIUtils;

import javax.swing.*;

public class GraphValidationReportContext extends JIPipeValidationReportContext {

    private final JIPipeGraph graph;

    public GraphValidationReportContext(JIPipeGraph graph) {
        this.graph = graph;
    }

    public GraphValidationReportContext(JIPipeValidationReportContext parent, JIPipeGraph graph) {
        super(parent);
        this.graph = graph;
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
