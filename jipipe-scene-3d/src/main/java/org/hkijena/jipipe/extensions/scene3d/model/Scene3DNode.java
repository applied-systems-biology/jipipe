package org.hkijena.jipipe.extensions.scene3d.model;

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
