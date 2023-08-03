package org.hkijena.jipipe.api.nodes.database;

import org.hkijena.jipipe.api.nodes.JIPipeNodeExample;

public class CreateNewNodeByExampleDatabaseEntry implements JIPipeNodeDatabaseEntry {
    private final String id;
    private final JIPipeNodeExample example;

    public CreateNewNodeByExampleDatabaseEntry(String id, JIPipeNodeExample example) {
        this.id = id;
        this.example = example;
    }

    @Override
    public String getId() {
        return id;
    }

    @Override
    public boolean exists() {
        return false;
    }

    public JIPipeNodeExample getExample() {
        return example;
    }
}
