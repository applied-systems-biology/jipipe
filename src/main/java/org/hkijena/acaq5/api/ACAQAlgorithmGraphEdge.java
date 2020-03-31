package org.hkijena.acaq5.api;

import org.jgrapht.graph.DefaultEdge;

/**
 * A custom graph edge
 */
public class ACAQAlgorithmGraphEdge extends DefaultEdge {

    private boolean userDisconnectable;

    /**
     * Initializes a new graph edge that is not user-disconnectable
     */
    public ACAQAlgorithmGraphEdge() {

    }

    /**
     * Initializes a new graph edge
     * @param userDisconnectable If a user is allowed to disconnect this edge
     */
    public ACAQAlgorithmGraphEdge(boolean userDisconnectable) {
        this.userDisconnectable = userDisconnectable;
    }

    /**
     * @return If users are allowed to disconnect this edge
     */
    public boolean isUserDisconnectable() {
        return userDisconnectable;
    }
}
