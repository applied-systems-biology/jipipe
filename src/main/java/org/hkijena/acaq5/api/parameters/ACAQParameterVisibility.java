package org.hkijena.acaq5.api.parameters;

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
