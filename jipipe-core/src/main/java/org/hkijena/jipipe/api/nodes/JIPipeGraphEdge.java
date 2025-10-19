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
import org.hkijena.jipipe.api.compat.JIPipeProjectUpgradable;
import org.hkijena.jipipe.api.validation.JIPipeValidationReport;
import org.hkijena.jipipe.api.validation.JIPipeValidationReportContext;
import org.hkijena.jipipe.utils.StringUtils;
import org.jgrapht.graph.DefaultEdge;

import java.util.UUID;

/**
 * A custom graph edge
 */
public class JIPipeGraphEdge extends DefaultEdge implements JIPipeProjectUpgradable {

    private boolean userCanDisconnect;

    private String uuid;
    private Shape uiShape = Shape.Elbow;

    /**
     * Initializes a new graph edge that cannot be disconnected by users
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

    @JsonGetter("ui-shape")
    public Shape getUiShape() {
        return uiShape;
    }

    @JsonSetter("ui-shape")
    public void setUiShape(Shape uiShape) {
        this.uiShape = uiShape;
    }

    public void setMetadataFrom(JIPipeGraphEdge other) {
        this.uiShape = other.uiShape;
    }

    @JsonGetter("uuid")
    public String getUuid() {
        if (StringUtils.isNullOrEmpty(uuid)) {
            uuid = UUID.randomUUID().toString();
        }
        return uuid;
    }

    @JsonSetter("uuid")
    public void setUuid(String uuid) {
        this.uuid = uuid;
    }

    @Override
    public void applyProjectUpgrade(String fromVersion, JIPipeValidationReportContext context, JIPipeValidationReport report) {
        // Nothing to do
    }

    /**
     * Available line shapes
     */
    public enum Shape {
        Elbow,
        Line
    }
}
