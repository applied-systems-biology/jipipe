package org.hkijena.jipipe.plugins.parameters.library.graph;

import org.hkijena.jipipe.plugins.parameters.api.collections.JIPipeListParameter;

public class GraphNodeReferenceParameterList extends JIPipeListParameter<GraphNodeReferenceParameter> {
    public GraphNodeReferenceParameterList() {
        super(GraphNodeReferenceParameter.class);
    }

    public GraphNodeReferenceParameterList(GraphNodeReferenceParameterList other) {
        super(GraphNodeReferenceParameter.class);
        for (GraphNodeReferenceParameter parameter : other) {
            add(new GraphNodeReferenceParameter(parameter));
        }
    }
}
