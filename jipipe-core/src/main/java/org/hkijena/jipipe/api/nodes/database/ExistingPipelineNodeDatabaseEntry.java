package org.hkijena.jipipe.api.nodes.database;

import org.hkijena.jipipe.api.nodes.JIPipeGraphNode;

public class ExistingPipelineNodeDatabaseEntry implements JIPipeNodeDatabaseEntry{
    private final String id;
    private final JIPipeGraphNode graphNode;

    public ExistingPipelineNodeDatabaseEntry(String id, JIPipeGraphNode graphNode) {
        this.id = id;
        this.graphNode = graphNode;
    }

    @Override
    public String getId() {
        return id;
    }

    public JIPipeGraphNode getGraphNode() {
        return graphNode;
    }

    @Override
    public boolean exists() {
        return true;
    }
}
