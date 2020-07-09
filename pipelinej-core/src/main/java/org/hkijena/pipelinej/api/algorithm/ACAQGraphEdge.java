/*
 * Copyright by Zoltán Cseresnyés, Ruman Gerst
 *
 * Research Group Applied Systems Biology - Head: Prof. Dr. Marc Thilo Figge
 * https://www.leibniz-hki.de/en/applied-systems-biology.html
 * HKI-Center for Systems Biology of Infection
 * Leibniz Institute for Natural Product Research and Infection Biology - Hans Knöll Institute (HKI)
 * Adolf-Reichwein-Straße 23, 07745 Jena, Germany
 *
 * The project code is licensed under BSD 2-Clause.
 * See the LICENSE file provided with the code for the full license.
 */

package org.hkijena.pipelinej.api.algorithm;

import org.jgrapht.graph.DefaultEdge;

/**
 * A custom graph edge
 */
public class ACAQGraphEdge extends DefaultEdge {

    private boolean userDisconnectable;

    /**
     * Initializes a new graph edge that is not user-disconnectable
     */
    public ACAQGraphEdge() {

    }

    /**
     * Initializes a new graph edge
     *
     * @param userDisconnectable If a user is allowed to disconnect this edge
     */
    public ACAQGraphEdge(boolean userDisconnectable) {
        this.userDisconnectable = userDisconnectable;
    }

    /**
     * @return If users are allowed to disconnect this edge
     */
    public boolean isUserDisconnectable() {
        return userDisconnectable;
    }
}
