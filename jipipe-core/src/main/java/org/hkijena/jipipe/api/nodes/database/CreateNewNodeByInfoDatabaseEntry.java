package org.hkijena.jipipe.api.nodes.database;

import org.hkijena.jipipe.api.nodes.JIPipeNodeInfo;
import org.hkijena.jipipe.api.nodes.JIPipeNodeMenuLocation;

import java.util.ArrayList;
import java.util.List;

public class CreateNewNodeByInfoDatabaseEntry implements JIPipeNodeDatabaseEntry {

    private final String id;
    private final JIPipeNodeInfo nodeInfo;
    private final List<String> tokens = new ArrayList<>();

    public CreateNewNodeByInfoDatabaseEntry(String id, JIPipeNodeInfo nodeInfo) {
        this.id = id;
        this.nodeInfo = nodeInfo;
        initializeTokens();
    }
    private void initializeTokens() {
        tokens.add(nodeInfo.getName());
        for (JIPipeNodeMenuLocation alias : nodeInfo.getAliases()) {
            tokens.add(alias.getAlternativeName());
        }
        tokens.add(nodeInfo.getCategory().getName() + "\n" + nodeInfo.getMenuPath());
        for (JIPipeNodeMenuLocation alias : nodeInfo.getAliases()) {
            tokens.add(alias.getCategory().getName() + "\n" + alias.getMenuPath());
        }
        tokens.add(nodeInfo.getDescription().getBody());
    }

    @Override
    public JIPipeNodeDatabaseRole getRole() {
        return JIPipeNodeDatabaseRole.PipelineNode;
    }

    @Override
    public List<String> getTokens() {
        return tokens;
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
