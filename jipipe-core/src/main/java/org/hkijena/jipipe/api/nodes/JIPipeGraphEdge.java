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

package org.hkijena.jipipe.api.nodes;

import com.fasterxml.jackson.annotation.JsonGetter;
import com.fasterxml.jackson.annotation.JsonSetter;
import org.hkijena.jipipe.JIPipe;
import org.hkijena.jipipe.plugins.settings.JIPipeGraphEditorUIApplicationSettings;
import org.jgrapht.graph.DefaultEdge;

/**
 * A custom graph edge
 */
public class JIPipeGraphEdge extends DefaultEdge {

    private boolean userCanDisconnect;

    private Visibility uiVisibility = Visibility.Smart;
    private Shape uiShape = Shape.Elbow;

    /**
     * Initializes a new graph edge that cannot be disconnected by users
     */
    public JIPipeGraphEdge() {
        if (JIPipe.isInstantiated()) {
            JIPipeGraphEditorUIApplicationSettings settings = JIPipe.getSettings().getById(JIPipeGraphEditorUIApplicationSettings.ID, JIPipeGraphEditorUIApplicationSettings.class);
            if (settings != null) {
                uiVisibility = settings.getDefaultEdgeVisibility();
            }
        }
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

    @JsonGetter("ui-shape")
    public Shape getUiShape() {
        return uiShape;
    }

    @JsonSetter("ui-shape")
    public void setUiShape(Shape uiShape) {
        this.uiShape = uiShape;
    }

    @JsonGetter("ui-visibility")
    public Visibility getUiVisibility() {
        return uiVisibility;
    }

    @JsonSetter("ui-visibility")
    public void setUiVisibility(Visibility uiVisibility) {
        this.uiVisibility = uiVisibility;
    }

    public void setMetadataFrom(JIPipeGraphEdge other) {
        this.uiShape = other.uiShape;
        this.uiVisibility = other.uiVisibility;
    }

    /**
     * Available line shapes
     */
    public enum Shape {
        Elbow,
        Line
    }

    public enum Visibility {
        /**
         * The edge is always visible
         */
        AlwaysVisible,
        /**
         * The edge will auto-hide if it is too far away
         * No label is always displayed next to input
         */
        Smart,
        /**
         * The edge will auto-hide if it is too far away
         * No label is shown
         *
         * @deprecated not supported and will behave the same as Smart (no label shown)
         */
        @Deprecated
        SmartSilent,
        /**
         * The edge is always hidden.
         * No label is shown
         */
        AlwaysHidden,
        /**
         * The edge is always hidden
         * A label is shown
         * (currently does the same as "AlwaysHidden")
         *
         * @deprecated not supported and will behave as AlwaysHidden
         */
        @Deprecated
        AlwaysHiddenWithLabel
    }
}
