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
