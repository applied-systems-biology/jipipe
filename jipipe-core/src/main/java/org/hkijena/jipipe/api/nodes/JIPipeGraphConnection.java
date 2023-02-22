package org.hkijena.jipipe.api.nodes;

import org.hkijena.jipipe.api.data.JIPipeDataSlot;

/**
 * A pair-like object that contains a connection within a {@link JIPipeGraph}
 */
public class JIPipeGraphConnection {

    private final JIPipeGraph graph;
    private final JIPipeDataSlot source;
    private final JIPipeDataSlot target;
    private final JIPipeGraphEdge edge;

    public JIPipeGraphConnection(JIPipeGraph graph, JIPipeDataSlot source, JIPipeDataSlot target, JIPipeGraphEdge edge) {
        this.graph = graph;
        this.source = source;
        this.target = target;
        this.edge = edge;
    }

    public JIPipeGraph getGraph() {
        return graph;
    }

    public JIPipeDataSlot getSource() {
        return source;
    }

    public JIPipeDataSlot getTarget() {
        return target;
    }

    public JIPipeGraphEdge getEdge() {
        return edge;
    }

    public JIPipeGraphNode getSourceNode() {
        return source != null ? source.getNode() : null;
    }

    public JIPipeGraphNode getTargetNode() {
        return target != null ? target.getNode() : null;
    }
}
