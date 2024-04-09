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

package org.hkijena.jipipe.plugins.scene3d.model;

/**
 * A node in the 3D scene
 */
public interface Scene3DNode {

    /**
     * Gets the name of this node
     *
     * @return the name
     */
    String getName();

    /**
     * Sets the name of this node
     *
     * @param name the name
     */
    void setName(String name);

    /**
     * Copies metadata from another node
     *
     * @param other the other node
     */
    default void copyMetadataFrom(Scene3DNode other) {
        setName(other.getName());
    }

    /**
     * Duplicates the node
     *
     * @return the copy
     */
    Scene3DNode duplicate();
}
