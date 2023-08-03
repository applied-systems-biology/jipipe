package org.hkijena.jipipe.api.nodes.database;

import org.hkijena.jipipe.api.nodes.JIPipeNodeInfo;

public class CreateNewNodeByInfoDatabaseEntry implements JIPipeNodeDatabaseEntry {

    private final String id;
    private final JIPipeNodeInfo nodeInfo;

    public CreateNewNodeByInfoDatabaseEntry(String id, JIPipeNodeInfo nodeInfo) {
        this.id = id;
        this.nodeInfo = nodeInfo;
    }

    @Override
    public boolean exists() {
        return false;
    }

    public JIPipeNodeInfo getNodeInfo() {
        return nodeInfo;
    }

    @Override
    public String getId() {
        return id;
    }
}
