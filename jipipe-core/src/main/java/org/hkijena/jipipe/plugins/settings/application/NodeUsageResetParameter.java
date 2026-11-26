/*
 * Copyright by Zoltán Cseresnyés, Ruman Gerst
 *
 * Research Group Applied Systems Biology - Head: Prof. Dr. Marc Thilo Figge
 * https://www.leibniz-hki.de/en/applied-systems-biology.html
 * HKI-Center for Systems Biology of Infection
 * Leibniz Institute for Natural Product Research and Infection Biology - Hans Knöll Institute (HKI)
 * Adolf-Reichwein-Straße 23, 07745 Jena, Germany
 *
 * The project code is licensed under MIT.
 * See the LICENSE file provided with the code for the full license.
 */

package org.hkijena.jipipe.plugins.settings.application;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Marker parameter type of the "Reset usage data" button on the "Node
 * search" settings page. The value itself carries no configuration: the
 * button is the custom editor
 * ({@link NodeUsageResetDesktopParameterEditorUI}), and pressing it calls
 * {@link org.hkijena.jipipe.api.nodes.database.NodeUsage#resetUserCounts()}
 * directly.
 * <p>
 * The settings persistence serializes every parameter of a sheet with the
 * global JSON mapper (member auto-detection off), so the marker exposes
 * one inert property: an empty bean has no serializer, and its failure
 * would take the whole sheet's settings write with it.
 */
public class NodeUsageResetParameter {

    private boolean marker;

    public NodeUsageResetParameter() {
    }

    @JsonProperty("marker")
    public boolean isMarker() {
        return marker;
    }

    @JsonProperty("marker")
    public void setMarker(boolean marker) {
        this.marker = marker;
    }
}