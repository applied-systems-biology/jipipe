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

package org.hkijena.pipelinej.api.parameters;

/**
 * Defines the visibility of a parameter
 */
public enum ACAQParameterVisibility {
    /**
     * Highest visibility: Visible to users, and to parent parameter holders
     */
    TransitiveVisible(1),
    /**
     * Visible to users if not a child of another parameter holder
     */
    Visible(2),
    /**
     * Hidden from users, but serialized via JSON
     */
    Hidden(4);

    int order;

    ACAQParameterVisibility(int order) {
        this.order = order;
    }

    /**
     * Gets the lower visibility of this one one the other one
     *
     * @param other The other visibility
     * @return the lower visibility
     */
    public ACAQParameterVisibility intersectWith(ACAQParameterVisibility other) {
        if (other.order > order)
            return other;
        else
            return this;
    }

    /**
     * Returns true if this visibility is visible in a container with a minimum visibility
     *
     * @param container The container visibility
     * @return If the visibility is visible in the container
     */
    public boolean isVisibleIn(ACAQParameterVisibility container) {
        return this.order <= container.order;
    }
}
