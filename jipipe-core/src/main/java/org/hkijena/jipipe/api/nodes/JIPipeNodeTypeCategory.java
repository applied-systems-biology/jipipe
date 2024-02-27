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

import javax.swing.*;
import java.awt.*;

/**
 * Describes a node category
 */
public interface JIPipeNodeTypeCategory {
    /**
     * Unique identifier of this category
     *
     * @return the id
     */
    String getId();

    /**
     * The name displayed in menus and other UI
     *
     * @return the name
     */
    String getName();

    /**
     * A description
     *
     * @return the description
     */
    String getDescription();

    /**
     * Categories are sorted by this value (lower value = first)
     *
     * @return UI order
     */
    int getUIOrder();

    /**
     * Icon shown in the UI
     *
     * @return the icon
     */
    Icon getIcon();

    /**
     * The fill color for algorithm nodes
     *
     * @return fill color
     */
    Color getFillColor();

    /**
     * The border color for algorithm nodes
     *
     * @return border color
     */
    Color getBorderColor();

    /**
     * The fill color for algorithm nodes (dark theme)
     *
     * @return fill color
     */
    Color getDarkFillColor();

    /**
     * The border color for algorithm nodes (dark theme)
     *
     * @return border color
     */
    Color getDarkBorderColor();

    /**
     * If the category should be available in the graph compartment editor
     *
     * @return if the category should be visible
     */
    boolean isVisibleInGraphCompartment();

    /**
     * If the category should be available in the compartment graph editor
     *
     * @return if the category should be visible
     */
    boolean isVisibleInCompartmentGraph();

    /**
     * Returns if the node can be extracted (copied) from the graph
     *
     * @return if the node can be extracted (copied) from the graph
     */
    default boolean canExtract() {
        return true;
    }

    /**
     * Returns if nodes of this category can be created by users
     *
     * @return if nodes of this category can be created by users
     */
    default boolean userCanCreate() {
        return true;
    }

    /**
     * Returns if nodes of this category can be deleted by users
     *
     * @return if nodes of this category can be deleted by users
     */
    default boolean userCanDelete() {
        return true;
    }

    /**
     * Returns true if nodes of this category can be run in a single run
     *
     * @return if nodes of this category can be run in a single run
     */
    default boolean isRunnable() {
        return true;
    }

}
