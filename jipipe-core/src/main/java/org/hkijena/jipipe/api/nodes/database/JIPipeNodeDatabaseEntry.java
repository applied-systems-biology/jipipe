package org.hkijena.jipipe.api.nodes.database;

import java.util.List;

public interface JIPipeNodeDatabaseEntry {

    String getId();
    List<String> getTokens();
    boolean exists();
    JIPipeNodeDatabaseRole getRole();
}
