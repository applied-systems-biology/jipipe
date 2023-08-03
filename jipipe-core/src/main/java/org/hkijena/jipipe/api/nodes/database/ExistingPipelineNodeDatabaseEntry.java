package org.hkijena.jipipe.api.nodes.database;

import org.hkijena.jipipe.api.nodes.JIPipeGraphNode;
import org.hkijena.jipipe.api.nodes.JIPipeNodeInfo;
import org.hkijena.jipipe.api.nodes.JIPipeNodeMenuLocation;

import java.util.ArrayList;
import java.util.List;

public class ExistingPipelineNodeDatabaseEntry implements JIPipeNodeDatabaseEntry{
    private final String id;
    private final JIPipeGraphNode graphNode;
    private final List<String> tokens = new ArrayList<>();

    public ExistingPipelineNodeDatabaseEntry(String id, JIPipeGraphNode graphNode) {
        this.id = id;
        this.graphNode = graphNode;
        initializeTokens();
    }

    private void initializeTokens() {
        JIPipeNodeInfo nodeInfo = graphNode.getInfo();
        tokens.add(graphNode.getName());
        tokens.add(graphNode.getCustomDescription().getBody());
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
