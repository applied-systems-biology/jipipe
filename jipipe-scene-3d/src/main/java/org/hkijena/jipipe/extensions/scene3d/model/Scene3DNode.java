package org.hkijena.jipipe.extensions.scene3d.model;

import org.hkijena.jipipe.api.JIPipeProgressInfo;
import org.hkijena.jipipe.extensions.scene3d.model.geometries.Scene3DMeshGeometry;
import org.hkijena.jipipe.extensions.scene3d.model.geometries.Scene3DUnindexedMeshGeometry;

import java.util.List;

/**
 * A node in the 3D scene
 */
public interface Scene3DNode {

    /**
     * Gets the name of this node
     * @return the name
     */
    String getName();

    /**
     * Sets the name of this node
     * @param name the name
     */
    void setName(String name);

    /**
     * Copies metadata from another node
     * @param other the other node
     */
    default void copyMetadataFrom(Scene3DNode other) {
        setName(other.getName());
    }
}
