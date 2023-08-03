package org.hkijena.jipipe.api.nodes.database;

public interface JIPipeNodeDatabaseEntry {

    String getId();
    boolean exists();
}
