package org.hkijena.acaq5.api;

import org.jgrapht.graph.DefaultEdge;

public class ACAQAlgorithmGraphEdge extends DefaultEdge {

    private boolean userDisconnectable;

    public ACAQAlgorithmGraphEdge() {

    }

    public ACAQAlgorithmGraphEdge(boolean userDisconnectable) {
        this.userDisconnectable = userDisconnectable;
    }

    public boolean isUserDisconnectable() {
        return userDisconnectable;
    }
}
