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

package org.hkijena.jipipe.api.algorithm;

import com.fasterxml.jackson.annotation.JsonGetter;
import com.fasterxml.jackson.annotation.JsonSetter;
import org.jgrapht.graph.DefaultEdge;

/**
 * A custom graph edge
 */
public class JIPipeGraphEdge extends DefaultEdge {

    private boolean userCanDisconnect;
    private boolean uiHidden = false;

    /**
     * Initializes a new graph edge that is not user-disconnectable
     */
    public JIPipeGraphEdge() {
    }

    /**
     * Initializes a new graph edge
     *
     * @param userCanDisconnect If a user is allowed to disconnect this edge
     */
    public JIPipeGraphEdge(boolean userCanDisconnect) {
        this.userCanDisconnect = userCanDisconnect;
    }

    /**
     * @return If users are allowed to disconnect this edge
     */
    public boolean isUserCanDisconnect() {
        return userCanDisconnect;
    }

    /**
     * Determines if the edge should be shown in UI
     *
     * @return if the edge should be shown in UI
     */
    @JsonGetter("ui-hidden")
    public boolean isUiHidden() {
        return uiHidden;
    }

    @JsonSetter("ui-hidden")
    public void setUiHidden(boolean uiHidden) {
        this.uiHidden = uiHidden;
    }

    public void setMetadataFrom(JIPipeGraphEdge other) {
        this.uiHidden = other.uiHidden;
    }
}
