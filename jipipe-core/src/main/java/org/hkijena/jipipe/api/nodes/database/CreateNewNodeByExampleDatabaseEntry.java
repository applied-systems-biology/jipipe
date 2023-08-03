package org.hkijena.jipipe.api.nodes.database;

import org.hkijena.jipipe.api.nodes.JIPipeNodeExample;
import org.hkijena.jipipe.api.nodes.JIPipeNodeMenuLocation;

import java.util.ArrayList;
import java.util.List;

public class CreateNewNodeByExampleDatabaseEntry implements JIPipeNodeDatabaseEntry {
    private final String id;
    private final JIPipeNodeExample example;
    private final List<String> tokens = new ArrayList<>();

    public CreateNewNodeByExampleDatabaseEntry(String id, JIPipeNodeExample example) {
        this.id = id;
        this.example = example;
        initializeTokens();
    }

    private void initializeTokens() {
        tokens.add(example.getNodeTemplate().getName());
        tokens.add(example.getNodeInfo().getName());
        for (JIPipeNodeMenuLocation alias : example.getNodeInfo().getAliases()) {
            tokens.add(alias.getAlternativeName());
        }
        tokens.add(example.getNodeInfo().getCategory().getName() + "\n" + example.getNodeInfo().getMenuPath());
        for (JIPipeNodeMenuLocation alias : example.getNodeInfo().getAliases()) {
            tokens.add(alias.getCategory().getName() + "\n" + alias.getMenuPath());
        }
        tokens.add(example.getNodeTemplate().getDescription().getBody());
        tokens.add(example.getNodeInfo().getDescription().getBody());
    }

    @Override
    public String getId() {
        return id;
    }

    @Override
    public boolean exists() {
        return false;
    }

    @Override
    public JIPipeNodeDatabaseRole getRole() {
        return JIPipeNodeDatabaseRole.PipelineNode;
    }

    @Override
    public List<String> getTokens() {
        return tokens;
    }

    public JIPipeNodeExample getExample() {
        return example;
    }
}
